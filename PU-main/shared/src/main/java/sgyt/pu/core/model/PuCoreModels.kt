package sgyt.pu.core.model

import sgyt.pu.model.PuEdition
import sgyt.pu.model.PuProfile

enum class PuEngineType {
    RENPY,
    RPG_MAKER_MV,
    RPG_MAKER_MZ,
    RPG_MAKER_XP,
    RPG_MAKER_VX,
    RPG_MAKER_ACE,
    RPG_MAKER_2000,
    RPG_MAKER_2003,
    WOLF_RPG,
    KIRIKIRI,
    UNITY,
    GENERIC_EXE,
    UNKNOWN,
}

data class PuVersion(
    val raw: String,
    val family: String? = null,
    val minor: Int? = null,
    val patch: Int? = null,
) {
    val isKnown: Boolean get() = family != null
}

data class PuDetectionResult(
    val engine: PuEngineType,
    val version: PuVersion,
    val title: String,
    val confidence: Float,
    val evidence: List<String>,
    val fingerprint: String,
    val supported: Boolean,
) {
    val summary: String
        get() = when {
            engine == PuEngineType.UNKNOWN -> "Unknown game"
            version.raw.isBlank() -> engine.displayName
            else -> "${engine.displayName} ${version.raw}"
        }
}

data class PuGameRecord(
    val id: String,
    val rootPath: String,
    val title: String,
    val engine: PuEngineType,
    val version: String,
    val fingerprint: String,
    val importedAt: Long,
    val updatedAt: Long,
    val originalSizeBytes: Long,
    val cacheSizeBytes: Long = 0,
    val selectedProfile: PuProfile? = null,
    val compatibility: PuCompatibilityEstimate = PuCompatibilityEstimate.LIKELY_SMOOTH,
    val appliedFixIds: List<String> = emptyList(),
    val sourceUri: String? = null,
) {
    val isSupported: Boolean get() = engine != PuEngineType.UNKNOWN
}

enum class PuDeviceTier {
    NATIVE,
    WINE_LITE,
    WINE_FULL,
}

data class PuDeviceSnapshot(
    val capturedAt: Long,
    val apiLevel: Int,
    val supportedAbis: List<String>,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val appMemoryClassMb: Int,
    val freeStorageBytes: Long,
    val totalStorageBytes: Long,
    val glEsMajor: Int,
    val glEsMinor: Int,
    val webViewVersionName: String?,
    val webViewVersionCode: Long,
    val isCharging: Boolean,
    val batteryPercent: Int,
    val thermalStatus: Int?,
) {
    val supportsGles3: Boolean get() = glEsMajor >= 3
    val isArm64: Boolean get() = supportedAbis.any { it.equals("arm64-v8a", ignoreCase = true) }
    val is32BitArm: Boolean get() = supportedAbis.any { it.equals("armeabi-v7a", ignoreCase = true) }

    val tier: PuDeviceTier
        get() = when {
            !isArm64 || totalRamBytes < 2_500L * 1024 * 1024 -> PuDeviceTier.NATIVE
            totalRamBytes < 4L * 1024 * 1024 * 1024 -> PuDeviceTier.WINE_LITE
            else -> PuDeviceTier.WINE_FULL
        }
}

data class PuEditionRecommendation(
    val edition: PuEdition,
    val reason: String,
    val profile: PuProfile,
)

data class PuRuntimeDescriptor(
    val id: String,
    val engine: PuEngineType,
    val versionFamily: String,
    val supportedMinors: List<Int>,
    val abi: String,
    val catalogRevision: Long,
    val packageUrl: String,
    val compressedBytes: Long,
    val installedBytes: Long,
    val sha256: String,
    val signature: String?,
    val minAndroidApi: Int,
    val requiresGles3: Boolean,
    val verifiedRelease: Boolean,
    val packageKeyId: String? = null,
    val packagePublicKey: String? = null,
)

data class PuRuntimeMatch(
    val runtime: PuRuntimeDescriptor,
    val exactMinor: Boolean,
    val warning: String? = null,
)

enum class PuRuntimeInstallationState {
    NOT_INSTALLED,
    DOWNLOADING,
    INSTALLED,
    CANCELLED,
    FAILED,
}

data class PuInstalledRuntime(
    val runtimeId: String,
    val revision: Long,
    val installPath: String,
    val installedBytes: Long,
    val installedAt: Long,
    val lastUsedAt: Long,
    val verifiedSha256: String,
)

data class PuRuntimeCatalog(
    val schemaVersion: Int,
    val revision: Long,
    val runtimes: List<PuRuntimeDescriptor>,
    val windowsModules: List<PuWindowsModuleManifest>,
    val source: String,
    val verifiedSha256: String,
)

enum class PuCompatibilityEstimate {
    LIKELY_SMOOTH,
    MIGHT_LAG,
    PROBABLY_WONT_RUN,
}

data class PuCapabilityCheck(
    val id: String,
    val label: String,
    val passed: Boolean,
    val detail: String,
    val blocking: Boolean,
)

data class PuRuntimeCapability(
    val checks: List<PuCapabilityCheck>,
    val blockers: List<String>,
) {
    val supported: Boolean get() = blockers.isEmpty()
}

/** Applies the same runtime requirements before download and before launch. */
fun PuDeviceSnapshot.assessRuntime(runtime: PuRuntimeDescriptor): PuRuntimeCapability {
    val androidPassed = apiLevel >= runtime.minAndroidApi
    val processorPassed = runtime.abi.equals("any", ignoreCase = true) ||
        supportedAbis.any { it.equals(runtime.abi, ignoreCase = true) }
    val graphicsPassed = !runtime.requiresGles3 || supportsGles3
    val checks = listOf(
        PuCapabilityCheck(
            id = "android-version",
            label = "Android version",
            passed = androidPassed,
            detail = "Device API $apiLevel; runtime requires API ${runtime.minAndroidApi}",
            blocking = true,
        ),
        PuCapabilityCheck(
            id = "processor",
            label = "Processor",
            passed = processorPassed,
            detail = "Runtime ABI ${runtime.abi}; device ${supportedAbis.joinToString()}",
            blocking = true,
        ),
        PuCapabilityCheck(
            id = "graphics",
            label = "OpenGL ES",
            passed = graphicsPassed,
            detail = if (runtime.requiresGles3) {
                "Runtime requires OpenGL ES 3.0; device reports $glEsMajor.$glEsMinor"
            } else {
                "Device reports OpenGL ES $glEsMajor.$glEsMinor"
            },
            blocking = true,
        ),
    )
    val blockers = buildList {
        if (!androidPassed) add("This runtime requires Android API ${runtime.minAndroidApi} or newer.")
        if (!processorPassed) add("This runtime does not support the processor architecture of this phone.")
        if (!graphicsPassed) add("This runtime requires OpenGL ES 3.0, which this phone does not provide.")
    }
    return PuRuntimeCapability(checks = checks, blockers = blockers)
}

data class PuLaunchAssessment(
    val estimate: PuCompatibilityEstimate,
    val checks: List<PuCapabilityCheck>,
    val warnings: List<String>,
    val blockers: List<String>,
    val recommendedProfile: PuProfile,
    val runtimeMatch: PuRuntimeMatch?,
) {
    val canLaunch: Boolean get() = blockers.isEmpty()
}

data class PuSandboxDescriptor(
    val gameId: String,
    val rootPath: String,
    val workingPath: String,
    val configPath: String,
    val cachePath: String,
    val logsPath: String,
    val networkAllowed: Boolean,
)

sealed interface PuLaunchPreparation {
    data class Ready(
        val game: PuGameRecord,
        val runtime: PuRuntimeDescriptor,
        val runtimeInstallPath: String,
        val sandbox: PuSandboxDescriptor,
        val assessment: PuLaunchAssessment,
        val warnings: List<String>,
    ) : PuLaunchPreparation

    data class NeedsRuntime(
        val game: PuGameRecord,
        val runtime: PuRuntimeDescriptor,
        val assessment: PuLaunchAssessment,
    ) : PuLaunchPreparation

    data class Blocked(
        val game: PuGameRecord,
        val assessment: PuLaunchAssessment,
    ) : PuLaunchPreparation
}

data class PuOptimizationProfileSettings(
    val profile: PuProfile,
    val maxImageDimension: Int,
    val webpQuality: Int,
    val videoBitrate: Int,
    val videoMaxDimension: Int,
    val audioBitrate: Int,
    val imageCacheBytes: Long,
    val rollbackHistory: Int,
    val predictiveLoading: Boolean,
) {
    companion object {
        fun forProfile(profile: PuProfile, lite: Boolean): PuOptimizationProfileSettings = when (profile) {
            PuProfile.ULTRA_LOW -> PuOptimizationProfileSettings(
                profile = profile,
                maxImageDimension = 720,
                webpQuality = 58,
                videoBitrate = 450_000,
                videoMaxDimension = 540,
                audioBitrate = 48_000,
                imageCacheBytes = 250L * 1024 * 1024,
                rollbackHistory = 16,
                predictiveLoading = false,
            )

            PuProfile.LOW -> PuOptimizationProfileSettings(
                profile = profile,
                maxImageDimension = 1280,
                webpQuality = 72,
                videoBitrate = 900_000,
                videoMaxDimension = 720,
                audioBitrate = 64_000,
                imageCacheBytes = (if (lite) 300L else 500L) * 1024 * 1024,
                rollbackHistory = 32,
                predictiveLoading = false,
            )

            PuProfile.BALANCED -> PuOptimizationProfileSettings(
                profile = profile,
                maxImageDimension = 1920,
                webpQuality = 84,
                videoBitrate = 1_800_000,
                videoMaxDimension = 1080,
                audioBitrate = 96_000,
                imageCacheBytes = (if (lite) 350L else 800L) * 1024 * 1024,
                rollbackHistory = 64,
                predictiveLoading = true,
            )
        }
    }
}

data class PuOptimizationAsset(
    val relativePath: String,
    val kind: PuAssetKind,
    val sourceSizeBytes: Long,
    val outputPath: String?,
    val outputSizeBytes: Long,
    val sourceFingerprint: String,
    val originalWidth: Int? = null,
    val originalHeight: Int? = null,
    val outputWidth: Int? = null,
    val outputHeight: Int? = null,
    val status: PuAssetStatus,
)

enum class PuAssetKind {
    IMAGE,
    VIDEO,
    AUDIO,
    OTHER,
}

enum class PuAssetStatus {
    OPTIMIZED,
    KEPT_ORIGINAL,
    PENDING,
    FAILED,
}

data class PuOptimizationManifest(
    val gameId: String,
    val profile: PuProfile,
    val schemaVersion: Int = 1,
    val updatedAt: Long,
    val assets: List<PuOptimizationAsset>,
) {
    val outputBytes: Long get() = assets.sumOf { asset ->
        if (asset.outputPath == null) 0L else asset.outputSizeBytes
    }
    val sourceBytes: Long get() = assets.sumOf { it.sourceSizeBytes }
    val optimizedAssets: Int get() = assets.count { it.outputPath != null }
    val keptOriginalAssets: Int get() = assets.count { it.outputPath == null }
}

/**
 * Blocks optimization work that could damage battery life or device health.
 * The thresholds are conservative defaults and still need calibration on the
 * 3 GB reference phone.
 */
fun PuDeviceSnapshot.optimizationBlockReason(): String? = when {
    thermalStatus != null && thermalStatus >= 3 -> "PU paused because this phone is too hot."
    batteryPercent < 20 && !isCharging -> "PU paused because the battery is below 20% and the phone is not charging."
    else -> null
}

data class PuFixProfile(
    val id: String,
    val title: String,
    val engine: PuEngineType,
    val versionFamily: String?,
    val fingerprint: String?,
    val settings: Map<String, String>,
    val revision: Long,
    val signature: String?,
)

data class PuSaveSlot(
    val id: String,
    val gameId: String,
    val engine: PuEngineType,
    val relativePath: String,
    val modifiedAt: Long,
    val sizeBytes: Long,
    val sha256: String,
)

data class PuBackupArchive(
    val id: String,
    val gameId: String,
    val createdAt: Long,
    val formatVersion: Int = 1,
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
    val slots: List<PuSaveSlot>,
)

data class PuControlBinding(
    val action: String,
    val touchX: Float,
    val touchY: Float,
    val touchRadius: Float,
    val keyboardKeyCode: Int?,
    val gamepadButton: Int?,
    val gamepadDeviceId: Int? = null,
)

data class PuControlProfile(
    val gameId: String,
    val overlayEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val keyboardVisible: Boolean,
    val bindings: List<PuControlBinding>,
    val updatedAt: Long,
)

data class PuWindowsModuleManifest(
    val id: String,
    val catalogRevision: Long,
    val packageUrl: String,
    val compressedBytes: Long,
    val installedBytes: Long,
    val sha256: String,
    val signature: String?,
    val minAndroidApi: Int,
    val abi: String,
    val version: String,
)

data class PuTranslationPack(
    val id: String,
    val languageCode: String,
    val sourceLanguage: String,
    val version: Long,
    val installedBytes: Long,
    val sha256: String,
    val installedAt: Long,
)

enum class PuEngineTypeWithDisplay {
    RENPY,
    RPG_MAKER,
    WINDOWS,
    UNKNOWN,
}

private val PuEngineType.displayName: String
    get() = when (this) {
        PuEngineType.RENPY -> "Ren'Py"
        PuEngineType.RPG_MAKER_MV -> "RPG Maker MV"
        PuEngineType.RPG_MAKER_MZ -> "RPG Maker MZ"
        PuEngineType.RPG_MAKER_XP -> "RPG Maker XP"
        PuEngineType.RPG_MAKER_VX -> "RPG Maker VX"
        PuEngineType.RPG_MAKER_ACE -> "RPG Maker Ace"
        PuEngineType.RPG_MAKER_2000 -> "RPG Maker 2000"
        PuEngineType.RPG_MAKER_2003 -> "RPG Maker 2003"
        PuEngineType.WOLF_RPG -> "Wolf RPG"
        PuEngineType.KIRIKIRI -> "KiriKiri"
        PuEngineType.UNITY -> "Unity"
        PuEngineType.GENERIC_EXE -> "Windows executable"
        PuEngineType.UNKNOWN -> "Unknown"
    }
