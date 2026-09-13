package com.example.cafeenlinea.ui.auth.data

import android.content.Context
import com.example.cafeenlinea.common.preferences.AppPreferences
import com.example.cafeenlinea.ui.auth.model.UserDto

private const val KEY_ACCESS = "auth_access"
private const val KEY_REFRESH = "auth_refresh"
private const val KEY_USERNAME = "auth_username"

/**
 * Expone, con nombres de dominio, la sesión del usuario (tokens JWT + username).
 * La persistencia real vive en [AppPreferences], igual que OnboardingPreferences.
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

    /** Hay sesión si tenemos un refresh token guardado. */
    fun isLoggedIn(): Boolean = !refreshToken().isNullOrBlank()

    /** Borra la sesión del dispositivo (logout). */
    fun clearSession() {
        appPreferences.remove(KEY_ACCESS, KEY_REFRESH, KEY_USERNAME)
    }
}