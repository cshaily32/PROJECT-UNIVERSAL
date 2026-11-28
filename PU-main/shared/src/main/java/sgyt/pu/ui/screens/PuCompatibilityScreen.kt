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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
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
import sgyt.pu.ui.theme.signalColors
import sgyt.pu.ui.theme.spacing
import kotlinx.coroutines.flow.collectLatest
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuFixProfile
import sgyt.pu.ui.components.NoticeTone
import sgyt.pu.ui.components.PuBody
import sgyt.pu.ui.components.PuCard
import sgyt.pu.ui.components.PuLabel
import sgyt.pu.ui.components.PuNotice
import sgyt.pu.ui.components.PuPrimaryCard
import sgyt.pu.ui.components.PuScreen
import sgyt.pu.ui.components.PuSecondaryButton
import sgyt.pu.ui.theme.PuTheme
import sgyt.pu.ui.viewmodels.PuCompatibilityAction
import sgyt.pu.ui.viewmodels.PuCompatibilityEvent
import sgyt.pu.ui.viewmodels.PuCompatibilityState
import sgyt.pu.ui.viewmodels.PuCompatibilityViewModel

@Composable
fun CompatibilityScreen(
    viewModel: PuCompatibilityViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PuCompatibilityEvent.FixApplied -> {
                    Toast.makeText(context, "${event.profileTitle} applied", Toast.LENGTH_SHORT).show()
                }
                PuCompatibilityEvent.CatalogUpdated -> {
                    Toast.makeText(context, "Compatibility database updated", Toast.LENGTH_SHORT).show()
                }
                is PuCompatibilityEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    PuCompatibilityScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}

@Composable
internal fun PuCompatibilityScreenContent(
    state: PuCompatibilityState,
    onAction: (PuCompatibilityAction) -> Unit,
    onBack: () -> Unit,
) {
    PuScreen(
        title = "Compatibility fixes",
        onBack = onBack,
    ) {
        when (state.screenState) {
            PuCompatibilityState.ScreenState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            PuCompatibilityState.ScreenState.Error -> {
                PuNotice(
                    text = state.errorMessage ?: "PU could not read the compatibility database.",
                    tone = NoticeTone.WARNING,
                )
                PuSecondaryButton(
                    text = "Try again",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    onClick = { onAction(PuCompatibilityAction.OnRefresh) },
                )
            }

            PuCompatibilityState.ScreenState.Success -> {
                PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.xs)) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Offline compatibility database",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${state.profiles.size} match${if (state.profiles.size == 1) "" else "es"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                            )
                        }
                        Text(
                            text = state.gameTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 2,
                            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                        )
                        Text(
                            text = "A fix profile writes runtime settings inside this game's sandbox. The original game files are never changed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                        )
                    }
                }

                PuSecondaryButton(
                    text = if (state.isRefreshingCatalog) "Updating database…" else "Update signed database",
                    icon = Icons.Outlined.Build,
                    enabled = !state.isRefreshingCatalog,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    onClick = { onAction(PuCompatibilityAction.OnRefreshCatalog) },
                )

                if (state.profiles.isEmpty()) {
                    PuNotice(
                        text = "No matching fix profile is available for this engine and version. Try a lower optimization profile from the game details screen.",
                        modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    )
                } else {
                    PuLabel("Available fixes", modifier = Modifier.padding(top = MaterialTheme.spacing.lg))
                    state.profiles.forEach { profile ->
                        val applied = profile.id in state.appliedFixIds
                        val applying = state.applyingFixId == profile.id
                        PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.md)) {
                            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                                ) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                                        Icon(
                                            Icons.Outlined.Build,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(MaterialTheme.spacing.md)
                                                .size(18.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        PuLabel(profile.title)
                                        PuBody(
                                            text = profile.settings.entries.joinToString(" · ") { entry ->
                                                "${entry.key}=${entry.value}"
                                            }.ifBlank { "Runtime compatibility settings" },
                                            modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                                        )
                                    }
                                    if (applied) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = "Applied",
                                            tint = MaterialTheme.signalColors.onReadyContainer,
                                        )
                                    }
                                }
                                PuSecondaryButton(
                                    text = when {
                                        applied -> "Applied"
                                        applying -> "Applying…"
                                        else -> "Apply fix"
                                    },
                                    icon = if (applied) Icons.Outlined.Check else Icons.Outlined.Build,
                                    enabled = !applied && !applying,
                                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                                    onClick = { onAction(PuCompatibilityAction.OnApply(profile.id)) },
                                )
                            }
                        }
                    }
                }

                state.errorMessage?.let { message ->
                    PuNotice(
                        text = message,
                        tone = NoticeTone.WARNING,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    )
                }

                PuNotice(
                    text = "Downloaded databases are accepted only when their signature matches the configured PU trust key. The bundled database remains available offline.",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PuCompatibilityScreenPreview() {
    val profile = PuFixProfile(
        id = "renpy-8-low-memory",
        title = "Ren'Py 8.x on low-memory devices",
        engine = PuEngineType.RENPY,
        versionFamily = "8",
        fingerprint = null,
        settings = mapOf("imageCacheMb" to "256", "predictiveLoading" to "false"),
        revision = 1,
        signature = null,
    )
    PuTheme {
        PuCompatibilityScreenContent(
            state = PuCompatibilityState(
                screenState = PuCompatibilityState.ScreenState.Success,
                gameTitle = "Hollow Bell",
                profiles = listOf(profile),
            ),
            onAction = {},
            onBack = {},
        )
    }
}
