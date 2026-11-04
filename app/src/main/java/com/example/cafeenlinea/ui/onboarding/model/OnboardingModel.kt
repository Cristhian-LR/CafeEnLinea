package com.example.cafeenlinea.ui.onboarding.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes

data class OnboardingPage(
    val icon: ImageVector,
    @StringRes val tag: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    /** Íconos pequeños que flotan alrededor de la ilustración principal (máximo 3). */
    val highlights: List<ImageVector> = emptyList()
)

data class OnboardingUiState(
    val pages: List<OnboardingPage> = emptyList(),
    val currentPageIndex: Int = 0
) {
    val isLastPage: Boolean
        get() = currentPageIndex == pages.lastIndex
}