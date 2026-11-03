package com.example.cafeenlinea.ui.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Scope con vida de proceso para la peticion "best-effort" de logout. No usamos
 * `viewModelScope` porque al cerrar sesion navegamos y destruimos este ViewModel:
 * eso cancelaria la llamada antes de que el servidor la reciba.
 */
private val logoutScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Cerrar sesion son dos cosas independientes:
 * 1. Borrar la sesion del dispositivo -> SIEMPRE y de inmediato.
 * 2. Avisar al servidor (mutacion `logout`) para borrar la sesion en Redis y que
 *    el token deje de servir -> en segundo plano. Si falla (sin red, servidor
 *    dormido, token ya expirado) da igual: la sesion local ya se cerro.
 */
class LogoutViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)

    fun logout(onLoggedOut: () -> Unit) {
        val teniaSesion = ApiClient.token != null

        session.clearSession()
        onLoggedOut()

        if (teniaSesion) {
            logoutScope.launch {
                try {
                    // Usa el token que sigue en memoria y lo limpia al terminar.
                    ApiClient.logout()
                } catch (_: Exception) {
                    // Best-effort: la sesion local ya se cerro.
                }
            }
        }
    }
}