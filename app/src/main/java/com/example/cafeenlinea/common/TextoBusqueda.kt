package com.example.cafeenlinea.common

import java.text.Normalizer

/**
 * Búsquedas que ignoran mayúsculas, acentos y espacios de más. Las usan las
 * sugerencias de escuela (registro) y de categoría (menú del vendedor).
 */
object TextoBusqueda {

    /** "  Tecnológico   de  Chihuahua" -> "tecnologico de chihuahua" */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")

    /** Quita espacios de más sin cambiar mayúsculas ni acentos: "  Bebidas  frías " -> "Bebidas frías" */
    fun limpiar(texto: String): String = texto.trim().replace(Regex("\\s+"), " ")

    /**
     * Opciones cuyo texto contiene lo escrito. Primero las que empiezan igual, luego
     * en orden alfabético.
     */
    fun <T> sugerir(
        opciones: List<T>,
        texto: String,
        minCaracteres: Int,
        maximo: Int,
        textosDe: (T) -> List<String>
    ): List<T> {
        val buscado = normalizar(texto)
        if (buscado.length < minCaracteres) return emptyList()
        return opciones
            .map { opcion -> opcion to textosDe(opcion).map(::normalizar) }
            .filter { (_, textos) -> textos.any { buscado in it } }
            .sortedWith(compareBy({ !it.second.first().startsWith(buscado) }, { it.second.first() }))
            .take(maximo)
            .map { it.first }
    }
}
