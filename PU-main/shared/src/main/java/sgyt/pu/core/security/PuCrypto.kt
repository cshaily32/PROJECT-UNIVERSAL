package sgyt.pu.core.security

import java.io.File
import java.io.InputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Cryptographic checks used before any downloaded PU package is executed. */
object PuCrypto {
    private const val SHA_256 = "SHA-256"
    private const val ECDSA_SHA_256 = "SHA256withECDSA"
    private const val HMAC_SHA_256 = "HmacSHA256"

    fun sha256(bytes: ByteArray): String = toHex(
        MessageDigest.getInstance(SHA_256).digest(bytes),
    )

    fun sha256(file: File): String {
        return toHex(sha256Digest(file))
    }

    fun sha256Digest(file: File): ByteArray {
        val digest = MessageDigest.getInstance(SHA_256)
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest()
    }

    fun sha256(stream: InputStream): String {
        val digest = MessageDigest.getInstance(SHA_256)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return toHex(digest.digest())
    }

    /**
     * Verifies a detached ECDSA P-256 signature over the exact payload bytes.
     * Catalogs can use the same primitive for package signatures.
     */
    fun verifyEcdsaSha256(publicKeyBase64: String, payload: ByteArray, signatureBase64: String): Boolean = runCatching {
        val keyBytes = PuBase64.decode(publicKeyBase64)
        val signatureBytes = PuBase64.decode(signatureBase64)
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
        Signature.getInstance(ECDSA_SHA_256).run {
            initVerify(key)
            update(payload)
            verify(signatureBytes)
        }
    }.getOrDefault(false)

    fun verifyEcdsaSha256(publicKeyBase64: String, payloadFile: File, signatureBase64: String): Boolean = runCatching {
        val keyBytes = PuBase64.decode(publicKeyBase64)
        val signatureBytes = PuBase64.decode(signatureBase64)
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
        Signature.getInstance(ECDSA_SHA_256).run {
            initVerify(key)
            payloadFile.inputStream().buffered().use { stream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    update(buffer, 0, read)
                }
            }
            verify(signatureBytes)
        }
    }.getOrDefault(false)

    fun hmacSha256(key: ByteArray, payload: ByteArray): String {
        val mac = Mac.getInstance(HMAC_SHA_256)
        mac.init(SecretKeySpec(key, HMAC_SHA_256))
        return toHex(mac.doFinal(payload))
    }

    fun constantTimeEquals(left: String, right: String): Boolean {
        val leftBytes = left.toByteArray(Charsets.UTF_8)
        val rightBytes = right.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(leftBytes, rightBytes)
    }

    private fun toHex(bytes: ByteArray): String = buildString(bytes.size * 2) {
        bytes.forEach { byte ->
            val value = byte.toInt() and 0xff
            append(HEX[value ushr 4])
            append(HEX[value and 0x0f])
        }
    }

    private const val HEX = "0123456789abcdef"
}

/** API-24-safe Base64 codec; java.util.Base64 is only available from API 26. */
internal object PuBase64 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun decode(value: String): ByteArray {
        val clean = value.trim().replace("\n", "").replace("\r", "")
        require(clean.length % 4 == 0) { "Invalid Base64 length" }
        val output = ArrayList<Byte>(clean.length / 4 * 3)
        var index = 0
        while (index < clean.length) {
            val first = decodeChar(clean[index++])
            val second = decodeChar(clean[index++])
            require(index < clean.length) { "Invalid Base64 length" }
            val thirdCharacter = clean[index++]
            val fourthCharacter = clean[index++]
            val third = if (thirdCharacter == '=') -1 else decodeChar(thirdCharacter)
            val fourth = if (fourthCharacter == '=') -1 else decodeChar(fourthCharacter)
            output += ((first shl 2) or (second shr 4)).toByte()
            if (third >= 0) output += (((second and 0x0f) shl 4) or (third shr 2)).toByte()
            if (fourth >= 0) output += (((third and 0x03) shl 6) or fourth).toByte()
        }
        return output.toByteArray()
    }

    fun encode(bytes: ByteArray): String = buildString((bytes.size + 2) / 3 * 4) {
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index].toInt() and 0xff
            val second = if (index + 1 < bytes.size) bytes[index + 1].toInt() and 0xff else 0
            val third = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xff else 0
            append(ALPHABET[first shr 2])
            append(ALPHABET[((first and 0x03) shl 4) or (second shr 4)])
            append(if (index + 1 < bytes.size) ALPHABET[((second and 0x0f) shl 2) or (third shr 6)] else '=')
            append(if (index + 2 < bytes.size) ALPHABET[third and 0x3f] else '=')
            index += 3
        }
    }

    private fun decodeChar(character: Char): Int = when (character) {
        in 'A'..'Z' -> character - 'A'
        in 'a'..'z' -> character - 'a' + 26
        in '0'..'9' -> character - '0' + 52
        '+' -> 62
        '/' -> 63
        '=' -> -1
        else -> error("Invalid Base64 character")
    }
}
