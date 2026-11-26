package sgyt.pu.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuRuntimeCapabilityTest {
    @Test
    fun gles3RequirementBlocksUnsupportedDevice() {
        val device = PuDeviceSnapshot(
            capturedAt = 0,
            apiLevel = 35,
            supportedAbis = listOf("arm64-v8a"),
            totalRamBytes = 4L * 1024 * 1024 * 1024,
            availableRamBytes = 2L * 1024 * 1024 * 1024,
            appMemoryClassMb = 256,
            freeStorageBytes = 4L * 1024 * 1024 * 1024,
            totalStorageBytes = 8L * 1024 * 1024 * 1024,
            glEsMajor = 2,
            glEsMinor = 0,
            webViewVersionName = null,
            webViewVersionCode = 0,
            isCharging = false,
            batteryPercent = 80,
            thermalStatus = null,
        )
        val runtime = runtime(requiresGles3 = true)

        val capability = device.assessRuntime(runtime)

        assertFalse(capability.supported)
        assertTrue(capability.blockers.any { it.contains("OpenGL ES 3.0") })
    }

    @Test
    fun compatibleDevicePassesRuntimeRequirements() {
        val device = PuDeviceSnapshot(
            capturedAt = 0,
            apiLevel = 35,
            supportedAbis = listOf("arm64-v8a"),
            totalRamBytes = 4L * 1024 * 1024 * 1024,
            availableRamBytes = 2L * 1024 * 1024 * 1024,
            appMemoryClassMb = 256,
            freeStorageBytes = 4L * 1024 * 1024 * 1024,
            totalStorageBytes = 8L * 1024 * 1024 * 1024,
            glEsMajor = 3,
            glEsMinor = 2,
            webViewVersionName = null,
            webViewVersionCode = 0,
            isCharging = false,
            batteryPercent = 80,
            thermalStatus = null,
        )

        val capability = device.assessRuntime(runtime(requiresGles3 = true))

        assertTrue(capability.supported)
        assertTrue(capability.checks.all { it.passed })
    }

    private fun runtime(requiresGles3: Boolean) = PuRuntimeDescriptor(
        id = "renpy-8-6",
        engine = PuEngineType.RENPY,
        versionFamily = "8.6",
        supportedMinors = listOf(6),
        abi = "arm64-v8a",
        catalogRevision = 1,
        packageUrl = "https://example.invalid/runtime.pkg",
        compressedBytes = 1024,
        installedBytes = 2048,
        sha256 = "0".repeat(64),
        signature = "signature",
        minAndroidApi = 24,
        requiresGles3 = requiresGles3,
        verifiedRelease = true,
    )
}
