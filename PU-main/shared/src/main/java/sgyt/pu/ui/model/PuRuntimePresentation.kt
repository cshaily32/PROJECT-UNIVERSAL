package sgyt.pu.ui.model

import sgyt.pu.core.model.PuDeviceSnapshot
import sgyt.pu.core.model.PuInstalledRuntime
import sgyt.pu.core.model.PuRuntimeDescriptor
import sgyt.pu.core.model.assessRuntime

data class PuRuntimeSelection(
    val runtimeId: String,
    val revision: Long,
)

data class PuRuntimeInfo(
    val runtimeId: String,
    val revision: Long,
    val name: String,
    val engine: String,
    val version: String,
    val compressedBytes: Long,
    val installedBytes: Long,
    val installed: Boolean,
    val supported: Boolean,
    val requirement: String,
    val verifiedRelease: Boolean,
)

enum class PuRuntimeFilter(val label: String) {
    ALL("All"),
    RENPY("Ren'Py"),
    RPG_MAKER("RPG Maker"),
    INSTALLED("Installed"),
}

fun PuRuntimeDescriptor.toRuntimeInfo(
    snapshot: PuDeviceSnapshot,
    installation: PuInstalledRuntime?,
): PuRuntimeInfo {
    val capability = snapshot.assessRuntime(this)
    val engineLabel = engine.label
    val versionLabel = versionFamily.ifBlank { engineLabel }.replace('-', '–')
    val requirements = buildList {
        if (minAndroidApi > 24) add("Android API $minAndroidApi")
        if (requiresGles3) add("OpenGL ES 3.0")
        if (!abi.equals("any", ignoreCase = true)) add(abi)
    }
    val requirement = capability.blockers.joinToString(" · ").ifBlank {
        requirements.joinToString(" · ").ifBlank { "Compatible with this phone" }
    }
    return PuRuntimeInfo(
        runtimeId = id,
        revision = catalogRevision,
        name = "$engineLabel $versionLabel",
        engine = engineLabel,
        version = versionLabel,
        compressedBytes = compressedBytes,
        installedBytes = installation?.installedBytes ?: installedBytes,
        installed = installation != null,
        supported = capability.supported,
        requirement = requirement,
        verifiedRelease = verifiedRelease,
    )
}
