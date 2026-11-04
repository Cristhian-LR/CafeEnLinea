package com.example.cafeenlinea

import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.data.remote.SinConexionException
import com.example.cafeenlinea.ui.orders.viewmodel.OrdersViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val pendiente = Pedido(id = "66000000bbbbbbbbbbbbbbbb", estado = "PENDIENTE")
    private val entregado = Pedido(id = "65000000aaaaaaaaaaaaaaaa", estado = "ENTREGADO")

    @Test fun `carga y separa los pedidos del usuario`() = runTest(dispatcher) {
        var pedidoPara: String? = null
        val vm = OrdersViewModel({ "u1" }) { id -> pedidoPara = id; listOf(entregado, pendiente) }
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("u1", pedidoPara)
        assertTrue(s.loaded)
        assertFalse(s.isLoading)
        assertEquals(listOf(pendiente), s.enCurso)
        assertEquals(listOf(entregado), s.historial)
    }

    @Test fun `sin conexion muestra error y no marca como cargado`() = runTest(dispatcher) {
        val vm = OrdersViewModel({ "u1" }) { throw SinConexionException() }
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.loaded)
        assertNotNull(s.error)
        assertTrue(s.error!!.contains("conectar"))
    }

    @Test fun `sin usuario en sesion no llama al servidor`() = runTest(dispatcher) {
        var llamadas = 0
        val vm = OrdersViewModel({ null }) { llamadas++; emptyList() }
        advanceUntilIdle()

        assertEquals(0, llamadas)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `actualizacion silenciosa no pregunta si no hay pedidos en curso`() = runTest(dispatcher) {
        var llamadas = 0
        val vm = OrdersViewModel({ "u1" }) { llamadas++; listOf(entregado) }
        advanceUntilIdle()

        vm.refreshSilently()
        advanceUntilIdle()
        assertEquals(1, llamadas)
    }

    @Test fun `actualizacion silenciosa no muestra errores`() = runTest(dispatcher) {
        var falla = false
        val vm = OrdersViewModel({ "u1" }) {
            if (falla) throw SinConexionException() else listOf(pendiente)
        }
        advanceUntilIdle()

        falla = true
        vm.refreshSilently()
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertEquals(listOf(pendiente), vm.uiState.value.enCurso)
    }
}
