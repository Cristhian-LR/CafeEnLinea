package com.example.cafeenlinea

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.auth.data.TokenCipher
import com.example.cafeenlinea.ui.auth.model.UserDto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas del cifrado de tokens. Son instrumentadas (corren en el teléfono o
 * emulador) porque el Android Keystore no existe en la JVM de las pruebas unitarias.
 */
@RunWith(AndroidJUnit4::class)
class TokenCipherTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val session = SessionPreferences(context)
    private val fakeJwt = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjoxfQ.firma"

    @After
    fun tearDown() {
        session.clearSession()
    }

    @Test
    fun encryptThenDecrypt_returnsOriginalText() {
        val stored = TokenCipher.encrypt(fakeJwt)

        assertNotEquals(fakeJwt, stored)
        assertFalse(stored.contains("eyJ"))
        assertEquals(fakeJwt, TokenCipher.decrypt(stored))
    }

    @Test
    fun encryptTwice_usesDifferentIv() {
        assertNotEquals(TokenCipher.encrypt(fakeJwt), TokenCipher.encrypt(fakeJwt))
    }

    @Test
    fun decryptPlainOrCorruptedText_returnsNull() {
        assertNull(TokenCipher.decrypt(fakeJwt))
        assertNull(TokenCipher.decrypt("abc:def"))
    }

    @Test
    fun sessionPreferences_storesTokensEncrypted() {
        session.saveSession("access-123", "refresh-456", UserDto(1, "mario", "m@x.com"))

        assertEquals("access-123", session.accessToken())
        assertEquals("refresh-456", session.refreshToken())
        assertTrue(session.isLoggedIn())

        val raw = context.getSharedPreferences("app_prefs", 0).getString("auth_refresh", null)
        assertNotEquals("refresh-456", raw)
    }

    @Test
    fun clearSession_turnsOffBiometrics() {
        session.saveSession("a", "r", UserDto(1, "mario", "m@x.com"))
        session.setBiometricEnabled(true)

        session.clearSession()

        assertFalse(session.isLoggedIn())
        assertFalse(session.isBiometricEnabled())
    }
}
