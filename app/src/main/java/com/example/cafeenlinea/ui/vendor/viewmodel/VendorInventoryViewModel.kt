package com.example.cafeenlinea.ui.vendor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.data.remote.mensajeDeError
import com.example.cafeenlinea.ui.vendor.data.VendorRepository
import com.example.cafeenlinea.ui.vendor.model.VendorInventoryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mayor cantidad que se deja reponer de una vez (evita errores de dedo como 2000). */
const val REPONER_MAXIMO = 999

/** Inventario del vendedor: lo que se está acabando arriba y botón para reponer. */
class VendorInventoryViewModel(
    private val cafeteriaId: String?,
    private val repo: VendorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VendorInventoryUiState())
    val uiState: StateFlow<VendorInventoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() = fetch(refrescando = false)
    fun refresh() = fetch(refrescando = true)

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMensaje() = _uiState.update { it.copy(mensaje = null) }

    /** Abre el diálogo con la cantidad de reposición del producto ya escrita. */
    fun abrirReponer(producto: Producto) = _uiState.update {
        it.copy(
            reponiendo = producto,
            cantidad = producto.inventario.cantidadReposicion.takeIf { c -> c > 0 }?.toString().orEmpty(),
            cantidadError = null
        )
    }

    fun cerrarReponer() = _uiState.update { it.copy(reponiendo = null, cantidad = "", cantidadError = null) }

    fun onCantidadChanged(value: String) =
        _uiState.update { it.copy(cantidad = value.filter(Char::isDigit).take(3), cantidadError = null) }

    fun confirmarReponer() {
        val state = _uiState.value
        val producto = state.reponiendo ?: return
        val cantidad = state.cantidad.toIntOrNull()
        if (cantidad == null || cantidad !in 1..REPONER_MAXIMO) {
            _uiState.update { it.copy(cantidadError = R.string.vendor_restock_error_quantity) }
            return
        }
        if (producto.id in state.enProceso) return

        _uiState.update { it.copy(enProceso = it.enProceso + producto.id, reponiendo = null, cantidad = "") }
        viewModelScope.launch {
            try {
                val actualizado = repo.reponer(producto.id, cantidad)
                _uiState.update { s ->
                    s.copy(
                        productos = s.productos.map { if (it.id == actualizado.id) actualizado else it },
                        enProceso = s.enProceso - producto.id,
                        mensaje = R.string.vendor_msg_restocked
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(enProceso = it.enProceso - producto.id, error = mensajeDeError(e, "reponer el producto"))
                }
            }
        }
    }

    private fun fetch(refrescando: Boolean) {
        val actual = _uiState.value
        if (actual.isLoading || actual.isRefreshing) return
        val id = cafeteriaId
        if (id.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Tu cuenta no tiene una cafetería asignada.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                if (refrescando) it.copy(isRefreshing = true, error = null) else it.copy(isLoading = true, error = null)
            }
            try {
                val productos = repo.productos(id)
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, loaded = true, productos = productos) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = mensajeDeError(e, "cargar el inventario"))
                }
            }
        }
    }
}
