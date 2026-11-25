package com.example.cafeenlinea.ui.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.Escuela
import com.example.cafeenlinea.data.remote.RegistroCafeteriaInput
import com.example.cafeenlinea.data.remote.Sesion
import com.example.cafeenlinea.data.remote.mensajeDeError
import com.example.cafeenlinea.ui.auth.model.BusinessRegisterUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Registro de una cafetería por su dueño ("Registra tu cafetería").
 *
 * @param cargarEscuelas Catálogo de escuelas para las sugerencias.
 * @param registrar Crea negocio, cafetería y cuenta; devuelve la sesión del dueño.
 * @param guardarSesion Guarda la sesión como en el login.
 */
class BusinessRegisterViewModel(
    private val cargarEscuelas: suspend () -> List<Escuela>,
    private val registrar: suspend (RegistroCafeteriaInput) -> Sesion,
    private val guardarSesion: (Sesion) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessRegisterUiState())
    val uiState: StateFlow<BusinessRegisterUiState> = _uiState.asStateFlow()

    init {
        loadEscuelas()
    }

    /** Cambia un campo y borra los errores generales (cada campo borra el suyo en la vista). */
    fun actualizar(cambio: (BusinessRegisterUiState) -> BusinessRegisterUiState) =
        _uiState.update { cambio(it).copy(error = null) }

    fun onEscuelaTextoChanged(value: String) = actualizar { it.copy(escuela = it.escuela.conTexto(value)) }
    fun onEscuelaSugeridaSelected(escuela: Escuela) = actualizar { it.copy(escuela = it.escuela.conSugerencia(escuela)) }
    fun onCiudadChanged(value: String) = actualizar { it.copy(escuela = it.escuela.conCiudad(value)) }

    fun loadEscuelas() {
        viewModelScope.launch {
            _uiState.update { it.copy(escuela = it.escuela.cargandoCatalogo()) }
            try {
                val escuelas = cargarEscuelas()
                _uiState.update { it.copy(escuela = it.escuela.conCatalogo(escuelas)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(escuela = it.escuela.conErrorCatalogo(mensajeDeError(e, "cargar las escuelas")))
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return
        val (validado, ok) = state.validado()
        if (!ok) {
            _uiState.value = validado
            return
        }

        _uiState.value = validado.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val sesion = registrar(validado.aInput())
                guardarSesion(sesion)
                _uiState.update { it.copy(isLoading = false, isRegistered = true) }
            } catch (e: Exception) {
                _uiState.update {
                    if (e is ApiException && e.codigo == "CORREO_DUPLICADO") {
                        it.copy(isLoading = false, correoDuplicado = true)
                    } else {
                        it.copy(isLoading = false, error = mensajeDeError(e, "registrar tu cafetería"))
                    }
                }
            }
        }
    }
}
