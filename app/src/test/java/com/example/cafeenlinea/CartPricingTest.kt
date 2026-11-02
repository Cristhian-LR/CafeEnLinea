package com.example.cafeenlinea

import com.example.cafeenlinea.ui.cart.model.CartItem
import com.example.cafeenlinea.ui.cart.model.buildCartItemId
import com.example.cafeenlinea.ui.cart.model.calculateLineTotal
import com.example.cafeenlinea.ui.home.model.Extra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CartPricingTest {

    private val leche = Extra(1, "Leche vegetal", 10.0, "$10.00")
    private val queso = Extra(4, "Queso extra", 15.0, "$15.00")

    @Test fun `sin extras es precio por cantidad`() {
        assertEquals(130.0, calculateLineTotal(65.0, emptyList(), 2), 0.001)
    }

    @Test fun `con extras es base mas extras por cantidad`() {
        assertEquals(180.0, calculateLineTotal(65.0, listOf(leche, queso), 2), 0.001)
    }

    @Test fun `lineTotal del CartItem coincide`() {
        val item = CartItem("p", "Burrito", 65.0, "$65.00", "", 3, listOf(queso))
        assertEquals(240.0, item.lineTotal, 0.001)
    }

    @Test fun `extras distintos dan ids distintos`() {
        assertNotEquals(buildCartItemId("p1", listOf(leche)), buildCartItemId("p1", listOf(queso)))
        assertNotEquals(buildCartItemId("p1", emptyList()), buildCartItemId("p1", listOf(leche)))
    }

    @Test fun `el orden de los extras no cambia el id`() {
        assertEquals(
            buildCartItemId("p1", listOf(leche, queso)),
            buildCartItemId("p1", listOf(queso, leche))
        )
    }
}