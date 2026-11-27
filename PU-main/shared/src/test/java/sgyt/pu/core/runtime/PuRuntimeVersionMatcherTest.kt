package sgyt.pu.core.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sgyt.pu.core.model.PuEngineType

/**
 * Guards runtime version matching.
 *
 * The family's own label can be a range ("8.4-8.5"), so the comparable
 * number has to come from the parsed major/minor. Reading it back out of the
 * range label yields null and silently blocks every Ren'Py release from 8.0
 * to 8.5 from finding its runtime.
 */
class PuRuntimeVersionMatcherTest {
    @Test
    fun renpyVersionInsideTheFamilyRangeMatches() {
        assertMatches("8.4", "8.4-8.5")
        assertMatches("8.5", "8.4-8.5")
        assertMatches("8.0", "8.0-8.3")
        assertMatches("8.3", "8.0-8.3")
        assertMatches("8.1.3", "8.0-8.3")
    }

    @Test
    fun renpyVersionOutsideTheFamilyRangeDoesNotMatch() {
        assertDoesNotMatch("8.1", "8.4-8.5")
        assertDoesNotMatch("8.6", "8.4-8.5")
        assertDoesNotMatch("8.4", "8.0-8.3")
        assertDoesNotMatch("8.4", "8.6")
    }

    @Test
    fun renpyVersionMatchesAnExactFamilyLabel() {
        assertMatches("8.6", "8.6")
        assertMatches("7.4", "7.x")
        assertMatches("6.0", "6.x")
    }

    @Test
    fun unparseableVersionNeverMatches() {
        assertDoesNotMatch("not-a-version", "8.4-8.5")
        assertDoesNotMatch("", "8.6")
    }

    @Test
    fun nonRenpyEnginesMatchTheirOwnFamilyName() {
        val version = PuRuntimeVersionMatcher.parse("2003")

        assertTrue(
            PuRuntimeVersionMatcher.matches(PuEngineType.RPG_MAKER_2003, "RPG_MAKER_2003", version),
        )
        assertTrue(
            PuRuntimeVersionMatcher.matches(PuEngineType.RPG_MAKER_2003, "", version),
        )
        assertFalse(
            PuRuntimeVersionMatcher.matches(PuEngineType.RPG_MAKER_2003, "RENPY", version),
        )
    }

    private fun assertMatches(gameVersion: String, family: String) {
        val parsed = PuRuntimeVersionMatcher.parse(gameVersion)
        assertTrue(
            "expected $gameVersion to match $family",
            PuRuntimeVersionMatcher.matches(PuEngineType.RENPY, family, parsed),
        )
    }

    private fun assertDoesNotMatch(gameVersion: String, family: String) {
        val parsed = PuRuntimeVersionMatcher.parse(gameVersion)
        assertFalse(
            "expected $gameVersion not to match $family",
            PuRuntimeVersionMatcher.matches(PuEngineType.RENPY, family, parsed),
        )
    }
}
