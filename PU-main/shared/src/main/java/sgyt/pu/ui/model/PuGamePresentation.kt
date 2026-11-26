package sgyt.pu.ui.model

import sgyt.pu.core.model.PuCompatibilityEstimate
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.model.Compatibility
import sgyt.pu.model.PuArtStyle
import sgyt.pu.model.PuGame
import java.util.Locale

/** Human-readable engine name used by the library and game details screens. */
val PuEngineType.label: String
    get() = when (this) {
        PuEngineType.RENPY -> "Ren'Py"
        PuEngineType.RPG_MAKER_MV -> "RPG Maker MV"
        PuEngineType.RPG_MAKER_MZ -> "RPG Maker MZ"
        PuEngineType.RPG_MAKER_XP -> "RPG Maker XP"
        PuEngineType.RPG_MAKER_VX -> "RPG Maker VX"
        PuEngineType.RPG_MAKER_ACE -> "RPG Maker Ace"
        PuEngineType.RPG_MAKER_2000 -> "RPG Maker 2000"
        PuEngineType.RPG_MAKER_2003 -> "RPG Maker 2003"
        PuEngineType.WOLF_RPG -> "Wolf RPG"
        PuEngineType.KIRIKIRI -> "KiriKiri"
        PuEngineType.UNITY -> "Unity"
        PuEngineType.GENERIC_EXE -> "Windows"
        PuEngineType.UNKNOWN -> "Unknown"
    }

/** Maps a stored compatibility estimate onto the template's chip vocabulary. */
fun PuCompatibilityEstimate.toPresentation(): Compatibility = when (this) {
    PuCompatibilityEstimate.LIKELY_SMOOTH -> Compatibility.SMOOTH
    PuCompatibilityEstimate.MIGHT_LAG -> Compatibility.LAG
    PuCompatibilityEstimate.PROBABLY_WONT_RUN -> Compatibility.BLOCKED
}

/** Converts an imported game record into the template's presentation model. */
fun PuGameRecord.toPresentation(): PuGame = PuGame(
    id = id,
    title = title.ifBlank { "Untitled game" },
    engine = engine.label,
    version = version.ifBlank { "Unknown version" },
    compatibility = compatibility.toPresentation(),
    originalSize = formatBytes(originalSizeBytes),
    cacheSize = if (cacheSizeBytes > 0) formatBytes(cacheSizeBytes) else "—",
    art = artStyleFor(id),
)

/** Formats a byte count for the compact labels used on the cards. */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return if (unitIndex <= 1) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex])
    }
}

/**
 * Picks a stable cover gradient for a game. Real cover art is not bundled, so
 * the template's cover art style is derived from the game id.
 */
private fun artStyleFor(gameId: String): PuArtStyle {
    val styles = PuArtStyle.entries
    val index = (gameId.hashCode().toLong() and 0x7fffffffL) % styles.size
    return styles[index.toInt()]
}
