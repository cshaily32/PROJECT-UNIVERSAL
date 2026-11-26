package sgyt.pu.core.launch

import sgyt.pu.core.device.PuDeviceInspector
import sgyt.pu.core.json.PuJson
import sgyt.pu.core.json.PuJsonValue
import sgyt.pu.core.model.PuCapabilityCheck
import sgyt.pu.core.model.PuCompatibilityEstimate
import sgyt.pu.core.model.PuDeviceSnapshot
import sgyt.pu.core.model.PuDeviceTier
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.core.model.PuLaunchAssessment
import sgyt.pu.core.model.PuLaunchPreparation
import sgyt.pu.core.model.PuRuntimeMatch
import sgyt.pu.core.model.PuSandboxDescriptor
import sgyt.pu.core.model.assessRuntime
import sgyt.pu.core.runtime.PuRuntimeManager
import sgyt.pu.core.storage.PuStorage
import sgyt.pu.model.PuEdition
import sgyt.pu.model.PuProfile
import java.io.File

/** Builds the per-game sandbox without changing any original game file. */
class PuSandboxManager(private val storage: PuStorage) {
    fun prepare(game: PuGameRecord, profile: PuProfile, networkAllowed: Boolean = false): PuSandboxDescriptor {
        val root = storage.sandboxDirectory(game.id)
        val working = File(root, "working").apply { mkdirs() }
        val config = File(root, "config").apply { mkdirs() }
        val logs = File(root, "logs").apply { mkdirs() }
        val cache = storage.cacheDirectory(game.id).apply { mkdirs() }

        val settings = PuJson.obj(
            "schemaVersion" to PuJson.number(1),
            "gameId" to PuJson.string(game.id),
            "profile" to PuJson.string(profile.name),
            "imageCacheBytes" to PuJson.number(
                PuOptimizationProfileSettingsFactory.forProfile(profile).imageCacheBytes,
            ),
            "rollbackHistory" to PuJson.number(
                PuOptimizationProfileSettingsFactory.forProfile(profile).rollbackHistory,
            ),
            "predictiveLoading" to PuJson.bool(
                PuOptimizationProfileSettingsFactory.forProfile(profile).predictiveLoading,
            ),
            "networkAllowed" to PuJson.bool(networkAllowed),
        )
        storage.writeJson(File(config, "pu-runtime.json"), settings)

        return PuSandboxDescriptor(
            gameId = game.id,
            rootPath = root.absolutePath,
            workingPath = working.absolutePath,
            configPath = config.absolutePath,
            cachePath = cache.absolutePath,
            logsPath = logs.absolutePath,
            networkAllowed = networkAllowed,
        )
    }

    fun clear(gameId: String) {
        storage.deleteRecursively(storage.sandboxDirectory(gameId))
    }
}

private object PuOptimizationProfileSettingsFactory {
    fun forProfile(profile: PuProfile): sgyt.pu.core.model.PuOptimizationProfileSettings =
        sgyt.pu.core.model.PuOptimizationProfileSettings.forProfile(profile, lite = false)
}

/**
 * Runs the mandatory pre-launch checks in order. No game process is started
 * until every blocking requirement has passed.
 */
class PuLaunchCoordinator(
    private val runtimeManager: PuRuntimeManager,
    private val deviceInspector: PuDeviceInspector,
    private val sandboxManager: PuSandboxManager,
) {
    fun prepare(
        game: PuGameRecord,
        edition: PuEdition,
        manualProfile: PuProfile? = null,
    ): PuLaunchPreparation {
        val device = deviceInspector.snapshot()
        val runtimeMatch = runtimeManager.match(game.engine, game.version, device.supportedAbis)
        if (runtimeMatch == null) {
            val assessment = PuLaunchAssessment(
                estimate = PuCompatibilityEstimate.PROBABLY_WONT_RUN,
                checks = listOf(
                    PuCapabilityCheck(
                        id = "runtime-match",
                        label = "Matching runtime",
                        passed = false,
                        detail = "No published runtime matches ${game.engine.name} ${game.version}",
                        blocking = true,
                    ),
                ),
                warnings = emptyList(),
                blockers = listOf("PU does not have a verified runtime for this game yet."),
                recommendedProfile = recommendedProfile(device),
                runtimeMatch = null,
            )
            return PuLaunchPreparation.Blocked(game, assessment)
        }

        val assessment = assess(game, edition, device, runtimeMatch)
        if (!assessment.canLaunch) return PuLaunchPreparation.Blocked(game, assessment)

        val installed = runtimeManager.listInstalled().firstOrNull {
            it.runtimeId == runtimeMatch.runtime.id && it.revision == runtimeMatch.runtime.catalogRevision
        }
        if (installed == null || !File(installed.installPath).isDirectory) {
            return PuLaunchPreparation.NeedsRuntime(game, runtimeMatch.runtime, assessment)
        }

        runtimeManager.markUsed(installed.runtimeId, installed.revision)
        val profile = manualProfile ?: game.selectedProfile ?: assessment.recommendedProfile
        val sandbox = sandboxManager.prepare(game, profile, networkAllowed = false)
        return PuLaunchPreparation.Ready(
            game = game,
            runtime = runtimeMatch.runtime,
            runtimeInstallPath = installed.installPath,
            sandbox = sandbox,
            assessment = assessment,
            warnings = assessment.warnings + listOfNotNull(runtimeMatch.warning),
        )
    }

    fun assess(
        game: PuGameRecord,
        edition: PuEdition,
        device: PuDeviceSnapshot,
        runtimeMatch: PuRuntimeMatch?,
    ): PuLaunchAssessment {
        val checks = mutableListOf<PuCapabilityCheck>()
        val warnings = mutableListOf<String>()
        val blockers = mutableListOf<String>()
        val runtime = runtimeMatch?.runtime

        runtime?.let { selectedRuntime ->
            val runtimeCapability = device.assessRuntime(selectedRuntime)
            checks += runtimeCapability.checks
            blockers += runtimeCapability.blockers
        }

        val isWindows = game.engine in WINDOWS_ENGINES
        if (isWindows) {
            val windowsAllowed = device.isArm64 && (edition == PuEdition.PLUS || game.engine != PuEngineType.UNITY)
            checks += PuCapabilityCheck(
                id = "windows-module",
                label = "Windows module",
                passed = windowsAllowed,
                detail = if (!device.isArm64) "Box64 requires an arm64 phone" else if (game.engine == PuEngineType.UNITY && edition != PuEdition.PLUS) "Unity is available only in PU Plus" else "arm64 device and edition policy passed",
                blocking = true,
            )
            if (!windowsAllowed) blockers += "This Windows game is not supported by the current device and edition."
        }

        if (game.engine == PuEngineType.RPG_MAKER_MV || game.engine == PuEngineType.RPG_MAKER_MZ) {
            val webViewPassed = device.webViewVersionCode > 0
            checks += PuCapabilityCheck(
                id = "webview",
                label = "System WebView",
                passed = webViewPassed,
                detail = device.webViewVersionName?.let { "$it (${device.webViewVersionCode})" } ?: "No WebView provider was found",
                blocking = true,
            )
            if (!webViewPassed) blockers += "RPG Maker MV and MZ require an updated system WebView."
        }

        val minimumFreeStorage = 512L * 1024 * 1024
        val storagePassed = device.freeStorageBytes >= minimumFreeStorage
        checks += PuCapabilityCheck(
            id = "storage",
            label = "Free storage",
            passed = storagePassed,
            detail = "${device.freeStorageBytes / 1024 / 1024} MB available",
            blocking = true,
        )
        if (!storagePassed) blockers += "At least 512 MB of free storage is required before launch."

        val memoryPassed = device.availableRamBytes >= 384L * 1024 * 1024
        checks += PuCapabilityCheck(
            id = "memory",
            label = "Available memory",
            passed = memoryPassed,
            detail = "${device.availableRamBytes / 1024 / 1024} MB currently available",
            blocking = true,
        )
        if (!memoryPassed) blockers += "There is not enough available memory to start the game safely."
        if (device.availableRamBytes < 1_200L * 1024 * 1024 || game.originalSizeBytes > 2L * 1024 * 1024 * 1024) {
            warnings += "This game may stutter on the current memory profile."
        }
        if (device.tier == PuDeviceTier.WINE_LITE) {
            warnings += "PU Lite limits Windows games to simple 2D profiles."
        }
        runtimeMatch?.warning?.let(warnings::add)

        val estimate = when {
            blockers.isNotEmpty() -> PuCompatibilityEstimate.PROBABLY_WONT_RUN
            warnings.isNotEmpty() -> PuCompatibilityEstimate.MIGHT_LAG
            else -> PuCompatibilityEstimate.LIKELY_SMOOTH
        }
        return PuLaunchAssessment(
            estimate = estimate,
            checks = checks,
            warnings = warnings.distinct(),
            blockers = blockers.distinct(),
            recommendedProfile = recommendedProfile(device),
            runtimeMatch = runtimeMatch,
        )
    }

    private fun recommendedProfile(device: PuDeviceSnapshot): PuProfile = when (device.tier) {
        PuDeviceTier.NATIVE -> PuProfile.ULTRA_LOW
        PuDeviceTier.WINE_LITE -> PuProfile.LOW
        PuDeviceTier.WINE_FULL -> PuProfile.BALANCED
    }

    private companion object {
        val WINDOWS_ENGINES = setOf(
            PuEngineType.WOLF_RPG,
            PuEngineType.KIRIKIRI,
            PuEngineType.UNITY,
            PuEngineType.GENERIC_EXE,
        )
    }
}
