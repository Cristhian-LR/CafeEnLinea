package com.example.cafeenlinea.ui.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.auth.model.LoginRequest
import com.example.cafeenlinea.ui.auth.model.LoginResponse
import com.example.cafeenlinea.ui.auth.model.LoginUiState
import com.example.cafeenlinea.ui.auth.network.AuthRetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException

/**
 * ViewModel de la pantalla de login. Dueño del estado ([LoginUiState]) y de la
 * lógica: llamar a la API, guardar la sesión y traducir los errores a mensajes.
 *
 * Extiende [AndroidViewModel] para pasarle el `Context` a [SessionPreferences]
 * sin arriesgar fugas de memoria (mismo criterio que `OnboardingViewModel`).
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)
    private val authService = AuthRetrofitClient.authService

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Intenta iniciar sesión. Flujo:
     * 1. Valida que los campos no estén vacíos.
     * 2. `isLoading = true`, limpia error previo.
     * 3. `POST /api/auth/login/` (con un reintento si el servidor está dormido).
     * 4. Guarda `access` + `refresh` + `user` y pone `isLoggedIn = true`.
     * 5. Cualquier fallo se traduce a un mensaje para el formulario.
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa usuario y contraseña") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = loginWithRetry(LoginRequest(username.trim(), password))
                session.saveSession(response.access, response.refresh, response.user)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: HttpException) {
                _uiState.update { it.copy(isLoading = false, error = messageForHttp(e.code())) }
            } catch (e: SocketTimeoutException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "El servidor está iniciando. Vuelve a intentar.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Sin conexión. Revisa tu internet e intenta de nuevo.")
                }
            }
        }
    }

    /** Limpia el mensaje de error (p. ej. cuando el usuario vuelve a escribir). */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Llama al login y, si el primer intento cae por timeout (cold start de Render),
     * reintenta una sola vez.
     */
    private suspend fun loginWithRetry(request: LoginRequest): LoginResponse =
        try {
            authService.login(request)
        } catch (e: SocketTimeoutException) {
            authService.login(request)
        }

    private fun messageForHttp(code: Int): String = when (code) {
        401 -> "Usuario o contraseña incorrectos"
        400 -> "Revisa los datos ingresados"
        else -> "No se pudo iniciar sesión (error $code)"
    }
}