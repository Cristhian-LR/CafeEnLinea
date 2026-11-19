package com.example.cafeenlinea.ui.home.model

/**
 * Modelos de la pantalla de la cafeteria. Ahora el menu se arma en la app a partir
 * de los productos de tu servidor (ver CafeteriaRepository), pero la pantalla sigue
 * usando estos mismos modelos.
 *
 * Los `id` (Int) son LOCALES de cada lista, igual que antes: solo sirven para las
 * claves de la UI. El id REAL del producto en el servidor viaja en `productId`
 * (lo necesita crearPedido en la siguiente etapa).
 */

/** Encabezado de la cafeteria. */
data class Shop(
    val name: String,
    val tagline: String,
    val promo: String
)

/** Un platillo destacado (carrusel superior). */
data class FeaturedItem(
    val id: Int,
    val name: String,
    val badge: String,
    val price: Double,
    val priceText: String,
    val image: String,
    val productId: String = ""
)

/** Un producto del menu dentro de una seccion. */
data class CafeteriaProduct(
    val id: Int,
    val name: String,
    val description: String,
    val badge: String,
    val section: String,
    val price: Double,
    val priceText: String,
    val image: String,
    val productId: String = ""
)

/** Una seccion del menu (en el servidor es la `categoria` del producto). */
data class CafeteriaSection(
    val name: String,
    val note: String,
    val products: List<CafeteriaProduct>
)

/** Un combo (platillo + bebida a precio cerrado). `includes` sale de la descripcion. */
data class Combo(
    val id: Int,
    val name: String,
    val includes: String,
    val price: Double,
    val priceText: String,
    val image: String,
    val productId: String = ""
)

/** Un extra que se cobra aparte (ej. leche vegetal). */
data class Extra(
    val id: Int,
    val name: String,
    val price: Double,
    val priceText: String,
    val productId: String = ""
)

/** Menu completo ya armado para la pantalla. */
data class CafeteriaHomeResponse(
    val shop: Shop,
    val featured: List<FeaturedItem>,
    val sections: List<CafeteriaSection>,
    val combos: List<Combo>,
    val combosNote: String,
    val extras: List<Extra>
)

/**
 * Contenido del menu de una cafeteria (datos crudos/cargados). Se reutiliza
 * con [com.example.cafeenlinea.ui.home.viewmodel.applyFilters] para busqueda y chips.
 */
data class CafeteriaHomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val shop: Shop? = null,
    val featured: List<FeaturedItem> = emptyList(),
    val sections: List<CafeteriaSection> = emptyList(),
    val combos: List<Combo> = emptyList(),
    val combosNote: String = "",
    val extras: List<Extra> = emptyList(),
    val searchQuery: String = "",
    val selectedSection: String? = null,
    val sectionNames: List<String> = emptyList(),
    val noResults: Boolean = false
)

/** Estado de una cafeteria en la lista desplegable del Home. */
data class CafeteriaListItemUiState(
    val id: String,
    val nombre: String,
    val expanded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Menu cacheado en memoria (crudo, sin filtrar) mientras la pantalla siga abierta. */
    val menu: CafeteriaHomeUiState? = null
) {
    val isEmpty: Boolean get() = !isLoading && error == null && menu != null &&
        menu.featured.isEmpty() && menu.sections.isEmpty() && menu.combos.isEmpty() && menu.extras.isEmpty()
}

/** Estado de la pantalla Home con multiples cafeterias desplegables. */
data class HomeScreenUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Mensaje informativo cuando no hay cafeterias que mostrar (no es error de sesion). */
    val emptyMessage: String? = null,
    val cafeterias: List<CafeteriaListItemUiState> = emptyList()
)