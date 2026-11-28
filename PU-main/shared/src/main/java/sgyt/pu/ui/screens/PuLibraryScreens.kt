package sgyt.pu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.model.PuEditionConfig
import sgyt.pu.model.PuGame
import sgyt.pu.ui.components.NoticeTone
import sgyt.pu.ui.components.PuBody
import sgyt.pu.ui.components.PuCard
import sgyt.pu.ui.components.PuCompatibilityChip
import sgyt.pu.ui.components.PuEditionBadge
import sgyt.pu.ui.components.PuGameCover
import sgyt.pu.ui.components.PuLabel
import sgyt.pu.ui.components.PuNotice
import sgyt.pu.ui.components.PuPrimaryButton
import sgyt.pu.ui.components.PuScreen
import sgyt.pu.ui.components.PuTruncatedTitle
import sgyt.pu.ui.model.toPresentation
import sgyt.pu.ui.theme.spacing
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
        // The grid owns its own scrolling so covers stay lazy on low-memory phones.
        scrollable = false,
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { onAction(PuLibraryAction.OnQueryChanged(it)) },
            // The label carries the meaning. Material's placeholder drops to 60% opacity, which
            // is too faint to read, so the field uses a full-strength label and no placeholder.
            label = { Text("Search your games", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = MaterialTheme.spacing.md, bottom = MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            PuLibraryFilter.entries.forEach { filter ->
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
                                onClick = { onAction(PuLibraryAction.OnFilterSelected(filter)) },
                            )
                            .heightIn(min = 44.dp)
                            .padding(horizontal = MaterialTheme.spacing.base, vertical = MaterialTheme.spacing.sm),
                    )
                }
            }
        }

        when (state.screenState) {
            PuLibraryState.ScreenState.Loading -> PuLibraryLoadingGrid()

            PuLibraryState.ScreenState.Error -> PuNotice(
                text = state.errorMessage ?: "PU could not read the library on this device.",
                tone = NoticeTone.WARNING,
            )

            PuLibraryState.ScreenState.Success -> {
                val visibleGames = state.visibleGames
                if (visibleGames.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PuEmptyLibrary()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.base),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                    ) {
                        items(
                            items = visibleGames,
                            key = { record -> record.id },
                        ) { record ->
                            PuGameTile(
                                game = record.toPresentation(),
                                onClick = { onOpenGame(record) },
                            )
                        }
                    }
                }
            }
        }

        PuPrimaryButton(
            text = "Add game",
            icon = Icons.Outlined.Add,
            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
            onClick = onAddGame,
        )
    }
}

/**
 * Placeholder geometry for the loading state.
 *
 * It mirrors the tile shape so the layout does not jump when real covers arrive, without
 * inventing a game that is not there.
 */
@Composable
private fun ColumnScope.PuLibraryLoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.base),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        userScrollEnabled = false,
    ) {
        items(4) {
            PuCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .padding(MaterialTheme.spacing.xs),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun PuEmptyLibrary() {
    PuCard {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
            PuLabel("No games yet")
            PuBody(
                text = "Add a Ren'Py or RPG Maker game from this phone. PU copies it into app storage, detects the engine and version, and leaves the original files untouched.",
                modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
            )
            PuNotice(
                text = "Detection and optimization run on this device. PU never uploads your game files.",
                modifier = Modifier.padding(top = MaterialTheme.spacing.md),
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
    PuCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.sm)) {
            PuGameCover(
                art = game.art,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f),
            )
            PuTruncatedTitle(
                text = game.title,
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.xs,
                    top = MaterialTheme.spacing.sm,
                ),
            )
            PuBody(
                text = "${game.engine} ${game.version}",
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.xs,
                    top = MaterialTheme.spacing.xs,
                ),
            )
            Box(
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.xs,
                    top = MaterialTheme.spacing.sm,
                    bottom = MaterialTheme.spacing.xs,
                ),
            ) {
                PuCompatibilityChip(game.compatibility)
            }
        }
    }
}
