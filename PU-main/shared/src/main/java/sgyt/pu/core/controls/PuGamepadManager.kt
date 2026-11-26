package sgyt.pu.core.controls

import android.view.InputDevice

data class PuGamepadInfo(
    val deviceId: Int,
    val name: String,
)

interface PuGamepadProbe {
    fun listGamepads(): List<PuGamepadInfo>

    fun isGamePadDevice(deviceId: Int): Boolean
}

/** Reads Android's connected gamepad devices without adding another dependency. */
class AndroidPuGamepadManager : PuGamepadProbe {
    override fun listGamepads(): List<PuGamepadInfo> = InputDevice.getDeviceIds()
        .mapNotNull { deviceId -> InputDevice.getDevice(deviceId) }
        .filter { device -> device.sources and GAMEPAD_SOURCES != 0 }
        .map { device ->
            PuGamepadInfo(
                deviceId = device.id,
                name = device.name?.takeIf { name -> name.isNotBlank() } ?: "Gamepad ${device.id}",
            )
        }
        .sortedBy { gamepad -> gamepad.name.lowercase() }

    override fun isGamePadDevice(deviceId: Int): Boolean {
        val device = InputDevice.getDevice(deviceId) ?: return false
        return device.sources and GAMEPAD_SOURCES != 0
    }

    private companion object {
        val GAMEPAD_SOURCES = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK
    }
}
