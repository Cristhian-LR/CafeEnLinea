package com.example.cafeenlinea.ui.cart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.data.remote.mensajeDeError
import com.example.cafeenlinea.ui.cart.model.CartItem
import com.example.cafeenlinea.ui.cart.model.buildOrderItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Envía el pedido al servidor: (usuarioId, cafeteriaId, productoId -> cantidad). */
typealias EnviarPedido = suspend (String, String, Map<String, Int>) -> Pedido

/**
 * Dueño del estado del carrito. Vive a nivel de MainScreen para que tanto el
 * Home (donde se agregan productos) como el Carrito (donde se muestran)
 * compartan la misma instancia.
 *
 * @param enviarPedido Se puede reemplazar en las pruebas por uno falso.
 */
class CartViewModel(
    private val enviarPedido: EnviarPedido = ApiClient::crearPedido
) : ViewModel() {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    /** Mensaje si falló confirmar el pedido. La vista lo muestra y llama a [clearCheckoutError]. */
    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()

    /** Pedido recién creado. La vista navega a Pedidos y llama a [consumeCreatedOrder]. */
    private val _createdOrder = MutableStateFlow<Pedido?>(null)
    val createdOrder: StateFlow<Pedido?> = _createdOrder.asStateFlow()

    /** Agrega un producto. Si ya existe (mismo id), solo sube la cantidad. */
    fun addItem(item: CartItem) {
        _items.update { current ->
            val existing = current.find { it.id == item.id }
            if (existing != null) {
                current.map {
                    if (it.id == item.id) it.copy(quantity = it.quantity + item.quantity) else it
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

    fun setSubmitting(submitting: Boolean) {
        _isSubmitting.value = submitting
    }

    /** Total calculado en el momento. */
    fun currentTotal(): Double = _items.value.sumOf { it.lineTotal }

    /**
     * Crea el pedido en el servidor. Si sale bien vacía el carrito y publica el
     * pedido en [createdOrder]; si falla, conserva el carrito y publica el error.
     */
    fun confirmOrder(usuarioId: String?, cafeteriaId: String?) {
        if (_isSubmitting.value || _items.value.isEmpty()) return

        if (usuarioId.isNullOrBlank() || cafeteriaId.isNullOrBlank()) {
            _checkoutError.value = "No encontramos tu sesión. Cierra sesión e inicia de nuevo."
            return
        }
        val orderItems = buildOrderItems(_items.value)
        if (orderItems == null) {
            _checkoutError.value = "Hay productos viejos en tu carrito. Vacíalo y vuelve a agregarlos."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _checkoutError.value = null
            try {
                val pedido = enviarPedido(usuarioId, cafeteriaId, orderItems)
                clearCart()
                _createdOrder.value = pedido
            } catch (e: Exception) {
                _checkoutError.value = mensajeDeError(e, "confirmar tu pedido")
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearCheckoutError() {
        _checkoutError.value = null
    }

    fun consumeCreatedOrder() {
        _createdOrder.value = null
    }
}
