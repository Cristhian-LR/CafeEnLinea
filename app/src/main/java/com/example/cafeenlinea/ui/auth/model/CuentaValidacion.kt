package com.example.cafeenlinea.ui.auth.model

import androidx.annotation.StringRes
import com.example.cafeenlinea.R

/** Mismo mínimo que valida el servidor al crear cuentas. */
const val PASSWORD_MIN_LENGTH = 6

/** Mismo formato de correo que valida el servidor. */
private val FORMATO_CORREO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

fun esCorreoValido(correo: String): Boolean = correo.length <= 254 && FORMATO_CORREO.matches(correo)

/** Errores de los datos de una cuenta (textos de strings.xml). */
data class CuentaErrores(
    @StringRes val nombre: Int? = null,
    @StringRes val correo: Int? = null,
    @StringRes val password: Int? = null,
    @StringRes val confirmPassword: Int? = null
) {
    val hayErrores: Boolean get() = listOf(nombre, correo, password, confirmPassword).any { it != null }
}

/**
 * Valida nombre, correo y contraseña de una cuenta nueva (alumno, dueño o vendedor).
 * [confirmPassword] null = el formulario no pide confirmarla.
 */
fun validarCuenta(nombre: String, correo: String, password: String, confirmPassword: String?): CuentaErrores {
    val correoLimpio = correo.trim()
    return CuentaErrores(
        nombre = if (nombre.isBlank()) R.string.register_error_name_required else null,
        correo = when {
            correoLimpio.isBlank() -> R.string.login_error_email_required
            !esCorreoValido(correoLimpio) -> R.string.register_error_email_invalid
            else -> null
        },
        password = when {
            password.isBlank() -> R.string.login_error_password_required
            password.length < PASSWORD_MIN_LENGTH -> R.string.register_error_password_short
            else -> null
        },
        confirmPassword = if (confirmPassword != null && confirmPassword != password)
            R.string.register_error_password_mismatch else null
    )
}
