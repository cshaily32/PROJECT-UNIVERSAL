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
import sgyt.pu.core.controls.PuControlProfiles
import sgyt.pu.core.controls.PuGamepadInfo
import sgyt.pu.core.controls.PuGamepadProbe
import sgyt.pu.core.library.PuGameLibrary
import sgyt.pu.core.model.PuControlBinding
import sgyt.pu.core.model.PuControlProfile

data class PuControlsState(
    val screenState: ScreenState = ScreenState.Loading,
    val scopeId: String = PuControlProfiles.GLOBAL_SCOPE_ID,
    val scopeTitle: String = "All games",
    val profile: PuControlProfile = PuControlProfile(
        gameId = PuControlProfiles.GLOBAL_SCOPE_ID,
        overlayEnabled = true,
        hapticsEnabled = true,
        keyboardVisible = false,
        bindings = emptyList(),
        updatedAt = 0,
    ),
    val persistedProfile: PuControlProfile = profile,
    val gamepads: List<PuGamepadInfo> = emptyList(),
    val captureTarget: CaptureTarget? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Success : ScreenState
        data object Error : ScreenState
    }

    sealed interface CaptureTarget {
        val action: String

        data class Touch(override val action: String) : CaptureTarget
        data class Keyboard(override val action: String) : CaptureTarget
        data class Gamepad(override val action: String) : CaptureTarget
    }

    val isDirty: Boolean
        get() = screenState == ScreenState.Success && profile != persistedProfile
}

sealed interface PuControlsAction {
    data class OnOverlayEnabledChanged(val enabled: Boolean) : PuControlsAction
    data class OnHapticsEnabledChanged(val enabled: Boolean) : PuControlsAction
    data class OnKeyboardVisibleChanged(val visible: Boolean) : PuControlsAction
    data class OnSelectTouch(val action: String) : PuControlsAction
    data class OnSelectKeyboard(val action: String) : PuControlsAction
    data class OnSelectGamepad(val action: String) : PuControlsAction
    data class OnInputCaptured(val keyCode: Int, val deviceId: Int) : PuControlsAction
    data class OnTouchMoved(val action: String, val x: Float, val y: Float) : PuControlsAction
    data object OnCancelCapture : PuControlsAction
    data object OnReset : PuControlsAction
    data object OnSave : PuControlsAction
    data object OnRefreshGamepads : PuControlsAction
}

sealed interface PuControlsEvent {
    data object Saved : PuControlsEvent
}

/** Manages per-game touch, keyboard, and gamepad bindings. */
class PuControlsViewModel(
    private val gameId: String?,
    private val library: PuGameLibrary,
    private val profiles: PuControlProfiles,
    private val gamepadProbe: PuGamepadProbe,
) : ViewModel() {
    private val scopeId = gameId ?: PuControlProfiles.GLOBAL_SCOPE_ID

    private val _state = MutableStateFlow(PuControlsState(scopeId = scopeId))
    val state = _state.asStateFlow()

    private val _events = Channel<PuControlsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: PuControlsAction) {
        when (action) {
            is PuControlsAction.OnOverlayEnabledChanged -> updateProfile { profile ->
                profile.copy(overlayEnabled = action.enabled)
            }

            is PuControlsAction.OnHapticsEnabledChanged -> updateProfile { profile ->
                profile.copy(hapticsEnabled = action.enabled)
            }

            is PuControlsAction.OnKeyboardVisibleChanged -> updateProfile { profile ->
                profile.copy(keyboardVisible = action.visible)
            }

            is PuControlsAction.OnSelectTouch -> selectCapture(PuControlsState.CaptureTarget.Touch(action.action))
            is PuControlsAction.OnSelectKeyboard -> selectCapture(PuControlsState.CaptureTarget.Keyboard(action.action))
            is PuControlsAction.OnSelectGamepad -> selectCapture(PuControlsState.CaptureTarget.Gamepad(action.action))
            is PuControlsAction.OnInputCaptured -> captureInput(action.keyCode, action.deviceId)
            is PuControlsAction.OnTouchMoved -> moveTouch(action.action, action.x, action.y)
            PuControlsAction.OnCancelCapture -> _state.update { it.copy(captureTarget = null) }
            PuControlsAction.OnReset -> resetProfile()
            PuControlsAction.OnSave -> save()
            PuControlsAction.OnRefreshGamepads -> refreshGamepads()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    screenState = PuControlsState.ScreenState.Loading,
                    errorMessage = null,
                )
            }
            try {
                val profile = withContext(Dispatchers.IO) { profiles.getProfile(scopeId) }
                val gameTitle = gameId?.let { selectedGameId ->
                    withContext(Dispatchers.IO) { library.getGame(selectedGameId) }?.title
                }
                val gamepads = withContext(Dispatchers.Default) { gamepadProbe.listGamepads() }
                _state.update {
                    it.copy(
                        screenState = PuControlsState.ScreenState.Success,
                        scopeTitle = gameTitle?.takeIf { title -> title.isNotBlank() } ?: "All games",
                        profile = profile,
                        persistedProfile = profile,
                        gamepads = gamepads,
                        captureTarget = null,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Control profile load failed", e)
                _state.update {
                    it.copy(
                        screenState = PuControlsState.ScreenState.Error,
                        errorMessage = "PU could not read the control profile on this device.",
                    )
                }
            }
        }
    }

    private fun updateProfile(transform: (PuControlProfile) -> PuControlProfile) {
        _state.update { current ->
            if (current.screenState != PuControlsState.ScreenState.Success) return@update current
            current.copy(profile = transform(current.profile), errorMessage = null)
        }
    }

    private fun selectCapture(target: PuControlsState.CaptureTarget) {
        _state.update { current ->
            if (current.screenState != PuControlsState.ScreenState.Success) return@update current
            current.copy(captureTarget = target, errorMessage = null)
        }
    }

    private fun captureInput(keyCode: Int, deviceId: Int) {
        val target = _state.value.captureTarget ?: return
        when (target) {
            is PuControlsState.CaptureTarget.Keyboard -> {
                if (gamepadProbe.isGamePadDevice(deviceId)) return
                updateBinding(target.action) { binding ->
                    binding.copy(keyboardKeyCode = keyCode)
                }
            }

            is PuControlsState.CaptureTarget.Gamepad -> {
                if (!gamepadProbe.isGamePadDevice(deviceId)) return
                updateBinding(target.action) { binding ->
                    binding.copy(gamepadButton = keyCode, gamepadDeviceId = deviceId)
                }
            }

            is PuControlsState.CaptureTarget.Touch -> Unit
        }
        _state.update { it.copy(captureTarget = null) }
    }

    private fun moveTouch(action: String, x: Float, y: Float) {
        if (_state.value.captureTarget !is PuControlsState.CaptureTarget.Touch) return
        updateBinding(action) { binding ->
            binding.copy(
                touchX = x.coerceIn(0f, 1f),
                touchY = y.coerceIn(0f, 1f),
            )
        }
        _state.update { it.copy(captureTarget = null) }
    }

    private fun updateBinding(
        action: String,
        transform: (PuControlBinding) -> PuControlBinding,
    ) {
        updateProfile { profile ->
            profile.copy(
                bindings = profile.bindings.map { binding ->
                    if (binding.action == action) transform(binding) else binding
                },
            )
        }
    }

    private fun resetProfile() {
        _state.update { current ->
            if (current.screenState != PuControlsState.ScreenState.Success) return@update current
            current.copy(
                profile = profiles.defaultProfile(scopeId),
                captureTarget = null,
                errorMessage = null,
            )
        }
    }

    private fun save() {
        val current = _state.value
        if (current.screenState != PuControlsState.ScreenState.Success || current.isSaving || !current.isDirty) return
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { profiles.saveProfile(current.profile) }
                _state.update {
                    it.copy(
                        profile = saved,
                        persistedProfile = saved,
                        isSaving = false,
                        captureTarget = null,
                        errorMessage = null,
                    )
                }
                _events.send(PuControlsEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Control profile save failed", e)
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "PU could not save this control profile.",
                    )
                }
            }
        }
    }

    private fun refreshGamepads() {
        viewModelScope.launch {
            val gamepads = withContext(Dispatchers.Default) { gamepadProbe.listGamepads() }
            _state.update { it.copy(gamepads = gamepads, errorMessage = null) }
        }
    }

    private companion object {
        const val TAG = "PuControls"
    }
}
