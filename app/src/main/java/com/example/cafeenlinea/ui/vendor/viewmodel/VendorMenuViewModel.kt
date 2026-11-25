package com.example.cafeenlinea.ui.vendor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.data.remote.mensajeDeError
import com.example.cafeenlinea.ui.vendor.data.VendorRepository
import com.example.cafeenlinea.ui.vendor.model.ProductForm
import com.example.cafeenlinea.ui.vendor.model.VendorMenuUiState
import com.example.cafeenlinea.ui.vendor.model.aInput
import com.example.cafeenlinea.ui.vendor.model.resolverCategoria
import com.example.cafeenlinea.ui.vendor.model.validar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Menú del vendedor: productos por categoría, interruptor de disponible/agotado
 * y formulario para crear o editar productos (con foto).
 *
 * @param subirImagen Sube la foto elegida (Uri como texto) y devuelve su URL https.
 */
class VendorMenuViewModel(
    private val cafeteriaId: String?,
    private val repo: VendorRepository,
    private val subirImagen: suspend (String) -> String
) : ViewModel() {

    private val _uiState = MutableStateFlow(VendorMenuUiState())
    val uiState: StateFlow<VendorMenuUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() = fetch(refrescando = false)
    fun refresh() = fetch(refrescando = true)

    fun onBusquedaChanged(value: String) = _uiState.update { it.copy(busqueda = value) }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMensaje() = _uiState.update { it.copy(mensaje = null) }

    /** "Agotado" / "Disponible". Se ve el cambio al instante y se revierte si el servidor falla. */
    fun toggleDisponible(producto: Producto) {
        if (producto.id in _uiState.value.enProceso) return
        val nuevo = !producto.disponible
        reemplazar(producto.copy(disponible = nuevo))
        _uiState.update { it.copy(enProceso = it.enProceso + producto.id) }
        viewModelScope.launch {
            try {
                val actualizado = repo.cambiarDisponibilidad(producto.id, nuevo)
                reemplazar(actualizado)
                _uiState.update { it.copy(enProceso = it.enProceso - producto.id) }
            } catch (e: Exception) {
                reemplazar(producto)
                _uiState.update {
                    it.copy(enProceso = it.enProceso - producto.id, error = mensajeDeError(e, "cambiar el producto"))
                }
            }
        }
    }

    // ---- Formulario ----

    fun nuevoProducto() = _uiState.update { it.copy(formulario = ProductForm.nuevo(it.moneda)) }

    fun editar(producto: Producto) = _uiState.update { it.copy(formulario = ProductForm.desde(producto)) }

    fun cerrarFormulario() {
        if (_uiState.value.formulario?.guardando == true) return
        _uiState.update { it.copy(formulario = null) }
    }

    /** Cambia un campo del formulario y borra su error. */
    fun actualizarFormulario(cambio: (ProductForm) -> ProductForm) = _uiState.update { s ->
        val form = s.formulario ?: return@update s
        s.copy(formulario = cambio(form).copy(error = null))
    }

    fun guardar() {
        val state = _uiState.value
        val form = state.formulario ?: return
        if (form.guardando) return
        val id = cafeteriaId
        if (id.isNullOrBlank()) return

        val errores = form.validar()
        if (errores.hayErrores) {
            _uiState.update { it.copy(formulario = form.copy(errores = errores)) }
            return
        }

        _uiState.update { it.copy(formulario = form.copy(guardando = true, errores = errores, error = null)) }
        viewModelScope.launch {
            try {
                // Primero la foto: si falla, no se guarda el producto a medias
                val imagen = form.imagenLocal?.let { subirImagen(it) } ?: form.imagenUrl
                val datos = form.aInput(resolverCategoria(state.nombresCategorias, form.categoria), imagen)
                val guardado = if (form.esNuevo) {
                    repo.registrarProducto(id, datos)
                } else {
                    repo.actualizarProducto(form.productoId!!, datos)
                }
                _uiState.update { s ->
                    val existe = s.productos.any { it.id == guardado.id }
                    s.copy(
                        productos = if (existe) s.productos.map { if (it.id == guardado.id) guardado else it }
                        else s.productos + guardado,
                        formulario = null,
                        mensaje = if (form.esNuevo) R.string.vendor_msg_product_created else R.string.vendor_msg_product_saved
                    )
                }
            } catch (e: Exception) {
                _uiState.update { s ->
                    s.copy(formulario = s.formulario?.copy(guardando = false, error = mensajeDeError(e, "guardar el producto")))
                }
            }
        }
    }

    private fun reemplazar(producto: Producto) = _uiState.update { s ->
        s.copy(productos = s.productos.map { if (it.id == producto.id) producto else it })
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
                    it.copy(isLoading = false, isRefreshing = false, error = mensajeDeError(e, "cargar el menú"))
                }
            }
        }
    }
}
