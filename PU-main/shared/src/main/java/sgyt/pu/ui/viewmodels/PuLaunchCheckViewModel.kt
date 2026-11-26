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
import sgyt.pu.core.launch.PuLaunchCoordinator
import sgyt.pu.core.model.PuLaunchPreparation
import sgyt.pu.core.library.PuGameLibrary
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.core.model.PuLaunchAssessment
import sgyt.pu.core.model.PuRuntimeDescriptor
import sgyt.pu.model.PuEdition
import sgyt.pu.ui.model.PuRuntimeSelection

data class PuLaunchCheckState(
    val screenState: ScreenState = ScreenState.Loading,
    val game: PuGameRecord? = null,
) {
    sealed interface ScreenState {
        data object Loading : ScreenState
        data class Blocked(val assessment: PuLaunchAssessment) : ScreenState
        data class NeedsRuntime(
            val assessment: PuLaunchAssessment,
            val runtime: PuRuntimeDescriptor,
        ) : ScreenState
        data class Ready(
            val assessment: PuLaunchAssessment,
            val runtime: PuRuntimeDescriptor,
        ) : ScreenState
        data class Error(val message: String) : ScreenState
    }
}

sealed interface PuLaunchCheckAction {
    data object OnCheck : PuLaunchCheckAction
    data object OnDownloadRuntime : PuLaunchCheckAction
}

sealed interface PuLaunchCheckEvent {
    data class RuntimeNeeded(val selection: PuRuntimeSelection) : PuLaunchCheckEvent
}

/** Runs the mandatory device and runtime checks before any game can start. */
class PuLaunchCheckViewModel(
    private val gameId: String,
    private val edition: PuEdition,
    private val library: PuGameLibrary,
    private val launchCoordinator: PuLaunchCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(PuLaunchCheckState())
    val state = _state.asStateFlow()

    private val _events = Channel<PuLaunchCheckEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var checkJob: Job? = null

    fun onAction(action: PuLaunchCheckAction) {
        when (action) {
            PuLaunchCheckAction.OnCheck -> check()
            PuLaunchCheckAction.OnDownloadRuntime -> requestRuntime()
        }
    }

    private fun check() {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _state.update { it.copy(screenState = PuLaunchCheckState.ScreenState.Loading) }
            try {
                val game = withContext(Dispatchers.IO) { library.getGame(gameId) }
                    ?: throw IllegalStateException("The game is no longer in the library")
                val preparation = withContext(Dispatchers.Default) {
                    launchCoordinator.prepare(game, edition)
                }
                val assessment = when (preparation) {
                    is PuLaunchPreparation.Blocked -> preparation.assessment
                    is PuLaunchPreparation.NeedsRuntime -> preparation.assessment
                    is PuLaunchPreparation.Ready -> preparation.assessment
                }
                try {
                    withContext(Dispatchers.IO) {
                        library.setCompatibility(gameId, assessment.estimate)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Could not save the compatibility estimate", e)
                }
                _state.update {
                    it.copy(
                        game = game,
                        screenState = when (preparation) {
                            is PuLaunchPreparation.Blocked -> {
                                PuLaunchCheckState.ScreenState.Blocked(assessment)
                            }
                            is PuLaunchPreparation.NeedsRuntime -> {
                                PuLaunchCheckState.ScreenState.NeedsRuntime(assessment, preparation.runtime)
                            }
                            is PuLaunchPreparation.Ready -> {
                                PuLaunchCheckState.ScreenState.Ready(assessment, preparation.runtime)
                            }
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Launch check failed", e)
                _state.update {
                    it.copy(
                        screenState = PuLaunchCheckState.ScreenState.Error(
                            "PU could not complete the launch check on this device.",
                        ),
                    )
                }
            }
        }
    }

    private fun requestRuntime() {
        val current = _state.value.screenState
        if (current !is PuLaunchCheckState.ScreenState.NeedsRuntime) return
        viewModelScope.launch {
            _events.send(
                PuLaunchCheckEvent.RuntimeNeeded(
                    PuRuntimeSelection(current.runtime.id, current.runtime.catalogRevision),
                ),
            )
        }
    }

    private companion object {
        const val TAG = "PuLaunchCheck"
    }
}
