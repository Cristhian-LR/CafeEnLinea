package com.example.cafeenlinea.ui.home.network

import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.ApiException
import com.example.cafeenlinea.data.remote.Dinero
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.ui.home.model.CafeteriaHomeResponse
import com.example.cafeenlinea.ui.home.model.CafeteriaProduct
import com.example.cafeenlinea.ui.home.model.CafeteriaSection
import com.example.cafeenlinea.ui.home.model.Combo
import com.example.cafeenlinea.ui.home.model.Extra
import com.example.cafeenlinea.ui.home.model.FeaturedItem
import com.example.cafeenlinea.ui.home.model.Shop
import java.util.Locale

/** Pide el menu a tu servidor y lo convierte al formato que usa la pantalla. */
object CafeteriaRepository {

    suspend fun cargarMenu(cafeteriaId: String): CafeteriaHomeResponse {
        val productos = ApiClient.productos(cafeteriaId)
        val nombre = try {
            ApiClient.cafeteria(cafeteriaId).nombre
        } catch (e: ApiException) {
            "Cafetería"
        }
        return construirMenu(nombre, productos)
    }
}

// Categorias con tratamiento especial: no son secciones del menu.
private const val CATEGORIA_EXTRAS = "extras"
private const val CATEGORIA_COMBOS = "combos"

/**
 * Reglas para armar la pantalla con los productos del servidor:
 *  - Solo se muestran productos disponibles y con stock.
 *  - categoria "Extras"  -> lista de extras (se cobran aparte).
 *  - categoria "Combos"  -> carrusel de combos (la descripcion dice que incluye).
 *  - destacado = true    -> carrusel de destacados (tambien aparece en su seccion).
 *  - Cualquier otra categoria -> una seccion del menu, en el orden en que aparece.
 */
internal fun construirMenu(nombreCafeteria: String, productos: List<Producto>): CafeteriaHomeResponse {
    val visibles = productos.filter { it.disponible && it.inventario.stock > 0 }
    fun Producto.categoriaNormalizada() = categoria.trim().lowercase()

    val extras = visibles
        .filter { it.categoriaNormalizada() == CATEGORIA_EXTRAS }
        .mapIndexed { i, p ->
            Extra(
                id = i + 1,
                name = p.nombre,
                price = p.precio.monto,
                priceText = precioTexto(p.precio),
                productId = p.id
            )
        }

    val combos = visibles
        .filter { it.categoriaNormalizada() == CATEGORIA_COMBOS }
        .mapIndexed { i, p ->
            Combo(
                id = i + 1,
                name = p.nombre,
                includes = p.descripcion,
                price = p.precio.monto,
                priceText = precioTexto(p.precio),
                image = p.imagenUrl.orEmpty(),
                productId = p.id
            )
        }

    val delMenu = visibles.filter {
        it.categoriaNormalizada() != CATEGORIA_EXTRAS && it.categoriaNormalizada() != CATEGORIA_COMBOS
    }

    val destacados = delMenu
        .filter { it.destacado }
        .mapIndexed { i, p ->
            FeaturedItem(
                id = i + 1,
                name = p.nombre,
                badge = p.etiqueta,
                price = p.precio.monto,
                priceText = precioTexto(p.precio),
                image = p.imagenUrl.orEmpty(),
                productId = p.id
            )
        }

    val secciones = delMenu
        .groupBy { it.categoria.trim().ifEmpty { "Menú" } }
        .map { (nombreSeccion, lista) ->
            CafeteriaSection(
                name = nombreSeccion,
                note = "",
                products = lista.mapIndexed { i, p ->
                    CafeteriaProduct(
                        id = i + 1,
                        name = p.nombre,
                        description = p.descripcion,
                        badge = p.etiqueta,
                        section = nombreSeccion,
                        price = p.precio.monto,
                        priceText = precioTexto(p.precio),
                        image = p.imagenUrl.orEmpty(),
                        productId = p.id
                    )
                }
            )
        }

    return CafeteriaHomeResponse(
        shop = Shop(name = nombreCafeteria, tagline = "", promo = ""),
        featured = destacados,
        sections = secciones,
        combos = combos,
        combosNote = "",
        extras = extras
    )
}

private fun precioTexto(d: Dinero): String =
    if (d.moneda == "MXN") String.format(Locale.US, "\$%.2f", d.monto)
    else String.format(Locale.US, "%.2f %s", d.monto, d.moneda)