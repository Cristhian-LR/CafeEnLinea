package com.example.cafeenlinea.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.ui.home.model.CafeteriaHomeUiState
import com.example.cafeenlinea.ui.home.network.CafeteriaRetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.example.cafeenlinea.ui.home.network.CafeteriaService

/**
 * ViewModel del home de la cafetería. Dueño del estado ([CafeteriaHomeUiState]) y
 * de la lógica: llamar a `GET /api/home/cafeteria/` y traducir los errores.
 *
 * Mismo patrón que
 * [com.jorgeromo.androidbasicsclass.ui.firstpartialpdm1.pickarooHome.viewmodel.PickarooHomeViewModel].
 */
class CafeteriaHomeViewModel(
    private val cafeteriaService: CafeteriaService = CafeteriaRetrofitClient.cafeteriaService
) : ViewModel() {

    // Datos crudos del servidor (sin filtrar)
    private val _uiState = MutableStateFlow(CafeteriaHomeUiState())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedSection = MutableStateFlow<String?>(null)

    // Lo que ve la UI: datos + búsqueda + sección combinados
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

    /**
     * Pide los datos del home y los vuelca al estado. Cualquier fallo se traduce
     * a un mensaje legible.
     */
    fun loadCafeteriaHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            delay(3000) // C03: Retardo temporal para observar la animación Lottie
            try {
                val response = cafeteriaService.getCafeteriaHome()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        shop = response.shop,
                        featured = response.featured,
                        sections = response.sections,
                        combos = response.combos,
                        combosNote = response.combosNote,
                        extras = response.extras
                    )
                }
            } catch (e: HttpException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "No se pudo cargar (error ${e.code()})")
                }
            } catch (e: SocketTimeoutException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "El servidor está iniciando. Vuelve a intentar.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Sin conexión. Revisa tu internet e intenta de nuevo.")
                }
            }
        }
    }

    /**
     * C02: Recarga el menú mediante el gesto Pull-to-Refresh.
     * Mantiene los datos visibles y la bandera `isRefreshing = true`.
     * Si falla, el error se envía al Snackbar y se preserva el menú actual.
     */
    fun refreshCafeteriaHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val response = cafeteriaService.getCafeteriaHome()
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        shop = response.shop,
                        featured = response.featured,
                        sections = response.sections,
                        combos = response.combos,
                        combosNote = response.combosNote,
                        extras = response.extras
                    )
                }
            } catch (e: HttpException) {
                _uiState.update {
                    it.copy(isRefreshing = false, error = "No se pudo actualizar (error ${e.code()})")
                }
            } catch (e: SocketTimeoutException) {
                _uiState.update {
                    it.copy(isRefreshing = false, error = "El servidor no respondió a tiempo.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isRefreshing = false, error = "Sin conexión para actualizar.")
                }
            }
        }
    }

    /** C02: Limpia el error para que el Snackbar no vuelva a dispararse en recomposiciones. */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}