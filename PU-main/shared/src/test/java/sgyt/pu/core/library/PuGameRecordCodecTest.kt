package sgyt.pu.core.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sgyt.pu.core.model.PuCompatibilityEstimate
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.model.PuProfile

/**
 * Guards the persisted-library round trip.
 *
 * `Map.mapNotNull` passes a `Map.Entry` to its lambda, so reading the library
 * back out of `games.json` has to use `entry.value`. Reading `entry` itself as
 * a JSON object always fails, which silently empties the whole library.
 */
class PuGameRecordCodecTest {
    @Test
    fun savedLibraryIsReadBackWithEveryRecord() {
        val records = listOf(record("game-1"), record("game-2"))

        val restored = PuGameRecordCodec.decodeAll(PuGameRecordCodec.encodeAll(records))

        assertEquals(2, restored.size)
        assertEquals(listOf("game-1", "game-2"), restored.map { it.id })
    }

    @Test
    fun restoredRecordKeepsEveryPersistedField() {
        val original = record("game-1").copy(
            title = "Chronicles",
            engine = PuEngineType.RENPY,
            version = "8.3",
            fingerprint = "abc123",
            importedAt = 100,
            updatedAt = 200,
            originalSizeBytes = 4_096,
            cacheSizeBytes = 2_048,
            selectedProfile = PuProfile.BALANCED,
            compatibility = PuCompatibilityEstimate.LIKELY_SMOOTH,
            appliedFixIds = listOf("fix-a", "fix-b"),
            sourceUri = "content://games/1",
        )

        val restored = PuGameRecordCodec.decode(PuGameRecordCodec.encode(original))

        assertEquals(original, restored)
    }

    @Test
    fun recordsWithoutAnIdOrRootPathAreDropped() {
        val encoded = PuGameRecordCodec.encode(record("game-1"))
        val withoutId = PuGameRecordCodec.encode(record("game-1")).copy(values = encoded.values - "id")
        val withoutRootPath = PuGameRecordCodec.encode(record("game-1")).copy(values = encoded.values - "rootPath")

        assertNull(PuGameRecordCodec.decode(withoutId))
        assertNull(PuGameRecordCodec.decode(withoutRootPath))
    }

    @Test
    fun unknownEngineAndCompatibilityFallBackToSafeDefaults() {
        val encoded = PuGameRecordCodec.encode(record("game-1"))
        val rewritten = encoded.copy(
            values = encoded.values + ("engine" to sgyt.pu.core.json.PuJson.string("NOT_A_ENGINE")) +
                ("compatibility" to sgyt.pu.core.json.PuJson.string("NOT_AN_ESTIMATE")),
        )

        val restored = PuGameRecordCodec.decode(rewritten)

        assertEquals(PuEngineType.UNKNOWN, restored?.engine)
        assertEquals(PuCompatibilityEstimate.LIKELY_SMOOTH, restored?.compatibility)
    }

    private fun record(id: String) = PuGameRecord(
        id = id,
        rootPath = "/data/games/$id",
        title = id,
        engine = PuEngineType.UNKNOWN,
        version = "",
        fingerprint = "",
        importedAt = 0,
        updatedAt = 0,
        originalSizeBytes = 0,
        cacheSizeBytes = 0,
        selectedProfile = null,
        compatibility = PuCompatibilityEstimate.LIKELY_SMOOTH,
        appliedFixIds = emptyList(),
        sourceUri = null,
    )
}
