package com.example.cafeenlinea.ui.owner.model

import androidx.annotation.StringRes
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.CafeteriaNegocio
import com.example.cafeenlinea.data.remote.Negocio
import com.example.cafeenlinea.data.remote.Usuario
import com.example.cafeenlinea.ui.auth.model.CuentaErrores
import com.example.cafeenlinea.ui.auth.model.EscuelaSeleccion
import com.example.cafeenlinea.ui.auth.model.validarCuenta

/** Estados del negocio en el servidor. */
object EstadoNegocio {
    const val EN_REVISION = "EN_REVISION"
    const val APROBADO = "APROBADO"
    const val RECHAZADO = "RECHAZADO"
}

/** Formulario para que el dueño dé de alta a un vendedor en una de sus cafeterías. */
data class VendedorForm(
    val nombre: String = "",
    val correo: String = "",
    val password: String = "",
    val cafeteriaId: String? = null,
    val errores: CuentaErrores = CuentaErrores(),
    @StringRes val cafeteriaError: Int? = null,
    val correoDuplicado: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null
) {
    /** Marca los errores. Devuelve el formulario con errores y si es válido. */
    fun validado(): Pair<VendedorForm, Boolean> {
        val cuenta = validarCuenta(nombre, correo, password, confirmPassword = null)
        val cafeteria = if (cafeteriaId == null) R.string.owner_error_cafeteria_required else null
        return copy(errores = cuenta, cafeteriaError = cafeteria, correoDuplicado = false) to
            (!cuenta.hayErrores && cafeteria == null)
    }
}

/** Formulario para abrir otra cafetería del negocio. */
data class CafeteriaForm(
    val nombre: String = "",
    val ubicacion: String = "",
    val escuela: EscuelaSeleccion = EscuelaSeleccion(),
    @StringRes val nombreError: Int? = null,
    val guardando: Boolean = false,
    val error: String? = null
) {
    fun validado(): Pair<CafeteriaForm, Boolean> {
        val (escuelaValidada, escuelaOk) = escuela.validada()
        val nombreError = if (nombre.isBlank()) R.string.owner_error_cafeteria_name else null
        return copy(escuela = escuelaValidada, nombreError = nombreError) to (escuelaOk && nombreError == null)
    }
}

/** Estado de la pantalla del dueño. */
data class OwnerUiState(
    val isLoading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val negocio: Negocio? = null,
    val vendedores: List<Usuario> = emptyList(),
    /** Cafetería que se está viendo en Pedidos, Menú, Stock y Resumen. */
    val cafeteriaSeleccionadaId: String? = null,
    val vendedorForm: VendedorForm? = null,
    val cafeteriaForm: CafeteriaForm? = null,
    @StringRes val mensaje: Int? = null
) {
    val estado: String get() = negocio?.estado ?: EstadoNegocio.APROBADO
    val enRevision: Boolean get() = estado == EstadoNegocio.EN_REVISION
    val rechazado: Boolean get() = estado == EstadoNegocio.RECHAZADO

    val cafeterias: List<CafeteriaNegocio> get() = negocio?.cafeterias.orEmpty()

    val cafeteriaSeleccionada: CafeteriaNegocio?
        get() = cafeterias.firstOrNull { it.id == cafeteriaSeleccionadaId } ?: cafeterias.firstOrNull()

    /** Vendedores agrupados por la cafetería donde atienden. */
    fun vendedoresDe(cafeteriaId: String): List<Usuario> = vendedores.filter { it.cafeteriaId == cafeteriaId }
}
