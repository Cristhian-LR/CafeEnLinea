package com.example.cafeenlinea.ui.auth.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "cafeenlinea_session_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_BITS = 128
private const val SEPARATOR = ":"

/**
 * Cifra y descifra texto (los tokens JWT) con una llave AES-256 que vive en el
 * Android Keystore. La app nunca ve la llave: solo le pide al sistema que cifre
 * o descifre, así que copiar el XML de preferencias no sirve sin el teléfono.
 *
 * Formato guardado: `Base64(IV):Base64(texto cifrado)`. El IV no es secreto,
 * pero se necesita para descifrar, por eso viaja junto al dato.
 */
object TokenCipher {

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return toBase64(cipher.iv) + SEPARATOR + toBase64(encrypted)
    }

    /**
     * Devuelve el texto original, o null si no se puede descifrar (llave borrada,
     * dato dañado o un token viejo guardado en plano). Null equivale a "no hay
     * sesión", así que el usuario simplemente vuelve a iniciar sesión.
     */
    fun decrypt(stored: String): String? = try {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 2) {
            null
        } else {
            val iv = fromBase64(parts[0])
            val encrypted = fromBase64(parts[1])
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }
    } catch (e: Exception) {
        null
    }

    /** Busca la llave por su alias; si es la primera vez, la genera dentro del Keystore. */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun fromBase64(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)
}
