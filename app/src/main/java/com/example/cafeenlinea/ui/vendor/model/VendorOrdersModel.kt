package com.example.cafeenlinea.ui.vendor.model

import androidx.annotation.StringRes
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.Pedido

/** Columnas del tablero de pedidos, como en las apps de restaurantes. */
enum class ColumnaPedido(@StringRes val titulo: Int) {
    NUEVOS(R.string.vendor_orders_new),
    PREPARANDO(R.string.vendor_orders_preparing),
    LISTOS(R.string.vendor_orders_ready)
}

/**
 * Flujo de estados del servidor:
 * PENDIENTE -> ACEPTADO -> EN_PREPARACION -> LISTO -> ENTREGADO
 * (PENDIENTE y ACEPTADO también pueden pasar a CANCELADO).
 */
object FlujoPedido {
    const val PENDIENTE = "PENDIENTE"
    const val ACEPTADO = "ACEPTADO"
    const val EN_PREPARACION = "EN_PREPARACION"
    const val LISTO = "LISTO"
    const val ENTREGADO = "ENTREGADO"
    const val CANCELADO = "CANCELADO"

    private fun clave(estado: String) = estado.trim().uppercase()

    fun columna(estado: String): ColumnaPedido? = when (clave(estado)) {
        PENDIENTE -> ColumnaPedido.NUEVOS
        ACEPTADO, EN_PREPARACION -> ColumnaPedido.PREPARANDO
        LISTO -> ColumnaPedido.LISTOS
        else -> null
    }

    /** El estado al que lleva el botón principal de la tarjeta, o null si ya terminó. */
    fun siguiente(estado: String): String? = when (clave(estado)) {
        PENDIENTE -> ACEPTADO
        ACEPTADO -> EN_PREPARACION
        EN_PREPARACION -> LISTO
        LISTO -> ENTREGADO
        else -> null
    }

    /** Texto del botón principal según el estado actual. */
    @StringRes
    fun accion(estado: String): Int? = when (clave(estado)) {
        PENDIENTE -> R.string.vendor_action_accept
        ACEPTADO -> R.string.vendor_action_start
        EN_PREPARACION -> R.string.vendor_action_ready
        LISTO -> R.string.vendor_action_deliver
        else -> null
    }

    @StringRes
    fun etiqueta(estado: String): Int? = when (clave(estado)) {
        PENDIENTE -> R.string.vendor_status_pending
        ACEPTADO -> R.string.vendor_status_accepted
        EN_PREPARACION -> R.string.vendor_status_preparing
        LISTO -> R.string.vendor_status_ready
        else -> null
    }

    /** Ya empezó a prepararse: el servidor no deja cancelarlo. */
    fun sePuedeRechazar(estado: String): Boolean = clave(estado) in setOf(PENDIENTE, ACEPTADO)

    /** Para entregar hay que escribir el código que muestra el comprador. */
    fun pideCodigo(estado: String): Boolean = siguiente(estado) == ENTREGADO
}

/** Formato del código de recogida que genera el servidor: 4 dígitos. */
fun esCodigoRecogidaValido(codigo: String): Boolean = codigo.length == 4 && codigo.all(Char::isDigit)

/** Estado del tablero de pedidos del vendedor. */
data class VendorOrdersUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val pedidos: List<Pedido> = emptyList(),
    val columna: ColumnaPedido = ColumnaPedido.NUEVOS,
    /** Pedidos con un cambio de estado en camino (su botón muestra que está cargando). */
    val enProceso: Set<String> = emptySet(),
    /** Pedido al que se le está pidiendo el código para entregarlo. */
    val entregando: Pedido? = null,
    val codigo: String = "",
    val codigoError: String? = null,
    /** Aviso corto (snackbar) tras una acción correcta. */
    @StringRes val mensaje: Int? = null
) {
    val visibles: List<Pedido> get() = pedidos.filter { FlujoPedido.columna(it.estado) == columna }

    fun cuantos(columna: ColumnaPedido): Int = pedidos.count { FlujoPedido.columna(it.estado) == columna }
}
