package sgyt.pu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sgyt.pu.core.library.PuGameLibrary
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuGameRecord

/**
 * Presentation state for the library screen.
 *
 * The filtered list is derived from the stored games instead of being kept in a
 * second field, so the two can never drift apart.
 */
data class PuLibraryState(
    val screenState: ScreenState = ScreenState.Loading,
    val games: List<PuGameRecord> = emptyList(),
    val query: String = "",
    val filter: PuLibraryFilter = PuLibraryFilter.ALL,
    val errorMessage: String? = null,
) {
    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Success : ScreenState
        data object Error : ScreenState
    }

    val visibleGames: List<PuGameRecord>
        get() {
            val trimmedQuery = query.trim()
            return games.filter { game ->
                val matchesQuery = trimmedQuery.isEmpty() ||
                    game.title.contains(trimmedQuery, ignoreCase = true) ||
                    game.engine.name.contains(trimmedQuery, ignoreCase = true) ||
                    game.version.contains(trimmedQuery, ignoreCase = true)
                val matchesFilter = when (filter) {
                    PuLibraryFilter.ALL -> true
                    PuLibraryFilter.RENPY -> game.engine == PuEngineType.RENPY
                    PuLibraryFilter.RPG_MAKER -> game.engine in RPG_MAKER_ENGINES
                    PuLibraryFilter.WINDOWS -> game.engine in WINDOWS_ENGINES
                }
                matchesQuery && matchesFilter
            }
        }
}

enum class PuLibraryFilter(val label: String) {
    ALL("All"),
    RENPY("Ren'Py"),
    RPG_MAKER("RPG Maker"),
    WINDOWS(".exe"),
}

sealed interface PuLibraryAction {
    data class OnQueryChanged(val query: String) : PuLibraryAction

    data class OnFilterSelected(val filter: PuLibraryFilter) : PuLibraryAction

    data object OnRefresh : PuLibraryAction
}

private val RPG_MAKER_ENGINES = setOf(
    PuEngineType.RPG_MAKER_MV,
    PuEngineType.RPG_MAKER_MZ,
    PuEngineType.RPG_MAKER_XP,
    PuEngineType.RPG_MAKER_VX,
    PuEngineType.RPG_MAKER_ACE,
    PuEngineType.RPG_MAKER_2000,
    PuEngineType.RPG_MAKER_2003,
)

private val WINDOWS_ENGINES = setOf(
    PuEngineType.WOLF_RPG,
    PuEngineType.KIRIKIRI,
    PuEngineType.UNITY,
    PuEngineType.GENERIC_EXE,
)

/**
 * Loads the imported game library from app-owned storage.
 *
 * Every read and write runs on the IO dispatcher; the screen only observes the
 * published state.
 */
class PuLibraryViewModel(
    private val library: PuGameLibrary,
) : ViewModel() {

    private val _state = MutableStateFlow(PuLibraryState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun onAction(action: PuLibraryAction) {
        when (action) {
            is PuLibraryAction.OnQueryChanged -> _state.update { it.copy(query = action.query) }
            is PuLibraryAction.OnFilterSelected -> _state.update { it.copy(filter = action.filter) }
            PuLibraryAction.OnRefresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(screenState = PuLibraryState.ScreenState.Loading, errorMessage = null) }
            val games = try {
                withContext(Dispatchers.IO) { library.listGames() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        screenState = PuLibraryState.ScreenState.Error,
                        errorMessage = "PU could not read the library on this device.",
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    screenState = PuLibraryState.ScreenState.Success,
                    games = games,
                    errorMessage = null,
                )
            }
        }
    }
}
