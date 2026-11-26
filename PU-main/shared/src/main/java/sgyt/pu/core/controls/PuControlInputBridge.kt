package sgyt.pu.core.controls

import sgyt.pu.core.model.PuControlProfile
import kotlin.math.hypot

/** Resolves Android input events to the game actions configured in a profile. */
class PuControlInputBridge {
    fun touchAction(profile: PuControlProfile, normalizedX: Float, normalizedY: Float): PuControlAction? {
        if (!normalizedX.isFinite() || !normalizedY.isFinite()) return null
        return profile.bindings
            .mapNotNull { binding ->
                val action = PuControlAction.fromId(binding.action) ?: return@mapNotNull null
                val distance = hypot(
                    (normalizedX - binding.touchX).toDouble(),
                    (normalizedY - binding.touchY).toDouble(),
                )
                Triple(action, distance, binding.touchRadius)
            }
            .filter { (_, distance, radius) -> distance <= radius }
            .minByOrNull { (_, distance, _) -> distance }
            ?.first
    }

    fun keyboardAction(profile: PuControlProfile, keyCode: Int): PuControlAction? = profile.bindings
        .firstOrNull { binding -> binding.keyboardKeyCode == keyCode }
        ?.action
        ?.let(PuControlAction::fromId)

    fun gamepadAction(profile: PuControlProfile, buttonCode: Int, deviceId: Int): PuControlAction? = profile.bindings
        .firstOrNull { binding ->
            binding.gamepadButton == buttonCode &&
                (binding.gamepadDeviceId == null || binding.gamepadDeviceId == deviceId)
        }
        ?.action
        ?.let(PuControlAction::fromId)
}
