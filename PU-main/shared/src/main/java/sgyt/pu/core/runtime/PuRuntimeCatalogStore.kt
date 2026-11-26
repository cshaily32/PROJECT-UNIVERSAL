package sgyt.pu.core.runtime

import android.content.Context
import sgyt.pu.core.json.PuJson
import sgyt.pu.core.json.PuJsonObject
import sgyt.pu.core.json.PuJsonValue
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuRuntimeCatalog
import sgyt.pu.core.model.PuRuntimeDescriptor
import sgyt.pu.core.model.PuWindowsModuleManifest
import sgyt.pu.core.network.PuApiConfig
import sgyt.pu.core.network.PuApiPaths
import sgyt.pu.core.security.PuCrypto
import sgyt.pu.core.storage.PuStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads the bundled offline catalog and refreshes it from a signed remote
 * catalog. A remote catalog is never trusted without its detached signature.
 */
class PuRuntimeCatalogStore(
    private val context: Context,
    private val storage: PuStorage,
    private val apiConfig: PuApiConfig = PuApiConfig(),
) {
    private val catalogFile = File(storage.catalogRoot, "runtime-catalog.json")
    private val signatureFile = File(storage.catalogRoot, "runtime-catalog.sig")

    @Synchronized
    fun current(): PuRuntimeCatalog {
        loadVerifiedDownloaded()?.let { return it }
        return loadBundled()
    }

    @Synchronized
    fun refresh(remoteUrl: String, publicKeyBase64: String): PuRuntimeCatalog =
        refresh(remoteUrl, "$remoteUrl.sig", publicKeyBase64)

    @Synchronized
    fun refresh(remoteUrl: String, signatureUrl: String, publicKeyBase64: String): PuRuntimeCatalog {
        require(publicKeyBase64.isNotBlank()) { "A catalog signing key is required" }
        val uri = URL(remoteUrl)
        require(uri.protocol.equals("https", ignoreCase = true)) { "The runtime catalog must use HTTPS" }
        val bytes = readSmallResponse(uri, MAX_CATALOG_BYTES)
        val signature = readSignature(signatureUrl)
        require(PuCrypto.verifyEcdsaSha256(publicKeyBase64, bytes, signature)) {
            "The runtime catalog signature is invalid"
        }
        val catalog = parse(bytes, "remote")
        setTrustKey(publicKeyBase64)
        storage.writeText(catalogFile, String(bytes, Charsets.UTF_8))
        storage.writeText(signatureFile, signature)
        return catalog
    }

    @Synchronized
    fun refreshFromApi(): PuRuntimeCatalog {
        val remoteUrl = apiConfig.resolve(PuApiPaths.RUNTIME_CATALOG)
            ?: throw IllegalStateException("The PU runtime API is not configured")
        val signatureUrl = apiConfig.resolve(PuApiPaths.RUNTIME_CATALOG_SIGNATURE)
            ?: throw IllegalStateException("The PU runtime API is not configured")
        if (apiConfig.runtimeCatalogPublicKey.isBlank()) {
            throw IllegalStateException("The PU runtime catalog signing key is not configured")
        }
        return refresh(remoteUrl, signatureUrl, apiConfig.runtimeCatalogPublicKey)
    }

    private fun loadBundled(): PuRuntimeCatalog {
        val bytes = runCatching {
            context.assets.open(BUNDLED_CATALOG).use { it.readBytes() }
        }.getOrElse { return emptyCatalog("bundled-missing") }
        return parse(bytes, "bundled")
    }

    private fun loadVerifiedDownloaded(): PuRuntimeCatalog? {
        if (!catalogFile.isFile || !signatureFile.isFile) return null
        val bytes = runCatching { catalogFile.readBytes() }.getOrNull() ?: return null
        val signature = runCatching { signatureFile.readText(Charsets.UTF_8).trim() }.getOrNull() ?: return null
        // A downloaded catalog is only accepted if the configured trust key is
        // supplied by the host application. Without it, the bundled catalog is
        // used rather than silently trusting arbitrary network content.
        val publicKey = configuredTrustKey()
        if (publicKey.isNullOrBlank()) return null
        if (!PuCrypto.verifyEcdsaSha256(publicKey, bytes, signature)) return null
        return runCatching { parse(bytes, "verified-download") }.getOrNull()
    }

    private fun configuredTrustKey(): String? = runCatching {
        context.getSharedPreferences("pu_catalog_trust", Context.MODE_PRIVATE)
            .getString("public_key", null)
    }.getOrNull()

    fun setTrustKey(publicKeyBase64: String) {
        context.getSharedPreferences("pu_catalog_trust", Context.MODE_PRIVATE)
            .edit()
            .putString("public_key", publicKeyBase64.trim())
            .apply()
    }

    private fun readSignature(signatureUrl: String): String {
        val url = URL(signatureUrl)
        require(url.protocol.equals("https", ignoreCase = true)) { "The catalog signature must use HTTPS" }
        return readSmallResponse(url, MAX_SIGNATURE_BYTES).toString(Charsets.UTF_8).trim()
    }

    private fun readSmallResponse(url: URL, limit: Int): ByteArray {
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json, text/plain")
            if (connection.responseCode !in 200..299) {
                throw IOException("Catalog request failed with HTTP ${connection.responseCode}")
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > limit) throw IOException("Catalog response is too large")
                    output.write(buffer, 0, read)
                }
            }
            return output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(bytes: ByteArray, source: String): PuRuntimeCatalog {
        val root = PuJson.parse(String(bytes, Charsets.UTF_8)) as? PuJsonValue.JsonObject
            ?: throw IOException("The runtime catalog is not a JSON object")
        val objectValue = PuJsonObject(root.values)
        val revision = objectValue.long("revision") ?: 0
        val runtimes = objectValue.array("runtimes").mapNotNull { value ->
            (value as? PuJsonValue.JsonObject)?.let(::runtimeFromJson)
        }
        val modules = objectValue.array("windowsModules").mapNotNull { value ->
            (value as? PuJsonValue.JsonObject)?.let(::moduleFromJson)
        }
        return PuRuntimeCatalog(
            schemaVersion = objectValue.int("schemaVersion") ?: 1,
            revision = revision,
            runtimes = runtimes,
            windowsModules = modules,
            source = source,
            verifiedSha256 = PuCrypto.sha256(bytes),
        )
    }

    private fun runtimeFromJson(json: PuJsonValue.JsonObject): PuRuntimeDescriptor? {
        val objectValue = PuJsonObject(json.values)
        val id = objectValue.string("id") ?: return null
        val engine = objectValue.string("engine")?.let { value ->
            runCatching { PuEngineType.valueOf(value) }.getOrNull()
        } ?: return null
        return PuRuntimeDescriptor(
            id = id,
            engine = engine,
            versionFamily = objectValue.string("versionFamily").orEmpty(),
            supportedMinors = objectValue.array("supportedMinors").mapNotNull { value ->
                (value as? PuJsonValue.JsonNumber)?.value?.toInt()
            },
            abi = objectValue.string("abi") ?: "any",
            catalogRevision = objectValue.long("catalogRevision") ?: 0,
            packageUrl = objectValue.string("packageUrl").orEmpty(),
            compressedBytes = objectValue.long("compressedBytes") ?: 0,
            installedBytes = objectValue.long("installedBytes") ?: 0,
            sha256 = objectValue.string("sha256").orEmpty(),
            signature = objectValue.string("signature"),
            minAndroidApi = objectValue.int("minAndroidApi") ?: 24,
            requiresGles3 = objectValue.boolean("requiresGles3") ?: false,
            verifiedRelease = objectValue.boolean("verifiedRelease") ?: false,
            packageKeyId = objectValue.string("packageKeyId"),
            packagePublicKey = objectValue.string("packagePublicKey"),
        )
    }

    private fun moduleFromJson(json: PuJsonValue.JsonObject): PuWindowsModuleManifest? {
        val objectValue = PuJsonObject(json.values)
        val id = objectValue.string("id") ?: return null
        return PuWindowsModuleManifest(
            id = id,
            catalogRevision = objectValue.long("catalogRevision") ?: 0,
            packageUrl = objectValue.string("packageUrl").orEmpty(),
            compressedBytes = objectValue.long("compressedBytes") ?: 0,
            installedBytes = objectValue.long("installedBytes") ?: 0,
            sha256 = objectValue.string("sha256").orEmpty(),
            signature = objectValue.string("signature"),
            minAndroidApi = objectValue.int("minAndroidApi") ?: 24,
            abi = objectValue.string("abi") ?: "arm64-v8a",
            version = objectValue.string("version") ?: "1",
        )
    }

    private fun emptyCatalog(source: String): PuRuntimeCatalog = PuRuntimeCatalog(
        schemaVersion = 1,
        revision = 0,
        runtimes = emptyList(),
        windowsModules = emptyList(),
        source = source,
        verifiedSha256 = "",
    )

    private companion object {
        const val BUNDLED_CATALOG = "catalog/runtime-catalog.json"
        const val MAX_CATALOG_BYTES = 5 * 1024 * 1024
        const val MAX_SIGNATURE_BYTES = 16 * 1024
    }
}
