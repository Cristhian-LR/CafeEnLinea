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

/**
 * ViewModel del home de la cafetería. Dueño del estado ([CafeteriaHomeUiState]) y
 * de la lógica: llamar a `GET /api/home/cafeteria/` y traducir los errores.
 *
 * Mismo patrón que
 * [com.jorgeromo.androidbasicsclass.ui.firstpartialpdm1.pickarooHome.viewmodel.PickarooHomeViewModel].
 */
class CafeteriaHomeViewModel : ViewModel() {

    private val cafeteriaService = CafeteriaRetrofitClient.cafeteriaService

    private val _uiState = MutableStateFlow(CafeteriaHomeUiState())
    val uiState: StateFlow<CafeteriaHomeUiState> = _uiState.asStateFlow()

    init {
        loadCafeteriaHome()
    }

    /**
     * Pide los datos del home y los vuelca al estado. Cualquier fallo se traduce
     * a un mensaje legible.
     */
    fun loadCafeteriaHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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
}