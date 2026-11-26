package sgyt.pu.core

import android.content.Context
import sgyt.pu.core.archive.PuArchiveExtractor
import sgyt.pu.core.compatibility.PuCompatibilityCatalog
import sgyt.pu.core.compatibility.PuCompatibilityService
import sgyt.pu.core.controls.AndroidPuGamepadManager
import sgyt.pu.core.controls.PuControlInputBridge
import sgyt.pu.core.controls.PuControlProfileStore
import sgyt.pu.core.controls.PuControlProfiles
import sgyt.pu.core.controls.PuGamepadProbe
import sgyt.pu.core.device.PuDeviceInspector
import sgyt.pu.core.device.PuDeviceProbe
import sgyt.pu.core.detection.PuGameDetector
import sgyt.pu.core.library.PuGameLibrary
import sgyt.pu.core.library.PuGameRepository
import sgyt.pu.core.launch.PuLaunchCoordinator
import sgyt.pu.core.launch.PuSandboxManager
import sgyt.pu.core.network.PuApiConfig
import sgyt.pu.core.optimizer.PuOptimizer
import sgyt.pu.core.runtime.PuDownloader
import sgyt.pu.core.runtime.PuRuntimeCatalogStore
import sgyt.pu.core.runtime.PuRuntimeLibrary
import sgyt.pu.core.runtime.PuRuntimeManager
import sgyt.pu.core.storage.PuStorage

/**
 * Composition root for the PU core services.
 *
 * The graph is created lazily by each edition's Application so cold start stays
 * cheap, and every screen reaches it through the shared graph owner interface
 * instead of building services by hand.
 */
class PuCoreGraph(
    context: Context,
    val apiConfig: PuApiConfig = PuApiConfig(),
) {
    private val appContext: Context = context.applicationContext

    val storage: PuStorage = PuStorage(appContext)

    val gameRepository: PuGameRepository = PuGameRepository(
        context = appContext,
        storage = storage,
        detector = PuGameDetector(),
        archives = PuArchiveExtractor(),
    )

    val deviceInspector: PuDeviceInspector = PuDeviceInspector(appContext)

    /** Domain-facing device contract used by the presentation layer. */
    val deviceProbe: PuDeviceProbe get() = deviceInspector

    val catalogStore: PuRuntimeCatalogStore = PuRuntimeCatalogStore(
        context = appContext,
        storage = storage,
        apiConfig = apiConfig,
    )

    val downloader: PuDownloader = PuDownloader(appContext)

    val runtimeManager: PuRuntimeManager = PuRuntimeManager(storage, catalogStore, downloader)

    /** Domain-facing runtime contract used by the presentation layer. */
    val runtimeLibrary: PuRuntimeLibrary get() = runtimeManager

    val sandboxManager: PuSandboxManager = PuSandboxManager(storage)

    val launchCoordinator: PuLaunchCoordinator = PuLaunchCoordinator(
        runtimeManager = runtimeManager,
        deviceInspector = deviceInspector,
        sandboxManager = sandboxManager,
    )

    val optimizer: PuOptimizer = PuOptimizer(storage)

    val compatibilityCatalog: PuCompatibilityCatalog = PuCompatibilityService(
        context = appContext,
        storage = storage,
        gameRepository = gameRepository,
        apiConfig = apiConfig,
    )

    /** Domain-facing library contract used by the presentation layer. */
    val gameLibrary: PuGameLibrary get() = gameRepository

    /** Persistent per-game and global touch, keyboard and gamepad mappings. */
    val controlProfiles: PuControlProfiles = PuControlProfileStore(storage)

    /** Connected Android input devices used by the control mapping screen. */
    val gamepadProbe: PuGamepadProbe = AndroidPuGamepadManager()

    /** Resolves touch and hardware input to the configured game actions. */
    val controlInputBridge: PuControlInputBridge = PuControlInputBridge()
}
