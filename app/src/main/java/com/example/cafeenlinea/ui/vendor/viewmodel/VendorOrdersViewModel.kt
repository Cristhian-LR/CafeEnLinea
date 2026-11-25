package com.example.cafeenlinea.ui.vendor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.data.remote.mensajeDeError
import com.example.cafeenlinea.ui.vendor.data.VendorRepository
import com.example.cafeenlinea.ui.vendor.model.ColumnaPedido
import com.example.cafeenlinea.ui.vendor.model.FlujoPedido
import com.example.cafeenlinea.ui.vendor.model.VendorOrdersUiState
import com.example.cafeenlinea.ui.vendor.model.esCodigoRecogidaValido
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Tablero de pedidos del vendedor: Nuevos -> Preparando -> Listos. Cada tarjeta
 * avanza su pedido un paso; para entregarlo se pide el código del comprador.
 *
 * @param cafeteriaId La cafetería que atiende el vendedor (de la sesión).
 */
class VendorOrdersViewModel(
    private val cafeteriaId: String?,
    private val repo: VendorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VendorOrdersUiState())
    val uiState: StateFlow<VendorOrdersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() = fetch(Modo.COMPLETA)
    fun refresh() = fetch(Modo.DESLIZAR)

    /** Revisa si llegaron pedidos nuevos, sin animación ni errores. */
    fun refreshSilently() = fetch(Modo.SILENCIOSA)

    fun selectColumna(columna: ColumnaPedido) = _uiState.update { it.copy(columna = columna) }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMensaje() = _uiState.update { it.copy(mensaje = null) }

    /** Botón principal de la tarjeta. Si el siguiente paso es entregar, abre el diálogo del código. */
    fun avanzar(pedido: Pedido) {
        val siguiente = FlujoPedido.siguiente(pedido.estado) ?: return
        if (siguiente == FlujoPedido.ENTREGADO) {
            _uiState.update { it.copy(entregando = pedido, codigo = "", codigoError = null) }
            return
        }
        cambiarEstado(pedido, siguiente)
    }

    /** El vendedor no puede atenderlo (se acabó algo, cerró la cafetería...). */
    fun rechazar(pedido: Pedido) {
        if (!FlujoPedido.sePuedeRechazar(pedido.estado)) return
        cambiarEstado(pedido, FlujoPedido.CANCELADO, mensaje = R.string.vendor_msg_rejected)
    }

    fun onCodigoChanged(value: String) = _uiState.update {
        it.copy(codigo = value.filter(Char::isDigit).take(4), codigoError = null)
    }

    fun cancelarEntrega() = _uiState.update { it.copy(entregando = null, codigo = "", codigoError = null) }

    fun confirmarEntrega(textoCodigoInvalido: String) {
        val state = _uiState.value
        val pedido = state.entregando ?: return
        if (!esCodigoRecogidaValido(state.codigo)) {
            _uiState.update { it.copy(codigoError = textoCodigoInvalido) }
            return
        }
        cambiarEstado(pedido, FlujoPedido.ENTREGADO, state.codigo, R.string.vendor_msg_delivered)
    }

    private fun cambiarEstado(
        pedido: Pedido,
        nuevoEstado: String,
        codigo: String? = null,
        mensaje: Int? = null
    ) {
        if (pedido.id in _uiState.value.enProceso) return
        _uiState.update { it.copy(enProceso = it.enProceso + pedido.id, error = null) }
        viewModelScope.launch {
            try {
                val actualizado = repo.cambiarEstadoPedido(pedido.id, nuevoEstado, codigo)
                _uiState.update { s ->
                    // Entregado o cancelado ya no está en la cola: se quita
                    val pedidos = if (FlujoPedido.columna(actualizado.estado) == null) {
                        s.pedidos.filterNot { it.id == pedido.id }
                    } else {
                        s.pedidos.map { if (it.id == pedido.id) actualizado else it }
                    }
                    s.copy(
                        pedidos = pedidos,
                        enProceso = s.enProceso - pedido.id,
                        entregando = if (s.entregando?.id == pedido.id) null else s.entregando,
                        codigo = if (s.entregando?.id == pedido.id) "" else s.codigo,
                        mensaje = mensaje
                    )
                }
            } catch (e: Exception) {
                _uiState.update { s ->
                    val enDialogo = s.entregando?.id == pedido.id
                    s.copy(
                        enProceso = s.enProceso - pedido.id,
                        // El error del código se muestra dentro del diálogo
                        codigoError = if (enDialogo && e is ApiException) e.message else s.codigoError,
                        error = if (enDialogo && e is ApiException) s.error
                        else mensajeDeError(e, "actualizar el pedido")
                    )
                }
                // Otro dispositivo pudo haberlo cambiado: se vuelve a pedir la cola
                if (e is ApiException) refreshSilently()
            }
        }
    }

    private enum class Modo { COMPLETA, DESLIZAR, SILENCIOSA }

    private fun fetch(modo: Modo) {
        val actual = _uiState.value
        if (actual.isLoading || actual.isRefreshing) return
        val id = cafeteriaId
        if (id.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Tu cuenta no tiene una cafetería asignada.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                when (modo) {
                    Modo.COMPLETA -> it.copy(isLoading = true, error = null)
                    Modo.DESLIZAR -> it.copy(isRefreshing = true, error = null)
                    Modo.SILENCIOSA -> it
                }
            }
            try {
                val pedidos = repo.colaPedidos(id)
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, loaded = true, pedidos = pedidos) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (modo == Modo.SILENCIOSA) it.error else mensajeDeError(e, "cargar los pedidos")
                    )
                }
            }
        }
    }
}
