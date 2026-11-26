package sgyt.pu.ui.model

import sgyt.pu.core.model.PuDeviceSnapshot
import sgyt.pu.core.model.PuDeviceTier
import sgyt.pu.model.PuEdition

enum class PuDeviceIcon {
    PROCESSOR,
    ANDROID,
    MEMORY,
    STORAGE,
    GRAPHICS,
    WEBVIEW,
}

enum class PuDeviceFactStatus {
    PASS,
    WARNING,
}

data class PuDeviceFact(
    val icon: PuDeviceIcon,
    val label: String,
    val value: String,
    val detail: String,
    val status: PuDeviceFactStatus,
)

/** Turns the raw device snapshot into the fact cards shown on the device check. */
fun PuDeviceSnapshot.toFacts(): List<PuDeviceFact> = listOf(
    PuDeviceFact(
        icon = PuDeviceIcon.PROCESSOR,
        label = "Processor",
        value = if (isArm64) "arm64" else "32-bit ARM",
        detail = supportedAbis.joinToString(", ").ifBlank { "Unknown architecture" },
        status = if (isArm64) PuDeviceFactStatus.PASS else PuDeviceFactStatus.WARNING,
    ),
    PuDeviceFact(
        icon = PuDeviceIcon.ANDROID,
        label = "Android",
        value = "API $apiLevel",
        detail = if (apiLevel >= MINIMUM_ANDROID_API) {
            "Android 7.0 minimum met"
        } else {
            "Android 7.0 or newer required"
        },
        status = if (apiLevel >= MINIMUM_ANDROID_API) PuDeviceFactStatus.PASS else PuDeviceFactStatus.WARNING,
    ),
    PuDeviceFact(
        icon = PuDeviceIcon.MEMORY,
        label = "Memory",
        value = formatBytes(totalRamBytes),
        detail = "${formatBytes(availableRamBytes)} available now",
        status = if (totalRamBytes >= RECOMMENDED_RAM_BYTES) PuDeviceFactStatus.PASS else PuDeviceFactStatus.WARNING,
    ),
    PuDeviceFact(
        icon = PuDeviceIcon.STORAGE,
        label = "Free storage",
        value = formatBytes(freeStorageBytes),
        detail = "of ${formatBytes(totalStorageBytes)} on this phone",
        status = if (freeStorageBytes >= MINIMUM_FREE_STORAGE_BYTES) {
            PuDeviceFactStatus.PASS
        } else {
            PuDeviceFactStatus.WARNING
        },
    ),
    PuDeviceFact(
        icon = PuDeviceIcon.GRAPHICS,
        label = "Graphics",
        value = "OpenGL ES $glEsMajor.$glEsMinor",
        detail = if (supportsGles3) {
            "Ren'Py 8.6 and newer are supported"
        } else {
            "OpenGL ES 3.0 is not available"
        },
        status = if (supportsGles3) PuDeviceFactStatus.PASS else PuDeviceFactStatus.WARNING,
    ),
    PuDeviceFact(
        icon = PuDeviceIcon.WEBVIEW,
        label = "System WebView",
        value = webViewVersionName ?: "Not detected",
        detail = "Required for RPG Maker MV and MZ",
        status = if (webViewVersionCode > 0) PuDeviceFactStatus.PASS else PuDeviceFactStatus.WARNING,
    ),
)

/** Human-readable name of the device tier. */
val PuDeviceTier.label: String
    get() = when (this) {
        PuDeviceTier.NATIVE -> "Native"
        PuDeviceTier.WINE_LITE -> "Wine Lite"
        PuDeviceTier.WINE_FULL -> "Wine Full"
    }

/** Launcher-style name of an edition. */
val PuEdition.editionLabel: String
    get() = when (this) {
        PuEdition.LITE -> "PU Lite"
        PuEdition.PLUS -> "PU Plus"
    }

private const val MINIMUM_ANDROID_API = 24
private const val RECOMMENDED_RAM_BYTES = 3L * 1024 * 1024 * 1024
private const val MINIMUM_FREE_STORAGE_BYTES = 512L * 1024 * 1024
