package com.example.cafeenlinea.ui.onboarding.model

import androidx.compose.ui.graphics.vector.ImageVector

data class OnboardingPage(
    val icon: ImageVector,
    val tag: String,
    val title: String,
    val description: String,
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