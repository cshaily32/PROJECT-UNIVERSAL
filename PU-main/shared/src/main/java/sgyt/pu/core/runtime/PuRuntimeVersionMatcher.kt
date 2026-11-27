package sgyt.pu.core.runtime

import sgyt.pu.core.model.PuEngineType

/**
 * Matches a game's engine version against a published runtime's version family.
 *
 * A version family can be a plain label ("8.6", "7.x") or an inclusive range
 * ("8.4-8.5"). The game's own family label is also a range for the same
 * releases, so the comparable number is parsed once and kept separately
 * instead of being read back out of the family label.
 */
internal object PuRuntimeVersionMatcher {
    data class ParsedVersion(
        val family: String?,
        val minor: Int?,
        val numeric: Double?,
    )

    fun parse(raw: String): ParsedVersion {
        val match = Regex("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?").find(raw)
            ?: return ParsedVersion(null, null, null)
        val major = match.groupValues[1].toInt()
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull()
        val family = when {
            major == 8 && minor != null && minor >= 6 -> "8.6"
            major == 8 && minor != null && minor >= 4 -> "8.4-8.5"
            major == 8 -> "8.0-8.3"
            major == 7 -> "7.x"
            major == 6 -> "6.x"
            else -> "$major.x"
        }
        val numeric = major + (minor ?: 0) / 10.0
        return ParsedVersion(family, minor, numeric)
    }

    fun matches(engine: PuEngineType, descriptorFamily: String, version: ParsedVersion): Boolean {
        if (engine != PuEngineType.RENPY) {
            return descriptorFamily.isBlank() || descriptorFamily.equals(engine.name, ignoreCase = true)
        }
        if (version.family == null) return false
        val family = descriptorFamily.trim()
        if (family.isBlank()) return false
        if (family.contains('-')) {
            val parts = family.split('-')
            val low = parts.first().trim().toDoubleOrNull()
            val high = parts.last().trim().toDoubleOrNull()
            val numeric = version.numeric
            return low != null && high != null && numeric != null && numeric in low..high
        }
        return family == version.family
    }
}
