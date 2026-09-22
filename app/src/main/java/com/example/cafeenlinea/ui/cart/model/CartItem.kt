package com.example.cafeenlinea.ui.cart.model

/**
 * Un producto dentro del carrito. Sirve para cualquier tipo de producto del
 * Home (destacado, producto de sección, o combo), por eso solo guarda lo
 * mínimo que se necesita mostrar y sumar.
 *
 * @property id Identificador único dentro del carrito (ej. "featured_1", "combo_2").
 * @property name Nombre del producto.
 * @property price Precio unitario como número (para calcular el total).
 * @property priceText Precio unitario ya formateado (ej. "$35.00").
 * @property image URL de la imagen.
 * @property quantity Cuántos de este producto hay en el carrito.
 */
data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val priceText: String,
    val image: String,
    val quantity: Int = 1
)