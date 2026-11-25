package com.example.cafeenlinea

import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.ui.cart.model.CartItem
import com.example.cafeenlinea.ui.cart.model.buildOrderItems
import com.example.cafeenlinea.ui.cart.viewmodel.CartViewModel
import com.example.cafeenlinea.ui.home.model.Extra
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
class CartCheckoutTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val leche = Extra(1, "Leche vegetal", 10.0, "$10.00", productId = "extra_leche")
    private val cafe = CartItem("c1", "Café", 30.0, "$30.00", "", quantity = 2, productId = "p_cafe")
    private val cafeConLeche = CartItem(
        "c2", "Café", 30.0, "$30.00", "", quantity = 1, extras = listOf(leche), productId = "p_cafe"
    )
    private val pedido = Pedido(id = "66000000bbbbbbbbbbbbbbbb", estado = "PENDIENTE")

    // --- buildOrderItems ---

    @Test fun `junta renglones del mismo producto y manda los extras como piezas`() {
        assertEquals(
            mapOf("p_cafe" to 3, "extra_leche" to 1),
            buildOrderItems(listOf(cafe, cafeConLeche))
        )
    }

    @Test fun `producto sin id del servidor no se puede enviar`() {
        assertNull(buildOrderItems(listOf(cafe.copy(productId = ""))))
    }

    // --- confirmOrder ---

    @Test fun `pedido correcto va a la cafeteria del carrito y lo vacia`() = runTest(dispatcher) {
        var enviado: Triple<String, String, Map<String, Int>>? = null
        val vm = CartViewModel { u, c, items -> enviado = Triple(u, c, items); pedido }
        vm.addItem(cafe, "caf1", "Latosos")

        vm.confirmOrder("u1")
        advanceUntilIdle()

        // El alumno no tiene cafetería en su cuenta: va a la de su carrito
        assertEquals(Triple("u1", "caf1", mapOf("p_cafe" to 2)), enviado)
        assertTrue(vm.items.value.isEmpty())
        assertNull(vm.cafeteria.value)
        assertEquals(pedido, vm.createdOrder.value)
        assertFalse(vm.isSubmitting.value)
    }

    @Test fun `si el servidor falla se conserva el carrito y hay error`() = runTest(dispatcher) {
        val vm = CartViewModel { _, _, _ -> throw ApiException("Stock insuficiente") }
        vm.addItem(cafe, "caf1", "Latosos")

        vm.confirmOrder("u1")
        advanceUntilIdle()

        assertEquals(listOf(cafe), vm.items.value)
        assertEquals("Stock insuficiente", vm.checkoutError.value)
        assertNull(vm.createdOrder.value)
        assertFalse(vm.isSubmitting.value)
    }

    @Test fun `sin sesion no se envia nada`() = runTest(dispatcher) {
        var llamadas = 0
        val vm = CartViewModel { _, _, _ -> llamadas++; pedido }
        vm.addItem(cafe, "caf1", "Latosos")

        vm.confirmOrder(null)
        advanceUntilIdle()

        assertEquals(0, llamadas)
        assertNotNull(vm.checkoutError.value)
    }

    // --- Una sola cafetería por pedido ---

    @Test fun `producto de otra cafeteria pregunta antes de vaciar`() {
        val vm = CartViewModel { _, _, _ -> pedido }
        vm.addItem(cafe, "caf1", "Latosos")
        vm.addItem(cafeConLeche, "caf2", "Tortas Lupe")

        // No se mezcla: queda pendiente
        assertEquals(listOf(cafe), vm.items.value)
        assertEquals("caf2", vm.pendiente.value?.cafeteria?.id)

        vm.dismissPending()
        assertNull(vm.pendiente.value)
        assertEquals("caf1", vm.cafeteria.value?.id)

        vm.addItem(cafeConLeche, "caf2", "Tortas Lupe")
        vm.replaceCartWithPending()
        assertEquals(listOf(cafeConLeche), vm.items.value)
        assertEquals("caf2", vm.cafeteria.value?.id)
        assertNull(vm.pendiente.value)
    }

    @Test fun `con el carrito vacio se puede agregar de cualquier cafeteria`() {
        val vm = CartViewModel { _, _, _ -> pedido }
        vm.addItem(cafe, "caf1", "Latosos")
        vm.removeItem(cafe.id)
        assertNull(vm.cafeteria.value)

        vm.addItem(cafeConLeche, "caf2", "Tortas Lupe")
        assertNull(vm.pendiente.value)
        assertEquals("caf2", vm.cafeteria.value?.id)
    }

    @Test fun `la misma cafeteria suma sin preguntar`() {
        val vm = CartViewModel { _, _, _ -> pedido }
        vm.addItem(cafe, "caf1", "Latosos")
        vm.addItem(cafe.copy(quantity = 1), "caf1", "Latosos")
        assertNull(vm.pendiente.value)
        assertEquals(3, vm.items.value.single().quantity)
    }
}
