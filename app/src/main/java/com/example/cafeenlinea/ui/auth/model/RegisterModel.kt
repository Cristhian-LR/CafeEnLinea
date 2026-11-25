package com.example.cafeenlinea.ui.auth.model

/**
 * Estado de la pantalla de registro de alumnos. Mismo patrón que [LoginUiState].
 * La escuela se escribe con sugerencias del catálogo (ver [EscuelaSeleccion]).
 */
data class RegisterUiState(
    val nombre: String = "",
    val correo: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    val nombreError: String? = null,
    val correoError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,

    val escuela: EscuelaSeleccion = EscuelaSeleccion(),

    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistered: Boolean = false
) {
    val isButtonEnabled: Boolean
        get() = nombre.isNotBlank() && correo.isNotBlank() && password.isNotBlank() &&
            confirmPassword.isNotBlank() && escuela.completa && !isLoading
}
