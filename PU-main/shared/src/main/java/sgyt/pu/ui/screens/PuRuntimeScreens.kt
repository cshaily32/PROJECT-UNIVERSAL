package sgyt.pu.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen

import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sgyt.pu.ui.theme.signalColors
import sgyt.pu.ui.theme.spacing
import kotlinx.coroutines.flow.collectLatest
import sgyt.pu.core.model.PuCapabilityCheck
import sgyt.pu.core.model.PuCompatibilityEstimate
import sgyt.pu.core.model.PuLaunchAssessment
import sgyt.pu.core.model.PuRuntimeInstallationState
import sgyt.pu.model.PuEditionConfig
import sgyt.pu.model.PuSampleData
import sgyt.pu.model.StepState
import sgyt.pu.ui.components.NoticeTone
import sgyt.pu.ui.components.PuBody
import sgyt.pu.ui.components.PuEditionBadge
import sgyt.pu.ui.components.PuCard
import sgyt.pu.ui.components.PuLabel
import sgyt.pu.ui.components.PuMetric
import sgyt.pu.ui.components.PuNotice
import sgyt.pu.ui.components.PuPrimaryButton
import sgyt.pu.ui.components.PuPrimaryCard
import sgyt.pu.ui.components.PuProgressBar
import sgyt.pu.ui.components.PuScreen
import sgyt.pu.ui.components.PuSecondaryButton
import sgyt.pu.ui.components.PuSettingRow
import sgyt.pu.ui.model.PuRuntimeFilter
import sgyt.pu.ui.model.PuRuntimeInfo
import sgyt.pu.ui.model.PuRuntimeSelection
import sgyt.pu.ui.model.formatBytes
import sgyt.pu.ui.model.label as engineLabel
import sgyt.pu.ui.model.toPresentation
import sgyt.pu.ui.viewmodels.PuAddGameAction
import sgyt.pu.ui.viewmodels.PuAddGameEvent
import sgyt.pu.ui.viewmodels.PuAddGameState
import sgyt.pu.ui.viewmodels.PuAddGameViewModel
import sgyt.pu.ui.viewmodels.PuImportSource
import sgyt.pu.ui.viewmodels.PuLaunchCheckAction
import sgyt.pu.ui.viewmodels.PuLaunchCheckEvent
import sgyt.pu.ui.viewmodels.PuLaunchCheckState
import sgyt.pu.ui.viewmodels.PuLaunchCheckViewModel
import sgyt.pu.ui.viewmodels.PuRuntimeDownloadAction
import sgyt.pu.ui.viewmodels.PuRuntimeDownloadEvent
import sgyt.pu.ui.viewmodels.PuRuntimeDownloadState
import sgyt.pu.ui.viewmodels.PuRuntimeDownloadViewModel
import sgyt.pu.ui.viewmodels.PuRuntimesAction
import sgyt.pu.ui.viewmodels.PuRuntimesEvent
import sgyt.pu.ui.viewmodels.PuRuntimesState
import sgyt.pu.ui.viewmodels.PuRuntimesViewModel

@Composable
fun AddGameScreen(
    viewModel: PuAddGameViewModel,
    onBack: () -> Unit,
    onImported: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.onAction(PuAddGameAction.OnUriChosen(it.toString())) }
    }
    val archivePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onAction(PuAddGameAction.OnUriChosen(it.toString())) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PuAddGameEvent.Imported -> onImported(event.gameId)
            }
        }
    }

    PuAddGameScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onPickFolder = { folderPicker.launch(null) },
        onPickArchive = {
            archivePicker.launch(
                arrayOf(
                    "application/zip",
                    "application/x-tar",
                    "application/gzip",
                    "application/octet-stream",
                ),
            )
        },
    )
}

@Composable
private fun PuAddGameScreenContent(
    state: PuAddGameState,
    onAction: (PuAddGameAction) -> Unit,
    onBack: () -> Unit,
    onPickFolder: () -> Unit,
    onPickArchive: () -> Unit,
) {
    PuScreen(
        title = "Add game",
        onBack = onBack,
    ) {
        PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.xs)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Local files only",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Step 1 of 3",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                    )
                }
                PuMetric(
                    value = "Add a game",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                )
                Text(
                    text = "PU identifies the engine and version, then prepares the matching runtime when it is needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                )
            }
        }

        PuLabel("Game source", modifier = Modifier.padding(top = MaterialTheme.spacing.lg))
        PuSourceCard(
            title = "Game folder",
            detail = "Choose the folder that contains the game files.",
            icon = Icons.Outlined.FolderOpen,
            selected = state.source == PuImportSource.FOLDER,
            enabled = state.screenState != PuAddGameState.ScreenState.Importing,
            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
            onClick = { onAction(PuAddGameAction.OnSourceSelected(PuImportSource.FOLDER)) },
        )
        PuSourceCard(
            title = "Game archive",
            detail = "Choose a packaged archive from this phone.",
            icon = Icons.Outlined.Archive,
            selected = state.source == PuImportSource.ARCHIVE,
            enabled = state.screenState != PuAddGameState.ScreenState.Importing,
            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
            onClick = { onAction(PuAddGameAction.OnSourceSelected(PuImportSource.ARCHIVE)) },
        )

        PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PuLabel("Detection", modifier = Modifier.weight(1f))
                    PuBody("Before optimization")
                }
                PuDetectionStep("Locate game files", locateDetail(state), locateState(state))
                PuDetectionStep("Identify engine and version", engineDetail(state), engineState(state))
                PuDetectionStep("Match a runtime", runtimeDetail(state), StepState.WAITING)
            }
        }

        if (state.screenState == PuAddGameState.ScreenState.Importing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.base),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        if (state.screenState == PuAddGameState.ScreenState.Success) {
            PuNotice(
                text = buildString {
                    append(state.importedTitle ?: "The game")
                    append(" was added to the library")
                    val engine = state.detectedEngine
                    val version = state.detectedVersion
                    if (engine != null && version != null) {
                        append(" as ")
                        append(engine)
                        append(' ')
                        append(version)
                    }
                    append('.')
                },
                tone = NoticeTone.SUCCESS,
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            )
        }

        if (state.errorMessage != null) {
            PuNotice(
                text = state.errorMessage,
                tone = NoticeTone.WARNING,
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            )
        }

        PuPrimaryButton(
            text = when (state.screenState) {
                PuAddGameState.ScreenState.Success -> "Open in library"
                PuAddGameState.ScreenState.Importing -> "Importing"
                PuAddGameState.ScreenState.Error -> "Try another source"
                PuAddGameState.ScreenState.Idle -> if (state.source == PuImportSource.FOLDER) {
                    "Choose game folder"
                } else {
                    "Choose game archive"
                }
            },
            icon = when {
                state.screenState == PuAddGameState.ScreenState.Success -> Icons.Outlined.Check
                state.source == PuImportSource.FOLDER -> Icons.Outlined.FolderOpen
                else -> Icons.Outlined.Archive
            },
            enabled = state.screenState != PuAddGameState.ScreenState.Importing,
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            onClick = {
                when (state.screenState) {
                    PuAddGameState.ScreenState.Success -> onAction(PuAddGameAction.OnOpenLibrary)
                    else -> if (state.source == PuImportSource.FOLDER) onPickFolder() else onPickArchive()
                }
            },
        )
    }
}

private fun locateState(state: PuAddGameState): StepState = when (state.screenState) {
    PuAddGameState.ScreenState.Idle -> StepState.WAITING
    PuAddGameState.ScreenState.Importing -> StepState.ACTIVE
    PuAddGameState.ScreenState.Success -> StepState.DONE
    PuAddGameState.ScreenState.Error -> StepState.FAILED
}

private fun locateDetail(state: PuAddGameState): String = when (state.screenState) {
    PuAddGameState.ScreenState.Idle -> "Waiting"
    PuAddGameState.ScreenState.Importing -> "Copying files"
    PuAddGameState.ScreenState.Success -> "Done"
    PuAddGameState.ScreenState.Error -> "Failed"
}

private fun engineState(state: PuAddGameState): StepState = when (state.screenState) {
    PuAddGameState.ScreenState.Success -> StepState.DONE
    PuAddGameState.ScreenState.Error -> StepState.WARNING
    else -> StepState.WAITING
}

private fun engineDetail(state: PuAddGameState): String {
    val engine = state.detectedEngine ?: return "Waiting"
    val version = state.detectedVersion
    return if (version.isNullOrBlank()) engine else "$engine $version"
}

private fun runtimeDetail(state: PuAddGameState): String = when (state.screenState) {
    PuAddGameState.ScreenState.Success -> "Checked on first launch"
    else -> "Waiting"
}

@Composable
private fun PuSourceCard(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    PuCard(modifier = modifier, onClick = if (enabled) onClick else null) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(MaterialTheme.spacing.md).size(19.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                PuLabel(title)
                PuBody(detail, modifier = Modifier.padding(top = MaterialTheme.spacing.xs))
            }
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.size(22.dp),
            ) {
                if (selected) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(MaterialTheme.spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun PuDetectionStep(label: String, detail: String, state: StepState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Surface(
            shape = CircleShape,
            color = when (state) {
                StepState.DONE -> MaterialTheme.signalColors.readyContainer
                StepState.ACTIVE -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (state == StepState.DONE) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.signalColors.onReadyContainer,
                        modifier = Modifier.padding(MaterialTheme.spacing.xs),
                    )
                }
            }
        }
        PuLabel(label, modifier = Modifier.weight(1f))
        PuBody(detail)
    }
}

@Composable
fun RuntimeDownloadScreen(
    viewModel: PuRuntimeDownloadViewModel,
    onBack: () -> Unit,
    onInstalled: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PuRuntimeDownloadEvent.Installed -> onInstalled()
            }
        }
    }

    PuRuntimeDownloadScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}

@Composable
private fun PuRuntimeDownloadScreenContent(
    state: PuRuntimeDownloadState,
    onAction: (PuRuntimeDownloadAction) -> Unit,
    onBack: () -> Unit,
) {
    PuScreen(
        title = "Runtime download",
        onBack = onBack,
    ) {
        when (state.screenState) {
            PuRuntimeDownloadState.ScreenState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            PuRuntimeDownloadState.ScreenState.Error -> PuNotice(
                text = state.errorMessage ?: "PU could not read this runtime from the signed catalog.",
                tone = NoticeTone.WARNING,
            )

            PuRuntimeDownloadState.ScreenState.Success -> {
                val runtime = state.runtime
                if (runtime == null) {
                    PuNotice(
                        text = "This runtime is no longer available in the signed catalog.",
                        tone = NoticeTone.WARNING,
                    )
                } else {
                    val status = when (state.installationState) {
                        PuRuntimeInstallationState.NOT_INSTALLED -> "Required"
                        PuRuntimeInstallationState.DOWNLOADING -> "Downloading"
                        PuRuntimeInstallationState.INSTALLED -> "Installed"
                        PuRuntimeInstallationState.CANCELLED -> "Paused"
                        PuRuntimeInstallationState.FAILED -> "Rejected"
                    }
                    PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.xs)) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = runtime.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                                )
                            }
                            PuMetric(
                                value = formatBytes(runtime.compressedBytes),
                                modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                            )
                            Text(
                                text = if (runtime.installed) {
                                    "This runtime is ready and shared by every compatible game."
                                } else {
                                    "Download once and share this runtime with every compatible game."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                                modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                            )
                            if (
                                state.installationState == PuRuntimeInstallationState.DOWNLOADING ||
                                state.installationState == PuRuntimeInstallationState.CANCELLED
                            ) {
                                PuProgressBar(
                                    progress = state.progress,
                                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                                    onPrimarySurface = true,
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = MaterialTheme.spacing.sm),
                                ) {
                                    Text(
                                        text = "${formatBytes(state.downloadedBytes)} of ${formatBytes(state.totalBytes)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = if (state.resumedFromBytes > 0) "Resumed" else "Resumable",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f),
                                    )
                                }
                            }
                        }
                    }

                    PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
                        Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base)) {
                            PuSettingRow(
                                icon = Icons.Outlined.Security,
                                title = "Signed package",
                                detail = "Hash and signature are checked before installation",
                                trailing = { Icon(Icons.Outlined.Check, contentDescription = null) },
                            )
                            PuSettingRow(
                                icon = Icons.Outlined.WarningAmber,
                                title = "Shared installation",
                                detail = "One copy works across every compatible game",
                                trailing = { Icon(Icons.Outlined.Check, contentDescription = null) },
                            )
                            PuSettingRow(
                                icon = Icons.Outlined.Wifi,
                                title = "Network",
                                detail = "Only the runtime package is downloaded",
                                trailing = { Icon(Icons.Outlined.Check, contentDescription = null) },
                            )
                        }
                    }

                    if (!runtime.supported) {
                        PuNotice(
                            text = runtime.requirement,
                            tone = NoticeTone.WARNING,
                            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                        )
                    }
                    if (state.errorMessage != null) {
                        PuNotice(
                            text = state.errorMessage,
                            tone = NoticeTone.WARNING,
                            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                        )
                    }

                    PuNotice(
                        text = "A failed update never replaces a working runtime. Corrupted packages are rejected.",
                        modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    )

                    when (state.installationState) {
                        PuRuntimeInstallationState.DOWNLOADING -> PuSecondaryButton(
                            text = "Cancel download",
                            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                            onClick = { onAction(PuRuntimeDownloadAction.OnCancel) },
                        )

                        PuRuntimeInstallationState.INSTALLED -> {
                            PuPrimaryButton(
                                text = "Back to runtimes",
                                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                                onClick = onBack,
                            )
                            PuSecondaryButton(
                                text = "Remove runtime",
                                icon = Icons.Outlined.DeleteOutline,
                                modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                                onClick = { onAction(PuRuntimeDownloadAction.OnRemove) },
                            )
                        }

                        PuRuntimeInstallationState.NOT_INSTALLED,
                        PuRuntimeInstallationState.CANCELLED,
                        PuRuntimeInstallationState.FAILED -> PuPrimaryButton(
                            text = when {
                                state.installationState == PuRuntimeInstallationState.CANCELLED -> "Resume download"
                                state.installationState == PuRuntimeInstallationState.FAILED -> "Try download again"
                                else -> "Download runtime"
                            },
                            icon = Icons.Outlined.Download,
                            enabled = runtime.supported,
                            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                            onClick = { onAction(PuRuntimeDownloadAction.OnDownload) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceGateScreen(
    viewModel: PuLaunchCheckViewModel,
    onBack: () -> Unit,
    onRuntimeNeeded: (PuRuntimeSelection) -> Unit,
    onReady: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onAction(PuLaunchCheckAction.OnCheck)
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PuLaunchCheckEvent.RuntimeNeeded -> onRuntimeNeeded(event.selection)
            }
        }
    }

    PuDeviceGateScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onReady = onReady,
    )
}

@Composable
private fun PuDeviceGateScreenContent(
    state: PuLaunchCheckState,
    onAction: (PuLaunchCheckAction) -> Unit,
    onBack: () -> Unit,
    onReady: () -> Unit,
) {
    PuScreen(
        title = "Launch check",
        onBack = onBack,
    ) {
        when (val screenState = state.screenState) {
            PuLaunchCheckState.ScreenState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            is PuLaunchCheckState.ScreenState.Error -> {
                PuNotice(
                    text = screenState.message,
                    tone = NoticeTone.WARNING,
                )
                PuPrimaryButton(
                    text = "Try again",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    onClick = { onAction(PuLaunchCheckAction.OnCheck) },
                )
            }

            is PuLaunchCheckState.ScreenState.Blocked -> {
                PuLaunchAssessmentCard(state, screenState.assessment)
                PuLaunchChecksCard(screenState.assessment.checks)
                PuLaunchMessages(
                    warnings = screenState.assessment.warnings,
                    blockers = screenState.assessment.blockers,
                )
                PuPrimaryButton(
                    text = "Check again",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    onClick = { onAction(PuLaunchCheckAction.OnCheck) },
                )
                PuSecondaryButton(
                    text = "Back to game",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                    onClick = onBack,
                )
            }

            is PuLaunchCheckState.ScreenState.NeedsRuntime -> {
                PuLaunchAssessmentCard(state, screenState.assessment)
                PuLaunchChecksCard(screenState.assessment.checks)
                PuLaunchMessages(
                    warnings = screenState.assessment.warnings,
                    blockers = screenState.assessment.blockers,
                )
                PuNotice(
                    text = "The matching ${screenState.runtime.engine.engineLabel} ${screenState.runtime.versionFamily} runtime is not installed. Download it before launch.",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                )
                PuPrimaryButton(
                    text = "Download ${screenState.runtime.engine.engineLabel} ${screenState.runtime.versionFamily}",
                    icon = Icons.Outlined.Download,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    onClick = { onAction(PuLaunchCheckAction.OnDownloadRuntime) },
                )
                PuSecondaryButton(
                    text = "Back to game",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                    onClick = onBack,
                )
            }

            is PuLaunchCheckState.ScreenState.Ready -> {
                PuLaunchAssessmentCard(state, screenState.assessment)
                PuLaunchChecksCard(screenState.assessment.checks)
                PuLaunchMessages(
                    warnings = screenState.assessment.warnings,
                    blockers = screenState.assessment.blockers,
                )
                PuNotice(
                    text = "All required checks passed. PU prepared the game sandbox and has not started the game process yet.",
                    tone = NoticeTone.SUCCESS,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                )
                PuPrimaryButton(
                    text = "Continue to optimization",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    onClick = onReady,
                )
                PuSecondaryButton(
                    text = "Back to game",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                    onClick = onBack,
                )
            }
        }
    }
}

@Composable
private fun PuLaunchAssessmentCard(
    state: PuLaunchCheckState,
    assessment: PuLaunchAssessment,
) {
    val game = state.game
    PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.xs)) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = game?.title ?: "Game",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = game?.let { "${it.engine.engineLabel} ${it.version}" }.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                )
            }
            PuMetric(
                value = assessment.estimate.label,
                modifier = Modifier.padding(top = MaterialTheme.spacing.md),
            )
            Text(
                text = "Compatibility was checked on this phone before launch.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
            )
        }
    }
}

@Composable
private fun PuLaunchChecksCard(checks: List<PuCapabilityCheck>) {
    PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                PuLabel("Device requirements", modifier = Modifier.weight(1f))
                PuBody("Checked before launch")
            }
            checks.forEach { check ->
                PuGateStep(
                    label = check.label,
                    detail = check.detail,
                    passed = check.passed,
                )
            }
        }
    }
}

@Composable
private fun PuLaunchMessages(
    warnings: List<String>,
    blockers: List<String>,
) {
    if (warnings.isNotEmpty()) {
        PuNotice(
            text = warnings.joinToString(separator = "\n\n"),
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
        )
    }
    if (blockers.isNotEmpty()) {
        PuNotice(
            text = blockers.joinToString(separator = "\n\n"),
            tone = NoticeTone.WARNING,
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
        )
    }
}

@Composable
private fun PuGateStep(label: String, detail: String, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Surface(
            shape = CircleShape,
            color = if (passed) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            },
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (passed) Icons.Outlined.Check else Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = if (passed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(MaterialTheme.spacing.xs),
                )
            }
        }
        PuLabel(label, modifier = Modifier.weight(1f))
        PuBody(detail)
    }
}

private val PuCompatibilityEstimate.label: String
    get() = when (this) {
        PuCompatibilityEstimate.LIKELY_SMOOTH -> "Smooth"
        PuCompatibilityEstimate.MIGHT_LAG -> "May lag"
        PuCompatibilityEstimate.PROBABLY_WONT_RUN -> "Blocked"
    }

@Composable
fun RuntimesScreen(
    viewModel: PuRuntimesViewModel,
    onDownload: (PuRuntimeInfo) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            val message = when (event) {
                PuRuntimesEvent.CatalogUpdated -> "Runtime catalog updated"
                is PuRuntimesEvent.ShowMessage -> event.message
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    PuRuntimesScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onDownload = onDownload,
        onBack = onBack,
    )
}

@Composable
private fun PuRuntimesScreenContent(
    state: PuRuntimesState,
    onAction: (PuRuntimesAction) -> Unit,
    onDownload: (PuRuntimeInfo) -> Unit,
    onBack: (() -> Unit)?,
) {
    PuScreen(
        title = "Runtimes",
        onBack = onBack,
    ) {
        PuBody("Downloaded once and shared by every game. PU verifies each signed package before use.")
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = MaterialTheme.spacing.md, bottom = MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            PuRuntimeFilter.entries.forEach { filter ->
                val selected = filter == state.filter
                Surface(
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onAction(PuRuntimesAction.OnFilterSelected(filter)) },
                            )
                            .heightIn(min = 44.dp)
                            .padding(horizontal = MaterialTheme.spacing.base, vertical = MaterialTheme.spacing.sm),
                    )
                }
            }
        }

        when (state.screenState) {
            PuRuntimesState.ScreenState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            PuRuntimesState.ScreenState.Error -> PuNotice(
                text = state.errorMessage ?: "PU could not read the runtime catalog on this device.",
                tone = NoticeTone.WARNING,
            )

            PuRuntimesState.ScreenState.Success -> {
                val visibleRuntimes = state.visibleRuntimes
                if (visibleRuntimes.isEmpty()) {
                    PuCard {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                            PuLabel("No runtime releases found")
                            PuBody(
                                text = if (state.runtimes.isEmpty()) {
                                    "No runtime releases are published in this build yet. A signed catalog entry makes a new release available without an app update."
                                } else {
                                    "No installed runtime matches this filter."
                                },
                                modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                            )
                        }
                    }
                } else {
                    visibleRuntimes.forEach { runtime ->
                        PuRuntimeRow(
                            runtime = runtime,
                            onDownload = { onDownload(runtime) },
                            onRemove = {
                                onAction(PuRuntimesAction.OnRemove(runtime.runtimeId, runtime.revision))
                            },
                            modifier = Modifier.padding(bottom = MaterialTheme.spacing.md),
                        )
                    }
                }
            }
        }

        PuSecondaryButton(
            text = if (state.isRefreshingCatalog) "Updating catalog…" else "Update runtime catalog",
            icon = Icons.Outlined.Security,
            enabled = !state.isRefreshingCatalog,
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            onClick = { onAction(PuRuntimesAction.OnRefreshCatalog) },
        )

        PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.sm)) {
            Row(
                modifier = Modifier.padding(MaterialTheme.spacing.base),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                    Icon(
                        Icons.Outlined.Security,
                        contentDescription = null,
                        modifier = Modifier.padding(MaterialTheme.spacing.md).size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    PuLabel("Signed and verified")
                    PuBody(
                        "Corrupted packages are rejected. Failed updates never replace a working runtime.",
                        modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun PuRuntimeRow(
    runtime: PuRuntimeInfo,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PuCard(
        modifier = modifier,
        onClick = if (!runtime.installed && runtime.supported) onDownload else null,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                Icon(
                    imageVector = when {
                        runtime.installed -> Icons.Outlined.Check
                        !runtime.supported -> Icons.Outlined.WarningAmber
                        else -> Icons.Outlined.Download
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(MaterialTheme.spacing.md).size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                PuLabel(runtime.name)
                PuBody(
                    text = when {
                        runtime.installed -> "${formatBytes(runtime.installedBytes)} installed · shared runtime"
                        !runtime.supported -> runtime.requirement
                        else -> "${formatBytes(runtime.compressedBytes)} download · ${runtime.requirement}"
                    },
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                )
            }
            if (!runtime.installed && runtime.supported) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                    Row(
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Download", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else if (runtime.installed) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove ${runtime.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)) {
                    Text(
                        text = "Blocked",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
fun WindowsModuleScreen(
    config: PuEditionConfig,
    onBack: () -> Unit,
) {
    PuScreen(
        title = "Windows games",
        onBack = onBack,
    ) {
        PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.xs)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Wine / Box64 module",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                        modifier = Modifier.weight(1f),
                    )
                    PuEditionBadge(config, inverse = true)
                }
                PuMetric(
                    value = if (config.windowsIncluded) "Included" else "Optional",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                )
                Text(
                    text = if (config.windowsIncluded) {
                        "The module is included and ready for supported 2D games."
                    } else {
                        "Experimental on 3 GB phones. Only simple 2D games are likely to run well."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                )
            }
        }

        PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PuLabel("Device check", modifier = Modifier.weight(1f))
                    PuBody("Before download")
                }
                PuSampleData.deviceChecks.forEach { check ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (check.state) {
                                StepState.DONE -> MaterialTheme.signalColors.readyContainer
                                StepState.WARNING -> MaterialTheme.signalColors.warningContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (check.state == StepState.DONE) Icons.Outlined.Check else Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = when (check.state) {
                                        StepState.DONE -> MaterialTheme.signalColors.onReadyContainer
                                        StepState.WARNING -> MaterialTheme.signalColors.onWarningContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(MaterialTheme.spacing.xs),
                                )
                            }
                        }
                        PuLabel(check.label, modifier = Modifier.weight(1f))
                        PuBody(check.detail)
                    }
                }
            }
        }

        PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PuBody("Compressed download", modifier = Modifier.weight(1f))
                    PuLabel("Up to 100 MB")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.md),
                ) {
                    PuBody("Unpacked storage", modifier = Modifier.weight(1f))
                    PuLabel("About 280 MB")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Wifi, contentDescription = null, modifier = Modifier.size(17.dp))
                    PuBody("Wi-Fi recommended", modifier = Modifier.padding(start = MaterialTheme.spacing.sm))
                }
            }
        }

        PuPrimaryButton(
            text = if (config.windowsIncluded) "Open module settings" else "Download module",
            icon = if (config.windowsIncluded) Icons.Outlined.Monitor else Icons.Outlined.Download,
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
        )
        PuSecondaryButton(
            text = "Remove module",
            icon = Icons.Outlined.DeleteOutline,
            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
        )
    }
}
