package sgyt.pu.core.library

import android.net.Uri
import sgyt.pu.core.model.PuCompatibilityEstimate
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.model.PuProfile

/**
 * Domain-facing contract for the game library.
 *
 * The presentation layer depends on this interface only, so storage and
 * detection details stay in the data layer.
 */
interface PuGameLibrary {
    fun listGames(): List<PuGameRecord>

    fun getGame(gameId: String): PuGameRecord?

    fun importUri(uri: Uri): PuImportResult

    fun deleteGame(gameId: String): Boolean

    fun renameGame(gameId: String, title: String): PuGameRecord?

    fun setProfile(gameId: String, profile: PuProfile): PuGameRecord?

    fun setCacheSize(gameId: String, sizeBytes: Long): PuGameRecord?

    fun setCompatibility(gameId: String, estimate: PuCompatibilityEstimate): PuGameRecord?
}
