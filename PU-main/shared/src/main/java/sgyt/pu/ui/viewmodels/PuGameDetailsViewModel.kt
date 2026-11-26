package sgyt.pu.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sgyt.pu.core.device.PuDeviceProbe
import sgyt.pu.core.library.PuGameLibrary
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.core.model.PuOptimizationManifest
import sgyt.pu.core.optimizer.PuOptimizer
import sgyt.pu.model.PuProfile

data class PuGameDetailsState(
    val screenState: ScreenState = ScreenState.Loading,
    val game: PuGameRecord? = null,
    val selectedProfile: PuProfile = PuProfile.LOW,
    val manifest: PuOptimizationManifest? = null,
    val cacheSizeBytes: Long = 0,
    val isClearingCache: Boolean = false,
    val errorMessage: String? = null,
) {
    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Success : ScreenState
        data object Error : ScreenState
    }

    val hasCurrentProfileCache: Boolean get() = manifest?.profile == selectedProfile
}

sealed interface PuGameDetailsAction {
    data class OnProfileSelected(val profile: PuProfile) : PuGameDetailsAction
    data object OnClearCache : PuGameDetailsAction
    data object OnRefresh : PuGameDetailsAction
}

sealed interface PuGameDetailsEvent {
    data object CacheCleared : PuGameDetailsEvent
}

/** Loads the real game record and persists the per-game optimization override. */
class PuGameDetailsViewModel(
    private val gameId: String,
    private val library: PuGameLibrary,
    private val optimizer: PuOptimizer,
    private val deviceProbe: PuDeviceProbe,
) : ViewModel() {

    private val _state = MutableStateFlow(PuGameDetailsState())
    val state = _state.asStateFlow()

    private val _events = Channel<PuGameDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var profileJob: Job? = null

    init {
        refresh()
    }

    fun onAction(action: PuGameDetailsAction) {
        when (action) {
            is PuGameDetailsAction.OnProfileSelected -> selectProfile(action.profile)
            PuGameDetailsAction.OnClearCache -> clearCache()
            PuGameDetailsAction.OnRefresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(screenState = PuGameDetailsState.ScreenState.Loading, errorMessage = null) }
            try {
                val game = withContext(Dispatchers.IO) { library.getGame(gameId) }
                    ?: throw IllegalStateException("The game is no longer in the library")
                val recommendedProfile = withContext(Dispatchers.Default) {
                    deviceProbe.recommend(deviceProbe.snapshot()).profile
                }
                val manifest = withContext(Dispatchers.IO) { optimizer.manifest(gameId) }
                val cacheSize = withContext(Dispatchers.IO) { optimizer.cacheSize(gameId) }
                _state.update {
                    it.copy(
                        screenState = PuGameDetailsState.ScreenState.Success,
                        game = game,
                        selectedProfile = game.selectedProfile ?: recommendedProfile,
                        manifest = manifest,
                        cacheSizeBytes = cacheSize,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Game details load failed", e)
                _state.update {
                    it.copy(
                        screenState = PuGameDetailsState.ScreenState.Error,
                        errorMessage = "PU could not read this game from the library.",
                    )
                }
            }
        }
    }

    private fun selectProfile(profile: PuProfile) {
        if (_state.value.screenState != PuGameDetailsState.ScreenState.Success) return
        if (_state.value.selectedProfile == profile) return
        val previous = _state.value.selectedProfile
        _state.update { it.copy(selectedProfile = profile, errorMessage = null) }
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) { library.setProfile(gameId, profile) }
                if (updated == null) throw IllegalStateException("The game is no longer in the library")
                _state.update { it.copy(game = updated) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Profile update failed", e)
                _state.update {
                    it.copy(
                        selectedProfile = previous,
                        errorMessage = "PU could not save the performance profile for this game.",
                    )
                }
            }
        }
    }

    private fun clearCache() {
        if (_state.value.isClearingCache) return
        _state.update { it.copy(isClearingCache = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { optimizer.clear(gameId) }
                withContext(Dispatchers.IO) { library.setCacheSize(gameId, 0) }
                _state.update {
                    it.copy(
                        game = it.game?.copy(cacheSizeBytes = 0),
                        manifest = null,
                        cacheSizeBytes = 0,
                        isClearingCache = false,
                    )
                }
                _events.send(PuGameDetailsEvent.CacheCleared)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Cache clear failed", e)
                _state.update {
                    it.copy(
                        isClearingCache = false,
                        errorMessage = "PU could not clear the optimized cache on this device.",
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "PuGameDetails"
    }
}
