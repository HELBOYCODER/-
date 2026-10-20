package ir.ersaz.sandogh

import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-CBC streaming encryption. Each file: [16-byte IV][ciphertext].
 * The key is derived from the dial code with PBKDF2, so files can only be
 * read back with the same code.
 */
object Crypto {
    private const val ITERATIONS = 60_000
    private const val TRANSFORM = "AES/CBC/PKCS5Padding"

    fun deriveKey(code: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(code.toCharArray(), salt, ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    /** Reads plaintext from [input], writes IV + ciphertext to [out]. Closes nothing. */
    fun encrypt(input: InputStream, out: OutputStream, key: SecretKey) {
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        out.write(iv)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val cos = CipherOutputStream(out, cipher)
        input.copyTo(cos, 64 * 1024)
        cos.close() // flushes final block; also closes out
    }

    /** Reads IV + ciphertext from [input], writes plaintext to [out]. */
    fun decrypt(input: InputStream, out: OutputStream, key: SecretKey) {
        val din = DataInputStream(input)
        val iv = ByteArray(16)
        din.readFully(iv)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        val cis = CipherInputStream(din, cipher)
        cis.copyTo(out, 64 * 1024)
        out.flush()
    }
}
