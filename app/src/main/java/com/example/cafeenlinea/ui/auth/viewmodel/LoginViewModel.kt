package com.example.cafeenlinea.ui.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.SinConexionException
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.auth.model.LoginUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.cafeenlinea.R
import androidx.annotation.StringRes

/**
 * Login contra el backend Node/GraphQL. El campo "usuario" de la pantalla ahora
 * recibe el CORREO (el servidor identifica a las personas por correo).
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private var failedAttempts = 0
    private val maxAttempts = 3
    private var lockOutJob: Job? = null

    /** Actualiza el usuario y evalua su error individual */
    fun onUsernameChanged(newUsername: String) {
        _uiState.update { currentState ->
            currentState.copy(
                usernameText = newUsername,
                usernameError = if (newUsername.isBlank()) str(R.string.login_error_email_required) else null,
                error = if (currentState.isLockedOut) currentState.error else null
            )
        }
    }

    /** Actualiza la contrasena y evalua su error individual */
    fun onPasswordChanged(newPassword: String) {
        _uiState.update { currentState ->
            currentState.copy(
                passwordText = newPassword,
                passwordError = if (newPassword.isBlank()) str(R.string.login_error_password_required) else null,
                error = if (currentState.isLockedOut) currentState.error else null
            )
        }
    }

    /** Intenta iniciar sesion con control de bloqueo por intentos fallidos */
    fun login(
        username: String = _uiState.value.usernameText,
        password: String = _uiState.value.passwordText
    ) {
        if (_uiState.value.isLockedOut) return

        val uError = if (username.isBlank()) str(R.string.login_error_email_required) else null
        val pError = if (password.isBlank()) str(R.string.login_error_password_required) else null

        if (uError != null || pError != null) {
            _uiState.update {
                it.copy(
                    usernameError = uError,
                    passwordError = pError,
                    error = str(R.string.login_error_fill_required)
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
                val sesion = ApiClient.login(username.trim(), password)
                session.saveSession(sesion.token, sesion.expiraEn, sesion.usuario)

                // Exito: reiniciamos el contador de fallos
                failedAttempts = 0
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: SinConexionException) {
                // Sin internet, o Render gratis estaba dormido y no alcanzo a despertar
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = str(R.string.error_no_connection)
                    )
                }
            } catch (e: ApiException) {
                if (e.codigo == "UNAUTHENTICATED") {
                    // Correo o contrasena incorrectos
                    failedAttempts++
                    if (failedAttempts >= maxAttempts) {
                        startLockOutTimer()
                    } else {
                        val remaining = maxAttempts - failedAttempts
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = str(R.string.login_error_wrong_credentials, remaining)
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: str(R.string.error_server)) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = str(R.string.login_error_generic))
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
                        error = getApplication<Application>().resources
                            .getQuantityString(R.plurals.login_error_locked, seconds, seconds)
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
}