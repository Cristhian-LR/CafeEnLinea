package com.example.cafeenlinea.ui.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.SesionExpiradaException
import com.example.cafeenlinea.data.remote.SinConexionException
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.home.model.CafeteriaHomeResponse
import com.example.cafeenlinea.ui.home.model.CafeteriaHomeUiState
import com.example.cafeenlinea.ui.home.model.CafeteriaListItemUiState
import com.example.cafeenlinea.ui.home.model.HomeScreenUiState
import com.example.cafeenlinea.ui.home.network.CafeteriaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del home: lista de cafeterias desplegables. Cada cafeteria carga
 * su menu bajo demanda al expandirse y se cachea en memoria mientras la pantalla
 * siga abierta. Solo una cafeteria puede estar expandida a la vez.
 */
class CafeteriaHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)

    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    init {
        cargarCafeterias()
    }

    fun cargarCafeterias() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, emptyMessage = null) }
            try {
                val cafeteriaId = session.cafeteriaId()
                val escuelaId = session.escuelaId()
                val items = when {
                    !cafeteriaId.isNullOrBlank() -> {
                        val nombre = runCatching { ApiClient.cafeteria(cafeteriaId).nombre }
                            .getOrDefault(str(R.string.home_cafeteria_default_name))
                        listOf(CafeteriaListItemUiState(id = cafeteriaId, nombre = nombre))
                    }
                    !escuelaId.isNullOrBlank() -> {
                        ApiClient.cafeteriasPorEscuela(escuelaId).map {
                            CafeteriaListItemUiState(id = it.id, nombre = it.nombre)
                        }
                    }
                    else -> emptyList()
                }
                val emptyMessage = if (items.isEmpty() && cafeteriaId.isNullOrBlank() && escuelaId.isNullOrBlank()) {
                    str(R.string.home_empty_no_escuela)
                } else null
                _uiState.update { it.copy(isLoading = false, cafeterias = items, emptyMessage = emptyMessage) }
                // Caso vendedor (una sola cafeteria): expandir y cargar de inmediato.
                if (items.size == 1) toggleExpand(items.first().id)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mensajeDeError(e, R.string.home_error_load)) }
            }
        }
    }

    /** Solo una expandida a la vez. Al abrir, carga menu si no esta en memoria. */
    fun toggleExpand(cafeteriaId: String) {
        val actual = _uiState.value.cafeterias.find { it.id == cafeteriaId } ?: return
        val expandir = !actual.expanded
        _uiState.update { state ->
            state.copy(
                cafeterias = state.cafeterias.map {
                    if (it.id == cafeteriaId) it.copy(expanded = expandir) else it.copy(expanded = false)
                }
            )
        }
        if (expandir) cargarMenuSiHaceFalta(cafeteriaId)
    }

    private fun cargarMenuSiHaceFalta(cafeteriaId: String, force: Boolean = false) {
        val item = _uiState.value.cafeterias.find { it.id == cafeteriaId } ?: return
        if (!force && (item.menu != null || item.isLoading)) return
        viewModelScope.launch {
            actualizarItem(cafeteriaId) { it.copy(isLoading = true, error = null) }
            try {
                val respuesta = CafeteriaRepository.cargarMenu(cafeteriaId)
                val menu = menuDesdeRespuesta(respuesta)
                actualizarItem(cafeteriaId) { it.copy(isLoading = false, menu = menu, error = null) }
            } catch (e: Exception) {
                actualizarItem(cafeteriaId) {
                    it.copy(isLoading = false, error = mensajeDeError(e, R.string.home_error_load))
                }
            }
        }
    }

    fun reintentarMenu(cafeteriaId: String) {
        cargarMenuSiHaceFalta(cafeteriaId, force = true)
    }

    fun onSearchQueryChange(cafeteriaId: String, query: String) {
        actualizarMenu(cafeteriaId) { it.copy(searchQuery = query) }
    }

    /** Tocar el chip activo lo desactiva. */
    fun onSectionSelected(cafeteriaId: String, name: String?) {
        actualizarMenu(cafeteriaId) { actual ->
            actual.copy(selectedSection = if (actual.selectedSection == name) null else name)
        }
    }

    /** Menu filtrado para mostrar (reutiliza applyFilters sobre el menu cacheado). */
    fun menuVisible(item: CafeteriaListItemUiState): CafeteriaHomeUiState? {
        val menu = item.menu ?: return null
        return applyFilters(menu, menu.searchQuery, menu.selectedSection)
    }

    private fun menuDesdeRespuesta(respuesta: CafeteriaHomeResponse) = CafeteriaHomeUiState(
        shop = respuesta.shop,
        featured = respuesta.featured,
        sections = respuesta.sections,
        combos = respuesta.combos,
        combosNote = respuesta.combosNote,
        extras = respuesta.extras,
        sectionNames = respuesta.sections.map { it.name }
    )

    private fun actualizarItem(cafeteriaId: String, transform: (CafeteriaListItemUiState) -> CafeteriaListItemUiState) {
        _uiState.update { state ->
            state.copy(cafeterias = state.cafeterias.map { if (it.id == cafeteriaId) transform(it) else it })
        }
    }

    private fun actualizarMenu(cafeteriaId: String, transform: (CafeteriaHomeUiState) -> CafeteriaHomeUiState) {
        actualizarItem(cafeteriaId) { item -> item.menu?.let { item.copy(menu = transform(it)) } ?: item }
    }

    private fun str(id: Int): String = getApplication<Application>().getString(id)

    private fun mensajeDeError(e: Exception, genericoRes: Int): String = when (e) {
        is SinConexionException -> str(R.string.error_no_connection)
        is SesionExpiradaException -> str(R.string.home_error_session_expired)
        is ApiException -> e.message ?: str(R.string.error_server)
        else -> str(genericoRes)
    }
}