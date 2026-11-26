package sgyt.pu.core.detection

import sgyt.pu.core.model.PuDetectionResult
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuVersion
import sgyt.pu.core.security.PuCrypto
import java.io.File
import java.util.Locale

/**
 * Detects the supported engine families from game-owned files. Detection is
 * entirely local and never uploads game names or file metadata.
 */
class PuGameDetector {
    fun detect(root: File): PuDetectionResult {
        require(root.exists()) { "Game path does not exist: $root" }
        val files = if (root.isFile) listOf(root) else indexFiles(root)
        val relativePaths = files.map { file ->
            if (root.isFile) file.name else file.relativeTo(root).invariantPath()
        }
        val lowerPaths = relativePaths.map { it.lowercase(Locale.ROOT) }

        val renPyEvidence = lowerPaths.filter { path ->
            path == "game" || path.startsWith("game/") || path == "renpy" || path.startsWith("renpy/")
        }
        if (renPyEvidence.isNotEmpty()) {
            val version = detectRenPyVersion(root, files)
            val title = detectRenPyTitle(root) ?: root.name
            return result(
                engine = PuEngineType.RENPY,
                version = version,
                title = title,
                confidence = if (version.isKnown) 0.98f else 0.9f,
                evidence = buildList {
                    addAll(renPyEvidence.take(4))
                    version.evidence?.let(::add)
                },
                root = root,
                files = files,
            )
        }

        detectRpgMaker(root, lowerPaths)?.let { detected ->
            return result(
                engine = detected.engine,
                version = PuVersion(detected.version),
                title = detected.title,
                confidence = detected.confidence,
                evidence = detected.evidence,
                root = root,
                files = files,
            )
        }

        val wolfArchive = lowerPaths.firstOrNull { it.endsWith(".wolf") }
        if (wolfArchive != null && lowerPaths.any { it == "game.exe" || it.endsWith("/game.exe") }) {
            return result(
                engine = PuEngineType.WOLF_RPG,
                version = PuVersion("Wolf RPG"),
                title = root.name,
                confidence = 0.98f,
                evidence = listOf("Game.exe", wolfArchive),
                root = root,
                files = files,
            )
        }

        val xp3Archive = lowerPaths.firstOrNull { it.endsWith(".xp3") }
        if (xp3Archive != null) {
            return result(
                engine = PuEngineType.KIRIKIRI,
                version = PuVersion("KiriKiri"),
                title = root.name,
                confidence = 0.98f,
                evidence = listOf(xp3Archive),
                root = root,
                files = files,
            )
        }

        val unityData = lowerPaths.firstOrNull { it.endsWith("_data") && it.contains('/') }
        if (unityData != null && lowerPaths.any { it.endsWith("unityplayer.dll") }) {
            return result(
                engine = PuEngineType.UNITY,
                version = PuVersion("Unity"),
                title = root.name,
                confidence = 0.97f,
                evidence = listOf("UnityPlayer.dll", unityData),
                root = root,
                files = files,
            )
        }

        val executable = relativePaths.firstOrNull { it.endsWith(".exe", ignoreCase = true) }
        if (executable != null) {
            return result(
                engine = PuEngineType.GENERIC_EXE,
                version = PuVersion(".exe"),
                title = root.name,
                confidence = 0.8f,
                evidence = listOf(executable),
                root = root,
                files = files,
            )
        }

        return result(
            engine = PuEngineType.UNKNOWN,
            version = PuVersion("Unknown"),
            title = root.name,
            confidence = 0f,
            evidence = emptyList(),
            root = root,
            files = files,
        )
    }

    private fun detectRpgMaker(
        root: File,
        lowerPaths: List<String>,
    ): RpgMakerDetection? {
        val systemJson = lowerPaths.firstOrNull { it == "www/data/system.json" || it.endsWith("/www/data/system.json") }
        val packageJson = lowerPaths.firstOrNull { it == "www/package.json" || it.endsWith("/www/package.json") }
        if (systemJson != null || packageJson != null) {
            val packageText = packageJson?.let { path -> readText(File(root, path), 256 * 1024, 256 * 1024).orEmpty() }
            val isMz = packageText.contains("\"RPGMZ\"", ignoreCase = true)
            val isMv = packageText.contains("\"RPGMV\"", ignoreCase = true)
            val title = findJsonString(packageText, "name")
                ?: findJsonString(readText(File(root, systemJson.orEmpty()), 512 * 1024).orEmpty(), "gameTitle")
                ?: root.name
            return RpgMakerDetection(
                engine = if (isMz) PuEngineType.RPG_MAKER_MZ else PuEngineType.RPG_MAKER_MV,
                version = if (isMz) "MZ" else "MV",
                title = title,
                confidence = if (isMz || isMv) 0.98f else 0.9f,
                evidence = listOfNotNull(systemJson, packageJson),
            )
        }

        val rpgData = lowerPaths.firstOrNull { path ->
            path.endsWith("data/actors.rpg2") ||
                path.endsWith("data/actors.rpgvx") ||
                path.endsWith("data/actors.rpgadv")
        }
        if (rpgData != null) {
            val engine = when {
                rpgData.endsWith("actors.rpg2") -> PuEngineType.RPG_MAKER_XP
                rpgData.endsWith("actors.rpgvx") -> PuEngineType.RPG_MAKER_VX
                else -> PuEngineType.RPG_MAKER_ACE
            }
            val version = when (engine) {
                PuEngineType.RPG_MAKER_XP -> "XP"
                PuEngineType.RPG_MAKER_VX -> "VX"
                else -> "Ace"
            }
            return RpgMakerDetection(engine, version, root.name, 0.98f, listOf(rpgData))
        }

        val legacy = lowerPaths.firstOrNull { path ->
            path.endsWith("rpg_rt.ldb") || path.endsWith("rpg_rt.lmt") || path.endsWith("rpg_rt.exe")
        }
        if (legacy != null) {
            val engine = if (lowerPaths.any { it.endsWith("rpg_rt.ldb") || it.endsWith("rpg_rt.lmt") }) {
                PuEngineType.RPG_MAKER_2000
            } else {
                PuEngineType.RPG_MAKER_2003
            }
            return RpgMakerDetection(engine, engine.name.removePrefix("RPG_MAKER_"), root.name, 0.9f, listOf(legacy))
        }
        return null
    }

    private fun detectRenPyVersion(root: File, files: List<File>): PuVersion {
        val candidates = files.asSequence()
            .filter { file ->
                val name = file.name.lowercase(Locale.ROOT)
                name == "version.py" || name == "__init__.py" || name == "options.rpy" || name == "00bootstrap.rpy"
            }
            .filter { file -> file.length() <= 2L * 1024 * 1024 }
            .sortedBy { file ->
                when {
                    file.name.equals("version.py", true) -> 0
                    file.name.equals("__init__.py", true) -> 1
                    file.name.equals("00bootstrap.rpy", true) -> 2
                    else -> 3
                }
            }
            .toList()

        candidates.forEach { file ->
            val text = readText(file, 2 * 1024 * 1024).orEmpty()
            val tuple = Regex(
                "(?:version_tuple|VERSION_TUPLE)\\s*=\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)(?:\\s*,\\s*(\\d+))?",
                RegexOption.IGNORE_CASE,
            ).find(text)
            if (tuple != null) {
                val major = tuple.groupValues[1].toInt()
                val minor = tuple.groupValues[2].toInt()
                val patch = tuple.groupValues.getOrNull(3)?.toIntOrNull() ?: 0
                return PuVersion(
                    raw = "$major.$minor" + if (patch > 0) ".$patch" else "",
                    family = renPyFamily(major, minor),
                    minor = minor,
                    patch = patch,
                )
            }
        }

        return PuVersion(raw = "Unknown")
    }

    private fun detectRenPyTitle(root: File): String? {
        val options = sequenceOf(
            File(root, "game/options.rpy"),
            File(root, "options.rpy"),
        ).firstOrNull { it.isFile } ?: return null
        val text = readText(options, 512 * 1024).orEmpty()
        return findAssignment(text, "config.name") ?: findAssignment(text, "config.title")
    }

    private fun result(
        engine: PuEngineType,
        version: PuVersion,
        title: String,
        confidence: Float,
        evidence: List<String>,
        root: File,
        files: List<File>,
    ): PuDetectionResult {
        val fingerprintSource = buildString {
            append(engine.name)
            append('|')
            append(version.raw)
            files.asSequence()
                .filter { file -> evidence.any { relativeEvidence -> file.name.equals(relativeEvidence.substringAfterLast('/'), true) } }
                .sortedBy { file -> file.absolutePath }
                .take(32)
                .forEach { file ->
                    append('|')
                    append(if (root.isFile) file.name else file.relativeTo(root).invariantPath())
                    append(':')
                    append(file.length())
                }
        }
        return PuDetectionResult(
            engine = engine,
            version = version,
            title = title.trim().ifBlank { root.name },
            confidence = confidence.coerceIn(0f, 1f),
            evidence = evidence.distinct().take(12),
            fingerprint = PuCrypto.sha256(fingerprintSource.toByteArray(Charsets.UTF_8)),
            supported = engine != PuEngineType.UNKNOWN,
        )
    }

    private fun indexFiles(root: File): List<File> {
        val files = mutableListOf<File>()
        val pending = ArrayDeque<File>()
        pending.addLast(root)
        while (pending.isNotEmpty() && files.size < MAX_INDEXED_FILES) {
            val directory = pending.removeFirst()
            directory.listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    val name = child.name
                    if (!name.startsWith('.') && name !in IGNORED_DIRECTORIES) pending.addLast(child)
                } else if (child.isFile) {
                    files += child
                }
            }
        }
        return files
    }

    private fun readText(file: File, maxBytes: Long): String? {
        if (!file.isFile || file.length() > maxBytes) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    private fun findAssignment(text: String, key: String): String? {
        val escapedKey = Regex.escape(key)
        val match = Regex("$escapedKey\\s*=\\s*([\"'])(.*?)\\1", RegexOption.DOT_MATCHES_ALL).find(text)
        return match?.groupValues?.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun findJsonString(text: String, key: String): String? {
        val match = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]*)\"").find(text)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun renPyFamily(major: Int, minor: Int): String = when {
        major == 8 && minor >= 6 -> "8.6"
        major == 8 && minor >= 4 -> "8.4-8.5"
        major == 8 -> "8.0-8.3"
        major == 7 -> "7.x"
        major == 6 -> "6.x"
        else -> "$major.x"
    }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')

    private data class RpgMakerDetection(
        val engine: PuEngineType,
        val version: String,
        val title: String,
        val confidence: Float,
        val evidence: List<String>,
    )

    private companion object {
        const val MAX_INDEXED_FILES = 20_000
        val IGNORED_DIRECTORIES = setOf("__pycache__", "node_modules", ".git")
    }
}

private val PuVersion.evidence: String?
    get() = if (isKnown) "runtime version $raw" else null
