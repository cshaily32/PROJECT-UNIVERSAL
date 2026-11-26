package sgyt.pu.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.Signature

class PuCryptoSignatureTest {
    @Test
    fun fileSignatureVerifiesTheExactPackageBytes() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val payload = "signed runtime package".encodeToByteArray()
        val packageFile = Files.createTempFile("pu-signature", ".pkg").toFile()
        try {
            packageFile.writeBytes(payload)
            val signatureBytes = Signature.getInstance("SHA256withECDSA").run {
                initSign(keyPair.private)
                update(payload)
                sign()
            }
            val publicKey = PuBase64.encode(keyPair.public.encoded)
            val signature = PuBase64.encode(signatureBytes)

            assertTrue(PuCrypto.verifyEcdsaSha256(publicKey, packageFile, signature))
            packageFile.writeText("tampered")
            assertFalse(PuCrypto.verifyEcdsaSha256(publicKey, packageFile, signature))
        } finally {
            packageFile.delete()
        }
    }
}
