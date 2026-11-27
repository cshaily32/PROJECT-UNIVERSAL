package sgyt.pu.core.runtime

import sgyt.pu.core.archive.PuArchiveExtractor
import sgyt.pu.core.json.PuJson
import sgyt.pu.core.json.PuJsonObject
import sgyt.pu.core.json.PuJsonValue
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuInstalledRuntime
import sgyt.pu.core.model.PuRuntimeCatalog
import sgyt.pu.core.model.PuRuntimeDescriptor
import sgyt.pu.core.model.PuRuntimeMatch
import sgyt.pu.core.storage.PuStorage
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Resolves, installs, reuses, and removes version-matched engine runtimes. */
class PuRuntimeManager(
    private val storage: PuStorage,
    private val catalogStore: PuRuntimeCatalogStore,
    private val downloader: PuDownloader,
    private val archives: PuArchiveExtractor = PuArchiveExtractor(),
) : PuRuntimeLibrary {
    private val installationsFile = File(storage.stateRoot, "runtime-installations.json")

    @Synchronized
    override fun listCatalog(): PuRuntimeCatalog = catalogStore.current()

    @Synchronized
    override fun refreshCatalog(): PuRuntimeCatalog = runCatching {
        catalogStore.refreshFromApi()
    }.getOrElse {
        listCatalog()
    }

    @Synchronized
    override fun getRuntime(runtimeId: String, revision: Long?): PuRuntimeDescriptor? =
        listCatalog().runtimes.firstOrNull { descriptor ->
            descriptor.id == runtimeId && (revision == null || descriptor.catalogRevision == revision)
        }

    @Synchronized
    override fun listInstalled(): List<PuInstalledRuntime> {
        val root = storage.readJson(installationsFile) as? PuJsonValue.JsonObject ?: return emptyList()
        return PuInstalledRuntimeCodec.decodeAll(root)
    }

    @Synchronized
    override fun isInstalled(runtime: PuRuntimeDescriptor): Boolean = listInstalled().any {
        it.runtimeId == runtime.id && it.revision == runtime.catalogRevision && File(it.installPath).isDirectory
    }

    @Synchronized
    fun match(
        engine: PuEngineType,
        version: String,
        supportedAbis: List<String>,
    ): PuRuntimeMatch? {
        val parsed = parseVersion(version)
        val candidates = catalogStore.current().runtimes.asSequence()
            .filter { descriptor -> descriptor.engine == engine }
            .filter { descriptor ->
                descriptor.abi == "any" || supportedAbis.any { it.equals(descriptor.abi, ignoreCase = true) }
            }
            .filter { descriptor -> familyMatches(engine, descriptor, parsed) }
            .toList()
        if (candidates.isEmpty()) return null

        val exact = candidates.filter { descriptor ->
            parsed.minor == null || descriptor.supportedMinors.isEmpty() || parsed.minor in descriptor.supportedMinors
        }
        val pool = exact.ifEmpty { candidates }
        val comparator = compareBy(
            { descriptor: PuRuntimeDescriptor ->
                descriptor.supportedMinors.minOfOrNull { minor -> kotlin.math.abs(minor - (parsed.minor ?: minor)) }
                    ?: 0
            },
            { descriptor: PuRuntimeDescriptor -> descriptor.catalogRevision },
        )
        val selected = pool.sortedWith(comparator).firstOrNull() ?: return null
        val exactMinor = parsed.minor != null && selected.supportedMinors.contains(parsed.minor)
        val warning = if (exactMinor) {
            null
        } else {
            "No exact runtime is published for $version. PU selected the nearest compatible release in the ${selected.versionFamily} family."
        }
        return PuRuntimeMatch(selected, exactMinor, warning)
    }

    override fun install(
        descriptor: PuRuntimeDescriptor,
        cancellation: AtomicBoolean,
        onProgress: (PuDownloadProgress) -> Unit,
    ): PuInstalledRuntime {
        require(descriptor.packageUrl.isNotBlank()) { "The runtime package URL is missing" }
        require(descriptor.sha256.length == 64) { "The runtime package hash is invalid" }
        require(!descriptor.signature.isNullOrBlank()) { "The runtime package signature is missing" }
        require(!descriptor.packagePublicKey.isNullOrBlank()) { "The runtime package signing key is missing" }
        val packageFile = File(storage.tempRoot, "${descriptor.id}-${descriptor.catalogRevision}.pkg")
        try {
            downloader.download(
                url = descriptor.packageUrl,
                target = packageFile,
                expectedBytes = descriptor.compressedBytes,
                expectedSha256 = descriptor.sha256,
                publicKeyBase64 = descriptor.packagePublicKey,
                signatureBase64 = descriptor.signature,
                cancellation = cancellation,
                onProgress = onProgress,
            )

            val staging = File(storage.tempRoot, "runtime-${descriptor.id}-${descriptor.catalogRevision}-${System.nanoTime()}")
            staging.mkdirs()
            try {
                archives.extract(packageFile, staging)
                if (cancellation.get()) throw IOException("Download cancelled")
                val installed = synchronized(this) {
                    val finalParent = storage.runtimeDirectory(descriptor.id)
                    finalParent.mkdirs()
                    val finalPath = File(finalParent, descriptor.catalogRevision.toString())
                    val previous = File(finalParent, "${descriptor.catalogRevision}.previous")
                    storage.deleteRecursively(previous)
                    if (finalPath.exists() && !finalPath.renameTo(previous)) {
                        throw IllegalStateException("Could not stage the runtime update safely")
                    }
                    if (!staging.renameTo(finalPath)) {
                        previous.renameTo(finalPath)
                        throw IllegalStateException("Could not activate the downloaded runtime")
                    }
                    storage.deleteRecursively(previous)

                    PuInstalledRuntime(
                        runtimeId = descriptor.id,
                        revision = descriptor.catalogRevision,
                        installPath = finalPath.absolutePath,
                        installedBytes = storage.directorySize(finalPath),
                        installedAt = System.currentTimeMillis(),
                        lastUsedAt = System.currentTimeMillis(),
                        verifiedSha256 = descriptor.sha256,
                    ).also(::saveInstalled)
                }
                return installed
            } finally {
                storage.deleteRecursively(staging)
            }
        } finally {
            packageFile.delete()
        }
    }

    @Synchronized
    fun markUsed(runtimeId: String, revision: Long) {
        val installations = listInstalled().toMutableList()
        val index = installations.indexOfFirst { it.runtimeId == runtimeId && it.revision == revision }
        if (index < 0) return
        installations[index] = installations[index].copy(lastUsedAt = System.currentTimeMillis())
        saveInstallations(installations)
    }

    @Synchronized
    override fun remove(runtimeId: String, revision: Long?): Boolean {
        val installations = listInstalled().toMutableList()
        val removed = installations.filter { installation ->
            installation.runtimeId == runtimeId && (revision == null || installation.revision == revision)
        }
        if (removed.isEmpty()) return false
        removed.forEach { storage.deleteRecursively(File(it.installPath)) }
        saveInstallations(installations.filterNot { installation ->
            installation.runtimeId == runtimeId && (revision == null || installation.revision == revision)
        })
        return true
    }

    @Synchronized
    fun removeUnused(usedRuntimeIds: Set<String>): List<PuInstalledRuntime> {
        val unused = listInstalled().filter { it.runtimeId !in usedRuntimeIds }
        unused.forEach { remove(it.runtimeId, it.revision) }
        return unused
    }

    private fun familyMatches(
        engine: PuEngineType,
        descriptor: PuRuntimeDescriptor,
        version: PuRuntimeVersionMatcher.ParsedVersion,
    ): Boolean = PuRuntimeVersionMatcher.matches(engine, descriptor.versionFamily, version)

    private fun parseVersion(raw: String): PuRuntimeVersionMatcher.ParsedVersion =
        PuRuntimeVersionMatcher.parse(raw)

    private fun saveInstalled(installation: PuInstalledRuntime) {
        val installations = listInstalled().filterNot {
            it.runtimeId == installation.runtimeId && it.revision == installation.revision
        } + installation
        saveInstallations(installations)
    }

    private fun saveInstallations(installations: List<PuInstalledRuntime>) {
        storage.writeJson(installationsFile, PuInstalledRuntimeCodec.encodeAll(installations))
    }
}

/**
 * Converts installed-runtime records to and from their persisted JSON form.
 *
 * `Map.mapNotNull` hands the lambda a `Map.Entry`, not the value, so every map
 * read here goes through `entry.value` explicitly. Reading the entry itself as
 * a JSON object always fails, which silently empties the installed list.
 */
internal object PuInstalledRuntimeCodec {
    fun encode(installation: PuInstalledRuntime): PuJsonValue.JsonObject = PuJson.obj(
        "runtimeId" to PuJson.string(installation.runtimeId),
        "revision" to PuJson.number(installation.revision),
        "installPath" to PuJson.string(installation.installPath),
        "installedBytes" to PuJson.number(installation.installedBytes),
        "installedAt" to PuJson.number(installation.installedAt),
        "lastUsedAt" to PuJson.number(installation.lastUsedAt),
        "verifiedSha256" to PuJson.string(installation.verifiedSha256),
    )

    fun encodeAll(installations: List<PuInstalledRuntime>): PuJsonValue.JsonObject =
        PuJsonValue.JsonObject(
            installations.associateBy { "${it.runtimeId}:${it.revision}" }
                .mapValues { entry -> encode(entry.value) },
        )

    fun decode(json: PuJsonValue.JsonObject): PuInstalledRuntime? {
        val objectValue = PuJsonObject(json.values)
        val runtimeId = objectValue.string("runtimeId") ?: return null
        val revision = objectValue.long("revision") ?: return null
        val installPath = objectValue.string("installPath") ?: return null
        return PuInstalledRuntime(
            runtimeId = runtimeId,
            revision = revision,
            installPath = installPath,
            installedBytes = objectValue.long("installedBytes") ?: 0,
            installedAt = objectValue.long("installedAt") ?: 0,
            lastUsedAt = objectValue.long("lastUsedAt") ?: 0,
            verifiedSha256 = objectValue.string("verifiedSha256").orEmpty(),
        )
    }

    fun decodeAll(root: PuJsonValue.JsonObject): List<PuInstalledRuntime> =
        root.values.entries.mapNotNull { entry ->
            decode(entry.value as? PuJsonValue.JsonObject ?: return@mapNotNull null)
        }
}
