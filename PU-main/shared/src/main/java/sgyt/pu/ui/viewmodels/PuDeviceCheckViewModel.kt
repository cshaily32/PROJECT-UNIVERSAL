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
import sgyt.pu.core.device.PuDeviceProbe
import sgyt.pu.core.model.PuDeviceSnapshot
import sgyt.pu.core.model.PuEditionRecommendation
import sgyt.pu.ui.model.PuDeviceFact
import sgyt.pu.ui.model.toFacts

data class PuDeviceCheckState(
    val screenState: ScreenState = ScreenState.Loading,
    val snapshot: PuDeviceSnapshot? = null,
    val recommendation: PuEditionRecommendation? = null,
    val errorMessage: String? = null,
) {
    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Success : ScreenState
        data object Error : ScreenState
    }

    /** Derived facts for the device cards; empty until the snapshot is read. */
    val facts: List<PuDeviceFact> get() = snapshot?.toFacts().orEmpty()
}

sealed interface PuDeviceCheckAction {
    data object OnRefresh : PuDeviceCheckAction
}

/**
 * Reads the real processor, memory, storage, Android, and graphics facts of
 * this phone and turns them into an edition recommendation.
 */
class PuDeviceCheckViewModel(
    private val probe: PuDeviceProbe,
) : ViewModel() {

    private val _state = MutableStateFlow(PuDeviceCheckState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun onAction(action: PuDeviceCheckAction) {
        when (action) {
            PuDeviceCheckAction.OnRefresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(screenState = PuDeviceCheckState.ScreenState.Loading, errorMessage = null) }
            val reading = try {
                withContext(Dispatchers.Default) {
                    val snapshot = probe.snapshot()
                    snapshot to probe.recommend(snapshot)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        screenState = PuDeviceCheckState.ScreenState.Error,
                        errorMessage = "PU could not read this phone's details.",
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    screenState = PuDeviceCheckState.ScreenState.Success,
                    snapshot = reading.first,
                    recommendation = reading.second,
                    errorMessage = null,
                )
            }
        }
    }
}
