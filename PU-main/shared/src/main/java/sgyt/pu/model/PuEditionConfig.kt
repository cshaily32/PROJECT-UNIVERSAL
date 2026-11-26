package sgyt.pu.model

import androidx.compose.runtime.staticCompositionLocalOf

data class PuEditionConfig(
    val edition: PuEdition,
    val label: String,
    val defaultProfile: PuProfile,
    val windowsIncluded: Boolean,
    val cloudSyncEnabled: Boolean,
    val translationEnabled: Boolean,
    val backgroundWorkEnabled: Boolean,
)

val LocalPuEditionConfig = staticCompositionLocalOf {
    PuEditionConfig(
        edition = PuEdition.LITE,
        label = "PU Lite",
        defaultProfile = PuProfile.LOW,
        windowsIncluded = false,
        cloudSyncEnabled = false,
        translationEnabled = false,
        backgroundWorkEnabled = false,
    )
}
