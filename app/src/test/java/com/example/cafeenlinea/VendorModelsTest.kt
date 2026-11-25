package com.example.cafeenlinea

import com.example.cafeenlinea.data.remote.CambioEstado
import com.example.cafeenlinea.data.remote.Dinero
import com.example.cafeenlinea.data.remote.Inventario
import com.example.cafeenlinea.data.remote.ItemPedido
import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.ui.vendor.model.ColumnaPedido
import com.example.cafeenlinea.ui.vendor.model.FlujoPedido
import com.example.cafeenlinea.ui.vendor.model.MasVendido
import com.example.cafeenlinea.ui.vendor.model.PedidosPorHora
import com.example.cafeenlinea.ui.vendor.model.ProductForm
import com.example.cafeenlinea.ui.vendor.model.VendorInventoryUiState
import com.example.cafeenlinea.ui.vendor.model.VendorMenuUiState
import com.example.cafeenlinea.ui.vendor.model.VendorOrdersUiState
import com.example.cafeenlinea.ui.vendor.model.aInput
import com.example.cafeenlinea.ui.vendor.model.agruparPorCategoria
import com.example.cafeenlinea.ui.vendor.model.calcularResumen
import com.example.cafeenlinea.ui.vendor.model.isoAMillis
import com.example.cafeenlinea.ui.vendor.model.esCodigoRecogidaValido
import com.example.cafeenlinea.ui.vendor.model.formatearPrecio
import com.example.cafeenlinea.ui.vendor.model.leerPrecio
import com.example.cafeenlinea.ui.vendor.model.resolverCategoria
import com.example.cafeenlinea.ui.vendor.model.sugerirCategorias
import com.example.cafeenlinea.ui.vendor.model.validar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/** Crea un producto de prueba con valores por defecto razonables. */
internal fun producto(
    id: String,
    nombre: String = "Producto $id",
    categoria: String = "Bebidas",
    disponible: Boolean = true,
    stock: Int = 10,
    puntoReorden: Int = 3,
    precio: Double = 30.0
) = Producto(
    id = id,
    nombre = nombre,
    disponible = disponible,
    precio = Dinero(precio, "MXN"),
    inventario = Inventario(stock, stock <= puntoReorden, puntoReorden, 10),
    descripcion = "",
    categoria = categoria,
    imagenUrl = null,
    destacado = false,
    etiqueta = "",
    tiempoPreparacionMin = 5
)

class VendorModelsTest {

    // ---- Pedidos ----

    @Test
    fun `el flujo avanza un paso a la vez hasta entregar`() {
        assertEquals("ACEPTADO", FlujoPedido.siguiente("PENDIENTE"))
        assertEquals("EN_PREPARACION", FlujoPedido.siguiente("aceptado"))
        assertEquals("LISTO", FlujoPedido.siguiente("EN_PREPARACION"))
        assertEquals("ENTREGADO", FlujoPedido.siguiente("LISTO"))
        assertNull(FlujoPedido.siguiente("ENTREGADO"))
        assertNull(FlujoPedido.siguiente("CANCELADO"))
    }

    @Test
    fun `solo se rechaza antes de empezar a preparar y solo entregar pide codigo`() {
        assertTrue(FlujoPedido.sePuedeRechazar("PENDIENTE"))
        assertTrue(FlujoPedido.sePuedeRechazar("ACEPTADO"))
        assertFalse(FlujoPedido.sePuedeRechazar("EN_PREPARACION"))
        assertTrue(FlujoPedido.pideCodigo("LISTO"))
        assertFalse(FlujoPedido.pideCodigo("EN_PREPARACION"))
    }

    @Test
    fun `los pedidos se reparten en columnas`() {
        val state = VendorOrdersUiState(
            pedidos = listOf(
                Pedido("1", "PENDIENTE"), Pedido("2", "ACEPTADO"),
                Pedido("3", "EN_PREPARACION"), Pedido("4", "LISTO"), Pedido("5", "PENDIENTE")
            ),
            columna = ColumnaPedido.PREPARANDO
        )
        assertEquals(2, state.cuantos(ColumnaPedido.NUEVOS))
        assertEquals(listOf("2", "3"), state.visibles.map { it.id })
        assertEquals(1, state.cuantos(ColumnaPedido.LISTOS))
    }

    @Test
    fun `el codigo de recogida tiene 4 digitos`() {
        assertTrue(esCodigoRecogidaValido("0427"))
        assertFalse(esCodigoRecogidaValido("427"))
        assertFalse(esCodigoRecogidaValido("04a7"))
    }

    // ---- Menú y categorías ----

    @Test
    fun `agrupa por categoria sin importar mayusculas ni acentos`() {
        val grupos = agruparPorCategoria(
            listOf(
                producto("1", "Té", categoria = "Bebidas"),
                producto("2", "Agua", categoria = "bebidas "),
                producto("3", "Molletes", categoria = "Desayunos"),
                producto("4", "Sin categoría", categoria = "")
            )
        )
        assertEquals(listOf("Bebidas", "Desayunos", "Menu"), grupos.map { it.categoria })
        // Productos en orden alfabético dentro de la categoría
        assertEquals(listOf("Agua", "Té"), grupos.first().productos.map { it.nombre })
    }

    @Test
    fun `sugiere categorias existentes y deja de sugerir al coincidir`() {
        val existentes = listOf("Bebidas", "Bebidas calientes", "Desayunos")
        assertEquals(existentes, sugerirCategorias(existentes, ""))
        assertEquals(listOf("Bebidas", "Bebidas calientes"), sugerirCategorias(existentes, "beb"))
        assertTrue(sugerirCategorias(existentes, "desayunos").isEmpty())
    }

    @Test
    fun `una categoria escrita distinto usa la que ya existe`() {
        val existentes = listOf("Bebidas", "Desayunos")
        assertEquals("Bebidas", resolverCategoria(existentes, "  bebidas "))
        assertEquals("Postres fríos", resolverCategoria(existentes, "Postres   fríos"))
    }

    @Test
    fun `la busqueda filtra por nombre o categoria`() {
        val state = VendorMenuUiState(
            productos = listOf(producto("1", "Café", "Bebidas"), producto("2", "Molletes", "Desayunos")),
            busqueda = "desay"
        )
        assertEquals(listOf("Molletes"), state.categorias.flatMap { it.productos }.map { it.nombre })
    }

    @Test
    fun `lee precios con punto o coma y maximo 2 decimales`() {
        assertEquals(35.5, leerPrecio("35,5")!!, 0.001)
        assertEquals(35.0, leerPrecio(" 35 ")!!, 0.001)
        assertNull(leerPrecio("0"))
        assertNull(leerPrecio("35.555"))
        assertNull(leerPrecio("abc"))
        assertEquals("35", formatearPrecio(35.0))
        assertEquals("35.50", formatearPrecio(35.5))
    }

    @Test
    fun `el formulario nuevo valida todos sus campos`() {
        val errores = ProductForm(precio = "0", categoria = " ", tiempoPreparacion = "200", stockInicial = "").validar()
        assertNotNull(errores.nombre)
        assertNotNull(errores.precio)
        assertNotNull(errores.categoria)
        assertNotNull(errores.tiempo)
        assertNotNull(errores.stock)
        assertTrue(errores.hayErrores)
    }

    @Test
    fun `al editar no se valida el inventario`() {
        val form = ProductForm.desde(producto("1", "Café", precio = 25.0)).copy(stockInicial = "")
        assertFalse(form.validar().hayErrores)
        assertFalse(form.esNuevo)
    }

    @Test
    fun `el formulario se convierte en datos limpios para el servidor`() {
        val form = ProductForm(
            nombre = "  Café   americano ", precio = "32,50", categoria = "bebidas",
            tiempoPreparacion = "4", stockInicial = "15", puntoReorden = "3", etiqueta = " Nuevo "
        )
        val datos = form.aInput("Bebidas", "https://img")
        assertEquals("Café americano", datos.nombre)
        assertEquals(32.5, datos.precio, 0.001)
        assertEquals("Bebidas", datos.categoria)
        assertEquals("Nuevo", datos.etiqueta)
        assertEquals(15, datos.stockInicial)
        assertEquals("https://img", datos.imagenUrl)
    }

    // ---- Inventario y resumen ----

    @Test
    fun `inventario separa lo que se esta acabando`() {
        val state = VendorInventoryUiState(
            productos = listOf(producto("1", stock = 2), producto("2", stock = 50), producto("3", stock = 0))
        )
        assertEquals(listOf("3", "1"), state.porReponer.map { it.id })
        assertEquals(listOf("2"), state.conStock.map { it.id })
    }

    @Test
    fun `lee las fechas ISO del servidor`() {
        assertEquals(0L, isoAMillis("1970-01-01T00:00:00.000Z"))
        assertEquals(60_000L, isoAMillis("1970-01-01T00:01:00Z"))
        assertNull(isoAMillis("ayer"))
        assertNull(isoAMillis(null))
    }

    // ---- Resumen (calculado con los pedidos de la cafetería) ----

    private val utc = TimeZone.getTimeZone("UTC")
    private val ahora = isoAMillis("2026-11-24T20:00:00.000Z")!!

    private fun pedidoResumen(
        id: String,
        estado: String,
        creado: String,
        total: Double = 50.0,
        items: List<ItemPedido> = listOf(ItemPedido("Café", 1, Dinero(25.0, "MXN"))),
        historial: List<CambioEstado> = emptyList()
    ) = Pedido(id, estado, Dinero(total, "MXN"), items, creadoEn = creado, historial = historial)

    @Test
    fun `el resumen cuenta solo lo de hoy y sin cancelados`() {
        val resumen = calcularResumen(
            listOf(
                pedidoResumen("1", "ENTREGADO", "2026-11-24T09:10:00.000Z", total = 80.0),
                pedidoResumen("2", "ENTREGADO", "2026-11-24T13:05:00.000Z", total = 40.5),
                pedidoResumen("3", "PENDIENTE", "2026-11-24T13:40:00.000Z"),
                pedidoResumen("4", "CANCELADO", "2026-11-24T13:50:00.000Z"),
                pedidoResumen("5", "ENTREGADO", "2026-11-23T13:00:00.000Z", total = 999.0)
            ),
            ahora, utc
        )
        assertEquals(3, resumen.pedidosHoy)
        assertEquals(2, resumen.entregadosHoy)
        assertEquals(120.5, resumen.ventasHoy, 0.001)
        // De las 9 a las 13, con las horas vacías en medio
        assertEquals((9..13).toList(), resumen.porHora.map { it.hora })
        assertEquals(PedidosPorHora(13, 2), resumen.horaPico)
    }

    @Test
    fun `lo mas vendido suma unidades de pedidos entregados`() {
        val resumen = calcularResumen(
            listOf(
                pedidoResumen("1", "ENTREGADO", "2026-11-20T09:00:00.000Z",
                    items = listOf(ItemPedido("Café", 2, Dinero(25.0, "MXN")), ItemPedido("Mollete", 1, Dinero(40.0, "MXN")))),
                pedidoResumen("2", "ENTREGADO", "2026-11-21T09:00:00.000Z",
                    items = listOf(ItemPedido("Café", 1, Dinero(25.0, "MXN")))),
                // No cuenta: no se entregó
                pedidoResumen("3", "PENDIENTE", "2026-11-21T09:00:00.000Z",
                    items = listOf(ItemPedido("Mollete", 5, Dinero(40.0, "MXN"))))
            ),
            ahora, utc
        )
        assertEquals(listOf(MasVendido("Café", 3, 75.0), MasVendido("Mollete", 1, 40.0)), resumen.masVendidos)
    }

    @Test
    fun `los tiempos salen del historial de estados`() {
        val historial = listOf(
            CambioEstado("ACEPTADO", "2026-11-24T10:02:00.000Z"),
            CambioEstado("EN_PREPARACION", "2026-11-24T10:05:00.000Z"),
            CambioEstado("LISTO", "2026-11-24T10:13:00.000Z"),
            CambioEstado("ENTREGADO", "2026-11-24T10:20:00.000Z")
        )
        val resumen = calcularResumen(
            listOf(
                pedidoResumen("1", "ENTREGADO", "2026-11-24T10:00:00.000Z", historial = historial),
                // Sin historial (pedidos viejos): no se mide
                pedidoResumen("2", "ENTREGADO", "2026-11-24T11:00:00.000Z")
            ),
            ahora, utc
        )
        assertEquals(8.0, resumen.preparacionPromedioMin!!, 0.001)
        assertEquals(20.0, resumen.entregaPromedioMin!!, 0.001)
        assertEquals(1, resumen.pedidosMedidos)
    }

    @Test
    fun `sin pedidos el resumen queda vacio`() {
        val resumen = calcularResumen(emptyList(), ahora, utc)
        assertEquals(0, resumen.pedidosHoy)
        assertNull(resumen.horaPico)
        assertNull(resumen.preparacionPromedioMin)
        assertTrue(resumen.masVendidos.isEmpty())
    }
}
