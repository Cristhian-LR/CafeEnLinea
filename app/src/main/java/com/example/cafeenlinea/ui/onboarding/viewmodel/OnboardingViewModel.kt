package com.example.cafeenlinea.ui.onboarding.viewmodel

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.lifecycle.AndroidViewModel
import com.example.cafeenlinea.ui.onboarding.data.OnboardingPreferences
import com.example.cafeenlinea.ui.onboarding.model.OnboardingPage
import com.example.cafeenlinea.ui.onboarding.model.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.cafeenlinea.R

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
            tag = R.string.onboarding_welcome_tag,
            title = R.string.onboarding_welcome_title,
            description = R.string.onboarding_welcome_description,
            highlights = listOf(
                Icons.Filled.Restaurant,
                Icons.Filled.BakeryDining,
                Icons.Filled.EmojiFoodBeverage
            )
        ),
        OnboardingPage(
            icon = Icons.Filled.ShoppingCart,
            tag = R.string.onboarding_orders_tag,
            title = R.string.onboarding_orders_title,
            description = R.string.onboarding_orders_description,
            highlights = listOf(
                Icons.Filled.Storefront,
                Icons.Filled.CreditCard,
                Icons.Filled.PhoneAndroid
            )
        ),
        OnboardingPage(
            icon = Icons.Filled.NotificationsActive,
            tag = R.string.onboarding_pickup_tag,
            title = R.string.onboarding_pickup_title,
            description = R.string.onboarding_pickup_description,
            highlights = listOf(
                Icons.Filled.Timer,
                Icons.Filled.CheckCircle,
                Icons.Filled.LocalMall
            )
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
