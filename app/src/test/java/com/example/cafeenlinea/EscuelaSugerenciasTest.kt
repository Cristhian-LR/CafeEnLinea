package com.example.cafeenlinea

import com.example.cafeenlinea.data.remote.Escuela
import com.example.cafeenlinea.ui.auth.model.EscuelaSeleccion
import com.example.cafeenlinea.ui.auth.model.EscuelaSugerencias
import com.example.cafeenlinea.ui.auth.model.RegisterUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EscuelaSugerenciasTest {

    private val laSalle = Escuela("1", "ULSA", "Universidad La Salle", "Chihuahua")
    private val tec = Escuela("2", "ITCH", "Instituto Tecnológico de Chihuahua", "Chihuahua")
    private val salleCdmx = Escuela("3", "SALLEMX", "La Salle Ciudad de México", "CDMX")
    private val catalogo = listOf(laSalle, tec, salleCdmx)

    @Test
    fun `normalizar ignora mayusculas acentos y espacios de mas`() {
        assertEquals("instituto tecnologico", EscuelaSugerencias.normalizar("  Instituto   TECNOLÓGICO "))
    }

    @Test
    fun `escribir la salle sugiere las escuelas que la contienen`() {
        val sugerencias = EscuelaSugerencias.buscar(catalogo, "la salle")
        // Primero la que empieza igual
        assertEquals(listOf(salleCdmx, laSalle), sugerencias)
    }

    @Test
    fun `se encuentra sin acentos y por clave`() {
        assertEquals(listOf(tec), EscuelaSugerencias.buscar(catalogo, "tecnologico"))
        assertEquals(listOf(tec), EscuelaSugerencias.buscar(catalogo, "itch"))
    }

    @Test
    fun `con menos de dos letras no sugiere nada`() {
        assertTrue(EscuelaSugerencias.buscar(catalogo, "l").isEmpty())
    }

    @Test
    fun `no sugiere mas del maximo`() {
        val muchas = (1..10).map { Escuela("$it", "S$it", "Secundaria $it", "") }
        assertEquals(EscuelaSugerencias.MAX_SUGERENCIAS, EscuelaSugerencias.buscar(muchas, "secundaria").size)
    }

    @Test
    fun `coincidencia exacta sin importar mayusculas ni acentos`() {
        assertEquals(tec, EscuelaSugerencias.coincidenciaExacta(catalogo, "instituto tecnologico  de chihuahua"))
        assertNull(EscuelaSugerencias.coincidenciaExacta(catalogo, "La Salle"))
        assertNull(EscuelaSugerencias.coincidenciaExacta(catalogo, "   "))
    }

    private fun registro(escuela: EscuelaSeleccion) = RegisterUiState(
        nombre = "Ana", correo = "ana@correo.mx", password = "secreto", confirmPassword = "secreto",
        escuela = escuela
    )

    @Test
    fun `una escuela nueva pide ciudad para habilitar el boton`() {
        val escuela = EscuelaSeleccion().conCatalogo(catalogo).conTexto("Prepa Nueva")
        assertTrue(escuela.esNueva)
        assertFalse(registro(escuela).isButtonEnabled)
        assertTrue(registro(escuela.conCiudad("Juárez")).isButtonEnabled)
    }

    @Test
    fun `una escuela del catalogo no pide ciudad ni se sugiere a si misma`() {
        val escuela = EscuelaSeleccion().conCatalogo(catalogo).conTexto("universidad la salle")
        assertEquals(laSalle, escuela.seleccionada)
        assertFalse(escuela.esNueva)
        assertTrue(registro(escuela).isButtonEnabled)
        assertFalse(laSalle in escuela.sugerencias)
        assertNull(escuela.ciudadParaEnviar)
    }

    @Test
    fun `si escribe antes de que cargue el catalogo se compara al llegar`() {
        val escuela = EscuelaSeleccion().conTexto("Universidad La Salle")
        assertTrue(escuela.esNueva)
        assertEquals(laSalle, escuela.conCatalogo(catalogo).seleccionada)
    }

    @Test
    fun `validar marca escuela vacia corta y ciudad faltante`() {
        val (vacia, okVacia) = EscuelaSeleccion().validada()
        assertFalse(okVacia)
        assertEquals(R.string.register_error_school_required, vacia.error)

        val (corta, _) = EscuelaSeleccion().conTexto("UA").validada()
        assertEquals(R.string.register_error_school_short, corta.error)

        val (sinCiudad, okSinCiudad) = EscuelaSeleccion().conTexto("Prepa Nueva").validada()
        assertFalse(okSinCiudad)
        assertEquals(R.string.register_error_city_required, sinCiudad.ciudadError)

        assertTrue(EscuelaSeleccion().conCatalogo(catalogo).conSugerencia(tec).validada().second)
    }
}
