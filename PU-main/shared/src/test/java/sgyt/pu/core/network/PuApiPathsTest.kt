package sgyt.pu.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PuApiPathsTest {
    @Test
    fun blankBaseUrlKeepsRemoteServicesDisabled() {
        assertNull(PuApiConfig().resolve(PuApiPaths.RUNTIME_CATALOG))
    }

    @Test
    fun signedCatalogPathsResolveBelowConfiguredBaseUrl() {
        val config = PuApiConfig(baseUrl = "https://services.example.invalid/")

        assertEquals(
            "https://services.example.invalid/catalog/runtime-catalog.json",
            config.resolve(PuApiPaths.RUNTIME_CATALOG),
        )
        assertEquals(
            "https://services.example.invalid/catalog/runtime-catalog.json.sig",
            config.resolve(PuApiPaths.RUNTIME_CATALOG_SIGNATURE),
        )
        assertEquals(
            "https://services.example.invalid/runtimes/renpy-8/2/package",
            config.resolve(PuApiPaths.runtimePackage("renpy-8", 2)),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonHttpsBaseUrlIsRejected() {
        PuApiConfig(baseUrl = "http://services.example.invalid")
            .resolve(PuApiPaths.RUNTIME_CATALOG)
    }

    @Test
    fun dynamicPathSegmentsAreEncoded() {
        assertEquals(
            "runtimes/renpy%2F8.6/12/package",
            PuApiPaths.runtimePackage("renpy/8.6", 12),
        )
        assertEquals(
            "games/game%2F1/save-backups",
            PuApiPaths.saveBackups("game/1"),
        )
    }
}
