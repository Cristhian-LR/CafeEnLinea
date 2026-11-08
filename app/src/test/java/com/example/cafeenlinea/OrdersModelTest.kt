package com.example.cafeenlinea

import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.ui.orders.model.OrderStatus
import com.example.cafeenlinea.ui.orders.model.creadoEnMillis
import com.example.cafeenlinea.ui.orders.model.estadoTexto
import com.example.cafeenlinea.ui.orders.model.numeroCorto
import com.example.cafeenlinea.ui.orders.model.separarPedidos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrdersModelTest {

    // ObjectIds reales: los primeros 8 hex son la fecha en segundos
    private val viejo = Pedido(id = "65000000aaaaaaaaaaaaaaaa", estado = "ENTREGADO")
    private val medio = Pedido(id = "66000000bbbbbbbbbbbbbbbb", estado = "PENDIENTE")
    private val nuevo = Pedido(id = "67000000cccccccccccccccc", estado = "LISTO")

    @Test fun `estados conocidos se traducen`() {
        assertEquals(OrderStatus.EN_PREPARACION, OrderStatus.from("EN_PREPARACION"))
        assertEquals(OrderStatus.LISTO, OrderStatus.from(" listo "))
        assertEquals("Listo para recoger", nuevo.estadoTexto())
    }

    @Test fun `estado desconocido muestra la clave del servidor`() {
        val raro = Pedido(id = "x", estado = "CANCELADO")
        assertEquals(OrderStatus.DESCONOCIDO, OrderStatus.from("CANCELADO"))
        assertEquals("CANCELADO", raro.estadoTexto())
    }

    @Test fun `separa en curso e historial, mas nuevos primero`() {
        val (enCurso, historial) = separarPedidos(listOf(viejo, medio, nuevo))
        assertEquals(listOf(nuevo, medio), enCurso)
        assertEquals(listOf(viejo), historial)
    }

    @Test fun `estado desconocido va al historial`() {
        val raro = Pedido(id = "68000000dddddddddddddddd", estado = "CANCELADO")
        val (enCurso, historial) = separarPedidos(listOf(raro))
        assertEquals(emptyList<Pedido>(), enCurso)
        assertEquals(listOf(raro), historial)
    }

    @Test fun `fecha sale del ObjectId`() {
        assertEquals(0x65000000L * 1000, viejo.creadoEnMillis())
    }

    @Test fun `id que no es ObjectId no tiene fecha`() {
        assertNull(Pedido(id = "abc", estado = "PENDIENTE").creadoEnMillis())
        assertNull(Pedido(id = "zz000000aaaaaaaaaaaaaaaa", estado = "PENDIENTE").creadoEnMillis())
    }

    @Test fun `numero corto son los ultimos 6 en mayusculas`() {
        assertEquals("#CCCCCC", nuevo.numeroCorto())
    }
}
