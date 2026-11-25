package com.example.cafeenlinea.ui.auth.model

import androidx.annotation.StringRes
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.TextoBusqueda
import com.example.cafeenlinea.data.remote.Escuela

/** Mismo mínimo que valida el servidor para el nombre de una escuela nueva. */
const val ESCUELA_MIN_LENGTH = 3

/**
 * Escuela escrita a mano con sugerencias del catálogo. La usan el registro de
 * alumnos, el registro de cafeterías y "agregar cafetería" del dueño.
 *
 * Si lo escrito coincide con una escuela del catálogo, queda [seleccionada].
 * Si no, es nueva: se pide la ciudad y el servidor la agrega al catálogo.
 */
data class EscuelaSeleccion(
    val texto: String = "",
    val ciudad: String = "",
    /** Catálogo para las sugerencias. */
    val escuelas: List<Escuela> = emptyList(),
    val seleccionada: Escuela? = null,
    @StringRes val error: Int? = null,
    @StringRes val ciudadError: Int? = null,
    val cargando: Boolean = false,
    /** Falló la carga del catálogo; se puede seguir (el servidor busca por nombre). */
    val catalogoError: String? = null
) {
    val sugerencias: List<Escuela>
        get() = EscuelaSugerencias.buscar(escuelas, texto).filter { it.id != seleccionada?.id }

    /** Lo escrito no está en el catálogo: se creará y hace falta la ciudad. */
    val esNueva: Boolean get() = texto.isNotBlank() && seleccionada == null

    /** Tiene lo mínimo para habilitar el botón de enviar. */
    val completa: Boolean get() = texto.isNotBlank() && (!esNueva || ciudad.isNotBlank())

    /** Nombre limpio para mandar al servidor. */
    val nombreLimpio: String get() = seleccionada?.nombre ?: TextoBusqueda.limpiar(texto)

    /** Ciudad para el servidor: solo si la escuela es nueva. */
    val ciudadParaEnviar: String? get() = if (seleccionada == null) ciudad.trim() else null

    fun conTexto(value: String) = copy(
        texto = value,
        seleccionada = EscuelaSugerencias.coincidenciaExacta(escuelas, value),
        error = null,
        ciudadError = null
    )

    fun conSugerencia(escuela: Escuela) =
        copy(texto = escuela.nombre, seleccionada = escuela, error = null, ciudadError = null)

    fun conCiudad(value: String) = copy(ciudad = value, ciudadError = null)

    fun cargandoCatalogo() = copy(cargando = true, catalogoError = null)

    /** Si ya había escrito algo antes de que cargara el catálogo, se vuelve a comparar. */
    fun conCatalogo(lista: List<Escuela>) = copy(
        escuelas = lista,
        cargando = false,
        seleccionada = EscuelaSugerencias.coincidenciaExacta(lista, texto)
    )

    fun conErrorCatalogo(mensaje: String) = copy(cargando = false, catalogoError = mensaje)

    /** Marca los errores de escuela y ciudad. Devuelve el estado con errores y si es válida. */
    fun validada(): Pair<EscuelaSeleccion, Boolean> {
        val limpio = TextoBusqueda.limpiar(texto)
        val error = when {
            limpio.isBlank() -> R.string.register_error_school_required
            limpio.length < ESCUELA_MIN_LENGTH -> R.string.register_error_school_short
            else -> null
        }
        val ciudadError = if (seleccionada == null && ciudad.isBlank()) R.string.register_error_city_required else null
        return copy(error = error, ciudadError = ciudadError) to (error == null && ciudadError == null)
    }
}

/** Búsqueda de escuelas sin importar mayúsculas, acentos ni espacios de más. */
object EscuelaSugerencias {

    const val MIN_CARACTERES = 2
    const val MAX_SUGERENCIAS = 5

    fun normalizar(texto: String): String = TextoBusqueda.normalizar(texto)

    /** La escuela del catálogo con exactamente ese nombre (normalizado), si existe. */
    fun coincidenciaExacta(escuelas: List<Escuela>, texto: String): Escuela? {
        val buscado = normalizar(texto)
        if (buscado.isEmpty()) return null
        return escuelas.firstOrNull { normalizar(it.nombre) == buscado }
    }

    /**
     * Escuelas cuyo nombre o clave contienen lo escrito. Primero las que empiezan
     * igual, luego por nombre.
     */
    fun buscar(escuelas: List<Escuela>, texto: String): List<Escuela> =
        TextoBusqueda.sugerir(escuelas, texto, MIN_CARACTERES, MAX_SUGERENCIAS) { listOf(it.nombre, it.clave) }
}
