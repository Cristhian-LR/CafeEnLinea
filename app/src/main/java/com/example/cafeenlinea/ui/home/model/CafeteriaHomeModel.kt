package com.example.cafeenlinea.ui.home.model

/**
 * Datos que devuelve `GET /api/home/cafeteria/` (endpoint público, sin token).
 *
 * Solo son datos: ni red ni UI. La red vive en `cafeteriaHome/network/CafeteriaService`
 * y el estado en `cafeteriaHome/viewmodel/CafeteriaHomeViewModel`. Mismo criterio que
 * [com.jorgeromo.androidbasicsclass.ui.firstpartialpdm1.pickarooHome.model.Product].
 *
 * Notas del backend:
 * - `price` es número → se mapea a `Double`.
 * - El `id` reinicia en 1 en cada sección: NO es único global. La clave real es
 *   `section` + `id`.
 * - `description` y `badge` siempre vienen, pero pueden ser `""`.
 */

/**
 * Encabezado de la cafetería.
 *
 * @property name Nombre del negocio.
 * @property tagline Lema corto.
 * @property promo Mensaje promocional del momento.
 */
data class Shop(
    val name: String,
    val tagline: String,
    val promo: String
)

/**
 * Un platillo destacado (carrusel superior).
 *
 * @property id Identificador dentro de la lista de destacados.
 * @property name Nombre del platillo.
 * @property badge Etiqueta nutricional (ej. `"Fuente de proteína"`).
 * @property price Precio como número.
 * @property priceText Precio ya formateado por el servidor (ej. `"$35.00"`).
 * @property image URL de la imagen.
 */
data class FeaturedItem(
    val id: Int,
    val name: String,
    val badge: String,
    val price: Double,
    val priceText: String,
    val image: String
)

/**
 * Un producto del menú dentro de una sección.
 *
 * @property id Identificador local de la sección (no es único global).
 * @property name Nombre del producto.
 * @property description Descripción (puede venir `""`).
 * @property badge Etiqueta (puede venir `""`).
 * @property section Nombre de la sección a la que pertenece.
 * @property price Precio como número.
 * @property priceText Precio ya formateado (ej. `"$65.00"`).
 * @property image URL de la imagen.
 */
data class CafeteriaProduct(
    val id: Int,
    val name: String,
    val description: String,
    val badge: String,
    val section: String,
    val price: Double,
    val priceText: String,
    val image: String
)

/**
 * Una sección del menú.
 *
 * @property name Nombre de la sección (ej. `"Burritos"`).
 * @property note Nota al pie de la sección (puede venir `""`, ej. `"+ Queso extra $15"`).
 * @property products Productos de la sección.
 */
data class CafeteriaSection(
    val name: String,
    val note: String,
    val products: List<CafeteriaProduct>
)

/**
 * Un combo (platillo + bebida a precio cerrado).
 *
 * @property id Identificador del combo.
 * @property name Nombre (ej. `"Combo 1"`).
 * @property includes Qué incluye (ej. `"Burrito + bebida"`).
 * @property price Precio como número.
 * @property priceText Precio ya formateado.
 * @property image URL de la imagen.
 */
data class Combo(
    val id: Int,
    val name: String,
    val includes: String,
    val price: Double,
    val priceText: String,
    val image: String
)

/**
 * Un extra que se cobra aparte (ej. leche vegetal).
 *
 * @property id Identificador del extra.
 * @property name Nombre.
 * @property price Precio como número.
 * @property priceText Precio ya formateado.
 */
data class Extra(
    val id: Int,
    val name: String,
    val price: Double,
    val priceText: String
)

/**
 * Cuerpo completo de la respuesta de `GET /api/home/cafeteria/`.
 *
 * @property shop Encabezado del negocio.
 * @property featured Platillos destacados.
 * @property sections Menú agrupado por sección.
 * @property combos Combos disponibles.
 * @property combosNote Nota general de los combos.
 * @property extras Extras que se cobran aparte.
 */
data class CafeteriaHomeResponse(
    val shop: Shop,
    val featured: List<FeaturedItem>,
    val sections: List<CafeteriaSection>,
    val combos: List<Combo>,
    val combosNote: String,
    val extras: List<Extra>
)

/**
 * Estado observable de la pantalla de la cafetería. Mismo patrón que
 * [com.jorgeromo.androidbasicsclass.ui.firstpartialpdm1.pickarooHome.model.PickarooHomeUiState].
 */
data class CafeteriaHomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val shop: Shop? = null,
    val featured: List<FeaturedItem> = emptyList(),
    val sections: List<CafeteriaSection> = emptyList(),
    val combos: List<Combo> = emptyList(),
    val combosNote: String = "",
    val extras: List<Extra> = emptyList()
)