package sgyt.pu.core.runtime

import android.content.Context
import android.os.StatFs
import sgyt.pu.core.model.DEFAULT_BUFFER_SIZE
import sgyt.pu.core.security.PuCrypto
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

data class PuDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val resumedFromBytes: Long,
)

/** Resumable HTTPS downloader with mandatory integrity verification. */
class PuDownloader(
    private val context: Context,
    private val allowInsecureLocalhost: Boolean = false,
) {
    fun download(
        url: String,
        target: File,
        expectedBytes: Long,
        expectedSha256: String,
        publicKeyBase64: String? = null,
        signatureBase64: String? = null,
        cancellation: AtomicBoolean = AtomicBoolean(false),
        onProgress: (PuDownloadProgress) -> Unit = {},
    ): File {
        val uri = URL(url)
        val secure = uri.protocol.equals("https", ignoreCase = true)
        val local = allowInsecureLocalhost && uri.protocol.equals("http", ignoreCase = true) &&
            (uri.host == "127.0.0.1" || uri.host == "localhost")
        require(secure || local) { "PU downloads require HTTPS" }

        val temporary = File(target.parentFile, "${target.name}.part")
        target.parentFile?.mkdirs()
        val existing = if (temporary.isFile) temporary.length() else 0
        if (existing > expectedBytes && expectedBytes > 0) temporary.delete()

        val connection = uri.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/octet-stream")
            connection.setRequestProperty("User-Agent", "PU-Android/1")
            if (existing > 0) connection.setRequestProperty("Range", "bytes=$existing-")

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_PARTIAL) {
                // Continue the previous transfer.
            } else if (status == HttpURLConnection.HTTP_OK) {
                if (existing > 0) temporary.delete()
            } else {
                throw IOException("Download failed with HTTP $status")
            }

            val responseLength = connection.contentLengthLong
            val total = when {
                responseLength >= 0 && status == HttpURLConnection.HTTP_PARTIAL -> existing + responseLength
                responseLength >= 0 -> responseLength
                expectedBytes > 0 -> expectedBytes
                else -> -1
            }
            if (expectedBytes > 0 && total != expectedBytes) {
                throw IOException("Package size changed: expected $expectedBytes bytes, received $total")
            }
            ensureFreeSpace(expectedBytes)

            val output = FileOutputStream(temporary, status == HttpURLConnection.HTTP_PARTIAL)
            BufferedInputStream(connection.inputStream).use { input ->
                output.use { target ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = if (status == HttpURLConnection.HTTP_PARTIAL) existing else 0
                    while (true) {
                        if (cancellation.get()) throw IOException("Download cancelled")
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(PuDownloadProgress(downloaded, total, existing))
                    }
                }
            }

            if (expectedBytes > 0 && temporary.length() != expectedBytes) {
                throw IOException("Incomplete package: ${temporary.length()} of $expectedBytes bytes")
            }
            val observedHash = PuCrypto.sha256(temporary)
            if (!PuCrypto.constantTimeEquals(observedHash, expectedSha256.lowercase())) {
                temporary.delete()
                throw IOException("Package integrity check failed")
            }
            if (publicKeyBase64 != null && signatureBase64 != null) {
                val valid = PuCrypto.verifyEcdsaSha256(publicKeyBase64, temporary, signatureBase64)
                if (!valid) {
                    temporary.delete()
                    throw IOException("Package signature verification failed")
                }
            }

            if (target.exists() && !target.delete()) throw IOException("Could not replace the previous package")
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            onProgress(PuDownloadProgress(target.length(), total, existing))
            return target
        } finally {
            connection.disconnect()
        }
    }

    private fun ensureFreeSpace(requiredBytes: Long) {
        if (requiredBytes <= 0) return
        val stats = StatFs(context.filesDir.path)
        val reserve = 32L * 1024 * 1024
        if (stats.availableBytes < requiredBytes + reserve) {
            throw IOException("Not enough free storage for this download")
        }
    }
}
