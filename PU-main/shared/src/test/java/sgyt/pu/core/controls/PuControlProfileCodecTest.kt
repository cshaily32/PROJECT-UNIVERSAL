package sgyt.pu.core.controls

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sgyt.pu.core.model.PuControlBinding
import sgyt.pu.core.model.PuControlProfile

class PuControlProfileCodecTest {
    @Test
    fun defaultProfileCoversEveryAction() {
        val profile = defaultProfile()

        assertEquals(PuControlAction.entries.size, profile.bindings.size)
        assertEquals(PuControlAction.entries.map { it.id }, profile.bindings.map { it.action })
    }

    @Test
    fun encodedProfileRoundTrips() {
        val profile = defaultProfile().copy(
            overlayEnabled = false,
            hapticsEnabled = false,
            keyboardVisible = true,
            updatedAt = 42,
            bindings = defaultProfile().bindings.map { binding ->
                if (binding.action == PuControlAction.CONFIRM.id) {
                    binding.copy(
                        touchX = 0.75f,
                        touchY = 0.81f,
                        keyboardKeyCode = 66,
                        gamepadButton = 97,
                        gamepadDeviceId = 4,
                    )
                } else {
                    binding
                }
            },
        )

        val decoded = PuControlProfileCodec.decode(PuControlProfileCodec.encode(profile), "game-1")
        val confirm = decoded.bindings.first { it.action == PuControlAction.CONFIRM.id }

        assertEquals(profile.overlayEnabled, decoded.overlayEnabled)
        assertEquals(profile.hapticsEnabled, decoded.hapticsEnabled)
        assertEquals(profile.keyboardVisible, decoded.keyboardVisible)
        assertEquals(66, confirm.keyboardKeyCode)
        assertEquals(97, confirm.gamepadButton)
        assertEquals(4, confirm.gamepadDeviceId)
        assertEquals(0.75f, confirm.touchX)
    }

    @Test
    fun inputBridgeResolvesConfiguredActions() {
        val profile = defaultProfile()
        val bridge = PuControlInputBridge()

        assertEquals(
            PuControlAction.CONFIRM,
            bridge.keyboardAction(profile, KeyEvent.KEYCODE_ENTER),
        )
        assertEquals(
            PuControlAction.CANCEL,
            bridge.gamepadAction(profile, KeyEvent.KEYCODE_BUTTON_B, 4),
        )
        assertNull(bridge.keyboardAction(profile, KeyEvent.KEYCODE_SPACE))
    }

    @Test
    fun touchHitTestUsesNormalizedRadius() {
        val profile = defaultProfile()
        val confirm = profile.bindings.first { it.action == PuControlAction.CONFIRM.id }

        assertEquals(
            PuControlAction.CONFIRM,
            PuControlInputBridge().touchAction(profile, confirm.touchX, confirm.touchY),
        )
        assertNull(PuControlInputBridge().touchAction(profile, 0.5f, 0.5f))
    }

    @Test
    fun normalizeRepairsMissingAndInvalidBindings() {
        val profile = PuControlProfile(
            gameId = "game-1",
            overlayEnabled = true,
            hapticsEnabled = true,
            keyboardVisible = false,
            bindings = listOf(
                PuControlBinding(
                    action = PuControlAction.UP.id,
                    touchX = Float.NaN,
                    touchY = 2f,
                    touchRadius = 0f,
                    keyboardKeyCode = KeyEvent.KEYCODE_DPAD_UP,
                    gamepadButton = KeyEvent.KEYCODE_DPAD_UP,
                ),
            ),
            updatedAt = 0,
        )

        val normalized = PuControlProfileCodec.normalize(profile)

        assertEquals(PuControlAction.entries.size, normalized.bindings.size)
        normalized.bindings.forEach { binding ->
            assert(binding.touchX in 0f..1f)
            assert(binding.touchY in 0f..1f)
            assert(binding.touchRadius >= 0.02f)
        }
    }

    private fun defaultProfile(): PuControlProfile = PuControlProfile(
        gameId = "game-1",
        overlayEnabled = true,
        hapticsEnabled = true,
        keyboardVisible = false,
        bindings = PuControlProfileCodec.defaultBindings(),
        updatedAt = 0,
    )
}
