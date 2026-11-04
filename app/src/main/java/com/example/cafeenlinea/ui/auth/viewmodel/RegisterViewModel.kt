package com.example.cafeenlinea.ui.auth.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.Cafeteria
import com.example.cafeenlinea.data.remote.Escuela
import com.example.cafeenlinea.data.remote.SinConexionException
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.auth.model.RegisterUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mismo mínimo que valida el servidor en la mutación `registro`. */
internal const val PASSWORD_MIN_LENGTH = 6

/**
 * Registro contra el backend GraphQL. Si sale bien, el servidor ya devuelve la
 * sesión, así que se guarda igual que en el login y se entra directo a la app.
 */
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var cafeteriasJob: Job? = null

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    init {
        loadEscuelas()
    }

    // ---- Campos ----

    fun onNombreChanged(value: String) =
        _uiState.update { it.copy(nombre = value, nombreError = null, error = null) }

    fun onCorreoChanged(value: String) =
        _uiState.update { it.copy(correo = value, correoError = null, error = null) }

    fun onPasswordChanged(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, error = null) }

    fun onConfirmPasswordChanged(value: String) =
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, error = null) }

    // ---- Escuela y cafetería ----

    fun loadEscuelas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCatalogos = true, catalogosError = null) }
            try {
                val escuelas = ApiClient.escuelas()
                _uiState.update { it.copy(isLoadingCatalogos = false, escuelas = escuelas) }
                // Si solo hay una escuela, se elige sola
                escuelas.singleOrNull()?.let { onEscuelaSelected(it) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCatalogos = false, catalogosError = mensajeCatalogos(e)) }
            }
        }
    }

    fun onEscuelaSelected(escuela: Escuela) {
        _uiState.update {
            it.copy(
                escuelaSeleccionada = escuela,
                cafeterias = emptyList(),
                cafeteriaSeleccionada = null,
                error = null
            )
        }
        cafeteriasJob?.cancel()
        cafeteriasJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCatalogos = true, catalogosError = null) }
            try {
                val cafeterias = ApiClient.cafeteriasPorEscuela(escuela.id)
                _uiState.update {
                    it.copy(
                        isLoadingCatalogos = false,
                        cafeterias = cafeterias,
                        cafeteriaSeleccionada = cafeterias.singleOrNull()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCatalogos = false, catalogosError = mensajeCatalogos(e)) }
            }
        }
    }

    fun onCafeteriaSelected(cafeteria: Cafeteria) =
        _uiState.update { it.copy(cafeteriaSeleccionada = cafeteria, error = null) }

    /** Reintenta lo que haya fallado: escuelas, o las cafeterías de la escuela elegida. */
    fun retryCatalogos() {
        val escuela = _uiState.value.escuelaSeleccionada
        if (escuela == null) loadEscuelas() else onEscuelaSelected(escuela)
    }

    // ---- Registro ----

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        val nombre = state.nombre.trim()
        val correo = state.correo.trim()

        val nombreError = if (nombre.isBlank()) str(R.string.register_error_name_required) else null
        val correoError = when {
            correo.isBlank() -> str(R.string.login_error_email_required)
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> str(R.string.register_error_email_invalid)
            else -> null
        }
        val passwordError = when {
            state.password.isBlank() -> str(R.string.login_error_password_required)
            state.password.length < PASSWORD_MIN_LENGTH -> str(R.string.register_error_password_short, PASSWORD_MIN_LENGTH)
            else -> null
        }
        val confirmError =
            if (state.confirmPassword != state.password) str(R.string.register_error_password_mismatch) else null
        val cafeteria = state.cafeteriaSeleccionada

        if (nombreError != null || correoError != null || passwordError != null ||
            confirmError != null || cafeteria == null
        ) {
            _uiState.update {
                it.copy(
                    nombreError = nombreError,
                    correoError = correoError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                    error = if (cafeteria == null) str(R.string.register_error_cafeteria_required) else null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val sesion = ApiClient.registro(nombre, correo, state.password, cafeteria.id)
                session.saveSession(sesion.token, sesion.expiraEn, sesion.usuario)
                _uiState.update { it.copy(isLoading = false, isRegistered = true) }
            } catch (e: SinConexionException) {
                _uiState.update { it.copy(isLoading = false, error = str(R.string.error_no_connection)) }
            } catch (e: ApiException) {
                if (e.codigo == "CORREO_DUPLICADO") {
                    _uiState.update {
                        it.copy(isLoading = false, correoError = str(R.string.register_error_email_taken))
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: str(R.string.error_server)) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = str(R.string.register_error_generic)) }
            }
        }
    }

    private fun mensajeCatalogos(e: Exception): String =
        if (e is SinConexionException) str(R.string.error_no_connection) else str(R.string.register_error_catalogs)
}
