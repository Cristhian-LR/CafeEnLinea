package com.example.cafeenlinea.ui.onboarding.model

import androidx.compose.ui.graphics.vector.ImageVector

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

data class OnboardingUiState(
    val pages: List<OnboardingPage> = emptyList(),
    val currentPageIndex: Int = 0
) {
    val isLastPage: Boolean
        get() = currentPageIndex == pages.lastIndex
}