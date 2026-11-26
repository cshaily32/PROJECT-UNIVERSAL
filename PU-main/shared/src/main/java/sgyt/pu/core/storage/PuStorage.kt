package sgyt.pu.core.storage

import android.content.Context
import sgyt.pu.core.json.PuJson
import sgyt.pu.core.json.PuJsonValue
import java.io.File

/**
 * Owns all app-private PU storage. Games are imported into app-owned storage so
 * engines never depend on the slower document-picker path and original files
 * remain separate from generated caches.
 */
class PuStorage(context: Context) {
    val root: File = File(context.filesDir, "pu")
    val gamesRoot: File = File(root, "games")
    val incomingRoot: File = File(root, "incoming")
    val runtimesRoot: File = File(root, "runtimes")
    val cachesRoot: File = File(root, "caches")
    val backupsRoot: File = File(root, "backups")
    val catalogRoot: File = File(root, "catalog")
    val fixesRoot: File = File(root, "fixes")
    val modulesRoot: File = File(root, "modules")
    val translationRoot: File = File(root, "translations")
    val controlsRoot: File = File(root, "controls")
    val sandboxRoot: File = File(root, "sandboxes")
    val stateRoot: File = File(root, "state")
    val tempRoot: File = File(root, "tmp")

    init {
        listOf(
            root,
            gamesRoot,
            incomingRoot,
            runtimesRoot,
            cachesRoot,
            backupsRoot,
            catalogRoot,
            fixesRoot,
            modulesRoot,
            translationRoot,
            controlsRoot,
            sandboxRoot,
            stateRoot,
            tempRoot,
        ).forEach { it.mkdirs() }
    }

    fun gameDirectory(gameId: String): File = safeChild(gamesRoot, gameId)
    fun cacheDirectory(gameId: String): File = safeChild(cachesRoot, gameId)
    fun sandboxDirectory(gameId: String): File = safeChild(sandboxRoot, gameId)
    fun runtimeDirectory(runtimeId: String): File = safeChild(runtimesRoot, runtimeId)
    fun moduleDirectory(moduleId: String): File = safeChild(modulesRoot, moduleId)

    fun readJson(file: File): PuJsonValue? {
        if (!file.isFile) return null
        return runCatching { PuJson.parse(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    fun writeJson(file: File, value: PuJsonValue) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(PuJson.encode(value, pretty = true), Charsets.UTF_8)
        if (!temporary.renameTo(file)) {
            file.writeText(temporary.readText(Charsets.UTF_8), Charsets.UTF_8)
            temporary.delete()
        }
    }

    fun writeText(file: File, text: String) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(text, Charsets.UTF_8)
        if (!temporary.renameTo(file)) {
            file.writeText(temporary.readText(Charsets.UTF_8), Charsets.UTF_8)
            temporary.delete()
        }
    }

    fun deleteRecursively(file: File): Boolean {
        if (!file.exists()) return true
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        return file.delete()
    }

    fun copyDirectory(source: File, destination: File) {
        require(source.isDirectory) { "Source is not a directory: $source" }
        destination.mkdirs()
        source.listFiles()?.forEach { child ->
            val target = File(destination, child.name)
            if (child.isDirectory) {
                copyDirectory(child, target)
            } else {
                child.inputStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    fun directorySize(file: File): Long {
        if (!file.exists()) return 0
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { directorySize(it) } ?: 0
    }

    /** Rejects traversal and absolute child paths before any file operation. */
    fun safeChild(parent: File, name: String): File {
        require(name.isNotBlank()) { "Child name must not be blank" }
        require(name != "." && name != "..") { "Invalid child name" }
        require(!name.contains('/') && !name.contains('\\')) { "Child name must not contain a path separator" }
        val child = File(parent, name).canonicalFile
        val canonicalParent = parent.canonicalFile
        require(child.parentFile == canonicalParent) { "Child escaped its parent directory" }
        return child
    }

    fun newTempFile(prefix: String, suffix: String): File = File.createTempFile(prefix, suffix, tempRoot)
}
