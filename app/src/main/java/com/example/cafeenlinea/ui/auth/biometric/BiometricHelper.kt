package com.example.cafeenlinea.ui.auth.biometric

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Huella (casi siempre STRONG) o rostro (en muchos teléfonos, como el Huawei de
 * pruebas, solo es WEAK). Aceptamos ambos porque la biometría funciona como
 * candado de entrada; los tokens ya van cifrados aparte con [TokenCipher].
 */
private const val AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

/** Resultado del diálogo biométrico, traducido a lo que le importa a la UI. */
sealed interface BiometricResult {
    data object Success : BiometricResult

    /** El usuario tocó "Usar contraseña". */
    data object UsePassword : BiometricResult

    /** Canceló, se bloqueó el sensor por intentos, etc. [message] viene del sistema. */
    data class Error(val message: String) : BiometricResult
}

/**
 * true si el teléfono tiene sensor y hay al menos una huella o rostro registrado.
 * Si es false, la opción de biometría no se muestra en ningún lado.
 */
fun canUseBiometrics(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

/**
 * Muestra el diálogo del sistema para pedir huella o rostro.
 *
 * Una huella que no coincide (`onAuthenticationFailed`) no se reporta: el
 * diálogo sigue abierto y deja reintentar. Solo avisamos el resultado final.
 */
fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onResult: (BiometricResult) -> Unit
) {
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onResult(BiometricResult.Success)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                onResult(BiometricResult.UsePassword)
            } else {
                onResult(BiometricResult.Error(errString.toString()))
            }
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText("Usar contraseña")
        .setAllowedAuthenticators(AUTHENTICATORS)
        .build()

    BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        .authenticate(promptInfo)
}

/**
 * En Compose, `LocalContext.current` puede venir envuelto (p. ej. por el tema),
 * así que recorremos los `ContextWrapper` hasta llegar a la Activity.
 * BiometricPrompt necesita un [FragmentActivity]; por eso MainActivity extiende de ella.
 */
fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
