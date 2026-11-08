package com.example.cafeenlinea.ui.cart.model

import com.example.cafeenlinea.ui.home.model.Extra

/** (precio base + extras) × cantidad. */
fun calculateLineTotal(basePrice: Double, extras: List<Extra>, quantity: Int): Double =
    (basePrice + extras.sumOf { it.price }) * quantity

/**
 * Id del renglón del carrito: producto + extras (ordenados, para que el
 * orden en que se eligieron no importe). Mismo producto con distintos
 * extras = ids distintos = renglones separados.
 */
fun buildCartItemId(productId: String, extras: List<Extra>): String =
    if (extras.isEmpty()) productId
    else productId + "|" + extras.map { it.id }.sorted().joinToString(",")

/**
 * Convierte el carrito en lo que pide crearPedido: productoId del servidor -> cantidad.
 * Los extras también son productos del servidor, así que van como piezas propias
 * (un extra por cada pieza del producto). Si dos renglones comparten producto, se suman.
 *
 * @return null si algún producto o extra no trae su id del servidor.
 */
fun buildOrderItems(items: List<CartItem>): Map<String, Int>? {
    val resultado = linkedMapOf<String, Int>()
    for (item in items) {
        if (item.productId.isBlank()) return null
        resultado.merge(item.productId, item.quantity, Int::plus)
        for (extra in item.extras) {
            if (extra.productId.isBlank()) return null
            resultado.merge(extra.productId, item.quantity, Int::plus)
        }
    }
    return resultado
}