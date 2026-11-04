package com.example.cafeenlinea.ui.auth.model

import com.example.cafeenlinea.data.remote.Cafeteria
import com.example.cafeenlinea.data.remote.Escuela

/**
 * Estado de la pantalla de registro. Mismo patrón que [LoginUiState].
 *
 * La escuela se elige primero; con ella se cargan sus cafeterías, y la
 * cafetería elegida es la que queda asignada al usuario.
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

    val escuelas: List<Escuela> = emptyList(),
    val escuelaSeleccionada: Escuela? = null,
    val cafeterias: List<Cafeteria> = emptyList(),
    val cafeteriaSeleccionada: Cafeteria? = null,
    val isLoadingCatalogos: Boolean = false,
    /** Falló la carga de escuelas o cafeterías; la vista ofrece reintentar. */
    val catalogosError: String? = null,

    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistered: Boolean = false
) {
    val isButtonEnabled: Boolean
        get() = nombre.isNotBlank() && correo.isNotBlank() && password.isNotBlank() &&
            confirmPassword.isNotBlank() && cafeteriaSeleccionada != null && !isLoading
}
