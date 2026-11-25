package com.example.cafeenlinea.ui.vendor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.data.remote.mensajeDeError
import com.example.cafeenlinea.ui.vendor.data.VendorRepository
import com.example.cafeenlinea.ui.vendor.model.VendorSummaryUiState
import com.example.cafeenlinea.ui.vendor.model.calcularResumen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Resumen del vendedor: pedidos y ventas de hoy, horas pico, lo más vendido y
 * tiempos de preparación, calculados con los pedidos de su cafetería.
 *
 * @param ahora Reloj inyectable para las pruebas.
 */
class VendorSummaryViewModel(
    private val cafeteriaId: String?,
    private val repo: VendorRepository,
    private val ahora: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(VendorSummaryUiState())
    val uiState: StateFlow<VendorSummaryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() = fetch(refrescando = false)
    fun refresh() = fetch(refrescando = true)

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
                val resumen = calcularResumen(repo.historialPedidos(id), ahora())
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, loaded = true, resumen = resumen) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = mensajeDeError(e, "cargar el resumen"))
                }
            }
        }
    }
}
