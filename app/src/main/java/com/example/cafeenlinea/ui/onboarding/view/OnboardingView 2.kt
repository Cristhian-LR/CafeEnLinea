package com.example.cafeenlinea.ui.onboarding.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeenlinea.ui.onboarding.model.OnboardingPage
import com.example.cafeenlinea.ui.onboarding.viewmodel.OnboardingViewModel

/**
 * Onboarding view, las 3 pantallas y el como se ven / estan definidas
 */
@Composable
fun OnboardingView(
    onFinishOnboarding: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    LaunchedEffect(uiState.currentPageIndex) {
        if (pagerState.currentPage != uiState.currentPageIndex) {
            pagerState.animateScrollToPage(uiState.currentPageIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingPageContent(uiState.pages[page])
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(uiState.pages.size) { index ->
                val isSelected = index == uiState.currentPageIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.goToPreviousPage() },
                enabled = uiState.currentPageIndex != 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Anterior")
            }

            Button(
                onClick = {
                    if (uiState.isLastPage) {
                        viewModel.completeOnboarding()
                        onFinishOnboarding()
                    } else {
                        viewModel.goToNextPage()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (uiState.isLastPage) "Comenzar" else "Siguiente")
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = page.title,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}