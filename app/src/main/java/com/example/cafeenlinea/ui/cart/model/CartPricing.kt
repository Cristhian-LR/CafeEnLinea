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