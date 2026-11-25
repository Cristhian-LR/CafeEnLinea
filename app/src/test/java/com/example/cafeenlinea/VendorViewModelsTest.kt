package com.example.cafeenlinea

import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.Dinero
import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.data.remote.ProductoInput
import com.example.cafeenlinea.data.remote.SinConexionException
import com.example.cafeenlinea.ui.vendor.data.VendorRepository
import com.example.cafeenlinea.ui.vendor.model.isoAMillis
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorInventoryViewModel
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorMenuViewModel
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorOrdersViewModel
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorSummaryViewModel
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

/** Servidor falso: guarda lo que se le pidió y responde con lo configurado. */
private class FakeVendorRepository : VendorRepository {
    var cola = mutableListOf<Pedido>()
    var productos = mutableListOf<Producto>()
    var fallarConEstado: Exception? = null
    var fallarDisponibilidad: Exception? = null
    val cambiosDeEstado = mutableListOf<Triple<String, String, String?>>()
    var registrado: ProductoInput? = null
    var actualizado: Pair<String, ProductoInput>? = null
    var repuesto: Pair<String, Int?>? = null

    override suspend fun colaPedidos(cafeteriaId: String) = cola.toList()

    override suspend fun cambiarEstadoPedido(pedidoId: String, nuevoEstado: String, codigoRecogida: String?): Pedido {
        fallarConEstado?.let { throw it }
        cambiosDeEstado += Triple(pedidoId, nuevoEstado, codigoRecogida)
        return Pedido(pedidoId, nuevoEstado)
    }

    override suspend fun productos(cafeteriaId: String) = productos.toList()

    override suspend fun registrarProducto(cafeteriaId: String, datos: ProductoInput): Producto {
        registrado = datos
        return producto("nuevo", datos.nombre, datos.categoria)
    }

    override suspend fun actualizarProducto(productoId: String, datos: ProductoInput): Producto {
        actualizado = productoId to datos
        return producto(productoId, datos.nombre, datos.categoria)
    }

    override suspend fun cambiarDisponibilidad(productoId: String, disponible: Boolean): Producto {
        fallarDisponibilidad?.let { throw it }
        return productos.first { it.id == productoId }.copy(disponible = disponible)
    }

    override suspend fun reponer(productoId: String, cantidad: Int?): Producto {
        repuesto = productoId to cantidad
        return producto(productoId, stock = 10 + (cantidad ?: 0))
    }

    var historial = mutableListOf<Pedido>()
    var fallarHistorial: Exception? = null

    override suspend fun historialPedidos(cafeteriaId: String): List<Pedido> {
        fallarHistorial?.let { throw it }
        return historial.toList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VendorViewModelsTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeVendorRepository()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    // ---- Pedidos ----

    @Test fun `aceptar un pedido lo pasa a preparando`() = runTest(dispatcher) {
        repo.cola = mutableListOf(Pedido("p1", "PENDIENTE"))
        val vm = VendorOrdersViewModel("c1", repo)
        advanceUntilIdle()

        vm.avanzar(vm.uiState.value.pedidos.first())
        advanceUntilIdle()

        assertEquals(Triple("p1", "ACEPTADO", null), repo.cambiosDeEstado.single())
        assertEquals("ACEPTADO", vm.uiState.value.pedidos.single().estado)
        assertTrue(vm.uiState.value.enProceso.isEmpty())
    }

    @Test fun `entregar pide el codigo y quita el pedido de la cola`() = runTest(dispatcher) {
        repo.cola = mutableListOf(Pedido("p1", "LISTO"))
        val vm = VendorOrdersViewModel("c1", repo)
        advanceUntilIdle()

        vm.avanzar(vm.uiState.value.pedidos.first())
        assertEquals("p1", vm.uiState.value.entregando?.id)
        assertTrue(repo.cambiosDeEstado.isEmpty())

        // Código incompleto: no llama al servidor
        vm.onCodigoChanged("12")
        vm.confirmarEntrega("inválido")
        assertEquals("inválido", vm.uiState.value.codigoError)
        assertTrue(repo.cambiosDeEstado.isEmpty())

        vm.onCodigoChanged("12a34")
        assertEquals("1234", vm.uiState.value.codigo)
        vm.confirmarEntrega("inválido")
        advanceUntilIdle()

        assertEquals(Triple("p1", "ENTREGADO", "1234"), repo.cambiosDeEstado.single())
        assertTrue(vm.uiState.value.pedidos.isEmpty())
        assertNull(vm.uiState.value.entregando)
        assertNotNull(vm.uiState.value.mensaje)
    }

    @Test fun `codigo equivocado se muestra en el dialogo`() = runTest(dispatcher) {
        repo.cola = mutableListOf(Pedido("p1", "LISTO"))
        repo.fallarConEstado = ApiException("El codigo de recogida no coincide.", "BAD_USER_INPUT")
        val vm = VendorOrdersViewModel("c1", repo)
        advanceUntilIdle()

        vm.avanzar(vm.uiState.value.pedidos.first())
        vm.onCodigoChanged("9999")
        vm.confirmarEntrega("inválido")
        advanceUntilIdle()

        assertEquals("El codigo de recogida no coincide.", vm.uiState.value.codigoError)
        assertNotNull(vm.uiState.value.entregando)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `rechazar cancela solo pedidos que no se empiezan a preparar`() = runTest(dispatcher) {
        repo.cola = mutableListOf(Pedido("p1", "PENDIENTE"), Pedido("p2", "EN_PREPARACION"))
        val vm = VendorOrdersViewModel("c1", repo)
        advanceUntilIdle()

        vm.rechazar(Pedido("p2", "EN_PREPARACION"))
        vm.rechazar(Pedido("p1", "PENDIENTE"))
        advanceUntilIdle()

        assertEquals(listOf(Triple("p1", "CANCELADO", null)), repo.cambiosDeEstado)
        assertEquals(listOf("p2"), vm.uiState.value.pedidos.map { it.id })
    }

    @Test fun `sin cafeteria no llama al servidor y muestra error`() = runTest(dispatcher) {
        val vm = VendorOrdersViewModel(null, repo)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.loaded)
    }

    // ---- Menú ----

    @Test fun `pausar un producto se revierte si el servidor falla`() = runTest(dispatcher) {
        repo.productos = mutableListOf(producto("x1", disponible = true))
        repo.fallarDisponibilidad = SinConexionException()
        val vm = VendorMenuViewModel("c1", repo) { it }
        advanceUntilIdle()

        vm.toggleDisponible(vm.uiState.value.productos.first())
        // Se ve el cambio al instante
        assertFalse(vm.uiState.value.productos.first().disponible)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.productos.first().disponible)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `crear producto sube la foto y usa la categoria existente`() = runTest(dispatcher) {
        repo.productos = mutableListOf(producto("x1", categoria = "Bebidas"))
        var fotoSubida: String? = null
        val vm = VendorMenuViewModel("c1", repo) { uri -> fotoSubida = uri; "https://cdn/foto.jpg" }
        advanceUntilIdle()

        vm.nuevoProducto()
        vm.actualizarFormulario {
            it.copy(nombre = "Latte", precio = "45", categoria = "bebidas", imagenLocal = "content://foto")
        }
        vm.guardar()
        advanceUntilIdle()

        assertEquals("content://foto", fotoSubida)
        val datos = repo.registrado!!
        assertEquals("Bebidas", datos.categoria)
        assertEquals("https://cdn/foto.jpg", datos.imagenUrl)
        assertNull(vm.uiState.value.formulario)
        assertEquals(2, vm.uiState.value.productos.size)
    }

    @Test fun `con errores el formulario no se envia`() = runTest(dispatcher) {
        val vm = VendorMenuViewModel("c1", repo) { it }
        advanceUntilIdle()

        vm.nuevoProducto()
        vm.guardar()
        advanceUntilIdle()

        assertNull(repo.registrado)
        assertTrue(vm.uiState.value.formulario!!.errores.hayErrores)
    }

    @Test fun `editar actualiza el producto en la lista`() = runTest(dispatcher) {
        repo.productos = mutableListOf(producto("x1", "Café"))
        val vm = VendorMenuViewModel("c1", repo) { it }
        advanceUntilIdle()

        vm.editar(vm.uiState.value.productos.first())
        vm.actualizarFormulario { it.copy(nombre = "Café de olla") }
        vm.guardar()
        advanceUntilIdle()

        assertEquals("x1", repo.actualizado?.first)
        assertEquals("Café de olla", vm.uiState.value.productos.single().nombre)
    }

    // ---- Inventario y resumen ----

    @Test fun `reponer valida la cantidad y actualiza el stock`() = runTest(dispatcher) {
        repo.productos = mutableListOf(producto("x1", stock = 1))
        val vm = VendorInventoryViewModel("c1", repo)
        advanceUntilIdle()

        vm.abrirReponer(vm.uiState.value.productos.first())
        vm.onCantidadChanged("0")
        vm.confirmarReponer()
        assertNotNull(vm.uiState.value.cantidadError)
        assertNull(repo.repuesto)

        vm.onCantidadChanged("15")
        vm.confirmarReponer()
        advanceUntilIdle()

        assertEquals("x1" to 15, repo.repuesto)
        assertEquals(25, vm.uiState.value.productos.single().inventario.stock)
        assertNull(vm.uiState.value.reponiendo)
    }

    @Test fun `el resumen se calcula con los pedidos de la cafeteria`() = runTest(dispatcher) {
        repo.historial = mutableListOf(
            Pedido("p1", "ENTREGADO", Dinero(60.0, "MXN"), creadoEn = "2026-11-24T15:00:00.000Z"),
            Pedido("p2", "PENDIENTE", Dinero(30.0, "MXN"), creadoEn = "2026-11-24T15:30:00.000Z")
        )
        val vm = VendorSummaryViewModel("c1", repo) { isoAMillis("2026-11-24T16:00:00.000Z")!! }
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.loaded)
        assertEquals(2, s.resumen.pedidosHoy)
        assertEquals(1, s.resumen.entregadosHoy)
    }

    @Test fun `si falla al actualizar el resumen conserva lo anterior`() = runTest(dispatcher) {
        repo.historial = mutableListOf(Pedido("p1", "ENTREGADO", Dinero(60.0, "MXN"), creadoEn = "2026-11-24T15:00:00.000Z"))
        val vm = VendorSummaryViewModel("c1", repo) { isoAMillis("2026-11-24T16:00:00.000Z")!! }
        advanceUntilIdle()

        repo.fallarHistorial = SinConexionException()
        vm.refresh()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertNotNull(s.error)
        assertEquals(1, s.resumen.entregadosHoy)
    }
}
