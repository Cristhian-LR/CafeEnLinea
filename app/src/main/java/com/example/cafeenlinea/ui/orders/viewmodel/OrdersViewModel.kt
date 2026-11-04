package com.example.cafeenlinea.ui.orders.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.data.remote.mensajeDeError
import com.example.cafeenlinea.ui.orders.model.OrdersUiState
import com.example.cafeenlinea.ui.orders.model.separarPedidos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del tab de Pedidos: pide al servidor los pedidos del usuario y los
 * separa en "en curso" e "historial".
 *
 * @param usuarioId Devuelve el id del usuario de la sesión.
 * @param cargarPedidos Pide los pedidos de un usuario (en la app, ApiClient::pedidosDelUsuario).
 */
class OrdersViewModel(
    private val usuarioId: () -> String?,
    private val cargarPedidos: suspend (String) -> List<Pedido>
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Carga con la animación de pantalla completa (primera vez o "Reintentar"). */
    fun load() = fetch(Modo.COMPLETA)

    /** Deslizar hacia abajo: mantiene la lista visible mientras actualiza. */
    fun refresh() = fetch(Modo.DESLIZAR)

    /**
     * Actualización automática mientras el tab está abierto. Solo pregunta si hay
     * pedidos en curso, y no muestra animación ni errores (lo intenta en la siguiente vuelta).
     */
    fun refreshSilently() {
        if (_uiState.value.enCurso.isEmpty()) return
        fetch(Modo.SILENCIOSA)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private enum class Modo { COMPLETA, DESLIZAR, SILENCIOSA }

    private fun fetch(modo: Modo) {
        val actual = _uiState.value
        if (actual.isLoading || actual.isRefreshing) return

        val id = usuarioId()
        if (id.isNullOrBlank()) {
            _uiState.update {
                it.copy(error = "No encontramos tu sesión. Cierra sesión e inicia de nuevo.")
            }
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
                val (enCurso, historial) = separarPedidos(cargarPedidos(id))
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        enCurso = enCurso,
                        historial = historial,
                        loaded = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (modo == Modo.SILENCIOSA) it.error
                        else mensajeDeError(e, "cargar tus pedidos")
                    )
                }
            }
        }
    }
}
