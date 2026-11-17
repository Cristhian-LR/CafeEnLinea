package com.example.cafeenlinea

import com.example.cafeenlinea.data.remote.Dinero
import com.example.cafeenlinea.data.remote.Inventario
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.ui.home.network.construirMenu
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductImageCatalogTest {

    private val gson = Gson()

    @Test
    fun `deserializa producto con imagenUrl de Cloudinary desde respuesta GraphQL`() {
        val json = """
            {
                "id": "prod_1",
                "nombre": "Café Americano",
                "disponible": true,
                "precio": { "monto": 35.0, "moneda": "MXN" },
                "inventario": { "stock": 10, "requiereReposicion": false },
                "descripcion": "Café tostado recién preparado",
                "categoria": "Bebidas Calientes",
                "imagenUrl": "https://res.cloudinary.com/demo/image/upload/v123456/cafe_americano.jpg",
                "destacado": true,
                "etiqueta": "Popular"
            }
        """.trimIndent()

        val producto = gson.fromJson(json, Producto::class.java)

        assertEquals("prod_1", producto.id)
        assertEquals("Café Americano", producto.nombre)
        assertEquals("https://res.cloudinary.com/demo/image/upload/v123456/cafe_americano.jpg", producto.imagenUrl)
        assertEquals(true, producto.destacado)
    }

    @Test
    fun `deserializa producto sin imagen dejando imagenUrl en null`() {
        val json = """
            {
                "id": "prod_2",
                "nombre": "Té Verde",
                "disponible": true,
                "precio": { "monto": 25.0, "moneda": "MXN" },
                "inventario": { "stock": 5, "requiereReposicion": false },
                "descripcion": "Té verde caliente",
                "categoria": "Bebidas Calientes",
                "destacado": false,
                "etiqueta": ""
            }
        """.trimIndent()

        val producto = gson.fromJson(json, Producto::class.java)

        assertEquals("prod_2", producto.id)
        assertNull(producto.imagenUrl)
    }

    @Test
    fun `construirMenu conserva imagenUrl en categorias combos destacados y menu general`() {
        val pDestacado = Producto(
            id = "p1",
            nombre = "Cappuccino",
            disponible = true,
            precio = Dinero(45.0, "MXN"),
            inventario = Inventario(20, false),
            descripcion = "Espresso con leche espumada",
            categoria = "Cafetería",
            imagenUrl = "https://res.cloudinary.com/demo/image/upload/cappuccino.jpg",
            destacado = true,
            etiqueta = "Top"
        )

        val pCombo = Producto(
            id = "p2",
            nombre = "Combo Desayuno",
            disponible = true,
            precio = Dinero(75.0, "MXN"),
            inventario = Inventario(15, false),
            descripcion = "Café + Croissant",
            categoria = "combos",
            imagenUrl = "https://res.cloudinary.com/demo/image/upload/combo1.jpg",
            destacado = false,
            etiqueta = "Ahorro"
        )

        val pSinImagen = Producto(
            id = "p3",
            nombre = "Galleta Avena",
            disponible = true,
            precio = Dinero(15.0, "MXN"),
            inventario = Inventario(30, false),
            descripcion = "Galleta horneada",
            categoria = "Snacks",
            imagenUrl = null,
            destacado = false,
            etiqueta = ""
        )

        val menu = construirMenu("Cafetería Central", listOf(pDestacado, pCombo, pSinImagen))

        // Verifica destacado
        assertEquals(1, menu.featured.size)
        assertEquals("https://res.cloudinary.com/demo/image/upload/cappuccino.jpg", menu.featured[0].image)

        // Verifica combo
        assertEquals(1, menu.combos.size)
        assertEquals("https://res.cloudinary.com/demo/image/upload/combo1.jpg", menu.combos[0].image)

        // Verifica sección del menú con imagen
        val seccionCafe = menu.sections.first { it.name == "Cafetería" }
        assertEquals("https://res.cloudinary.com/demo/image/upload/cappuccino.jpg", seccionCafe.products[0].image)

        // Verifica sección del menú sin imagen (debe mapear orEmpty() para activar el fallback del UI)
        val seccionSnacks = menu.sections.first { it.name == "Snacks" }
        assertEquals("", seccionSnacks.products[0].image)
    }
}
