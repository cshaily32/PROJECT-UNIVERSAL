package sgyt.pu.ui.screens

import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sgyt.pu.ui.theme.spacing
import kotlinx.coroutines.flow.collectLatest
import sgyt.pu.core.controls.PuControlAction
import sgyt.pu.core.controls.PuControlProfileCodec
import sgyt.pu.core.controls.PuGamepadInfo
import sgyt.pu.ui.components.NoticeTone
import sgyt.pu.ui.components.PuBody
import sgyt.pu.ui.components.PuCard
import sgyt.pu.ui.components.PuLabel
import sgyt.pu.ui.components.PuNotice
import sgyt.pu.ui.components.PuPrimaryButton
import sgyt.pu.ui.components.PuPrimaryCard
import sgyt.pu.ui.components.PuScreen
import sgyt.pu.ui.components.PuSecondaryButton
import sgyt.pu.ui.components.PuSettingRow
import sgyt.pu.ui.theme.PuTheme
import sgyt.pu.ui.viewmodels.PuControlsAction
import sgyt.pu.ui.viewmodels.PuControlsEvent
import sgyt.pu.ui.viewmodels.PuControlsState
import sgyt.pu.ui.viewmodels.PuControlsViewModel

@Composable
fun ControlsScreen(
    viewModel: PuControlsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PuControlsEvent.Saved -> Toast.makeText(context, "Controls saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ControlsScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}

@Composable
internal fun ControlsScreenContent(
    state: PuControlsState,
    onAction: (PuControlsAction) -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.captureTarget) {
        if (state.captureTarget is PuControlsState.CaptureTarget.Keyboard ||
            state.captureTarget is PuControlsState.CaptureTarget.Gamepad
        ) {
            focusRequester.requestFocus()
        }
    }

    PuScreen(
        title = "Touch controls",
        modifier = Modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                val capture = state.captureTarget
                if (capture !is PuControlsState.CaptureTarget.Keyboard &&
                    capture !is PuControlsState.CaptureTarget.Gamepad
                ) {
                    return@onPreviewKeyEvent false
                }
                val key = keyEvent.nativeKeyEvent
                if (key.action != KeyEvent.ACTION_DOWN || key.repeatCount != 0) {
                    return@onPreviewKeyEvent true
                }
                onAction(
                    PuControlsAction.OnInputCaptured(
                        keyCode = key.keyCode,
                        deviceId = key.deviceId,
                    ),
                )
                true
            },
        onBack = onBack,
    ) {
        when (state.screenState) {
            PuControlsState.ScreenState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            PuControlsState.ScreenState.Error -> {
                PuNotice(
                    text = state.errorMessage ?: "PU could not read the control profile.",
                    tone = NoticeTone.WARNING,
                )
            }

            PuControlsState.ScreenState.Success -> {
                PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.xs)) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Control profile",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (state.scopeId == sgyt.pu.core.controls.PuControlProfiles.GLOBAL_SCOPE_ID) {
                                    "All games"
                                } else {
                                    "Per game"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                            )
                        }
                        Text(
                            text = state.scopeTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                        )
                        Text(
                            text = "Map every game action to touch, keyboard, and gamepad controls. Changes stay on this device until you save them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                        )
                    }
                }

                PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
                    Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base)) {
                        PuSettingRow(
                            icon = Icons.Outlined.PhoneAndroid,
                            title = "Touch overlay",
                            detail = "Show the on-screen game controls",
                            trailing = {
                                Switch(
                                    checked = state.profile.overlayEnabled,
                                    onCheckedChange = { enabled ->
                                        onAction(PuControlsAction.OnOverlayEnabledChanged(enabled))
                                    },
                                )
                            },
                        )
                        PuSettingRow(
                            icon = Icons.Outlined.Keyboard,
                            title = "Keyboard layout",
                            detail = "Keep keyboard shortcuts visible when needed",
                            trailing = {
                                Switch(
                                    checked = state.profile.keyboardVisible,
                                    onCheckedChange = { visible ->
                                        onAction(PuControlsAction.OnKeyboardVisibleChanged(visible))
                                    },
                                )
                            },
                        )
                        PuSettingRow(
                            icon = Icons.Outlined.PhoneAndroid,
                            title = "Haptic feedback",
                            detail = "Confirm touch actions with a short vibration",
                            trailing = {
                                Switch(
                                    checked = state.profile.hapticsEnabled,
                                    onCheckedChange = { enabled ->
                                        onAction(PuControlsAction.OnHapticsEnabledChanged(enabled))
                                    },
                                )
                            },
                        )
                    }
                }

                PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PuLabel("Connected gamepads", modifier = Modifier.weight(1f))
                            PuBody("${state.gamepads.size}")
                        }
                        if (state.gamepads.isEmpty()) {
                            PuBody(
                                text = "No gamepad detected. Connect one, then use Detect gamepads.",
                                modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                            )
                        } else {
                            state.gamepads.forEach { gamepad ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = MaterialTheme.spacing.md),
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Outlined.Gamepad,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = gamepad.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        PuSecondaryButton(
                            text = "Detect gamepads",
                            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                            onClick = { onAction(PuControlsAction.OnRefreshGamepads) },
                        )
                    }
                }

                PuLabel("Touch layout", modifier = Modifier.padding(top = MaterialTheme.spacing.lg))
                PuBody(
                    text = if (state.captureTarget is PuControlsState.CaptureTarget.Touch) {
                        "Tap the preview to place the selected action."
                    } else {
                        "Choose Touch below an action to reposition it."
                    },
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                )
                TouchControlPreview(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                )

                PuLabel("Action mapping", modifier = Modifier.padding(top = MaterialTheme.spacing.lg))
                PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.md)) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                        CaptureInstruction(state.captureTarget)
                        PuControlAction.entries.forEach { action ->
                            ActionMappingRow(
                                action = action,
                                state = state,
                                onAction = onAction,
                            )
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

                PuPrimaryButton(
                    text = if (state.isSaving) "Saving…" else "Save controls",
                    enabled = state.isDirty && !state.isSaving,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                    onClick = { onAction(PuControlsAction.OnSave) },
                )
                PuSecondaryButton(
                    text = "Reset mappings",
                    enabled = !state.isSaving,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                    onClick = { onAction(PuControlsAction.OnReset) },
                )
            }
        }
    }
}

@Composable
private fun CaptureInstruction(captureTarget: PuControlsState.CaptureTarget?) {
    when (captureTarget) {
        is PuControlsState.CaptureTarget.Keyboard -> PuNotice(
            text = "Press a keyboard key to assign it to ${captureTarget.action.replaceFirstChar(Char::uppercase)}.",
            tone = NoticeTone.INFO,
        )

        is PuControlsState.CaptureTarget.Gamepad -> PuNotice(
            text = "Press a gamepad button to assign it to ${captureTarget.action.replaceFirstChar(Char::uppercase)}.",
            tone = NoticeTone.INFO,
        )

        is PuControlsState.CaptureTarget.Touch -> PuNotice(
            text = "Tap the touch preview to place this action.",
            tone = NoticeTone.INFO,
        )

        null -> Unit
    }
}

@Composable
private fun ActionMappingRow(
    action: PuControlAction,
    state: PuControlsState,
    onAction: (PuControlsAction) -> Unit,
) {
    val binding = state.profile.bindings.firstOrNull { item -> item.action == action.id }
    val captureTarget = state.captureTarget
    val touchSelected = captureTarget is PuControlsState.CaptureTarget.Touch &&
        captureTarget.action == action.id
    val keyboardSelected = captureTarget is PuControlsState.CaptureTarget.Keyboard &&
        captureTarget.action == action.id
    val gamepadSelected = captureTarget is PuControlsState.CaptureTarget.Gamepad &&
        captureTarget.action == action.id

    Column(modifier = Modifier.padding(top = MaterialTheme.spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                PuLabel(action.label)
                PuBody(
                    text = "Key: ${keyLabel(binding?.keyboardKeyCode)} · Pad: ${keyLabel(binding?.gamepadButton)}",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                )
            }
            MappingButton(
                text = "Touch",
                selected = touchSelected,
                onClick = { onAction(PuControlsAction.OnSelectTouch(action.id)) },
            )
            MappingButton(
                text = "Key",
                selected = keyboardSelected,
                onClick = { onAction(PuControlsAction.OnSelectKeyboard(action.id)) },
            )
            MappingButton(
                text = "Pad",
                selected = gamepadSelected,
                onClick = { onAction(PuControlsAction.OnSelectGamepad(action.id)) },
            )
        }
    }
}

@Composable
private fun MappingButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun TouchControlPreview(
    state: PuControlsState,
    onAction: (PuControlsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedAction = (state.captureTarget as? PuControlsState.CaptureTarget.Touch)?.action
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(selectedAction) {
                detectTapGestures { position ->
                    if (selectedAction != null && size.width > 0 && size.height > 0) {
                        onAction(
                            PuControlsAction.OnTouchMoved(
                                action = selectedAction,
                                x = position.x.toFloat() / size.width,
                                y = position.y.toFloat() / size.height,
                            ),
                        )
                    }
                }
            },
    ) {
        state.profile.bindings.forEach { binding ->
            val action = PuControlAction.fromId(binding.action) ?: return@forEach
            val selected = selectedAction == action.id
            Surface(
                modifier = Modifier
                    .offset(
                        x = maxWidth * binding.touchX - 24.dp,
                        y = maxHeight * binding.touchY - 24.dp,
                    )
                    .size(48.dp)
                    .semantics { contentDescription = "${action.label} touch control" },
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.36f)
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = action.shortLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

private fun keyLabel(keyCode: Int?): String {
    if (keyCode == null) return "Not assigned"
    return when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> "Up"
        KeyEvent.KEYCODE_DPAD_DOWN -> "Down"
        KeyEvent.KEYCODE_DPAD_LEFT -> "Left"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "Right"
        KeyEvent.KEYCODE_ENTER -> "Enter"
        KeyEvent.KEYCODE_BACK -> "Back"
        KeyEvent.KEYCODE_MENU -> "Menu"
        KeyEvent.KEYCODE_BUTTON_A -> "A"
        KeyEvent.KEYCODE_BUTTON_B -> "B"
        KeyEvent.KEYCODE_BUTTON_START -> "Start"
        else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
    }
}

@Preview(showBackground = true)
@Composable
private fun PuControlsScreenPreview() {
    val profile = sgyt.pu.core.model.PuControlProfile(
        gameId = "preview",
        overlayEnabled = true,
        hapticsEnabled = true,
        keyboardVisible = false,
        bindings = PuControlProfileCodec.defaultBindings(),
        updatedAt = 0,
    )
    PuTheme {
        ControlsScreenContent(
            state = PuControlsState(
                screenState = PuControlsState.ScreenState.Success,
                scopeId = "preview",
                scopeTitle = "Hollow Bell",
                profile = profile,
                persistedProfile = profile,
                gamepads = listOf(PuGamepadInfo(deviceId = 2, name = "Wireless Controller")),
            ),
            onAction = {},
            onBack = {},
        )
    }
}
