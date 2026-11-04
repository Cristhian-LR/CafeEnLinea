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
import com.example.cafeenlinea.ui.auth.view.RegisterScreenView


object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
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

        // Login y registro terminan igual: ofrecer biometría (una vez) y entrar al Home.
        // popUpTo(LOGIN) saca del historial al login y, si venía de ahí, al registro.
        val goHome = {
            navController.navigate(NavRoutes.HOME) {
                popUpTo(NavRoutes.LOGIN) { inclusive = true }
            }
        }

        composable(NavRoutes.LOGIN) {
            AfterAuthentication(sessionPreferences, onDone = goHome) { onAuthenticated ->
                LoginScreenView(
                    onLoginSuccess = onAuthenticated,
                    onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) }
                )
            }
        }

        composable(NavRoutes.REGISTER) {
            AfterAuthentication(sessionPreferences, onDone = goHome) { onAuthenticated ->
                RegisterScreenView(
                    onRegisterSuccess = onAuthenticated,
                    onBack = { navController.popBackStack() }
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

/**
 * Envuelve una pantalla que inicia sesión (login o registro). Tras el primer
 * acceso con contraseña ofrece la biometría una sola vez y luego llama a [onDone].
 */
@Composable
private fun AfterAuthentication(
    sessionPreferences: SessionPreferences,
    onDone: () -> Unit,
    content: @Composable (onAuthenticated: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showBiometricOffer by remember { mutableStateOf(false) }

    fun finishBiometricOffer(enable: Boolean) {
        sessionPreferences.markBiometricOffered()
        sessionPreferences.setBiometricEnabled(enable)
        showBiometricOffer = false
        onDone()
    }

    content {
        if (canUseBiometrics(context) && !sessionPreferences.wasBiometricOffered()) {
            showBiometricOffer = true
        } else {
            onDone()
        }
    }

    if (showBiometricOffer) {
        BiometricOfferDialog(
            onAccept = { finishBiometricOffer(enable = true) },
            onDecline = { finishBiometricOffer(enable = false) }
        )
    }
}
