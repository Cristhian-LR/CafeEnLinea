package com.example.cafeenlinea.ui.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.auth.model.LoginRequest
import com.example.cafeenlinea.ui.auth.model.LoginResponse
import com.example.cafeenlinea.ui.auth.model.LoginUiState
import com.example.cafeenlinea.ui.auth.network.AuthRetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)
    private val authService = AuthRetrofitClient.authService

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var failedAttempts = 0
    private val maxAttempts = 3
    private var lockOutJob: Job? = null

    /** Actualiza el usuario y evalúa su error individual */
    fun onUsernameChanged(newUsername: String) {
        _uiState.update { currentState ->
            currentState.copy(
                usernameText = newUsername,
                usernameError = if (newUsername.isBlank()) "El usuario es requerido" else null,
                error = if (currentState.isLockedOut) currentState.error else null
            )
        }
    }

    /** Actualiza la contraseña y evalúa su error individual */
    fun onPasswordChanged(newPassword: String) {
        _uiState.update { currentState ->
            currentState.copy(
                passwordText = newPassword,
                passwordError = if (newPassword.isBlank()) "La contraseña es requerida" else null,
                error = if (currentState.isLockedOut) currentState.error else null
            )
        }
    }

    /** Intenta iniciar sesión con reintentos y control de bloqueo */
    fun login(
        username: String = _uiState.value.usernameText,
        password: String = _uiState.value.passwordText
    ) {
        if (_uiState.value.isLockedOut) return

        val uError = if (username.isBlank()) "El usuario es requerido" else null
        val pError = if (password.isBlank()) "La contraseña es requerida" else null

        if (uError != null || pError != null) {
            _uiState.update {
                it.copy(
                    usernameError = uError,
                    passwordError = pError,
                    error = "Completa los campos requeridos"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    usernameError = null,
                    passwordError = null
                )
            }
            try {
                val response = loginWithRetry(LoginRequest(username.trim(), password))
                session.saveSession(response.access, response.refresh, response.user)

                // Éxito: reiniciamos el contador de fallos
                failedAttempts = 0
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    failedAttempts++
                    if (failedAttempts >= maxAttempts) {
                        startLockOutTimer()
                    } else {
                        val remaining = maxAttempts - failedAttempts
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Usuario o contraseña incorrectos. Intentos restantes: $remaining"
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = messageForHttp(e.code())) }
                }
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

    /** Temporizador de bloqueo temporal por demasiados intentos */
    private fun startLockOutTimer() {
        lockOutJob?.cancel()
        lockOutJob = viewModelScope.launch {
            for (seconds in 30 downTo 1) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLockedOut = true,
                        lockOutSeconds = seconds,
                        error = "Demasiados intentos fallidos. Espera $seconds segundos."
                    )
                }
                delay(1000)
            }
            failedAttempts = 0
            _uiState.update {
                it.copy(
                    isLockedOut = false,
                    lockOutSeconds = 0,
                    error = null
                )
            }
        }
    }

    fun clearError() {
        if (!_uiState.value.isLockedOut) {
            _uiState.update { it.copy(error = null) }
        }
    }

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