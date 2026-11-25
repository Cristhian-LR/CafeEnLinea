package com.example.cafeenlinea.ui.auth.model

import androidx.annotation.StringRes
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.TextoBusqueda
import com.example.cafeenlinea.data.remote.RegistroCafeteriaInput

/**
 * Registro de una cafetería por su dueño. Se crea su negocio en revisión, la
 * cafetería (oculta a los alumnos) y su cuenta de JEFE. El equipo de la
 * plataforma la aprueba al cerrar el trato.
 */
data class BusinessRegisterUiState(
    // Cafetería
    val nombreNegocio: String = "",
    /** Como la verán los alumnos; si se deja vacío se usa el nombre del negocio. */
    val nombreCafeteria: String = "",
    val ubicacion: String = "",
    val escuela: EscuelaSeleccion = EscuelaSeleccion(),
    // Cuenta del dueño
    val nombre: String = "",
    val correo: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    @StringRes val negocioError: Int? = null,
    val cuentaErrores: CuentaErrores = CuentaErrores(),
    /** El correo ya tiene cuenta (lo dice el servidor). */
    val correoDuplicado: Boolean = false,

    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistered: Boolean = false
) {
    val isButtonEnabled: Boolean
        get() = nombreNegocio.isNotBlank() && escuela.completa && nombre.isNotBlank() &&
            correo.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading

    /** Datos para el servidor. Llamar solo después de validar. */
    fun aInput(): RegistroCafeteriaInput {
        val negocio = TextoBusqueda.limpiar(nombreNegocio)
        return RegistroCafeteriaInput(
            nombreDueno = nombre.trim(),
            correo = correo.trim(),
            password = password,
            nombreNegocio = negocio,
            nombreCafeteria = TextoBusqueda.limpiar(nombreCafeteria).ifBlank { negocio },
            ubicacion = ubicacion.trim(),
            escuelaId = escuela.seleccionada?.id,
            nombreEscuela = escuela.nombreLimpio,
            ciudadEscuela = escuela.ciudadParaEnviar
        )
    }

    /** Marca los errores de todo el formulario. Devuelve el estado con errores y si es válido. */
    fun validado(): Pair<BusinessRegisterUiState, Boolean> {
        val cuenta = validarCuenta(nombre, correo, password, confirmPassword)
        val (escuelaValidada, escuelaOk) = escuela.validada()
        val negocioError = if (nombreNegocio.isBlank()) R.string.business_error_name_required else null
        val conErrores = copy(
            cuentaErrores = cuenta,
            escuela = escuelaValidada,
            negocioError = negocioError,
            correoDuplicado = false
        )
        return conErrores to (!cuenta.hayErrores && escuelaOk && negocioError == null)
    }
}
