package sgyt.pu.core.controls

import android.view.KeyEvent
import sgyt.pu.core.json.PuJson
import sgyt.pu.core.json.PuJsonObject
import sgyt.pu.core.json.PuJsonValue
import sgyt.pu.core.model.PuControlBinding
import sgyt.pu.core.model.PuControlProfile
import sgyt.pu.core.storage.PuStorage
import java.io.File

interface PuControlProfiles {
    fun getProfile(scopeId: String): PuControlProfile

    fun saveProfile(profile: PuControlProfile): PuControlProfile

    fun defaultProfile(scopeId: String): PuControlProfile

    companion object {
        const val GLOBAL_SCOPE_ID = "global"
    }
}

/** Persists per-game control profiles and the fallback profile for all games. */
class PuControlProfileStore(
    private val storage: PuStorage,
) : PuControlProfiles {
    private val profilesFile = File(storage.controlsRoot, "control-profiles.json")

    @Synchronized
    override fun getProfile(scopeId: String): PuControlProfile {
        require(scopeId.isNotBlank()) { "A control profile scope is required" }
        val root = storage.readJson(profilesFile) as? PuJsonValue.JsonObject
            ?: return defaultProfile(scopeId)
        val profile = root.values[scopeId] as? PuJsonValue.JsonObject
            ?: root.values[PuControlProfiles.GLOBAL_SCOPE_ID] as? PuJsonValue.JsonObject
            ?: return defaultProfile(scopeId)
        return PuControlProfileCodec.decode(profile, scopeId)
    }

    @Synchronized
    override fun saveProfile(profile: PuControlProfile): PuControlProfile {
        require(profile.gameId.isNotBlank()) { "A control profile scope is required" }
        val normalized = PuControlProfileCodec.normalize(
            profile.copy(updatedAt = System.currentTimeMillis()),
        )
        val root = storage.readJson(profilesFile) as? PuJsonValue.JsonObject
        storage.writeJson(
            profilesFile,
            PuJsonValue.JsonObject(
                (root?.values.orEmpty() + (profile.gameId to PuControlProfileCodec.encode(normalized))),
            ),
        )
        return normalized
    }

    @Synchronized
    override fun defaultProfile(scopeId: String): PuControlProfile {
        require(scopeId.isNotBlank()) { "A control profile scope is required" }
        return PuControlProfile(
            gameId = scopeId,
            overlayEnabled = true,
            hapticsEnabled = true,
            keyboardVisible = false,
            bindings = PuControlProfileCodec.defaultBindings(),
            updatedAt = 0,
        )
    }
}

internal object PuControlProfileCodec {
    fun decode(json: PuJsonValue.JsonObject, scopeId: String): PuControlProfile {
        val objectValue = PuJsonObject(json.values)
        val existing = objectValue.array("bindings").mapNotNull { value ->
            (value as? PuJsonValue.JsonObject)?.let(::decodeBinding)
        }.associateBy { binding -> binding.action }
        return normalize(
            PuControlProfile(
                gameId = scopeId,
                overlayEnabled = objectValue.boolean("overlayEnabled") ?: true,
                hapticsEnabled = objectValue.boolean("hapticsEnabled") ?: true,
                keyboardVisible = objectValue.boolean("keyboardVisible") ?: false,
                bindings = PuControlAction.entries.map { action ->
                    existing[action.id] ?: defaultBinding(action)
                },
                updatedAt = objectValue.long("updatedAt") ?: 0,
            ),
        )
    }

    fun encode(profile: PuControlProfile): PuJsonValue.JsonObject = PuJson.obj(
        "overlayEnabled" to PuJson.bool(profile.overlayEnabled),
        "hapticsEnabled" to PuJson.bool(profile.hapticsEnabled),
        "keyboardVisible" to PuJson.bool(profile.keyboardVisible),
        "updatedAt" to PuJson.number(profile.updatedAt),
        "bindings" to PuJson.arr(profile.bindings.map(::encodeBinding)),
    )

    fun normalize(profile: PuControlProfile): PuControlProfile {
        val existing = profile.bindings.associateBy { binding -> binding.action }
        return profile.copy(
            bindings = PuControlAction.entries.map { action ->
                val fallback = defaultBinding(action)
                val binding = existing[action.id] ?: fallback
                binding.copy(
                    touchX = binding.touchX.safeRange(0f, 1f, fallback.touchX),
                    touchY = binding.touchY.safeRange(0f, 1f, fallback.touchY),
                    touchRadius = binding.touchRadius.safeRange(0.02f, 0.20f, fallback.touchRadius),
                )
            },
        )
    }

    fun defaultBindings(): List<PuControlBinding> = PuControlAction.entries.map(::defaultBinding)

    private fun defaultBinding(action: PuControlAction): PuControlBinding = when (action) {
        PuControlAction.UP -> binding(action, 0.28f, 0.54f, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_BUTTON_DPAD_UP)
        PuControlAction.DOWN -> binding(action, 0.28f, 0.82f, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_BUTTON_DPAD_DOWN)
        PuControlAction.LEFT -> binding(action, 0.14f, 0.68f, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BUTTON_DPAD_LEFT)
        PuControlAction.RIGHT -> binding(action, 0.42f, 0.68f, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BUTTON_DPAD_RIGHT)
        PuControlAction.CONFIRM -> binding(action, 0.82f, 0.72f, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A)
        PuControlAction.CANCEL -> binding(action, 0.66f, 0.72f, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B)
        PuControlAction.MENU -> binding(action, 0.86f, 0.48f, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BUTTON_START)
    }

    private fun binding(
        action: PuControlAction,
        touchX: Float,
        touchY: Float,
        keyboardKeyCode: Int,
        gamepadButton: Int,
    ) = PuControlBinding(
        action = action.id,
        touchX = touchX,
        touchY = touchY,
        touchRadius = 0.06f,
        keyboardKeyCode = keyboardKeyCode,
        gamepadButton = gamepadButton,
    )

    private fun encodeBinding(binding: PuControlBinding): PuJsonValue.JsonObject = PuJson.obj(
        "action" to PuJson.string(binding.action),
        "touchX" to PuJson.number(binding.touchX.toDouble()),
        "touchY" to PuJson.number(binding.touchY.toDouble()),
        "touchRadius" to PuJson.number(binding.touchRadius.toDouble()),
        "keyboardKeyCode" to PuJson.nullableString(binding.keyboardKeyCode?.toString()),
        "gamepadButton" to PuJson.nullableString(binding.gamepadButton?.toString()),
        "gamepadDeviceId" to PuJson.nullableString(binding.gamepadDeviceId?.toString()),
    )

    private fun decodeBinding(json: PuJsonValue.JsonObject): PuControlBinding? {
        val objectValue = PuJsonObject(json.values)
        val action = objectValue.string("action") ?: return null
        return PuControlBinding(
            action = action,
            touchX = objectValue.double("touchX")?.toFloat() ?: 0f,
            touchY = objectValue.double("touchY")?.toFloat() ?: 0f,
            touchRadius = objectValue.double("touchRadius")?.toFloat() ?: 0.06f,
            keyboardKeyCode = objectValue.string("keyboardKeyCode")?.toIntOrNull(),
            gamepadButton = objectValue.string("gamepadButton")?.toIntOrNull(),
            gamepadDeviceId = objectValue.string("gamepadDeviceId")?.toIntOrNull(),
        )
    }

    private fun Float.safeRange(minimum: Float, maximum: Float, fallback: Float): Float =
        if (isFinite()) coerceIn(minimum, maximum) else fallback
}
