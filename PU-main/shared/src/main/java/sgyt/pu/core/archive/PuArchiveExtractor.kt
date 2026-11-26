package sgyt.pu.core.archive

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

enum class PuArchiveFormat {
    ZIP,
    TAR_GZ,
    UNKNOWN,
}

data class PuArchiveEntry(
    val path: String,
    val sizeBytes: Long,
    val directory: Boolean,
)

data class PuArchiveInspection(
    val format: PuArchiveFormat,
    val entries: List<PuArchiveEntry>,
    val totalUncompressedBytes: Long,
)

/**
 * Safe ZIP and TAR.GZ reader used for user-provided game archives.
 *
 * It rejects traversal paths, links, excessive entry counts, oversized files,
 * and archives whose expanded size exceeds the configured safety budget.
 */
class PuArchiveExtractor(
    private val maxEntries: Int = 100_000,
    private val maxExpandedBytes: Long = 12L * 1024 * 1024 * 1024,
    private val maxSingleFileBytes: Long = 3L * 1024 * 1024 * 1024,
) {
    fun inspect(source: File): PuArchiveInspection {
        val format = detectFormat(source)
        val entries = when (format) {
            PuArchiveFormat.ZIP -> inspectZip(source)
            PuArchiveFormat.TAR_GZ -> inspectTarGz(source)
            PuArchiveFormat.UNKNOWN -> emptyList()
        }
        return PuArchiveInspection(
            format = format,
            entries = entries,
            totalUncompressedBytes = entries.sumOf { it.sizeBytes },
        )
    }

    fun extract(source: File, destination: File) {
        require(destination.mkdirs() || destination.isDirectory) { "Cannot create extraction directory" }
        require((destination.listFiles()?.size ?: 0) == 0) { "Extraction directory must be empty" }
        when (detectFormat(source)) {
            PuArchiveFormat.ZIP -> extractZip(source, destination)
            PuArchiveFormat.TAR_GZ -> extractTarGz(source, destination)
            PuArchiveFormat.UNKNOWN -> throw IOException("Unsupported archive format: ${source.name}")
        }
    }

    fun detectFormat(source: File): PuArchiveFormat {
        if (!source.isFile || source.length() < 4) return PuArchiveFormat.UNKNOWN
        FileInputStream(source).use { input ->
            val header = ByteArray(4)
            if (input.read(header) != 4) return PuArchiveFormat.UNKNOWN
            if (header[0] == 0x50.toByte() && header[1] == 0x4b.toByte() &&
                (header[2] == 0x03.toByte() || header[2] == 0x05.toByte() || header[2] == 0x07.toByte())
            ) {
                return PuArchiveFormat.ZIP
            }
            if (header[0] == 0x1f.toByte() && header[1] == 0x8b.toByte()) return PuArchiveFormat.TAR_GZ
        }
        return PuArchiveFormat.UNKNOWN
    }

    private fun inspectZip(source: File): List<PuArchiveEntry> {
        val entries = mutableListOf<PuArchiveEntry>()
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(FileInputStream(source))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entries.size >= maxEntries) throw IOException("Archive contains too many entries")
                val path = safeRelativePath(entry.name)
                if (entry.isDirectory) {
                    entries += PuArchiveEntry(path, 0, true)
                } else {
                    val declaredSize = if (entry.size >= 0) entry.size else 0
                    if (declaredSize > maxSingleFileBytes) throw IOException("Archive entry is too large: $path")
                    totalBytes += declaredSize
                    if (totalBytes > maxExpandedBytes) throw IOException("Archive expands beyond the safety limit")
                    entries += PuArchiveEntry(path, declaredSize, false)
                }
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun inspectTarGz(source: File): List<PuArchiveEntry> {
        val entries = mutableListOf<PuArchiveEntry>()
        var totalBytes = 0L
        GZIPInputStream(BufferedInputStream(FileInputStream(source))).use { gzip ->
            val header = ByteArray(512)
            while (true) {
                if (!readBlock(gzip, header)) break
                if (header.all { it == 0.toByte() }) continue
                val name = readTarString(header, 0, 100)
                val prefix = readTarString(header, 345, 155)
                val fullName = if (prefix.isBlank()) name else "$prefix/$name"
                val path = safeRelativePath(fullName)
                val size = readTarSize(header)
                val type = header[156].toInt().toChar()
                if (type == '1' || type == '2') throw IOException("Archive links are not allowed")
                if (size > maxSingleFileBytes) throw IOException("Archive entry is too large: $path")
                if (type != '5') {
                    totalBytes += size
                    if (totalBytes > maxExpandedBytes) throw IOException("Archive expands beyond the safety limit")
                }
                if (entries.size >= maxEntries) throw IOException("Archive contains too many entries")
                entries += PuArchiveEntry(path, if (type == '5') 0 else size, type == '5')
                skipFully(gzip, paddedSize(size))
            }
        }
        return entries
    }

    private fun extractZip(source: File, destination: File) {
        var count = 0
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(FileInputStream(source))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++count > maxEntries) throw IOException("Archive contains too many entries")
                val path = safeRelativePath(entry.name)
                val target = resolveTarget(destination, path)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    val bytes = copyLimited(zip, target, maxSingleFileBytes)
                    totalBytes += bytes
                    if (totalBytes > maxExpandedBytes) {
                        target.delete()
                        throw IOException("Archive expands beyond the safety limit")
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun extractTarGz(source: File, destination: File) {
        var count = 0
        var totalBytes = 0L
        GZIPInputStream(BufferedInputStream(FileInputStream(source))).use { gzip ->
            val header = ByteArray(512)
            while (true) {
                if (!readBlock(gzip, header)) break
                if (header.all { it == 0.toByte() }) continue
                if (++count > maxEntries) throw IOException("Archive contains too many entries")
                val name = readTarString(header, 0, 100)
                val prefix = readTarString(header, 345, 155)
                val path = safeRelativePath(if (prefix.isBlank()) name else "$prefix/$name")
                val size = readTarSize(header)
                val type = header[156].toInt().toChar()
                if (type == '1' || type == '2') throw IOException("Archive links are not allowed")
                val target = resolveTarget(destination, path)
                if (type == '5') {
                    target.mkdirs()
                    skipFully(gzip, paddedSize(size))
                } else {
                    target.parentFile?.mkdirs()
                    totalBytes += copyExactly(gzip, target, size)
                    if (totalBytes > maxExpandedBytes) {
                        target.delete()
                        throw IOException("Archive expands beyond the safety limit")
                    }
                    skipFully(gzip, paddedSize(size) - size)
                }
            }
        }
    }

    private fun copyLimited(input: java.io.InputStream, target: File, limit: Long): Long {
        var written = 0L
        BufferedOutputStream(FileOutputStream(target)).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                written += read
                if (written > limit) throw IOException("Archive entry is too large")
                output.write(buffer, 0, read)
            }
        }
        return written
    }

    private fun copyExactly(input: java.io.InputStream, target: File, expectedBytes: Long): Long {
        require(expectedBytes <= maxSingleFileBytes) { "Archive entry is too large" }
        var remaining = expectedBytes
        var written = 0L
        BufferedOutputStream(FileOutputStream(target)).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (remaining > 0) {
                val read = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
                if (read < 0) throw IOException("Unexpected end of archive")
                output.write(buffer, 0, read)
                remaining -= read
                written += read
            }
        }
        return written
    }

    private fun safeRelativePath(rawPath: String): String {
        val normalized = rawPath.replace('\\', '/').trim()
        require(normalized.isNotBlank()) { "Archive entry has an empty path" }
        require(!normalized.startsWith('/')) { "Absolute archive paths are not allowed" }
        require(!WINDOWS_ABSOLUTE.matches(normalized)) { "Absolute archive paths are not allowed" }
        val segments = normalized.split('/').filter { it.isNotBlank() && it != "." }
        require(segments.none { it == ".." }) { "Archive path traversal is not allowed" }
        require(segments.isNotEmpty()) { "Archive entry has an empty path" }
        return segments.joinToString("/")
    }

    private fun resolveTarget(destination: File, relativePath: String): File {
        val root = destination.canonicalFile
        val target = File(root, relativePath).canonicalFile
        require(target == root || target.path.startsWith(root.path + File.separator)) {
            "Archive path escaped the extraction directory"
        }
        return target
    }

    private fun readBlock(input: java.io.InputStream, block: ByteArray): Boolean {
        var offset = 0
        while (offset < block.size) {
            val read = input.read(block, offset, block.size - offset)
            if (read < 0) return false
            offset += read
        }
        return true
    }

    private fun readTarString(header: ByteArray, offset: Int, length: Int): String {
        val end = (offset until offset + length).firstOrNull { header[it] == 0 } ?: offset + length
        return String(header, offset, end - offset, Charsets.UTF_8).trim()
    }

    private fun readTarSize(header: ByteArray): Long {
        val value = readTarString(header, 124, 12)
        return value.toLongOrNull(8) ?: throw IOException("Invalid TAR archive size")
    }

    private fun paddedSize(size: Long): Long = ((size + 511L) / 512L) * 512L

    private fun skipFully(input: java.io.InputStream, bytes: Long) {
        var remaining = bytes
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
            if (read < 0) throw IOException("Unexpected end of archive")
            remaining -= read
        }
    }

    private companion object {
        val WINDOWS_ABSOLUTE = Regex("^[A-Za-z]:/.*")
    }
}

private fun File.isArchiveName(): Boolean = name.lowercase(Locale.ROOT).let { name ->
    name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".tgz")
}
