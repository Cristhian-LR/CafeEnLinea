package com.example.cafeenlinea.ui.auth.data

import android.content.Context
import com.example.cafeenlinea.common.preferences.AppPreferences
import com.example.cafeenlinea.data.remote.Usuario

private const val KEY_ACCESS = "auth_access"
private const val KEY_REFRESH = "auth_refresh"          // heredado del backend anterior
private const val KEY_USERNAME = "auth_username"
private const val KEY_USER_ID = "auth_user_id"
private const val KEY_CAFETERIA_ID = "auth_cafeteria_id"
private const val KEY_EXPIRES_AT = "auth_expires_at"
private const val KEY_PHOTO_PATH = "auth_photo_path"
private const val KEY_BIOMETRIC_ENABLED = "auth_biometric_enabled"
private const val KEY_BIOMETRIC_OFFERED = "auth_biometric_offered"

/**
 * Sesion del usuario (token JWT + datos basicos) y preferencia de biometria.
 * La persistencia real vive en [AppPreferences]. El token se guarda cifrado con
 * [TokenCipher] (Android Keystore), igual que antes.
 *
 * El backend nuevo (Node + GraphQL) entrega UN solo token con vida limitada
 * (`expiraEn`, en segundos) y no tiene refresh token.
 */
class SessionPreferences(context: Context) {

    private val appPreferences = AppPreferences(context)

    /** Guarda la sesion tras un login correcto. */
    fun saveSession(token: String, expiraEnSegundos: Int, usuario: Usuario) {
        appPreferences.putString(KEY_ACCESS, TokenCipher.encrypt(token))
        appPreferences.putString(KEY_USERNAME, usuario.nombre)
        appPreferences.putString(KEY_USER_ID, usuario.id)
        appPreferences.putString(KEY_CAFETERIA_ID, usuario.cafeteriaId)
        val expiraEn = System.currentTimeMillis() + expiraEnSegundos * 1000L
        appPreferences.putString(KEY_EXPIRES_AT, expiraEn.toString())
    }

    fun accessToken(): String? = appPreferences.getString(KEY_ACCESS)?.let(TokenCipher::decrypt)

    /** Ya no existe refresh token; se deja para no romper codigo del backend anterior. */
    fun refreshToken(): String? = appPreferences.getString(KEY_REFRESH)?.let(TokenCipher::decrypt)

    fun username(): String? = appPreferences.getString(KEY_USERNAME)
    fun userId(): String? = appPreferences.getString(KEY_USER_ID)
    fun cafeteriaId(): String? = appPreferences.getString(KEY_CAFETERIA_ID)

    /** Guarda la ruta local del archivo de la foto de perfil. */
    fun saveProfilePhotoPath(path: String) {
        appPreferences.putString(KEY_PHOTO_PATH, path)
    }

    /** Ruta local de la foto de perfil, o null si no ha elegido ninguna. */
    fun profilePhotoPath(): String? = appPreferences.getString(KEY_PHOTO_PATH)

    /** Hay sesion si hay token que se puede descifrar y todavia no ha expirado. */
    fun isLoggedIn(): Boolean {
        if (accessToken().isNullOrBlank()) return false
        val expiraEn = appPreferences.getString(KEY_EXPIRES_AT)?.toLongOrNull() ?: return false
        return System.currentTimeMillis() < expiraEn
    }

    /** true si el usuario eligio entrar con huella o rostro. */
    fun isBiometricEnabled(): Boolean = appPreferences.getBoolean(KEY_BIOMETRIC_ENABLED)

    fun setBiometricEnabled(enabled: Boolean) {
        appPreferences.putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
    }

    /** true si ya se le pregunto al usuario por la biometria (solo se pregunta una vez). */
    fun wasBiometricOffered(): Boolean = appPreferences.getBoolean(KEY_BIOMETRIC_OFFERED)

    fun markBiometricOffered() {
        appPreferences.putBoolean(KEY_BIOMETRIC_OFFERED, true)
    }

    /** Borra la sesion del dispositivo (logout). Sin sesion, la biometria no tiene que proteger. */
    fun clearSession() {
        appPreferences.remove(
            KEY_ACCESS, KEY_REFRESH, KEY_USERNAME, KEY_USER_ID, KEY_CAFETERIA_ID,
            KEY_EXPIRES_AT, KEY_PHOTO_PATH, KEY_BIOMETRIC_ENABLED
        )
    }
}