package com.example.cafeenlinea.ui.auth.model

/**
* Estado de la pantalla de login. El login va contra el backend GraphQL
* (`ApiClient.login` en `data/remote/GraphQLClient.kt`); la lógica vive en
* `LoginViewModel` y la persistencia en `ui/auth/data/SessionPreferences`.
*/

/**
 * Estado observable de la pantalla de login. Mismo patrón que `OnboardingUiState`.
 *
 * @property isLoading true mientras se hace la petición (mostrar spinner).
 * @property error mensaje a mostrar bajo el formulario, o null si no hay error.
 * @property isLoggedIn pasa a true cuando el login fue correcto y ya se guardó la
 *   sesión; la vista lo observa para navegar a "tabs".
 */
data class LoginUiState(
    val usernameText: String = "",
    val passwordText: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isLockedOut: Boolean = false,     // True cuando excede los intentos
    val lockOutSeconds: Int = 0           // Segundos restantes de espera
) {
    val isButtonEnabled: Boolean
        get() = usernameText.isNotBlank() && passwordText.isNotBlank() && !isLoading && !isLockedOut
}