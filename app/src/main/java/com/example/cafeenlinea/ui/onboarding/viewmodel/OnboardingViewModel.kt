package com.example.cafeenlinea.ui.onboarding.viewmodel

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Notifications
import androidx.lifecycle.AndroidViewModel
import com.example.cafeenlinea.ui.onboarding.data.OnboardingPreferences
import com.example.cafeenlinea.ui.onboarding.model.OnboardingPage
import com.example.cafeenlinea.ui.onboarding.model.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel para la pantalla de onboarding de Café en Línea.
 * Mantiene la lista de páginas y la página actualmente seleccionada, y marca en
 * [OnboardingPreferences] cuando el usuario ya lo completó para no volver a mostrarlo.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = OnboardingPreferences(application)

    private val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.LocalCafe,
            title = "Bienvenido a Café en Línea",
            description = "Ordena tu comida y snacks favoritos desde cualquier punto del campus."
        ),
        OnboardingPage(
            icon = Icons.Filled.ShoppingCart,
            title = "Ordena y paga desde tu celular",
            description = "Elige tu cafetería favorita, arma tu pedido y paga sin salir de la app."
        ),
        OnboardingPage(
            icon = Icons.Filled.Notifications,
            title = "Recoge sin hacer fila",
            description = "Te avisamos en cuanto tu pedido esté listo para que solo pases a recogerlo."
        )
    )

    private val _uiState = MutableStateFlow(OnboardingUiState(pages = pages))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Sincroniza el estado con la página actual del [androidx.compose.foundation.pager.PagerState],
     * por ejemplo cuando el usuario desliza manualmente.
     */
    fun onPageChanged(index: Int) {
        _uiState.update { it.copy(currentPageIndex = index) }
    }

    fun goToNextPage() {
        _uiState.update { state ->
            state.copy(currentPageIndex = (state.currentPageIndex + 1).coerceAtMost(state.pages.lastIndex))
        }
    }

    fun goToPreviousPage() {
        _uiState.update { state ->
            state.copy(currentPageIndex = (state.currentPageIndex - 1).coerceAtLeast(0))
        }
    }

    /** Marca el onboarding como visto para que no vuelva a mostrarse */
    fun completeOnboarding() {
        preferences.setOnboardingCompleted()
    }
}
