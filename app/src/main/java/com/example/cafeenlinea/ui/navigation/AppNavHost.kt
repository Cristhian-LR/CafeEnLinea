package com.example.cafeenlinea.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cafeenlinea.ui.onboarding.data.OnboardingPreferences
import com.example.cafeenlinea.ui.onboarding.view.OnboardingView


object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }

    val startDestination = if (onboardingPreferences.hasCompletedOnboarding()) {
        NavRoutes.HOME
    } else {
        NavRoutes.ONBOARDING
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingView(
                onFinishOnboarding = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomePlaceholder()
        }

    }
}

@Composable
private fun HomePlaceholder() {
    Text("Home :)")
}