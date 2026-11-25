package com.example.cafeenlinea.ui.vendor.model

import androidx.annotation.StringRes
import com.example.cafeenlinea.data.remote.Producto

/** Estado de la pestaña Inventario. */
data class VendorInventoryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val productos: List<Producto> = emptyList(),
    val enProceso: Set<String> = emptySet(),
    /** Producto al que se le está capturando cuánto reponer. */
    val reponiendo: Producto? = null,
    val cantidad: String = "",
    @StringRes val cantidadError: Int? = null,
    @StringRes val mensaje: Int? = null
) {
    /** Llegaron a su punto de reorden; primero los que tienen menos. */
    val porReponer: List<Producto>
        get() = productos.filter { it.inventario.requiereReposicion }.sortedBy { it.inventario.stock }

    /** El resto, también de menos a más stock. */
    val conStock: List<Producto>
        get() = productos.filterNot { it.inventario.requiereReposicion }.sortedBy { it.inventario.stock }
}
