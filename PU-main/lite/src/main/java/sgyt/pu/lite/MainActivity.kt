package sgyt.pu.lite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import sgyt.pu.core.PuGraphOwner
import sgyt.pu.model.PuEdition
import sgyt.pu.model.PuEditionConfig
import sgyt.pu.model.PuProfile
import sgyt.pu.ui.PuApp
import sgyt.pu.ui.theme.PuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as? PuGraphOwner)?.puGraph
            ?: error("The PU application class must implement PuGraphOwner")
        setContent {
            PuTheme {
                PuApp(liteEditionConfig, graph)
            }
        }
    }
}

private val liteEditionConfig = PuEditionConfig(
    edition = if (BuildConfig.PU_EDITION == "plus") PuEdition.PLUS else PuEdition.LITE,
    label = "PU Lite",
    defaultProfile = if (BuildConfig.PU_DEFAULT_PROFILE == "Balanced") {
        PuProfile.BALANCED
    } else {
        PuProfile.LOW
    },
    windowsIncluded = BuildConfig.PU_WINDOWS_INCLUDED,
    cloudSyncEnabled = BuildConfig.PU_CLOUD_SYNC_ENABLED,
    translationEnabled = BuildConfig.PU_TRANSLATION_ENABLED,
    backgroundWorkEnabled = BuildConfig.PU_BACKGROUND_WORK_ENABLED,
)
