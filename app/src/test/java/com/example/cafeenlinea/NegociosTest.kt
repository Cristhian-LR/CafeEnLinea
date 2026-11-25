package com.example.cafeenlinea

import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.CafeteriaNegocio
import com.example.cafeenlinea.data.remote.Escuela
import com.example.cafeenlinea.data.remote.Negocio
import com.example.cafeenlinea.data.remote.RegistroCafeteriaInput
import com.example.cafeenlinea.data.remote.Sesion
import com.example.cafeenlinea.data.remote.SinConexionException
import com.example.cafeenlinea.data.remote.Usuario
import com.example.cafeenlinea.ui.admin.AdminRepository
import com.example.cafeenlinea.ui.admin.AdminViewModel
import com.example.cafeenlinea.ui.admin.FiltroNegocios
import com.example.cafeenlinea.ui.auth.model.esCorreoValido
import com.example.cafeenlinea.ui.auth.model.validarCuenta
import com.example.cafeenlinea.ui.auth.viewmodel.BusinessRegisterViewModel
import com.example.cafeenlinea.ui.owner.data.OwnerRepository
import com.example.cafeenlinea.ui.owner.viewmodel.OwnerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val ulsa = Escuela("e1", "ULSA", "Universidad La Salle", "Chihuahua")
private val usuarioJefe = Usuario("u1", "Lupe", "lupe@x.mx", escuelaId = "e1", cafeteriaId = null, rol = "JEFE")
private fun cafeteria(id: String) = CafeteriaNegocio(id, "Cafetería $id", activa = false, escuela = ulsa)
private fun negocio(id: String, estado: String = "EN_REVISION", cafeterias: List<CafeteriaNegocio> = listOf(cafeteria("c1"))) =
    Negocio(id, "Negocio $id", estado, cafeterias = cafeterias)

@OptIn(ExperimentalCoroutinesApi::class)
class NegociosTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    // ---- Validación de cuentas ----

    @Test fun `valida los datos de una cuenta nueva`() {
        assertTrue(esCorreoValido("ana@ulsa.mx"))
        assertFalse(esCorreoValido("ana@ulsa"))
        val errores = validarCuenta(" ", "ana", "123", "124")
        assertNotNull(errores.nombre)
        assertNotNull(errores.correo)
        assertNotNull(errores.password)
        assertNotNull(errores.confirmPassword)
        // Sin confirmación (alta de vendedor) no se valida
        assertNull(validarCuenta("Ana", "ana@ulsa.mx", "123456", null).confirmPassword)
        assertFalse(validarCuenta("Ana", "ana@ulsa.mx", "123456", "123456").hayErrores)
    }

    // ---- Registro de la cafetería ----

    @Test fun `registrar la cafeteria manda la escuela del catalogo y guarda la sesion`() = runTest(dispatcher) {
        var enviado: RegistroCafeteriaInput? = null
        var guardada: Sesion? = null
        val vm = BusinessRegisterViewModel(
            cargarEscuelas = { listOf(ulsa) },
            registrar = { enviado = it; Sesion("tok", 3600, usuarioJefe) },
            guardarSesion = { guardada = it }
        )
        advanceUntilIdle()

        vm.actualizar {
            it.copy(
                nombreNegocio = "  Tortas   Lupe ", nombre = "Lupe", correo = "lupe@x.mx",
                password = "secreto", confirmPassword = "secreto"
            )
        }
        vm.onEscuelaTextoChanged("universidad la salle")
        assertTrue(vm.uiState.value.isButtonEnabled)
        vm.register()
        advanceUntilIdle()

        val datos = enviado!!
        assertEquals("Tortas Lupe", datos.nombreNegocio)
        // Sin nombre de cafetería se usa el del negocio
        assertEquals("Tortas Lupe", datos.nombreCafeteria)
        assertEquals("e1", datos.escuelaId)
        assertNull(datos.ciudadEscuela)
        assertEquals("tok", guardada?.token)
        assertTrue(vm.uiState.value.isRegistered)
    }

    @Test fun `escuela nueva sin ciudad no se envia`() = runTest(dispatcher) {
        var llamadas = 0
        val vm = BusinessRegisterViewModel({ listOf(ulsa) }, { llamadas++; Sesion("t", 1, usuarioJefe) }, {})
        advanceUntilIdle()
        vm.actualizar {
            it.copy(nombreNegocio = "Lupe", nombre = "Lupe", correo = "lupe@x.mx", password = "secreto", confirmPassword = "secreto")
        }
        vm.onEscuelaTextoChanged("Prepa Nueva")
        vm.register()
        advanceUntilIdle()

        assertEquals(0, llamadas)
        assertNotNull(vm.uiState.value.escuela.ciudadError)
    }

    @Test fun `correo ya registrado se marca en el campo`() = runTest(dispatcher) {
        val vm = BusinessRegisterViewModel(
            { listOf(ulsa) },
            { throw ApiException("Ya existe", "CORREO_DUPLICADO") },
            {}
        )
        advanceUntilIdle()
        vm.actualizar {
            it.copy(nombreNegocio = "Lupe", nombre = "Lupe", correo = "lupe@x.mx", password = "secreto", confirmPassword = "secreto")
        }
        vm.onEscuelaSugeridaSelected(ulsa)
        vm.register()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.correoDuplicado)
        assertFalse(vm.uiState.value.isRegistered)
    }

    // ---- Pantalla del dueño ----

    private class FakeOwnerRepo(var negocio: Negocio) : OwnerRepository {
        val vendedores = mutableListOf<Usuario>()
        var fallarNegocio: Exception? = null
        var vendedorCreado: List<String>? = null
        override suspend fun miNegocio(): Negocio { fallarNegocio?.let { throw it }; return negocio }
        override suspend fun vendedores() = vendedores.toList()
        override suspend fun registrarVendedor(cafeteriaId: String, nombre: String, correo: String, password: String): Usuario {
            vendedorCreado = listOf(cafeteriaId, nombre, correo, password)
            return Usuario("v9", nombre, correo, escuelaId = "e1", cafeteriaId = cafeteriaId, rol = "VENDEDOR")
        }
        override suspend fun registrarCafeteria(nombre: String, ubicacion: String, escuelaId: String?, nombreEscuela: String, ciudadEscuela: String?) =
            CafeteriaNegocio("c9", nombre, ubicacion, activa = false, escuela = ulsa)
        override suspend fun escuelas() = listOf(ulsa)
    }

    @Test fun `el duenio en revision ve su negocio con la primera cafeteria elegida`() = runTest(dispatcher) {
        val vm = OwnerViewModel(FakeOwnerRepo(negocio("n1", cafeterias = listOf(cafeteria("c1"), cafeteria("c2")))))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.enRevision)
        assertEquals("c1", s.cafeteriaSeleccionada?.id)
        vm.seleccionarCafeteria("c2")
        assertEquals("c2", vm.uiState.value.cafeteriaSeleccionada?.id)
    }

    @Test fun `si falla cargar el negocio se muestra el error sin cerrar la app`() = runTest(dispatcher) {
        val repo = FakeOwnerRepo(negocio("n1")).apply { fallarNegocio = SinConexionException() }
        val vm = OwnerViewModel(repo)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.loaded)
    }

    @Test fun `dar de alta un vendedor en la cafeteria elegida`() = runTest(dispatcher) {
        val repo = FakeOwnerRepo(negocio("n1", "APROBADO"))
        val vm = OwnerViewModel(repo)
        advanceUntilIdle()

        vm.nuevoVendedor()
        vm.actualizarVendedor { it.copy(nombre = "Beto", correo = "beto@x.mx", password = "123") }
        vm.guardarVendedor()
        advanceUntilIdle()
        // Contraseña corta: no se envía
        assertNull(repo.vendedorCreado)
        assertNotNull(vm.uiState.value.vendedorForm?.errores?.password)

        vm.actualizarVendedor { it.copy(password = "123456") }
        vm.guardarVendedor()
        advanceUntilIdle()

        assertEquals(listOf("c1", "Beto", "beto@x.mx", "123456"), repo.vendedorCreado)
        assertNull(vm.uiState.value.vendedorForm)
        assertEquals(1, vm.uiState.value.vendedoresDe("c1").size)
    }

    @Test fun `abrir otra cafeteria la agrega y la deja elegida`() = runTest(dispatcher) {
        val vm = OwnerViewModel(FakeOwnerRepo(negocio("n1", "APROBADO")))
        advanceUntilIdle()

        vm.nuevaCafeteria()
        advanceUntilIdle()
        vm.actualizarCafeteria { it.copy(nombre = "Lupe Norte") }
        vm.onEscuelaSugeridaSelected(ulsa)
        vm.guardarCafeteria()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(2, s.cafeterias.size)
        assertEquals("c9", s.cafeteriaSeleccionada?.id)
        assertNull(s.cafeteriaForm)
    }

    // ---- Pantalla del administrador ----

    private class FakeAdminRepo : AdminRepository {
        val porEstado = mutableMapOf(
            "EN_REVISION" to listOf(negocio("n1"), negocio("n2")),
            "APROBADO" to listOf(negocio("n3", "APROBADO"))
        )
        val aprobados = mutableListOf<String>()
        val rechazados = mutableListOf<Pair<String, String>>()
        override suspend fun negocios(estado: String) = porEstado[estado].orEmpty()
        override suspend fun aprobar(negocioId: String): Negocio { aprobados += negocioId; return negocio(negocioId, "APROBADO") }
        override suspend fun rechazar(negocioId: String, motivo: String): Negocio {
            rechazados += negocioId to motivo
            return negocio(negocioId, "RECHAZADO")
        }
    }

    @Test fun `aprobar quita el negocio de la lista de revision`() = runTest(dispatcher) {
        val repo = FakeAdminRepo()
        val vm = AdminViewModel(repo)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.negocios.size)

        vm.aprobar(vm.uiState.value.negocios.first())
        advanceUntilIdle()

        assertEquals(listOf("n1"), repo.aprobados)
        assertEquals(listOf("n2"), vm.uiState.value.negocios.map { it.id })
        assertNotNull(vm.uiState.value.mensaje)
    }

    @Test fun `rechazar pide motivo`() = runTest(dispatcher) {
        val repo = FakeAdminRepo()
        val vm = AdminViewModel(repo)
        advanceUntilIdle()

        vm.abrirRechazo(vm.uiState.value.negocios.first())
        vm.confirmarRechazo()
        assertNotNull(vm.uiState.value.motivoError)
        assertTrue(repo.rechazados.isEmpty())

        vm.onMotivoChanged("  Falta contrato firmado ")
        vm.confirmarRechazo()
        advanceUntilIdle()
        assertEquals(listOf("n1" to "Falta contrato firmado"), repo.rechazados)
        assertNull(vm.uiState.value.rechazando)
    }

    @Test fun `cambiar de pestania carga ese estado`() = runTest(dispatcher) {
        val vm = AdminViewModel(FakeAdminRepo())
        // Cambia antes de que termine la primera carga
        vm.selectFiltro(FiltroNegocios.APROBADOS)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(FiltroNegocios.APROBADOS, s.filtro)
        assertEquals(listOf("n3"), s.negocios.map { it.id })
        assertFalse(s.puedeAprobar)
        assertTrue(s.puedeRechazar)
    }
}
