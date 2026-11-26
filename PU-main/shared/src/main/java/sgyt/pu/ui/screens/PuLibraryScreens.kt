package sgyt.pu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.model.PuEditionConfig
import sgyt.pu.model.PuGame
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
import sgyt.pu.ui.components.PuTruncatedTitle
import sgyt.pu.ui.model.toPresentation
import sgyt.pu.ui.viewmodels.PuLibraryAction
import sgyt.pu.ui.viewmodels.PuLibraryFilter
import sgyt.pu.ui.viewmodels.PuLibraryState
import sgyt.pu.ui.viewmodels.PuLibraryViewModel

@Composable
fun LibraryScreen(
    config: PuEditionConfig,
    viewModel: PuLibraryViewModel,
    onOpenGame: (PuGameRecord) -> Unit,
    onAddGame: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PuLibraryScreenContent(
        state = state,
        config = config,
        onAction = viewModel::onAction,
        onOpenGame = onOpenGame,
        onAddGame = onAddGame,
    )
}

@Composable
private fun PuLibraryScreenContent(
    state: PuLibraryState,
    config: PuEditionConfig,
    onAction: (PuLibraryAction) -> Unit,
    onOpenGame: (PuGameRecord) -> Unit,
    onAddGame: () -> Unit,
) {
    PuScreen(
        title = "Library",
        trailing = { PuEditionBadge(config) },
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { onAction(PuLibraryAction.OnQueryChanged(it)) },
            placeholder = { Text("Search your games") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PuLibraryFilter.entries.forEach { filter ->
                val selected = filter == state.filter
                Surface(
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                    },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
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
                            .clickable { onAction(PuLibraryAction.OnFilterSelected(filter)) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }

        when (state.screenState) {
            PuLibraryState.ScreenState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            PuLibraryState.ScreenState.Error -> PuNotice(
                text = state.errorMessage ?: "PU could not read the library on this device.",
                tone = NoticeTone.WARNING,
            )

            PuLibraryState.ScreenState.Success -> {
                val visibleGames = state.visibleGames
                if (visibleGames.isEmpty()) {
                    PuEmptyLibrary(onAddGame = onAddGame)
                } else {
                    visibleGames.chunked(2).forEach { rowGames ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowGames.forEach { record ->
                                val game = record.toPresentation()
                                PuGameTile(
                                    game = game,
                                    onClick = { onOpenGame(record) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowGames.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        PuPrimaryButton(
            text = "Add game",
            icon = Icons.Outlined.Add,
            modifier = Modifier.padding(top = 16.dp),
            onClick = onAddGame,
        )
    }
}

@Composable
private fun PuEmptyLibrary(onAddGame: () -> Unit) {
    PuGlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            PuLabel("No games yet")
            PuBody(
                text = "Add a Ren'Py or RPG Maker game from this phone. PU copies it into app storage, detects the engine and version, and leaves the original files untouched.",
                modifier = Modifier.padding(top = 6.dp),
            )
            PuNotice(
                text = "Detection and optimization run on this device. PU never uploads your game files.",
                modifier = Modifier.padding(top = 14.dp),
            )
            PuPrimaryButton(
                text = "Add game",
                icon = Icons.Outlined.Add,
                modifier = Modifier.padding(top = 16.dp),
                onClick = onAddGame,
            )
        }
    }
}

@Composable
private fun PuGameTile(
    game: PuGame,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PuGlassCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(8.dp)) {
            PuGameCover(
                art = game.art,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f),
            )
            PuTruncatedTitle(
                text = game.title,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp),
            )
            PuBody(
                text = "${game.engine} ${game.version}",
                modifier = Modifier.padding(start = 4.dp, top = 3.dp),
            )
            Box(modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)) {
                PuCompatibilityChip(game.compatibility)
            }
        }
    }
}
