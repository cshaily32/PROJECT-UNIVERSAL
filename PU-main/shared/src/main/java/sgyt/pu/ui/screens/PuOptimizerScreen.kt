package sgyt.pu.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
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
import sgyt.pu.core.model.PuAssetKind
import sgyt.pu.core.model.PuOptimizationManifest
import sgyt.pu.model.PuProfile
import sgyt.pu.ui.components.NoticeTone
import sgyt.pu.ui.components.PuBody
import sgyt.pu.ui.components.PuGlassCard
import sgyt.pu.ui.components.PuLabel
import sgyt.pu.ui.components.PuMetric
import sgyt.pu.ui.components.PuNotice
import sgyt.pu.ui.components.PuPrimaryButton
import sgyt.pu.ui.components.PuProgressBar
import sgyt.pu.ui.components.PuScreen
import sgyt.pu.ui.components.PuSecondaryButton
import sgyt.pu.ui.model.formatBytes
import sgyt.pu.ui.theme.PuTheme
import sgyt.pu.ui.viewmodels.PuOptimizerAction
import sgyt.pu.ui.viewmodels.PuOptimizerEvent
import sgyt.pu.ui.viewmodels.PuOptimizerPhase
import sgyt.pu.ui.viewmodels.PuOptimizerState
import sgyt.pu.ui.viewmodels.PuOptimizerViewModel
import kotlin.math.roundToInt

@Composable
fun OptimizingScreen(
    viewModel: PuOptimizerViewModel,
    onBack: () -> Unit,
    onCompatibility: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PuOptimizerEvent.Completed -> {
                    Toast.makeText(context, "Optimization complete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    PuOptimizerScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onCompatibility = onCompatibility,
    )
}

@Composable
internal fun PuOptimizerScreenContent(
    state: PuOptimizerState,
    onAction: (PuOptimizerAction) -> Unit,
    onBack: () -> Unit,
    onCompatibility: () -> Unit,
) {
    PuScreen(
        title = "Optimizing",
        onBack = onBack,
    ) {
        PuBody(state.gameTitle.ifBlank { "Game" })
        Text(
            text = when (state.phase) {
                PuOptimizerPhase.IDLE -> "Build a faster copy"
                PuOptimizerPhase.RUNNING -> "Optimizing assets"
                PuOptimizerPhase.PAUSED -> "Optimization paused"
                PuOptimizerPhase.COMPLETED -> "Optimization complete"
                PuOptimizerPhase.ERROR -> "Optimization could not finish"
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 3.dp),
        )

        PuPrimaryCard(modifier = Modifier.padding(top = 20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PuBody(
                        if (state.totalAssets > 0) {
                            "${state.completedAssets} / ${state.totalAssets} assets"
                        } else {
                            "Ready to scan local files"
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = state.profile.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                    )
                }
                PuMetric(
                    value = "${(state.progress * 100).roundToInt()}%",
                    modifier = Modifier.padding(top = 10.dp),
                )
                PuProgressBar(
                    progress = state.progress,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    PuBody(
                        if (state.savedBytes > 0) "${formatBytes(state.savedBytes)} smaller" else "No size change yet",
                        modifier = Modifier.weight(1f),
                    )
                    PuBody("${state.currentAsset.substringAfterLast('/').ifBlank { "Waiting" }}")
                }
            }
        }

        state.errorMessage?.let { message ->
            PuNotice(
                text = message,
                tone = if (state.phase == PuOptimizerPhase.ERROR) NoticeTone.WARNING else NoticeTone.INFO,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        PuGlassCard(modifier = Modifier.padding(top = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PuLabel("Cache summary", modifier = Modifier.weight(1f))
                    PuBody(if (state.manifest == null) "No cache" else "Resumable")
                }
                PuOptimizerStat(
                    label = "Original files scanned",
                    value = if (state.sourceBytes > 0) formatBytes(state.sourceBytes) else "—",
                )
                PuOptimizerStat(
                    label = "Optimized cache",
                    value = if (state.cacheBytes > 0) formatBytes(state.cacheBytes) else "—",
                )
                PuOptimizerStat(
                    label = "Assets optimized",
                    value = state.manifest?.optimizedAssets?.toString() ?: "0",
                )
                PuOptimizerStat(
                    label = "Original kept",
                    value = state.manifest?.keptOriginalAssets?.toString() ?: "0",
                )
            }
        }

        PuGlassCard(modifier = Modifier.padding(top = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PuLabel("Asset types", modifier = Modifier.weight(1f))
                    PuBody("On-device")
                }
                PuOptimizerAssetRow(
                    label = "Images",
                    count = state.manifest?.assetCount(PuAssetKind.IMAGE) ?: 0,
                    active = state.currentAsset.substringAfterLast('/').substringAfterLast('.').equals(
                        "png",
                        ignoreCase = true,
                    ) || state.currentAsset.substringAfterLast('/').substringAfterLast('.').equals(
                        "jpg",
                        ignoreCase = true,
                    ) || state.currentAsset.substringAfterLast('/').substringAfterLast('.').equals(
                        "jpeg",
                        ignoreCase = true,
                    ) || state.currentAsset.substringAfterLast('/').substringAfterLast('.').equals(
                        "webp",
                        ignoreCase = true,
                    ),
                )
                PuOptimizerAssetRow(
                    label = "Video",
                    count = state.manifest?.assetCount(PuAssetKind.VIDEO) ?: 0,
                    active = false,
                )
                PuOptimizerAssetRow(
                    label = "Audio",
                    count = state.manifest?.assetCount(PuAssetKind.AUDIO) ?: 0,
                    active = false,
                )
            }
        }

        PuNotice(
            text = when (state.originalFilesUnchanged) {
                true -> "Original game files were verified unchanged. Generated images stay in the app cache and are used only when they are smaller than the source file."
                false -> "PU stopped because an original game file changed during optimization. Review the imported files before trying again."
                null -> "Original game files stay untouched. Generated images stay in the app cache and are used only when they are smaller than the source file."
            },
            tone = when (state.originalFilesUnchanged) {
                true -> NoticeTone.SUCCESS
                false -> NoticeTone.WARNING
                null -> NoticeTone.INFO
            },
            modifier = Modifier.padding(top = 16.dp),
        )

        when (state.phase) {
            PuOptimizerPhase.IDLE -> PuPrimaryButton(
                text = "Start optimization",
                icon = Icons.Outlined.PlayArrow,
                modifier = Modifier.padding(top = 16.dp),
                onClick = { onAction(PuOptimizerAction.OnStart) },
            )

            PuOptimizerPhase.RUNNING -> PuSecondaryButton(
                text = "Pause optimization",
                icon = Icons.Outlined.Pause,
                modifier = Modifier.padding(top = 16.dp),
                onClick = { onAction(PuOptimizerAction.OnPause) },
            )

            PuOptimizerPhase.PAUSED -> PuPrimaryButton(
                text = "Resume optimization",
                icon = Icons.Outlined.PlayArrow,
                modifier = Modifier.padding(top = 16.dp),
                onClick = { onAction(PuOptimizerAction.OnResume) },
            )

            PuOptimizerPhase.COMPLETED -> PuPrimaryButton(
                text = "Optimize again",
                icon = Icons.Outlined.PlayArrow,
                modifier = Modifier.padding(top = 16.dp),
                onClick = { onAction(PuOptimizerAction.OnStart) },
            )

            PuOptimizerPhase.ERROR -> PuPrimaryButton(
                text = "Try again",
                icon = Icons.Outlined.PlayArrow,
                modifier = Modifier.padding(top = 16.dp),
                onClick = { onAction(PuOptimizerAction.OnStart) },
            )
        }

        PuSecondaryButton(
            text = "Compatibility fixes",
            modifier = Modifier.padding(top = 10.dp),
            onClick = onCompatibility,
        )
    }
}

@Composable
private fun PuOptimizerStat(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
    ) {
        PuBody(label, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PuOptimizerAssetRow(label: String, count: Int, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (active) "…" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        PuLabel(label, modifier = Modifier.weight(1f))
        PuBody(count.toString())
    }
}

private fun PuOptimizationManifest.assetCount(kind: PuAssetKind): Int = assets.count { it.kind == kind }

@Preview(showBackground = true)
@Composable
private fun PuOptimizerScreenPreview() {
    val manifest = PuOptimizationManifest(
        gameId = "preview",
        profile = PuProfile.LOW,
        updatedAt = 0,
        assets = emptyList(),
    )
    PuTheme {
        PuOptimizerScreenContent(
            state = PuOptimizerState(
                phase = PuOptimizerPhase.IDLE,
                gameTitle = "Lantern Season",
                profile = PuProfile.LOW,
                manifest = manifest,
            ),
            onAction = {},
            onBack = {},
            onCompatibility = {},
        )
    }
}
