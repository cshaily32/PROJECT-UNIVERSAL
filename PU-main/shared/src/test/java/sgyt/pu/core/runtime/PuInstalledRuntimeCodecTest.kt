package sgyt.pu.core.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sgyt.pu.core.model.PuInstalledRuntime

/**
 * Guards the persisted installed-runtime round trip.
 *
 * `Map.mapNotNull` passes a `Map.Entry` to its lambda, so reading the
 * installation list back out of `runtime-installations.json` has to use
 * `entry.value`. Reading `entry` itself as a JSON object always fails, which
 * silently reports every runtime as not installed.
 */
class PuInstalledRuntimeCodecTest {
    @Test
    fun savedInstallationsAreReadBackWithEveryRecord() {
        val installations = listOf(installation("renpy", 8), installation("rpgmaker", 3))

        val restored = PuInstalledRuntimeCodec.decodeAll(PuInstalledRuntimeCodec.encodeAll(installations))

        assertEquals(2, restored.size)
        assertEquals(listOf("renpy", "rpgmaker"), restored.map { it.runtimeId })
    }

    @Test
    fun restoredInstallationKeepsEveryPersistedField() {
        val original = installation("renpy", 8).copy(
            installPath = "/data/pu/runtimes/renpy/8",
            installedBytes = 12_345,
            installedAt = 1_000,
            lastUsedAt = 2_000,
            verifiedSha256 = "deadbeef",
        )

        val restored = PuInstalledRuntimeCodec.decode(PuInstalledRuntimeCodec.encode(original))

        assertEquals(original, restored)
    }

    @Test
    fun installationsWithoutRequiredFieldsAreDropped() {
        val encoded = PuInstalledRuntimeCodec.encode(installation("renpy", 8))
        val withoutRuntimeId = encoded.copy(values = encoded.values - "runtimeId")
        val withoutRevision = encoded.copy(values = encoded.values - "revision")
        val withoutInstallPath = encoded.copy(values = encoded.values - "installPath")

        assertNull(PuInstalledRuntimeCodec.decode(withoutRuntimeId))
        assertNull(PuInstalledRuntimeCodec.decode(withoutRevision))
        assertNull(PuInstalledRuntimeCodec.decode(withoutInstallPath))
    }

    private fun installation(runtimeId: String, revision: Long) = PuInstalledRuntime(
        runtimeId = runtimeId,
        revision = revision,
        installPath = "/data/pu/runtimes/$runtimeId/$revision",
        installedBytes = 0,
        installedAt = 0,
        lastUsedAt = 0,
        verifiedSha256 = "",
    )
}
