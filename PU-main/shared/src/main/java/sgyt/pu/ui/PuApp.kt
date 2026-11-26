package sgyt.pu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import sgyt.pu.core.PuCoreGraph
import sgyt.pu.model.LocalPuEditionConfig
import sgyt.pu.model.PuEditionConfig
import sgyt.pu.ui.components.PuBottomBar
import sgyt.pu.ui.components.PuMainTab
import sgyt.pu.ui.screens.AddGameScreen
import sgyt.pu.ui.screens.BackgroundScreen
import sgyt.pu.ui.screens.CompatibilityScreen
import sgyt.pu.ui.screens.ControlsScreen
import sgyt.pu.ui.screens.DeviceCheckScreen
import sgyt.pu.ui.screens.DeviceGateScreen
import sgyt.pu.ui.screens.GameScreen
import sgyt.pu.ui.screens.LibraryScreen
import sgyt.pu.ui.screens.OptimizingScreen
import sgyt.pu.ui.screens.RuntimesScreen
import sgyt.pu.ui.screens.RuntimeDownloadScreen
import sgyt.pu.ui.screens.SavesScreen
import sgyt.pu.ui.screens.SettingsScreen
import sgyt.pu.ui.screens.TranslationScreen
import sgyt.pu.ui.screens.WindowsModuleScreen
import sgyt.pu.ui.model.PuRuntimeSelection
import sgyt.pu.ui.theme.PuPalette
import sgyt.pu.ui.viewmodels.PuAddGameViewModel
import sgyt.pu.ui.viewmodels.PuCompatibilityViewModel
import sgyt.pu.ui.viewmodels.PuControlsViewModel
import sgyt.pu.ui.viewmodels.PuDeviceCheckViewModel
import sgyt.pu.ui.viewmodels.PuGameDetailsViewModel
import sgyt.pu.ui.viewmodels.PuLaunchCheckViewModel
import sgyt.pu.ui.viewmodels.PuOptimizerViewModel
import sgyt.pu.ui.viewmodels.PuLibraryAction
import sgyt.pu.ui.viewmodels.PuLibraryViewModel
import sgyt.pu.ui.viewmodels.PuRuntimeDownloadViewModel
import sgyt.pu.ui.viewmodels.PuRuntimesAction
import sgyt.pu.ui.viewmodels.PuRuntimesViewModel
import sgyt.pu.ui.viewmodels.PuViewModelFactory

private object PuRoutes {
    const val DEVICE_CHECK = "device-check"
    const val LIBRARY = "library"
    const val ADD_GAME = "add-game"
    const val GAME = "game"
    const val RUNTIME_DOWNLOAD = "runtime-download"
    const val DEVICE_GATE = "device-gate"
    const val OPTIMIZE = "optimize"
    const val COMPATIBILITY = "compatibility"
    const val CONTROLS = "controls"
    const val SAVES = "saves"
    const val RUNTIMES = "runtimes"
    const val WINDOWS = "windows"
    const val TRANSLATION = "translation"
    const val BACKGROUND = "background"
    const val SETTINGS = "settings"
}

@Composable
fun PuApp(
    config: PuEditionConfig,
    graph: PuCoreGraph,
) {
    val navController = rememberNavController()
    var selectedGameId by remember { mutableStateOf<String?>(null) }
    var selectedControlGameId by remember { mutableStateOf<String?>(null) }
    var selectedRuntime by remember { mutableStateOf<PuRuntimeSelection?>(null) }
    val libraryViewModel: PuLibraryViewModel = viewModel(
        factory = PuViewModelFactory { PuLibraryViewModel(graph.gameLibrary) },
    )
    val runtimesViewModel: PuRuntimesViewModel = viewModel(
        factory = PuViewModelFactory {
            PuRuntimesViewModel(graph.runtimeLibrary, graph.deviceProbe)
        },
    )

    CompositionLocalProvider(LocalPuEditionConfig provides config) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val selectedTab = PuMainTab.entries.firstOrNull { it.route == currentRoute }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PuPalette.Background)
                .safeDrawingPadding(),
        ) {
            NavHost(
                navController = navController,
                startDestination = PuRoutes.LIBRARY,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(PuRoutes.DEVICE_CHECK) {
                    val deviceCheckViewModel: PuDeviceCheckViewModel = viewModel(
                        factory = PuViewModelFactory { PuDeviceCheckViewModel(graph.deviceProbe) },
                    )
                    DeviceCheckScreen(
                        config = config,
                        viewModel = deviceCheckViewModel,
                        onContinue = {
                            navController.navigate(PuRoutes.LIBRARY) {
                                popUpTo(navController.graph.findStartDestination().id)
                            }
                        },
                    )
                }
                composable(PuRoutes.LIBRARY) {
                    LibraryScreen(
                        config = config,
                        viewModel = libraryViewModel,
                        onOpenGame = { game ->
                            selectedGameId = game.id
                            navController.navigate(PuRoutes.GAME)
                        },
                        onAddGame = { navController.navigate(PuRoutes.ADD_GAME) },
                    )
                }
                composable(PuRoutes.ADD_GAME) {
                    val addGameViewModel: PuAddGameViewModel = viewModel(
                        factory = PuViewModelFactory { PuAddGameViewModel(graph.gameLibrary) },
                    )
                    AddGameScreen(
                        viewModel = addGameViewModel,
                        onBack = { navController.popBackStack() },
                        onImported = {
                            libraryViewModel.onAction(PuLibraryAction.OnRefresh)
                            navController.popBackStack()
                        },
                    )
                }
                composable(PuRoutes.GAME) {
                    val gameId = selectedGameId
                    if (gameId == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        val gameDetailsViewModel: PuGameDetailsViewModel = viewModel(
                            factory = PuViewModelFactory {
                                PuGameDetailsViewModel(
                                    gameId = gameId,
                                    library = graph.gameLibrary,
                                    optimizer = graph.optimizer,
                                    deviceProbe = graph.deviceProbe,
                                )
                            },
                        )
                        GameScreen(
                            config = config,
                            viewModel = gameDetailsViewModel,
                            onBack = {
                                libraryViewModel.onAction(PuLibraryAction.OnRefresh)
                                navController.popBackStack()
                            },
                            onOptimize = { navController.navigate(PuRoutes.OPTIMIZE) },
                            onCompatibility = { navController.navigate(PuRoutes.COMPATIBILITY) },
                            onControls = {
                                selectedControlGameId = gameId
                                navController.navigate(PuRoutes.CONTROLS)
                            },
                            onSaves = { navController.navigate(PuRoutes.SAVES) },
                            onPlay = { navController.navigate(PuRoutes.DEVICE_GATE) },
                        )
                    }
                }
                composable(PuRoutes.RUNTIME_DOWNLOAD) {
                    val runtime = selectedRuntime
                    if (runtime == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        val runtimeId = runtime.runtimeId
                        val revision = runtime.revision
                        val runtimeDownloadViewModel: PuRuntimeDownloadViewModel = viewModel(
                            factory = PuViewModelFactory {
                                PuRuntimeDownloadViewModel(
                                    runtimeId = runtimeId,
                                    revision = revision,
                                    runtimeLibrary = graph.runtimeLibrary,
                                    deviceProbe = graph.deviceProbe,
                                )
                            },
                        )
                        RuntimeDownloadScreen(
                            viewModel = runtimeDownloadViewModel,
                            onBack = {
                                runtimesViewModel.onAction(PuRuntimesAction.OnRefresh)
                                navController.popBackStack()
                            },
                            onInstalled = {
                                runtimesViewModel.onAction(PuRuntimesAction.OnRefresh)
                            },
                        )
                    }
                }
                composable(PuRoutes.DEVICE_GATE) {
                    val gameId = selectedGameId
                    if (gameId == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        val launchCheckViewModel: PuLaunchCheckViewModel = viewModel(
                            factory = PuViewModelFactory {
                                PuLaunchCheckViewModel(
                                    gameId = gameId,
                                    edition = config.edition,
                                    library = graph.gameLibrary,
                                    launchCoordinator = graph.launchCoordinator,
                                )
                            },
                        )
                        DeviceGateScreen(
                            viewModel = launchCheckViewModel,
                            onBack = { navController.popBackStack() },
                            onRuntimeNeeded = { selection ->
                                selectedRuntime = selection
                                navController.navigate(PuRoutes.RUNTIME_DOWNLOAD)
                            },
                            onReady = {
                                navController.navigate(PuRoutes.OPTIMIZE) {
                                    popUpTo(PuRoutes.DEVICE_GATE) { inclusive = true }
                                }
                            },
                        )
                    }
                }
                composable(PuRoutes.OPTIMIZE) {
                    val gameId = selectedGameId
                    if (gameId == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        val optimizerViewModel: PuOptimizerViewModel = viewModel(
                            factory = PuViewModelFactory {
                                PuOptimizerViewModel(
                                    gameId = gameId,
                                    edition = config.edition,
                                    library = graph.gameLibrary,
                                    optimizer = graph.optimizer,
                                    deviceProbe = graph.deviceProbe,
                                )
                            },
                        )
                        OptimizingScreen(
                            viewModel = optimizerViewModel,
                            onBack = { navController.popBackStack() },
                            onCompatibility = { navController.navigate(PuRoutes.COMPATIBILITY) },
                        )
                    }
                }
                composable(PuRoutes.COMPATIBILITY) {
                    val gameId = selectedGameId
                    if (gameId == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        val compatibilityViewModel: PuCompatibilityViewModel = viewModel(
                            factory = PuViewModelFactory {
                                PuCompatibilityViewModel(
                                    gameId = gameId,
                                    library = graph.gameLibrary,
                                    catalog = graph.compatibilityCatalog,
                                )
                            },
                        )
                        CompatibilityScreen(
                            viewModel = compatibilityViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable(PuRoutes.CONTROLS) {
                    val controlsViewModel: PuControlsViewModel = viewModel(
                        factory = PuViewModelFactory {
                            PuControlsViewModel(
                                gameId = selectedControlGameId,
                                library = graph.gameLibrary,
                                profiles = graph.controlProfiles,
                                gamepadProbe = graph.gamepadProbe,
                            )
                        },
                    )
                    ControlsScreen(
                        viewModel = controlsViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(PuRoutes.SAVES) {
                    SavesScreen(
                        config = config,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(PuRoutes.RUNTIMES) {
                    RuntimesScreen(
                        viewModel = runtimesViewModel,
                        onDownload = { runtime ->
                            selectedRuntime = PuRuntimeSelection(runtime.runtimeId, runtime.revision)
                            navController.navigate(PuRoutes.RUNTIME_DOWNLOAD)
                        },
                    )
                }
                composable(PuRoutes.WINDOWS) {
                    WindowsModuleScreen(
                        config = config,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(PuRoutes.TRANSLATION) {
                    TranslationScreen(
                        config = config,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(PuRoutes.BACKGROUND) {
                    BackgroundScreen(
                        config = config,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(PuRoutes.SETTINGS) {
                    SettingsScreen(
                        config = config,
                        onOpenWindows = { navController.navigate(PuRoutes.WINDOWS) },
                        onOpenTranslation = { navController.navigate(PuRoutes.TRANSLATION) },
                        onOpenBackground = { navController.navigate(PuRoutes.BACKGROUND) },
                        onOpenSaves = { navController.navigate(PuRoutes.SAVES) },
                        onOpenControls = {
                            selectedControlGameId = null
                            navController.navigate(PuRoutes.CONTROLS)
                        },
                        onOpenDeviceCheck = { navController.navigate(PuRoutes.DEVICE_CHECK) },
                    )
                }
            }

            if (selectedTab != null) {
                PuBottomBar(
                    selected = selectedTab,
                    onSelected = { tab ->
                        if (tab == PuMainTab.RUNTIMES) {
                            runtimesViewModel.onAction(PuRuntimesAction.OnRefresh)
                        }
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}
