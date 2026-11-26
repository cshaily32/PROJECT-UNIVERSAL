package sgyt.pu.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sgyt.pu.core.compatibility.PuCompatibilityCatalog
import sgyt.pu.core.library.PuGameLibrary
import sgyt.pu.core.model.PuFixProfile

data class PuCompatibilityState(
    val screenState: ScreenState = ScreenState.Loading,
    val gameTitle: String = "",
    val profiles: List<PuFixProfile> = emptyList(),
    val appliedFixIds: Set<String> = emptySet(),
    val applyingFixId: String? = null,
    val isRefreshingCatalog: Boolean = false,
    val errorMessage: String? = null,
) {
    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Success : ScreenState
        data object Error : ScreenState
    }
}

sealed interface PuCompatibilityAction {
    data class OnApply(val profileId: String) : PuCompatibilityAction
    data object OnRefresh : PuCompatibilityAction
    data object OnRefreshCatalog : PuCompatibilityAction
}

sealed interface PuCompatibilityEvent {
    data class FixApplied(val profileTitle: String) : PuCompatibilityEvent
    data object CatalogUpdated : PuCompatibilityEvent
    data class ShowMessage(val message: String) : PuCompatibilityEvent
}

/** Loads matching offline fix profiles and applies them without changing game files. */
class PuCompatibilityViewModel(
    private val gameId: String,
    private val library: PuGameLibrary,
    private val catalog: PuCompatibilityCatalog,
) : ViewModel() {

    private val _state = MutableStateFlow(PuCompatibilityState())
    val state = _state.asStateFlow()

    private val _events = Channel<PuCompatibilityEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun onAction(action: PuCompatibilityAction) {
        when (action) {
            is PuCompatibilityAction.OnApply -> apply(action.profileId)
            PuCompatibilityAction.OnRefresh -> refresh()
            PuCompatibilityAction.OnRefreshCatalog -> refreshCatalog()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    screenState = PuCompatibilityState.ScreenState.Loading,
                    errorMessage = null,
                )
            }
            try {
                val game = withContext(Dispatchers.IO) { library.getGame(gameId) }
                    ?: throw IllegalStateException("The game is no longer in the library")
                val profiles = withContext(Dispatchers.IO) { catalog.findMatches(game) }
                _state.update {
                    it.copy(
                        screenState = PuCompatibilityState.ScreenState.Success,
                        gameTitle = game.title.ifBlank { "Untitled game" },
                        profiles = profiles,
                        appliedFixIds = game.appliedFixIds.toSet(),
                        applyingFixId = null,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Compatibility load failed", e)
                _state.update {
                    it.copy(
                        screenState = PuCompatibilityState.ScreenState.Error,
                        applyingFixId = null,
                        errorMessage = "PU could not read the offline compatibility database.",
                    )
                }
            }
        }
    }

    private fun refreshCatalog() {
        if (_state.value.isRefreshingCatalog) return
        _state.update { it.copy(isRefreshingCatalog = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { catalog.refreshFromApi() }
                refresh()
                _events.send(PuCompatibilityEvent.CatalogUpdated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Compatibility catalog refresh failed", e)
                _events.send(
                    PuCompatibilityEvent.ShowMessage(
                        "The compatibility database could not be updated. Check the API URL and signing key.",
                    ),
                )
            } finally {
                _state.update { it.copy(isRefreshingCatalog = false) }
            }
        }
    }

    private fun apply(profileId: String) {
        if (_state.value.applyingFixId != null) return
        val profile = _state.value.profiles.firstOrNull { it.id == profileId } ?: return
        _state.update { it.copy(applyingFixId = profileId, errorMessage = null) }
        viewModelScope.launch {
            try {
                val game = withContext(Dispatchers.IO) { library.getGame(gameId) }
                    ?: throw IllegalStateException("The game is no longer in the library")
                val applied = withContext(Dispatchers.IO) { catalog.apply(game, profile) }
                if (!applied) throw IllegalStateException("The fix profile could not be applied")
                val updated = withContext(Dispatchers.IO) { library.getGame(gameId) }
                _state.update {
                    it.copy(
                        appliedFixIds = updated?.appliedFixIds?.toSet() ?: (it.appliedFixIds + profileId),
                        applyingFixId = null,
                        errorMessage = null,
                    )
                }
                _events.send(PuCompatibilityEvent.FixApplied(profile.title))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Compatibility fix failed", e)
                _state.update {
                    it.copy(
                        applyingFixId = null,
                        errorMessage = "PU could not apply this fix profile. The original game files were not changed.",
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "PuCompatibility"
    }
}
