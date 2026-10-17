package com.example.cafeenlinea.ui.auth.data

import android.content.Context
import com.example.cafeenlinea.common.preferences.AppPreferences
import com.example.cafeenlinea.ui.auth.model.UserDto

private const val KEY_ACCESS = "auth_access"
private const val KEY_REFRESH = "auth_refresh"
private const val KEY_USERNAME = "auth_username"
private const val KEY_PHOTO_PATH = "auth_photo_path"

/**
 * Expone, con nombres de dominio, la sesión del usuario (tokens JWT + username
 * + foto de perfil). La persistencia real vive en [AppPreferences], igual que
 * OnboardingPreferences.
 *
 * Nota: se guarda en `SharedPreferences` plano para mantener el estándar del
 * proyecto. Si se quisiera cifrar, el cambio queda aislado en esta clase.
 */
class SessionPreferences(context: Context) {

    private val appPreferences = AppPreferences(context)

    /** Guarda la sesión tras un login correcto. */
    fun saveSession(access: String, refresh: String, user: UserDto) {
        appPreferences.putString(KEY_ACCESS, access)
        appPreferences.putString(KEY_REFRESH, refresh)
        appPreferences.putString(KEY_USERNAME, user.username)
    }

    fun accessToken(): String? = appPreferences.getString(KEY_ACCESS)
    fun refreshToken(): String? = appPreferences.getString(KEY_REFRESH)
    fun username(): String? = appPreferences.getString(KEY_USERNAME)

    /** Guarda la ruta local del archivo de la foto de perfil. */
    fun saveProfilePhotoPath(path: String) {
        appPreferences.putString(KEY_PHOTO_PATH, path)
    }

    /** Ruta local de la foto de perfil, o null si no ha elegido ninguna. */
    fun profilePhotoPath(): String? = appPreferences.getString(KEY_PHOTO_PATH)

    /** Hay sesión si tenemos un refresh token guardado. */
    fun isLoggedIn(): Boolean = !refreshToken().isNullOrBlank()

    /** Borra la sesión del dispositivo (logout). */
    fun clearSession() {
        appPreferences.remove(KEY_ACCESS, KEY_REFRESH, KEY_USERNAME, KEY_PHOTO_PATH)
    }
}