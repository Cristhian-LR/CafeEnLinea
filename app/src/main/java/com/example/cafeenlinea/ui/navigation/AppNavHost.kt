package com.example.cafeenlinea.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.auth.view.LoginScreenView
import com.example.cafeenlinea.ui.home.view.CafeteriaHomeView
import com.example.cafeenlinea.ui.onboarding.data.OnboardingPreferences
import com.example.cafeenlinea.ui.onboarding.view.OnboardingView

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val HOME = "home"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }
    val sessionPreferences = remember { SessionPreferences(context) }

    // Una sesión activa gana sobre todo lo demás; si no hay sesión, se decide
    // entre login u onboarding según si ya vio el onboarding antes.
    val startDestination = when {
        sessionPreferences.isLoggedIn() -> NavRoutes.HOME
        onboardingPreferences.hasCompletedOnboarding() -> NavRoutes.LOGIN
        else -> NavRoutes.ONBOARDING
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingView(
                onFinishOnboarding = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.LOGIN) {
            LoginScreenView(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            CafeteriaHomeView()
        }
    }
}