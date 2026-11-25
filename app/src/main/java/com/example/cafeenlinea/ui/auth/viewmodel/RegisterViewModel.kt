package com.example.cafeenlinea.ui.auth.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.Escuela
import com.example.cafeenlinea.data.remote.SinConexionException
import com.example.cafeenlinea.data.remote.mensajeDeError
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.auth.model.PASSWORD_MIN_LENGTH
import com.example.cafeenlinea.ui.auth.model.RegisterUiState
import com.example.cafeenlinea.ui.auth.model.validarCuenta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Registro de alumnos contra el backend GraphQL. Si sale bien, el servidor ya
 * devuelve la sesión, así que se guarda igual que en el login y se entra directo.
 */
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private fun str(@StringRes id: Int?): String? = id?.let { str(it, PASSWORD_MIN_LENGTH) }

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

    // ---- Escuela (escrita a mano, con sugerencias del catálogo) ----

    fun loadEscuelas() {
        viewModelScope.launch {
            _uiState.update { it.copy(escuela = it.escuela.cargandoCatalogo()) }
            try {
                val escuelas = ApiClient.escuelas()
                _uiState.update { it.copy(escuela = it.escuela.conCatalogo(escuelas)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(escuela = it.escuela.conErrorCatalogo(mensajeCatalogos(e))) }
            }
        }
    }

    fun onEscuelaTextoChanged(value: String) =
        _uiState.update { it.copy(escuela = it.escuela.conTexto(value), error = null) }

    fun onEscuelaSugeridaSelected(escuela: Escuela) =
        _uiState.update { it.copy(escuela = it.escuela.conSugerencia(escuela), error = null) }

    fun onCiudadChanged(value: String) =
        _uiState.update { it.copy(escuela = it.escuela.conCiudad(value), error = null) }

    // ---- Registro ----

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        val cuenta = validarCuenta(state.nombre, state.correo, state.password, state.confirmPassword)
        val (escuela, escuelaValida) = state.escuela.validada()

        if (cuenta.hayErrores || !escuelaValida) {
            _uiState.update {
                it.copy(
                    nombreError = str(cuenta.nombre),
                    correoError = str(cuenta.correo),
                    passwordError = str(cuenta.password),
                    confirmPasswordError = str(cuenta.confirmPassword),
                    escuela = escuela
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val sesion = ApiClient.registro(
                    nombre = state.nombre.trim(),
                    correo = state.correo.trim(),
                    password = state.password,
                    escuelaId = escuela.seleccionada?.id,
                    nombreEscuela = escuela.nombreLimpio,
                    ciudadEscuela = escuela.ciudadParaEnviar
                )
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
                    _uiState.update { it.copy(isLoading = false, error = mensajeDeError(e, "crear la cuenta")) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = str(R.string.register_error_generic)) }
            }
        }
    }

    private fun mensajeCatalogos(e: Exception): String =
        if (e is SinConexionException) str(R.string.error_no_connection) else str(R.string.register_error_catalogs)
}
