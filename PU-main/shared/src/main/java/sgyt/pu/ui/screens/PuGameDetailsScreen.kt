package sgyt.pu.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.WarningAmber

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.model.PuEditionConfig
import sgyt.pu.model.PuProfile
import sgyt.pu.ui.components.NoticeTone
import sgyt.pu.ui.components.PuBody
import sgyt.pu.ui.components.PuCompatibilityChip
import sgyt.pu.ui.components.PuEditionBadge
import sgyt.pu.ui.components.PuGameCover
import sgyt.pu.ui.components.PuGlassCard
import sgyt.pu.ui.components.PuLabel
import sgyt.pu.ui.components.PuNotice
import sgyt.pu.ui.components.PuPrimaryButton
import sgyt.pu.ui.components.PuScreen
import sgyt.pu.ui.components.PuSecondaryButton
import sgyt.pu.ui.components.PuSegmentedControl
import sgyt.pu.ui.components.PuSettingRow
import sgyt.pu.ui.model.formatBytes
import sgyt.pu.ui.model.toPresentation
import sgyt.pu.ui.theme.PuTheme
import sgyt.pu.ui.viewmodels.PuGameDetailsAction
import sgyt.pu.ui.viewmodels.PuGameDetailsEvent
import sgyt.pu.ui.viewmodels.PuGameDetailsState
import sgyt.pu.ui.viewmodels.PuGameDetailsViewModel

@Composable
fun GameScreen(
    config: PuEditionConfig,
    viewModel: PuGameDetailsViewModel,
    onBack: () -> Unit,
    onOptimize: () -> Unit,
    onCompatibility: () -> Unit,
    onControls: () -> Unit,
    onSaves: () -> Unit,
    onPlay: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PuGameDetailsEvent.CacheCleared -> {
                    Toast.makeText(context, "Optimized cache cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    PuGameDetailsScreenContent(
        state = state,
        config = config,
        onAction = viewModel::onAction,
        onBack = onBack,
        onOptimize = onOptimize,
        onCompatibility = onCompatibility,
        onControls = onControls,
        onSaves = onSaves,
        onPlay = onPlay,
    )
}

@Composable
internal fun PuGameDetailsScreenContent(
    state: PuGameDetailsState,
    config: PuEditionConfig,
    onAction: (PuGameDetailsAction) -> Unit,
    onBack: () -> Unit,
    onOptimize: () -> Unit,
    onCompatibility: () -> Unit,
    onControls: () -> Unit,
    onSaves: () -> Unit,
    onPlay: () -> Unit,
) {
    PuScreen(
        title = "Game details",
        onBack = onBack,
        trailing = { PuEditionBadge(config) },
    ) {
        when (state.screenState) {
            PuGameDetailsState.ScreenState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            PuGameDetailsState.ScreenState.Error -> {
                PuNotice(
                    text = state.errorMessage ?: "PU could not read this game.",
                    tone = NoticeTone.WARNING,
                )
                PuPrimaryButton(
                    text = "Try again",
                    modifier = Modifier.padding(top = 18.dp),
                    onClick = { onAction(PuGameDetailsAction.OnRefresh) },
                )
            }

            PuGameDetailsState.ScreenState.Success -> {
                val record = state.game ?: return@PuScreen
                val game = record.toPresentation()

                PuGameCover(
                    art = game.art,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = game.title, style = MaterialTheme.typography.headlineMedium)
                        PuBody(
                            text = "${game.engine} ${game.version} · ${game.originalSize} original",
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PuCompatibilityChip(game.compatibility)
                    Spacer(Modifier.width(10.dp))
                    PuBody(
                        if (state.cacheSizeBytes > 0) {
                            "${formatBytes(state.cacheSizeBytes)} optimized"
                        } else {
                            "No optimized cache"
                        },
                    )
                }

                PuNotice(
                    text = "PU checks this phone's Android version, graphics, memory, storage, and matching runtime before the game can start.",
                    modifier = Modifier.padding(top = 18.dp),
                )

                PuLabel("Performance profile", modifier = Modifier.padding(top = 22.dp))
                PuSegmentedControl(
                    options = PuProfile.entries.map { it.displayName },
                    selectedIndex = PuProfile.entries.indexOf(state.selectedProfile),
                    modifier = Modifier.padding(top = 8.dp),
                    onSelected = { index ->
                        onAction(PuGameDetailsAction.OnProfileSelected(PuProfile.entries[index]))
                    },
                )
                PuBody(
                    text = when (state.selectedProfile) {
                        PuProfile.ULTRA_LOW -> "About 540–720p, lite media, smallest runtime cache"
                        PuProfile.LOW -> "720p assets and a capped runtime image cache"
                        PuProfile.BALANCED -> "Near-original quality with light runtime tuning"
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (state.manifest != null && !state.hasCurrentProfileCache) {
                    PuNotice(
                        text = "The existing cache uses the ${state.manifest.profile.displayName} profile. Run optimization again to rebuild it for ${state.selectedProfile.displayName}.",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                state.errorMessage?.let { message ->
                    PuNotice(
                        text = message,
                        tone = NoticeTone.WARNING,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                PuGlassCard(
                    onClick = onCompatibility,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp).size(18.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            PuLabel("Compatibility fixes")
                            PuBody(
                                "Check the signed offline fix database for this game.",
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    }
                }

                PuGlassCard(modifier = Modifier.padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PuSettingRow(
                            icon = Icons.Outlined.SportsEsports,
                            title = "Touch overlay and gamepad",
                            detail = "Map controls for keyboard-only games",
                            trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
                            onClick = onControls,
                        )
                        PuSettingRow(
                            icon = Icons.Outlined.Save,
                            title = "Save backup",
                            detail = if (config.cloudSyncEnabled) "Local and cloud sync" else "Local backup",
                            trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
                            onClick = onSaves,
                        )
                        PuSettingRow(
                            icon = Icons.Outlined.WarningAmber,
                            title = "Optimize assets",
                            detail = if (state.cacheSizeBytes > 0) {
                                "${formatBytes(state.cacheSizeBytes)} in the current cache"
                            } else {
                                "Build a non-destructive optimized cache"
                            },
                            trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
                            onClick = onOptimize,
                        )
                    }
                }

                if (state.cacheSizeBytes > 0) {
                    PuSecondaryButton(
                        text = if (state.isClearingCache) "Clearing cache…" else "Clear optimized cache",
                        icon = Icons.Outlined.DeleteOutline,
                        enabled = !state.isClearingCache,
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = { onAction(PuGameDetailsAction.OnClearCache) },
                    )
                }

                PuPrimaryButton(
                    text = "Play",
                    modifier = Modifier.padding(top = 20.dp),
                    onClick = onPlay,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PuGameDetailsScreenPreview() {
    val game = PuGameRecord(
        id = "preview",
        rootPath = "/preview",
        title = "Lantern Season",
        engine = PuEngineType.RENPY,
        version = "8.6",
        fingerprint = "preview",
        importedAt = 0,
        updatedAt = 0,
        originalSizeBytes = 2_400_000_000,
        cacheSizeBytes = 900_000_000,
        selectedProfile = PuProfile.LOW,
    )
    PuTheme {
        PuGameDetailsScreenContent(
            state = PuGameDetailsState(
                screenState = PuGameDetailsState.ScreenState.Success,
                game = game,
                selectedProfile = PuProfile.LOW,
                cacheSizeBytes = 900_000_000,
            ),
            config = PuEditionConfig(
                edition = sgyt.pu.model.PuEdition.LITE,
                label = "PU Lite",
                defaultProfile = PuProfile.LOW,
                windowsIncluded = false,
                cloudSyncEnabled = false,
                translationEnabled = false,
                backgroundWorkEnabled = false,
            ),
            onAction = {},
            onBack = {},
            onOptimize = {},
            onCompatibility = {},
            onControls = {},
            onSaves = {},
            onPlay = {},
        )
    }
}
