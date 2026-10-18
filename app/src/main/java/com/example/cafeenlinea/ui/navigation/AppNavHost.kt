package com.example.cafeenlinea.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cafeenlinea.ui.auth.biometric.BiometricLockView
import com.example.cafeenlinea.ui.auth.biometric.BiometricOfferDialog
import com.example.cafeenlinea.ui.auth.biometric.canUseBiometrics
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.auth.view.LoginScreenView
import com.example.cafeenlinea.ui.main.MainScreen
import com.example.cafeenlinea.ui.onboarding.data.OnboardingPreferences
import com.example.cafeenlinea.ui.onboarding.view.OnboardingView

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val BIOMETRIC_LOCK = "biometric_lock"
    const val HOME = "home"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }
    val sessionPreferences = remember { SessionPreferences(context) }

    // El candado solo aplica si hay sesión, el usuario lo activó y el teléfono
    // todavía tiene biometría (si borró sus huellas, no se queda atrapado).
    val startDestination = when {
        sessionPreferences.isLoggedIn() &&
            sessionPreferences.isBiometricEnabled() &&
            canUseBiometrics(context) -> NavRoutes.BIOMETRIC_LOCK
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

        composable(NavRoutes.BIOMETRIC_LOCK) {
            BiometricLockView(
                onUnlocked = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.BIOMETRIC_LOCK) { inclusive = true }
                    }
                },
                onUsePassword = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.BIOMETRIC_LOCK) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.LOGIN) {
            var showBiometricOffer by remember { mutableStateOf(false) }

            fun goHome() {
                navController.navigate(NavRoutes.HOME) {
                    popUpTo(NavRoutes.LOGIN) { inclusive = true }
                }
            }

            fun finishBiometricOffer(enable: Boolean) {
                sessionPreferences.markBiometricOffered()
                sessionPreferences.setBiometricEnabled(enable)
                showBiometricOffer = false
                goHome()
            }

            LoginScreenView(
                onLoginSuccess = {
                    // Tras el primer login con contraseña, ofrecer la biometría una sola vez.
                    if (canUseBiometrics(context) && !sessionPreferences.wasBiometricOffered()) {
                        showBiometricOffer = true
                    } else {
                        goHome()
                    }
                }
            )

            if (showBiometricOffer) {
                BiometricOfferDialog(
                    onAccept = { finishBiometricOffer(enable = true) },
                    onDecline = { finishBiometricOffer(enable = false) }
                )
            }
        }

        composable(NavRoutes.HOME) {
            MainScreen(
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}