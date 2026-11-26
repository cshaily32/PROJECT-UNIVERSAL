package sgyt.pu.plus

import android.app.Application
import sgyt.pu.core.PuCoreGraph
import sgyt.pu.core.PuGraphOwner
import sgyt.pu.core.network.PuApiConfig

/**
 * Plus edition entry point.
 *
 * The core graph is created lazily so the app does not touch storage or the
 * device inspector during cold start.
 */
class PuApplication : Application(), PuGraphOwner {
    override val puGraph: PuCoreGraph by lazy {
        PuCoreGraph(
            context = this,
            apiConfig = PuApiConfig(
                baseUrl = BuildConfig.PU_API_BASE_URL,
                runtimeCatalogPublicKey = BuildConfig.PU_RUNTIME_CATALOG_PUBLIC_KEY,
                compatibilityPublicKey = BuildConfig.PU_COMPATIBILITY_PUBLIC_KEY,
            ),
        )
    }
}
