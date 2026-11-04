package com.example.cafeenlinea.ui.orders.model

import com.example.cafeenlinea.data.remote.Pedido

/**
 * Estados del pedido según el catálogo del servidor (clave -> texto para mostrar).
 * [paso] sirve para dibujar el avance: 1 = recibido ... 4 = entregado.
 */
enum class OrderStatus(val clave: String, val etiqueta: String, val paso: Int, val enCurso: Boolean) {
    PENDIENTE("PENDIENTE", "Pendiente", 1, true),
    EN_PREPARACION("EN_PREPARACION", "En preparación", 2, true),
    LISTO("LISTO", "Listo para recoger", 3, true),
    ENTREGADO("ENTREGADO", "Entregado", 4, false),

    /** Un estado que la app todavía no conoce. Se muestra en el historial con su clave. */
    DESCONOCIDO("", "", 0, false);

    companion object {
        const val TOTAL_PASOS = 4

        fun from(clave: String): OrderStatus =
            entries.firstOrNull { it != DESCONOCIDO && it.clave.equals(clave.trim(), ignoreCase = true) }
                ?: DESCONOCIDO
    }
}

/** Texto del estado, con la clave del servidor si la app no lo conoce. */
fun Pedido.estadoTexto(): String {
    val status = OrderStatus.from(estado)
    return if (status == OrderStatus.DESCONOCIDO) estado else status.etiqueta
}

/** Número corto para mostrar: los últimos 6 caracteres del id, ej. "#3F9A1C". */
fun Pedido.numeroCorto(): String = "#" + id.takeLast(6).uppercase()

/**
 * El id de Mongo (ObjectId) empieza con la fecha de creación en segundos (8 dígitos hex).
 * Así sacamos la fecha aunque el servidor no la mande. null si el id no tiene ese formato.
 */
fun Pedido.creadoEnMillis(): Long? {
    if (id.length != 24 || !id.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return id.substring(0, 8).toLongOrNull(16)?.times(1000)
}

/** Separa en "en curso" e "historial", los más nuevos primero. */
fun separarPedidos(pedidos: List<Pedido>): Pair<List<Pedido>, List<Pedido>> {
    val ordenados = pedidos.sortedByDescending { it.creadoEnMillis() ?: Long.MIN_VALUE }
    return ordenados.partition { OrderStatus.from(it.estado).enCurso }
}

/** Estado de la pantalla de Pedidos. */
data class OrdersUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val enCurso: List<Pedido> = emptyList(),
    val historial: List<Pedido> = emptyList(),
    /** true después de la primera carga correcta (para no confundir "sin pedidos" con "no ha cargado"). */
    val loaded: Boolean = false
) {
    val isEmpty: Boolean get() = enCurso.isEmpty() && historial.isEmpty()
}
