package sgyt.pu.ui.viewmodels

import android.net.Uri
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
import sgyt.pu.core.library.PuGameLibrary

enum class PuImportSource {
    FOLDER,
    ARCHIVE,
}

data class PuAddGameState(
    val screenState: ScreenState = ScreenState.Idle,
    val source: PuImportSource = PuImportSource.FOLDER,
    val importedGameId: String? = null,
    val importedTitle: String? = null,
    val detectedEngine: String? = null,
    val detectedVersion: String? = null,
    val detectedSupported: Boolean = false,
    val errorMessage: String? = null,
) {
    sealed interface ScreenState {
        data object Idle : ScreenState
        data object Importing : ScreenState
        data object Success : ScreenState
        data object Error : ScreenState
    }
}

sealed interface PuAddGameAction {
    data class OnSourceSelected(val source: PuImportSource) : PuAddGameAction

    data class OnUriChosen(val uri: String) : PuAddGameAction

    data object OnOpenLibrary : PuAddGameAction
}

sealed interface PuAddGameEvent {
    data class Imported(val gameId: String) : PuAddGameEvent
}

/**
 * Imports a user-picked folder or archive into app-owned storage, detects the
 * engine on the device, and reports the result to the host screen.
 */
class PuAddGameViewModel(
    private val library: PuGameLibrary,
) : ViewModel() {

    private val _state = MutableStateFlow(PuAddGameState())
    val state = _state.asStateFlow()

    private val _events = Channel<PuAddGameEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: PuAddGameAction) {
        when (action) {
            is PuAddGameAction.OnSourceSelected -> _state.update { it.copy(source = action.source) }
            is PuAddGameAction.OnUriChosen -> import(action.uri)
            PuAddGameAction.OnOpenLibrary -> openLibrary()
        }
    }

    private fun import(uri: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    screenState = PuAddGameState.ScreenState.Importing,
                    errorMessage = null,
                    importedGameId = null,
                    importedTitle = null,
                    detectedEngine = null,
                    detectedVersion = null,
                )
            }
            val result = try {
                withContext(Dispatchers.IO) { library.importUri(Uri.parse(uri)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Import failed", e)
                _state.update {
                    it.copy(
                        screenState = PuAddGameState.ScreenState.Error,
                        errorMessage = "PU could not import this game. Check that it contains game files and try again.",
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    screenState = PuAddGameState.ScreenState.Success,
                    importedGameId = result.game.id,
                    importedTitle = result.game.title,
                    detectedEngine = result.detection.engine.name,
                    detectedVersion = result.detection.version.raw,
                    detectedSupported = result.detection.supported,
                    errorMessage = null,
                )
            }
        }
    }

    private fun openLibrary() {
        val gameId = _state.value.importedGameId ?: return
        viewModelScope.launch {
            _events.send(PuAddGameEvent.Imported(gameId))
        }
    }

    private companion object {
        const val TAG = "PuAddGame"
    }
}
