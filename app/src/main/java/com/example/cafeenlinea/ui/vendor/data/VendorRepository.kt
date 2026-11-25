package com.example.cafeenlinea.ui.vendor.data

import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.data.remote.ProductoInput

/**
 * Lo que necesitan las pantallas del vendedor del servidor. Es una interfaz para
 * que los ViewModel se puedan probar con datos falsos.
 */
interface VendorRepository {
    suspend fun colaPedidos(cafeteriaId: String): List<Pedido>
    suspend fun cambiarEstadoPedido(pedidoId: String, nuevoEstado: String, codigoRecogida: String? = null): Pedido

    suspend fun productos(cafeteriaId: String): List<Producto>
    suspend fun registrarProducto(cafeteriaId: String, datos: ProductoInput): Producto
    suspend fun actualizarProducto(productoId: String, datos: ProductoInput): Producto
    suspend fun cambiarDisponibilidad(productoId: String, disponible: Boolean): Producto
    suspend fun reponer(productoId: String, cantidad: Int?): Producto

    /** Todos los pedidos de la cafetería, para calcular el resumen. */
    suspend fun historialPedidos(cafeteriaId: String): List<Pedido>
}

/** Implementación real contra el backend GraphQL. */
object ApiVendorRepository : VendorRepository {
    override suspend fun colaPedidos(cafeteriaId: String) = ApiClient.colaCafeteria(cafeteriaId)

    override suspend fun cambiarEstadoPedido(pedidoId: String, nuevoEstado: String, codigoRecogida: String?) =
        ApiClient.actualizarEstadoPedido(pedidoId, nuevoEstado, codigoRecogida)

    override suspend fun productos(cafeteriaId: String) = ApiClient.productosVendedor(cafeteriaId)

    override suspend fun registrarProducto(cafeteriaId: String, datos: ProductoInput) =
        ApiClient.registrarProducto(cafeteriaId, datos)

    override suspend fun actualizarProducto(productoId: String, datos: ProductoInput) =
        ApiClient.actualizarProducto(productoId, datos)

    override suspend fun cambiarDisponibilidad(productoId: String, disponible: Boolean) =
        ApiClient.cambiarDisponibilidadProducto(productoId, disponible)

    override suspend fun reponer(productoId: String, cantidad: Int?) =
        ApiClient.reponerInventario(productoId, cantidad)

    override suspend fun historialPedidos(cafeteriaId: String) = ApiClient.pedidosCafeteria(cafeteriaId)
}
