package sgyt.pu.model

enum class PuEdition {
    LITE,
    PLUS,
}

enum class PuProfile(val displayName: String) {
    ULTRA_LOW("Ultra Low"),
    LOW("Low"),
    BALANCED("Balanced"),
}

enum class Compatibility(val label: String) {
    SMOOTH("Likely smooth"),
    LAG("Might lag"),
    BLOCKED("Probably won't run"),
}

enum class PuArtStyle {
    LANTERN,
    HOLLOW_BELL,
    SALTWIND,
    NINTH_FERRY,
}

data class PuGame(
    val id: String,
    val title: String,
    val engine: String,
    val version: String,
    val compatibility: Compatibility,
    val originalSize: String,
    val cacheSize: String,
    val art: PuArtStyle,
)

data class PuRuntime(
    val id: String,
    val name: String,
    val detail: String,
    val size: String? = null,
    val installed: Boolean = false,
    val supported: Boolean = true,
)

data class PuProfileOption(
    val profile: PuProfile,
    val detail: String,
    val recommended: Boolean = false,
)

data class PuStep(
    val label: String,
    val detail: String,
    val state: StepState,
)

enum class StepState {
    DONE,
    ACTIVE,
    WAITING,
    WARNING,
    FAILED,
}

data class PuFix(
    val id: String,
    val title: String,
    val detail: String,
    val recommended: Boolean = false,
)

object PuSampleData {
    val games = listOf(
        PuGame(
            id = "lantern-season",
            title = "Lantern Season",
            engine = "Ren'Py",
            version = "7.8",
            compatibility = Compatibility.SMOOTH,
            originalSize = "2.4 GB",
            cacheSize = "1.1 GB",
            art = PuArtStyle.LANTERN,
        ),
        PuGame(
            id = "hollow-bell",
            title = "Hollow Bell",
            engine = "Ren'Py",
            version = "8.6",
            compatibility = Compatibility.LAG,
            originalSize = "1.9 GB",
            cacheSize = "0.9 GB",
            art = PuArtStyle.HOLLOW_BELL,
        ),
        PuGame(
            id = "saltwind-academy",
            title = "Saltwind Academy",
            engine = "RPG Maker",
            version = "MV",
            compatibility = Compatibility.SMOOTH,
            originalSize = "780 MB",
            cacheSize = "430 MB",
            art = PuArtStyle.SALTWIND,
        ),
        PuGame(
            id = "ninth-ferry",
            title = "Ninth Ferry",
            engine = "Windows",
            version = ".exe",
            compatibility = Compatibility.BLOCKED,
            originalSize = "640 MB",
            cacheSize = "—",
            art = PuArtStyle.NINTH_FERRY,
        ),
    )

    val runtimes = listOf(
        PuRuntime(
            id = "renpy-86",
            name = "Ren'Py 8.6",
            detail = "Needs OpenGL ES 3.0",
            supported = false,
        ),
        PuRuntime(
            id = "renpy-85",
            name = "Ren'Py 8.5",
            detail = "Used by 1 game",
            installed = true,
        ),
        PuRuntime(
            id = "renpy-84",
            name = "Ren'Py 8.4",
            detail = "Signed runtime package",
            size = "52 MB",
        ),
        PuRuntime(
            id = "renpy-78",
            name = "Ren'Py 7.8",
            detail = "Used by 2 games",
            installed = true,
        ),
        PuRuntime(
            id = "rpg-maker",
            name = "RPG Maker engines",
            detail = "MV/MZ, XP/VX/Ace, 2000/2003",
            installed = true,
        ),
    )

    val profiles = listOf(
        PuProfileOption(
            profile = PuProfile.ULTRA_LOW,
            detail = "About 540–720p, lite video, smallest cache",
        ),
        PuProfileOption(
            profile = PuProfile.LOW,
            detail = "720p assets, capped image cache",
            recommended = true,
        ),
        PuProfileOption(
            profile = PuProfile.BALANCED,
            detail = "Near-original quality, light tuning",
        ),
    )

    val optimizationSteps = listOf(
        PuStep("Scan game files", "Done", StepState.DONE),
        PuStep("Images", "1,240 of 2,010", StepState.ACTIVE),
        PuStep("Audio", "Waiting", StepState.WAITING),
        PuStep("Video", "Waiting", StepState.WAITING),
    )

    val deviceChecks = listOf(
        PuStep("arm64 processor", "Passed", StepState.DONE),
        PuStep("Free storage", "Passed", StepState.DONE),
        PuStep("Memory, 3 GB", "Warning", StepState.WARNING),
    )

    val fixes = listOf(
        PuFix(
            id = "renpy-low-memory",
            title = "Ren'Py 8.6 on 3 GB devices",
            detail = "Lower the image cache and disable predictive loading.",
            recommended = true,
        ),
        PuFix(
            id = "low-memory-render",
            title = "Low-memory render path",
            detail = "Shorten rollback history and cap decoded image memory.",
        ),
    )
}
