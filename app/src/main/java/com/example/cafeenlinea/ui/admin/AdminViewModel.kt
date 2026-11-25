package com.example.cafeenlinea.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.Negocio
import com.example.cafeenlinea.data.remote.mensajeDeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pantalla del equipo de la plataforma: las cafeterías que se registraron
 * esperan aquí a que se cierre el trato con su dueño y se aprueben.
 */
class AdminViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    /** Carga en curso; se cancela al cambiar de pestaña. Va antes de init porque load() la usa. */
    private var carga: Job? = null

    init {
        load()
    }

    fun load() = fetch(refrescando = false)
    fun refresh() = fetch(refrescando = true)

    fun selectFiltro(filtro: FiltroNegocios) {
        if (filtro == _uiState.value.filtro) return
        // La carga de la pestaña anterior ya no sirve
        carga?.cancel()
        _uiState.update {
            it.copy(filtro = filtro, negocios = emptyList(), loaded = false, error = null, isLoading = false, isRefreshing = false)
        }
        load()
    }

    fun clearMensaje() = _uiState.update { it.copy(mensaje = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun aprobar(negocio: Negocio) = cambiar(negocio, R.string.admin_msg_approved) { repo.aprobar(negocio.id) }

    fun abrirRechazo(negocio: Negocio) =
        _uiState.update { it.copy(rechazando = negocio, motivo = "", motivoError = null) }

    fun cerrarRechazo() = _uiState.update { it.copy(rechazando = null, motivo = "", motivoError = null) }

    fun onMotivoChanged(value: String) =
        _uiState.update { it.copy(motivo = value.take(MOTIVO_RECHAZO_MAX), motivoError = null) }

    fun confirmarRechazo() {
        val state = _uiState.value
        val negocio = state.rechazando ?: return
        val motivo = state.motivo.trim()
        if (motivo.isEmpty()) {
            _uiState.update { it.copy(motivoError = R.string.admin_error_reason) }
            return
        }
        _uiState.update { it.copy(rechazando = null, motivo = "") }
        cambiar(negocio, R.string.admin_msg_rejected) { repo.rechazar(negocio.id, motivo) }
    }

    /** Aprueba o rechaza y lo quita de la lista actual (pasa a otra pestaña). */
    private fun cambiar(negocio: Negocio, mensaje: Int, accion: suspend () -> Negocio) {
        if (negocio.id in _uiState.value.enProceso) return
        _uiState.update { it.copy(enProceso = it.enProceso + negocio.id, error = null) }
        viewModelScope.launch {
            try {
                accion()
                _uiState.update { s ->
                    s.copy(
                        negocios = s.negocios.filterNot { it.id == negocio.id },
                        enProceso = s.enProceso - negocio.id,
                        mensaje = mensaje
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(enProceso = it.enProceso - negocio.id, error = mensajeDeError(e, "guardar el cambio"))
                }
            }
        }
    }

    private fun fetch(refrescando: Boolean) {
        val actual = _uiState.value
        if (actual.isLoading || actual.isRefreshing) return
        val filtro = actual.filtro
        carga = viewModelScope.launch {
            _uiState.update {
                if (refrescando) it.copy(isRefreshing = true, error = null) else it.copy(isLoading = true, error = null)
            }
            try {
                val negocios = repo.negocios(filtro.estado)
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, loaded = true, negocios = negocios) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = mensajeDeError(e, "cargar los negocios"))
                }
            }
        }
    }
}
