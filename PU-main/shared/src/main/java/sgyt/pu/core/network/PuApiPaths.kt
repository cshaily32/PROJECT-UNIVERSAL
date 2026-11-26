package sgyt.pu.core.network

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Canonical relative paths for PU's optional remote services. */
object PuApiPaths {
    const val RUNTIME_CATALOG = "catalog/runtime-catalog.json"
    const val RUNTIME_CATALOG_SIGNATURE = "catalog/runtime-catalog.json.sig"
    const val COMPATIBILITY_FIXES = "catalog/compatibility-fixes.json"
    const val COMPATIBILITY_FIXES_SIGNATURE = "catalog/compatibility-fixes.json.sig"
    const val WINDOWS_MODULES = "catalog/windows-modules.json"
    const val TRANSLATION_PACKS = "catalog/translation-packs.json"

    fun runtimePackage(runtimeId: String, revision: Long): String =
        "runtimes/${segment(runtimeId)}/$revision/package"

    fun saveBackups(gameId: String): String =
        "games/${segment(gameId)}/save-backups"

    fun controlProfiles(gameId: String): String =
        "games/${segment(gameId)}/control-profiles"

    private fun segment(value: String): String {
        val clean = value.trim()
        require(clean.isNotBlank()) { "An API path segment is required" }
        return URLEncoder.encode(clean, StandardCharsets.UTF_8.name())
    }
}

/**
 * Build-time API configuration. An empty base URL keeps PU fully offline and
 * lets the bundled catalogs remain authoritative.
 */
data class PuApiConfig(
    val baseUrl: String = "",
    val runtimeCatalogPublicKey: String = "",
    val compatibilityPublicKey: String = "",
) {
    fun resolve(path: String): String? {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isEmpty()) return null
        val resolved = "$base/${path.trimStart('/')}"
        val uri = runCatching { URI(resolved) }.getOrNull()
            ?: throw IllegalArgumentException("The PU API URL is invalid")
        require(uri.scheme.equals("https", ignoreCase = true)) {
            "The PU API must use HTTPS"
        }
        return resolved
    }
}
