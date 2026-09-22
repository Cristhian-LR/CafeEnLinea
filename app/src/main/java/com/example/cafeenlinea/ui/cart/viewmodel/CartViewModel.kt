package com.example.cafeenlinea.ui.cart.viewmodel

import androidx.lifecycle.ViewModel
import com.example.cafeenlinea.ui.cart.model.CartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Dueño del estado del carrito. Vive a nivel de MainScreen para que tanto el
 * Home (donde se agregan productos) como el Carrito (donde se muestran)
 * compartan la misma instancia.
 */
class CartViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    /** Agrega un producto. Si ya existe (mismo id), solo sube la cantidad. */
    fun addItem(item: CartItem) {
        _items.update { current ->
            val existing = current.find { it.id == item.id }
            if (existing != null) {
                current.map {
                    if (it.id == item.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                current + item
            }
        }
    }

    fun increaseQuantity(id: String) {
        _items.update { current ->
            current.map { if (it.id == id) it.copy(quantity = it.quantity + 1) else it }
        }
    }

    /** Baja la cantidad; si llega a 0, lo quita del carrito. */
    fun decreaseQuantity(id: String) {
        _items.update { current ->
            current.mapNotNull {
                if (it.id == id) {
                    if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
                } else it
            }
        }
    }

    fun removeItem(id: String) {
        _items.update { current -> current.filterNot { it.id == id } }
    }

    fun clearCart() {
        _items.update { emptyList() }
    }

    /** Total calculado en el momento. */
    fun currentTotal(): Double = _items.value.sumOf { it.price * it.quantity }
}