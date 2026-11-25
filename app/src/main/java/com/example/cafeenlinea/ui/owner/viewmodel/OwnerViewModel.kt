package com.example.cafeenlinea.ui.owner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.Escuela
import com.example.cafeenlinea.data.remote.mensajeDeError
import com.example.cafeenlinea.ui.owner.data.OwnerRepository
import com.example.cafeenlinea.ui.owner.model.CafeteriaForm
import com.example.cafeenlinea.ui.owner.model.OwnerUiState
import com.example.cafeenlinea.ui.owner.model.VendedorForm
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pantalla del dueño: su negocio (y si está en revisión), sus cafeterías y sus
 * vendedores. Pedidos, menú, stock y resumen los manejan los ViewModel del
 * vendedor con la cafetería elegida.
 */
class OwnerViewModel(private val repo: OwnerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OwnerUiState())
    val uiState: StateFlow<OwnerUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // coroutineScope: si falla una consulta, el error llega a este catch
                // en lugar de tumbar la corrutina del ViewModel
                val (n, lista) = coroutineScope {
                    val negocio = async { repo.miNegocio() }
                    // Si falla la lista de vendedores, el negocio se muestra igual
                    val vendedores = async { runCatching { repo.vendedores() }.getOrDefault(emptyList()) }
                    negocio.await() to vendedores.await()
                }
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        loaded = true,
                        negocio = n,
                        vendedores = lista,
                        // Conserva la cafetería elegida si sigue existiendo
                        cafeteriaSeleccionadaId = s.cafeteriaSeleccionadaId
                            ?.takeIf { id -> n.cafeterias.orEmpty().any { it.id == id } }
                            ?: n.cafeterias?.firstOrNull()?.id
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mensajeDeError(e, "cargar tu negocio")) }
            }
        }
    }

    fun seleccionarCafeteria(id: String) = _uiState.update { it.copy(cafeteriaSeleccionadaId = id) }

    fun clearMensaje() = _uiState.update { it.copy(mensaje = null) }

    // ---- Alta de vendedores ----

    fun nuevoVendedor() = _uiState.update {
        it.copy(vendedorForm = VendedorForm(cafeteriaId = it.cafeteriaSeleccionada?.id))
    }

    fun cerrarVendedor() {
        if (_uiState.value.vendedorForm?.guardando == true) return
        _uiState.update { it.copy(vendedorForm = null) }
    }

    fun actualizarVendedor(cambio: (VendedorForm) -> VendedorForm) = _uiState.update { s ->
        s.vendedorForm?.let { s.copy(vendedorForm = cambio(it).copy(error = null)) } ?: s
    }

    fun guardarVendedor() {
        val form = _uiState.value.vendedorForm ?: return
        if (form.guardando) return
        val (validado, ok) = form.validado()
        if (!ok) {
            _uiState.update { it.copy(vendedorForm = validado) }
            return
        }
        _uiState.update { it.copy(vendedorForm = validado.copy(guardando = true)) }
        viewModelScope.launch {
            try {
                val vendedor = repo.registrarVendedor(
                    validado.cafeteriaId!!, validado.nombre.trim(), validado.correo.trim(), validado.password
                )
                _uiState.update {
                    it.copy(
                        vendedores = (it.vendedores + vendedor).sortedBy { v -> v.nombre.lowercase() },
                        vendedorForm = null,
                        mensaje = R.string.owner_msg_vendor_created
                    )
                }
            } catch (e: Exception) {
                _uiState.update { s ->
                    val actual = s.vendedorForm ?: return@update s
                    s.copy(
                        vendedorForm = if (e is ApiException && e.codigo == "CORREO_DUPLICADO") {
                            actual.copy(guardando = false, correoDuplicado = true)
                        } else {
                            actual.copy(guardando = false, error = mensajeDeError(e, "dar de alta al vendedor"))
                        }
                    )
                }
            }
        }
    }

    // ---- Abrir otra cafetería ----

    fun nuevaCafeteria() {
        _uiState.update { it.copy(cafeteriaForm = CafeteriaForm()) }
        cargarEscuelas()
    }

    fun cerrarCafeteria() {
        if (_uiState.value.cafeteriaForm?.guardando == true) return
        _uiState.update { it.copy(cafeteriaForm = null) }
    }

    fun actualizarCafeteria(cambio: (CafeteriaForm) -> CafeteriaForm) = _uiState.update { s ->
        s.cafeteriaForm?.let { s.copy(cafeteriaForm = cambio(it).copy(error = null)) } ?: s
    }

    fun onEscuelaTextoChanged(value: String) = actualizarCafeteria { it.copy(escuela = it.escuela.conTexto(value)) }
    fun onEscuelaSugeridaSelected(escuela: Escuela) = actualizarCafeteria { it.copy(escuela = it.escuela.conSugerencia(escuela)) }
    fun onCiudadChanged(value: String) = actualizarCafeteria { it.copy(escuela = it.escuela.conCiudad(value)) }

    fun cargarEscuelas() {
        viewModelScope.launch {
            actualizarCafeteria { it.copy(escuela = it.escuela.cargandoCatalogo()) }
            try {
                val escuelas = repo.escuelas()
                actualizarCafeteria { it.copy(escuela = it.escuela.conCatalogo(escuelas)) }
            } catch (e: Exception) {
                actualizarCafeteria {
                    it.copy(escuela = it.escuela.conErrorCatalogo(mensajeDeError(e, "cargar las escuelas")))
                }
            }
        }
    }

    fun guardarCafeteria() {
        val form = _uiState.value.cafeteriaForm ?: return
        if (form.guardando) return
        val (validado, ok) = form.validado()
        if (!ok) {
            _uiState.update { it.copy(cafeteriaForm = validado) }
            return
        }
        _uiState.update { it.copy(cafeteriaForm = validado.copy(guardando = true)) }
        viewModelScope.launch {
            try {
                val escuela = validado.escuela
                val nueva = repo.registrarCafeteria(
                    nombre = validado.nombre.trim(),
                    ubicacion = validado.ubicacion.trim(),
                    escuelaId = escuela.seleccionada?.id,
                    nombreEscuela = escuela.nombreLimpio,
                    ciudadEscuela = escuela.ciudadParaEnviar
                )
                _uiState.update { s ->
                    val negocio = s.negocio?.let { n -> n.copy(cafeterias = n.cafeterias.orEmpty() + nueva) }
                    s.copy(
                        negocio = negocio,
                        cafeteriaForm = null,
                        cafeteriaSeleccionadaId = nueva.id,
                        mensaje = R.string.owner_msg_cafeteria_created
                    )
                }
            } catch (e: Exception) {
                _uiState.update { s ->
                    s.copy(cafeteriaForm = s.cafeteriaForm?.copy(guardando = false, error = mensajeDeError(e, "abrir la cafetería")))
                }
            }
        }
    }
}
