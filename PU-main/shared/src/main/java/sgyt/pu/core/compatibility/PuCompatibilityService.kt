package sgyt.pu.core.compatibility

import android.content.Context
import sgyt.pu.core.json.PuJson
import sgyt.pu.core.json.PuJsonObject
import sgyt.pu.core.json.PuJsonValue
import sgyt.pu.core.library.PuGameRepository
import sgyt.pu.core.model.DEFAULT_BUFFER_SIZE
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuFixProfile
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.core.network.PuApiConfig
import sgyt.pu.core.network.PuApiPaths
import sgyt.pu.core.security.PuCrypto
import sgyt.pu.core.storage.PuStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

interface PuCompatibilityCatalog {
    fun listProfiles(): List<PuFixProfile>
    fun findMatches(game: PuGameRecord): List<PuFixProfile>
    fun apply(game: PuGameRecord, profile: PuFixProfile): Boolean
    fun refresh(remoteUrl: String, publicKeyBase64: String): List<PuFixProfile>
    fun refreshFromApi(): List<PuFixProfile>
}

internal fun PuFixProfile.matches(game: PuGameRecord): Boolean {
    val family = game.version.substringBefore('.').ifBlank { null }
    val engineMatches = engine == game.engine || engine == PuEngineType.UNKNOWN
    val fingerprintMatches = fingerprint == null || fingerprint == game.fingerprint
    val versionMatches = versionFamily == null ||
        versionFamily.equals(family, ignoreCase = true) ||
        game.version.startsWith(versionFamily, ignoreCase = true)
    return engineMatches && fingerprintMatches && versionMatches
}

/**
 * Signed compatibility profiles with an offline bundled fallback. Applying a
 * profile writes an engine configuration overlay in the sandbox and never
 * changes an original game file.
 */
class PuCompatibilityService(
    private val context: Context,
    private val storage: PuStorage,
    private val gameRepository: PuGameRepository,
    private val apiConfig: PuApiConfig = PuApiConfig(),
) : PuCompatibilityCatalog {
    private val localFile = File(storage.fixesRoot, "compatibility-fixes.json")
    private val localSignature = File(storage.fixesRoot, "compatibility-fixes.sig")

    @Synchronized
    override fun listProfiles(): List<PuFixProfile> {
        loadVerifiedDownloaded()?.let { return it }
        return loadBundled()
    }

    @Synchronized
    override fun findMatches(game: PuGameRecord): List<PuFixProfile> {
        return listProfiles().asSequence()
            .filter { profile -> profile.matches(game) }
            .sortedByDescending { profile ->
                if (profile.fingerprint == game.fingerprint) 2 else if (profile.versionFamily != null) 1 else 0
            }
            .toList()
    }

    @Synchronized
    override fun apply(game: PuGameRecord, profile: PuFixProfile): Boolean {
        val configDirectory = File(storage.sandboxDirectory(game.id), "config").apply { mkdirs() }
        val overlay = PuJson.obj(
            "schemaVersion" to PuJson.number(1),
            "fixId" to PuJson.string(profile.id),
            "revision" to PuJson.number(profile.revision),
            "engine" to PuJson.string(profile.engine.name),
            "settings" to PuJsonValue.JsonObject(profile.settings.mapValues { (_, value) -> PuJson.string(value) }),
        )
        storage.writeJson(storage.safeChild(configDirectory, "compatibility-${profile.id}.json"), overlay)
        return gameRepository.applyFix(game.id, profile.id) != null
    }

    @Synchronized
    override fun refresh(remoteUrl: String, publicKeyBase64: String): List<PuFixProfile> =
        refresh(remoteUrl, "$remoteUrl.sig", publicKeyBase64)

    @Synchronized
    fun refresh(remoteUrl: String, signatureUrl: String, publicKeyBase64: String): List<PuFixProfile> {
        require(publicKeyBase64.isNotBlank()) { "A compatibility signing key is required" }
        val bytes = readResponse(URL(remoteUrl), MAX_DATABASE_BYTES)
        val signature = readResponse(URL(signatureUrl), MAX_SIGNATURE_BYTES).toString(Charsets.UTF_8).trim()
        require(PuCrypto.verifyEcdsaSha256(publicKeyBase64, bytes, signature)) {
            "The compatibility database signature is invalid"
        }
        val profiles = parse(bytes)
        setTrustKey(publicKeyBase64)
        storage.writeText(localFile, String(bytes, Charsets.UTF_8))
        storage.writeText(localSignature, signature)
        return profiles
    }

    @Synchronized
    override fun refreshFromApi(): List<PuFixProfile> {
        val remoteUrl = apiConfig.resolve(PuApiPaths.COMPATIBILITY_FIXES) ?: return listProfiles()
        val signatureUrl = apiConfig.resolve(PuApiPaths.COMPATIBILITY_FIXES_SIGNATURE) ?: return listProfiles()
        if (apiConfig.compatibilityPublicKey.isBlank()) return listProfiles()
        return runCatching {
            refresh(remoteUrl, signatureUrl, apiConfig.compatibilityPublicKey)
        }.getOrElse {
            listProfiles()
        }
    }

    private fun loadBundled(): List<PuFixProfile> {
        val bytes = runCatching {
            context.assets.open(BUNDLED_FIXES).use { it.readBytes() }
        }.getOrNull() ?: return emptyList()
        return runCatching { parse(bytes) }.getOrDefault(emptyList())
    }

    private fun loadVerifiedDownloaded(): List<PuFixProfile>? {
        if (!localFile.isFile || !localSignature.isFile) return null
        val bytes = runCatching { localFile.readBytes() }.getOrNull() ?: return null
        val signature = runCatching { localSignature.readText(Charsets.UTF_8).trim() }.getOrNull() ?: return null
        val publicKey = configuredTrustKey()
        if (publicKey.isNullOrBlank()) return null
        if (!PuCrypto.verifyEcdsaSha256(publicKey, bytes, signature)) return null
        return runCatching { parse(bytes) }.getOrNull()
    }

    fun setTrustKey(publicKeyBase64: String) {
        storage.writeText(File(storage.stateRoot, "compatibility-trust-key.txt"), publicKeyBase64.trim())
    }

    private fun configuredTrustKey(): String? = File(storage.stateRoot, "compatibility-trust-key.txt")
        .takeIf { it.isFile }
        ?.readText(Charsets.UTF_8)
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private fun parse(bytes: ByteArray): List<PuFixProfile> {
        val root = PuJson.parse(String(bytes, Charsets.UTF_8)) as? PuJsonValue.JsonObject ?: return emptyList()
        val objectValue = PuJsonObject(root.values)
        return objectValue.array("profiles").mapNotNull { value ->
            val profile = value as? PuJsonValue.JsonObject ?: return@mapNotNull null
            val profileObject = PuJsonObject(profile.values)
            val id = profileObject.string("id") ?: return@mapNotNull null
            val engine = profileObject.string("engine")?.let { engineName ->
                runCatching { PuEngineType.valueOf(engineName) }.getOrDefault(PuEngineType.UNKNOWN)
            } ?: PuEngineType.UNKNOWN
            val settingsObject = profileObject.jsonObject("settings")
            val settings = settingsObject?.values.orEmpty().mapNotNull { (key, settingValue) ->
                (settingValue as? PuJsonValue.JsonString)?.value?.let { key to it }
            }.toMap()
            PuFixProfile(
                id = id,
                title = profileObject.string("title") ?: id,
                engine = engine,
                versionFamily = profileObject.string("versionFamily"),
                fingerprint = profileObject.string("fingerprint"),
                settings = settings,
                revision = profileObject.long("revision") ?: 0,
                signature = profileObject.string("signature"),
            )
        }
    }

    private fun readResponse(url: URL, limit: Int): ByteArray {
        require(url.protocol.equals("https", ignoreCase = true)) { "Compatibility data must use HTTPS" }
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            if (connection.responseCode !in 200..299) {
                throw IOException("Compatibility request failed with HTTP ${connection.responseCode}")
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > limit) throw IOException("Compatibility response is too large")
                    output.write(buffer, 0, read)
                }
            }
            return output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val BUNDLED_FIXES = "catalog/compatibility-fixes.json"
        private const val MAX_DATABASE_BYTES = 5 * 1024 * 1024
        private const val MAX_SIGNATURE_BYTES = 16 * 1024
    }
}
