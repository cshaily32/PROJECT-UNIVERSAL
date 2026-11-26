package sgyt.pu.core.optimizer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import sgyt.pu.core.json.PuJson
import sgyt.pu.core.json.PuJsonValue
import sgyt.pu.core.model.PuAssetKind
import sgyt.pu.core.model.PuAssetStatus
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.core.model.PuOptimizationAsset
import sgyt.pu.core.model.PuOptimizationManifest
import sgyt.pu.core.model.PuOptimizationProfileSettings
import sgyt.pu.core.security.PuCrypto
import sgyt.pu.core.storage.PuStorage
import sgyt.pu.model.PuProfile
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class PuOptimizationProgress(
    val completedAssets: Int,
    val totalAssets: Int,
    val completedBytes: Long,
    val savedBytes: Long,
    val currentPath: String,
)

data class PuOptimizationResult(
    val manifest: PuOptimizationManifest,
    val originalFilesUnchanged: Boolean,
    val cacheBytes: Long,
)

class PuOptimizationCancelledException : Exception("Optimization cancelled")

class PuOriginalFilesChangedException : Exception("An original game file changed during optimization")

interface PuMediaOptimizer {
    /** Returns an optimized file, or null when the codec cannot improve it. */
    fun optimize(source: File, target: File, settings: PuOptimizationProfileSettings): File?
}

/**
 * Non-destructive, incremental optimization pipeline.
 *
 * Every generated file lives below the per-game cache. Source files are opened
 * read-only and are never moved, deleted, or rewritten.
 */
class PuOptimizer(
    private val storage: PuStorage,
    private val mediaOptimizer: PuMediaOptimizer = UnsupportedMediaOptimizer(),
    private val sourceFingerprint: (File) -> String = ::fingerprint,
) {
    fun optimize(
        game: PuGameRecord,
        profile: PuProfile,
        lite: Boolean,
        cancellation: AtomicBoolean = AtomicBoolean(false),
        onProgress: (PuOptimizationProgress) -> Unit = {},
    ): PuOptimizationResult {
        val root = File(game.rootPath)
        require(root.isDirectory) { "The imported game files are missing" }
        val settings = PuOptimizationProfileSettings.forProfile(profile, lite)
        val cache = storage.cacheDirectory(game.id).apply { mkdirs() }
        val assetsRoot = File(cache, "assets").apply { mkdirs() }
        val manifest = loadManifest(game.id, profile) ?: PuOptimizationManifest(
            gameId = game.id,
            profile = profile,
            updatedAt = System.currentTimeMillis(),
            assets = emptyList(),
        )
        val previousAssets = manifest.assets.associateBy { it.relativePath }
        val sources = scanAssets(root)
        val sourceFingerprints = sources.associate { source ->
            source.relativeTo(root).invariantPath() to sourceFingerprint(source)
        }
        val completed = mutableListOf<PuOptimizationAsset>()
        var completedBytes = 0L
        var savedBytes = 0L

        sources.forEachIndexed { index, source ->
            if (cancellation.get()) throw PuOptimizationCancelledException()
            val relativePath = source.relativeTo(root).invariantPath()
            val fingerprint = sourceFingerprints.getValue(relativePath)
            val previous = previousAssets[relativePath]
            val target = File(assetsRoot, relativePath)
            val reusable = previous != null && previous.sourceFingerprint == fingerprint && previous.sourceSizeBytes == source.length()
            val asset = if (reusable && previous.outputPath != null && File(previous.outputPath).isFile) {
                previous
            } else {
                optimizeOne(source, target, relativePath, fingerprint, settings)
            }
            completed += asset
            completedBytes += asset.sourceSizeBytes
            savedBytes += (asset.sourceSizeBytes - asset.outputSizeBytes).coerceAtLeast(0)
            onProgress(
                PuOptimizationProgress(
                    completedAssets = index + 1,
                    totalAssets = sources.size,
                    completedBytes = completedBytes,
                    savedBytes = savedBytes,
                    currentPath = relativePath,
                ),
            )
            saveManifest(
                manifest.copy(
                    profile = profile,
                    updatedAt = System.currentTimeMillis(),
                    assets = completed.toList(),
                ),
            )
        }

        val originalFilesUnchanged = sources.all { source ->
            val relativePath = source.relativeTo(root).invariantPath()
            sourceFingerprint(source) == sourceFingerprints[relativePath]
        }
        if (!originalFilesUnchanged) throw PuOriginalFilesChangedException()

        val finalManifest = manifest.copy(
            profile = profile,
            updatedAt = System.currentTimeMillis(),
            assets = completed,
        )
        saveManifest(finalManifest)
        removeOrphanedOutputs(assetsRoot, completed.mapNotNull { it.outputPath })
        return PuOptimizationResult(
            manifest = finalManifest,
            originalFilesUnchanged = originalFilesUnchanged,
            cacheBytes = storage.directorySize(cache),
        )
    }

    fun clear(gameId: String): Long {
        storage.deleteRecursively(storage.cacheDirectory(gameId))
        storage.cacheDirectory(gameId).mkdirs()
        return 0L
    }

    fun manifest(gameId: String): PuOptimizationManifest? = loadManifest(gameId, null)

    fun cacheSize(gameId: String): Long = storage.directorySize(storage.cacheDirectory(gameId))

    /** Resolves the overlay cache first and then falls back to the original. */
    fun resolveAsset(gameId: String, relativePath: String): File {
        val safeRelative = relativePath.replace('\\', '/').removePrefix("/")
        val original = File(File(storage.gameDirectory(gameId), safeRelative).canonicalPath)
        val gameRoot = storage.gameDirectory(gameId).canonicalFile
        require(original == gameRoot || original.path.startsWith(gameRoot.path + File.separator)) {
            "Asset path escaped the game directory"
        }
        val cached = loadManifest(gameId, null)
            ?.assets
            ?.firstOrNull { it.relativePath == safeRelative }
            ?.outputPath
            ?.let(::File)
            ?.takeIf { it.isFile }
            ?: File(storage.cacheDirectory(gameId), "assets/$safeRelative").takeIf { it.isFile }
        return cached ?: original
    }

    private fun optimizeOne(
        source: File,
        target: File,
        relativePath: String,
        fingerprint: String,
        settings: PuOptimizationProfileSettings,
    ): PuOptimizationAsset {
        val kind = classify(source)
        if (kind == PuAssetKind.IMAGE) {
            return optimizeImage(source, target, relativePath, fingerprint, settings)
        }
        if (kind == PuAssetKind.VIDEO || kind == PuAssetKind.AUDIO) {
            target.parentFile?.mkdirs()
            val optimized = runCatching {
                mediaOptimizer.optimize(source, target, settings)
            }.getOrNull()
            if (optimized != null && optimized.isFile && optimized.length() < source.length()) {
                return PuOptimizationAsset(
                    relativePath = relativePath,
                    kind = kind,
                    sourceSizeBytes = source.length(),
                    outputPath = optimized.absolutePath,
                    outputSizeBytes = optimized.length(),
                    sourceFingerprint = fingerprint,
                    status = PuAssetStatus.OPTIMIZED,
                )
            }
            optimized?.delete()
            return PuOptimizationAsset(
                relativePath = relativePath,
                kind = kind,
                sourceSizeBytes = source.length(),
                outputPath = null,
                outputSizeBytes = source.length(),
                sourceFingerprint = fingerprint,
                status = PuAssetStatus.KEPT_ORIGINAL,
            )
        }
        return PuOptimizationAsset(
            relativePath = relativePath,
            kind = kind,
            sourceSizeBytes = source.length(),
            outputPath = null,
            outputSizeBytes = source.length(),
            sourceFingerprint = fingerprint,
            status = PuAssetStatus.KEPT_ORIGINAL,
        )
    }

    private fun optimizeImage(
        source: File,
        target: File,
        relativePath: String,
        fingerprint: String,
        settings: PuOptimizationProfileSettings,
    ): PuOptimizationAsset {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return keptAsset(source, relativePath, fingerprint, PuAssetKind.IMAGE)
        }
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (largest / sampleSize > settings.maxImageDimension * 2) sampleSize *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = runCatching { BitmapFactory.decodeFile(source.absolutePath, options) }.getOrNull()
            ?: return keptAsset(source, relativePath, fingerprint, PuAssetKind.IMAGE)
        val scale = minOf(1f, settings.maxImageDimension.toFloat() / maxOf(decoded.width, decoded.height))
        val width = (decoded.width * scale).toInt().coerceAtLeast(1)
        val height = (decoded.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(decoded, width, height, true) else decoded
        val output = if (target.extension.equals("webp", true)) target else File(target.parentFile, "${target.name}.webp")
        output.parentFile?.mkdirs()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        val compressed = runCatching {
            output.outputStream().use { stream -> scaled.compress(format, settings.webpQuality, stream) }
        }.getOrDefault(false)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        if (!compressed || !output.isFile || output.length() >= source.length()) {
            output.delete()
            return keptAsset(source, relativePath, fingerprint, PuAssetKind.IMAGE)
        }
        return PuOptimizationAsset(
            relativePath = relativePath,
            kind = PuAssetKind.IMAGE,
            sourceSizeBytes = source.length(),
            outputPath = output.absolutePath,
            outputSizeBytes = output.length(),
            sourceFingerprint = fingerprint,
            originalWidth = bounds.outWidth,
            originalHeight = bounds.outHeight,
            outputWidth = width,
            outputHeight = height,
            status = PuAssetStatus.OPTIMIZED,
        )
    }

    private fun keptAsset(
        source: File,
        relativePath: String,
        fingerprint: String,
        kind: PuAssetKind,
    ): PuOptimizationAsset = PuOptimizationAsset(
        relativePath = relativePath,
        kind = kind,
        sourceSizeBytes = source.length(),
        outputPath = null,
        outputSizeBytes = source.length(),
        sourceFingerprint = fingerprint,
        status = PuAssetStatus.KEPT_ORIGINAL,
    )

    private fun scanAssets(root: File): List<File> {
        val output = mutableListOf<File>()
        val pending = ArrayDeque<File>()
        pending.addLast(root)
        while (pending.isNotEmpty()) {
            val directory = pending.removeFirst()
            directory.listFiles()?.forEach { child ->
                if (child.isDirectory) pending.addLast(child)
                else if (child.isFile && classify(child) != PuAssetKind.OTHER) output += child
            }
        }
        return output.sortedBy { it.absolutePath }
    }

    private fun classify(file: File): PuAssetKind = when (file.extension.lowercase(Locale.ROOT)) {
        "png", "jpg", "jpeg", "webp", "bmp", "gif" -> PuAssetKind.IMAGE
        "mp4", "webm", "mkv", "avi", "mov", "m4v", "ogv" -> PuAssetKind.VIDEO
        "ogg", "oga", "opus", "wav", "mp3", "m4a", "aac", "flac" -> PuAssetKind.AUDIO
        else -> PuAssetKind.OTHER
    }

    private fun loadManifest(gameId: String, profile: PuProfile?): PuOptimizationManifest? {
        val file = File(storage.cacheDirectory(gameId), "manifest.json")
        val root = storage.readJson(file) as? PuJsonValue.JsonObject ?: return null
        val objectValue = sgyt.pu.core.json.PuJsonObject(root.values)
        val storedProfile = objectValue.string("profile")?.let { value ->
            runCatching { PuProfile.valueOf(value) }.getOrNull()
        } ?: return null
        if (profile != null && storedProfile != profile) return null
        val assets = objectValue.array("assets").mapNotNull { value ->
            val asset = value as? PuJsonValue.JsonObject ?: return@mapNotNull null
            val assetObject = sgyt.pu.core.json.PuJsonObject(asset.values)
            val relativePath = assetObject.string("relativePath") ?: return@mapNotNull null
            PuOptimizationAsset(
                relativePath = relativePath,
                kind = assetObject.string("kind")?.let { kind ->
                    runCatching { PuAssetKind.valueOf(kind) }.getOrDefault(PuAssetKind.OTHER)
                } ?: PuAssetKind.OTHER,
                sourceSizeBytes = assetObject.long("sourceSizeBytes") ?: 0,
                outputPath = assetObject.string("outputPath"),
                outputSizeBytes = assetObject.long("outputSizeBytes") ?: 0,
                sourceFingerprint = assetObject.string("sourceFingerprint").orEmpty(),
                originalWidth = assetObject.int("originalWidth"),
                originalHeight = assetObject.int("originalHeight"),
                outputWidth = assetObject.int("outputWidth"),
                outputHeight = assetObject.int("outputHeight"),
                status = assetObject.string("status")?.let { status ->
                    runCatching { PuAssetStatus.valueOf(status) }.getOrDefault(PuAssetStatus.PENDING)
                } ?: PuAssetStatus.PENDING,
            )
        }
        return PuOptimizationManifest(
            gameId = objectValue.string("gameId") ?: gameId,
            profile = storedProfile,
            schemaVersion = objectValue.int("schemaVersion") ?: 1,
            updatedAt = objectValue.long("updatedAt") ?: 0,
            assets = assets,
        )
    }

    private fun saveManifest(manifest: PuOptimizationManifest) {
        val file = File(storage.cacheDirectory(gameId = manifest.gameId), "manifest.json")
        storage.writeJson(
            file,
            PuJsonValue.JsonObject(
                mapOf(
                    "schemaVersion" to PuJson.number(manifest.schemaVersion),
                    "gameId" to PuJson.string(manifest.gameId),
                    "profile" to PuJson.string(manifest.profile.name),
                    "updatedAt" to PuJson.number(manifest.updatedAt),
                    "assets" to PuJson.arr(manifest.assets.map(::assetToJson)),
                ),
            ),
        )
    }

    private fun assetToJson(asset: PuOptimizationAsset): PuJsonValue = PuJson.obj(
        "relativePath" to PuJson.string(asset.relativePath),
        "kind" to PuJson.string(asset.kind.name),
        "sourceSizeBytes" to PuJson.number(asset.sourceSizeBytes),
        "outputPath" to PuJson.nullableString(asset.outputPath),
        "outputSizeBytes" to PuJson.number(asset.outputSizeBytes),
        "sourceFingerprint" to PuJson.string(asset.sourceFingerprint),
        "originalWidth" to PuJson.nullableNumber(asset.originalWidth?.toLong()),
        "originalHeight" to PuJson.nullableNumber(asset.originalHeight?.toLong()),
        "outputWidth" to PuJson.nullableNumber(asset.outputWidth?.toLong()),
        "outputHeight" to PuJson.nullableNumber(asset.outputHeight?.toLong()),
        "status" to PuJson.string(asset.status.name),
    )

    private fun removeOrphanedOutputs(assetsRoot: File, retainedPaths: List<String>) {
        val retained = retainedPaths.map { File(it).canonicalPath }.toSet()
        fun removeUnretained(directory: File) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    removeUnretained(file)
                    if (file.listFiles().isNullOrEmpty()) file.delete()
                } else if (file.canonicalPath !in retained) {
                    file.delete()
                }
            }
        }
        removeUnretained(assetsRoot)
    }

    internal fun fingerprint(file: File): String {
        return if (file.length() <= 256L * 1024 * 1024) {
            "sha256:${PuCrypto.sha256(file)}"
        } else {
            "meta:${file.length()}:${file.lastModified()}"
        }
    }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')
}

private class UnsupportedMediaOptimizer : PuMediaOptimizer {
    override fun optimize(
        source: File,
        target: File,
        settings: PuOptimizationProfileSettings,
    ): File? = null
}
