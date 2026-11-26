package sgyt.pu.core.compatibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuFixProfile
import sgyt.pu.core.model.PuGameRecord

class PuCompatibilityMatchTest {
    @Test
    fun versionFamilyMatchesOnlyItsEngineFamily() {
        val game = game(engine = PuEngineType.RENPY, version = "8.6.1")
        val profile = profile(engine = PuEngineType.RENPY, versionFamily = "8")

        assertTrue(profile.matches(game))
        assertFalse(profile.copy(versionFamily = "7").matches(game))
        assertFalse(profile.copy(engine = PuEngineType.RPG_MAKER_MV).matches(game))
    }

    @Test
    fun fingerprintAndGenericProfilesStaySafe() {
        val game = game(
            engine = PuEngineType.RENPY,
            version = "8.6.1",
            fingerprint = "game-a",
        )

        assertTrue(profile(fingerprint = "game-a").matches(game))
        assertFalse(profile(fingerprint = "game-b").matches(game))
        assertTrue(profile(engine = PuEngineType.UNKNOWN, versionFamily = null).matches(game))
    }

    private fun game(
        engine: PuEngineType,
        version: String,
        fingerprint: String = "fingerprint",
    ) = PuGameRecord(
        id = "game",
        rootPath = "/game",
        title = "Game",
        engine = engine,
        version = version,
        fingerprint = fingerprint,
        importedAt = 0,
        updatedAt = 0,
        originalSizeBytes = 0,
    )

    private fun profile(
        engine: PuEngineType = PuEngineType.RENPY,
        versionFamily: String? = null,
        fingerprint: String? = null,
    ) = PuFixProfile(
        id = "fix",
        title = "Fix",
        engine = engine,
        versionFamily = versionFamily,
        fingerprint = fingerprint,
        settings = emptyMap(),
        revision = 1,
        signature = null,
    )
}
