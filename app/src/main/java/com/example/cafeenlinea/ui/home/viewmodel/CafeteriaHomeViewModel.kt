package com.example.cafeenlinea.ui.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.SesionExpiradaException
import com.example.cafeenlinea.data.remote.SinConexionException
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.home.model.CafeteriaHomeResponse
import com.example.cafeenlinea.ui.home.model.CafeteriaHomeUiState
import com.example.cafeenlinea.ui.home.network.CafeteriaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del home de la cafeteria. Dueno del estado ([CafeteriaHomeUiState]) y
 * de la logica: pedir el menu a tu servidor (GraphQL) y traducir los errores.
 */
class CafeteriaHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)

    // Datos crudos del servidor (sin filtrar)
    private val _uiState = MutableStateFlow(CafeteriaHomeUiState())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedSection = MutableStateFlow<String?>(null)

    // Lo que ve la UI: datos + busqueda + seccion combinados
    val uiState: StateFlow<CafeteriaHomeUiState> =
        combine(_uiState, _searchQuery, _selectedSection) { raw, query, section ->
            applyFilters(raw, query, section)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, CafeteriaHomeUiState())

    init {
        loadCafeteriaHome()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** Tocar el chip activo lo desactiva. */
    fun onSectionSelected(name: String?) {
        _selectedSection.value = if (_selectedSection.value == name) null else name
    }

    /** Pide el menu y lo vuelca al estado. Cualquier fallo se traduce a un mensaje legible. */
    fun loadCafeteriaHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                aplicarMenu(pedirMenu())
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mensajeDeError(e, "cargar el menú")) }
            }
        }
    }

    /**
     * C02: Recarga con Pull-to-Refresh. Mantiene los datos visibles; si falla, el
     * error va al Snackbar y se conserva el menu actual.
     */
    fun refreshCafeteriaHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                aplicarMenu(pedirMenu())
                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, error = mensajeDeError(e, "actualizar")) }
            }
        }
    }

    /** C02: Limpia el error para que el Snackbar no vuelva a dispararse. */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun pedirMenu(): CafeteriaHomeResponse {
        val cafeteriaId = session.cafeteriaId()
            ?: throw ApiException("Tu usuario no tiene una cafetería asignada.")
        return CafeteriaRepository.cargarMenu(cafeteriaId)
    }

    private fun aplicarMenu(respuesta: CafeteriaHomeResponse) {
        _uiState.update {
            it.copy(
                shop = respuesta.shop,
                featured = respuesta.featured,
                sections = respuesta.sections,
                combos = respuesta.combos,
                combosNote = respuesta.combosNote,
                extras = respuesta.extras
            )
        }
    }

    private fun mensajeDeError(e: Exception, accion: String): String = when (e) {
        is SinConexionException ->
            "No se pudo conectar. Si el servidor estaba iniciando, vuelve a intentar."
        is SesionExpiradaException ->
            "Tu sesión expiró. Cierra sesión desde Perfil e inicia de nuevo."
        is ApiException -> e.message ?: "Error del servidor"
        else -> "No se pudo $accion. Intenta de nuevo."
    }
}