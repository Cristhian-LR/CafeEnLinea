package com.example.cafeenlinea.ui.vendor.model

import androidx.annotation.StringRes
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.TextoBusqueda
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.data.remote.ProductoInput
import java.util.Locale

/** Moneda con la que se crean productos si la cafetería todavía no tiene ninguno. */
const val MONEDA_POR_DEFECTO = "MXN"

/** Mismos límites que valida el servidor. */
const val TIEMPO_PREPARACION_MIN = 1
const val TIEMPO_PREPARACION_MAX = 120

/** Categoría que pone el servidor cuando no se manda ninguna. */
const val CATEGORIA_POR_DEFECTO = "Menu"

data class CategoriaProductos(val categoria: String, val productos: List<Producto>)

/**
 * Agrupa por categoría sin importar mayúsculas ni acentos ("bebidas" y "Bebidas"
 * quedan juntas con el nombre de la primera). Categorías y productos en orden alfabético.
 */
fun agruparPorCategoria(productos: List<Producto>): List<CategoriaProductos> =
    productos
        .groupBy { TextoBusqueda.normalizar(it.categoria.ifBlank { CATEGORIA_POR_DEFECTO }) }
        .map { (clave, lista) ->
            clave to CategoriaProductos(
                categoria = TextoBusqueda.limpiar(lista.first().categoria.ifBlank { CATEGORIA_POR_DEFECTO }),
                productos = lista.sortedBy { TextoBusqueda.normalizar(it.nombre) }
            )
        }
        .sortedBy { it.first }
        .map { it.second }

/** Categorías que ya usa la cafetería, para sugerirlas al crear o editar un producto. */
fun categoriasExistentes(productos: List<Producto>): List<String> =
    agruparPorCategoria(productos).map { it.categoria }

/** Sugerencias para lo escrito; vacía si ya coincide exactamente con una categoría. */
fun sugerirCategorias(existentes: List<String>, texto: String): List<String> {
    val buscado = TextoBusqueda.normalizar(texto)
    if (existentes.any { TextoBusqueda.normalizar(it) == buscado }) return emptyList()
    // Con el campo vacío se muestran todas, para que el vendedor vea cuáles tiene
    if (buscado.isEmpty()) return existentes
    return TextoBusqueda.sugerir(existentes, texto, minCaracteres = 1, maximo = 6) { listOf(it) }
}

/**
 * Si lo escrito ya existe con otras mayúsculas o acentos, se usa la categoría
 * existente para no partir el menú en dos ("bebidas" -> "Bebidas").
 */
fun resolverCategoria(existentes: List<String>, texto: String): String {
    val limpio = TextoBusqueda.limpiar(texto)
    val buscado = TextoBusqueda.normalizar(limpio)
    return existentes.firstOrNull { TextoBusqueda.normalizar(it) == buscado } ?: limpio
}

/** Errores del formulario de producto (textos de strings.xml). */
data class ProductFormErrors(
    @StringRes val nombre: Int? = null,
    @StringRes val precio: Int? = null,
    @StringRes val categoria: Int? = null,
    @StringRes val tiempo: Int? = null,
    @StringRes val stock: Int? = null,
    @StringRes val puntoReorden: Int? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, precio, categoria, tiempo, stock, puntoReorden).any { it != null }
}

/**
 * Formulario de crear o editar producto. Los números se guardan como texto tal
 * como los escribe el vendedor y se validan al guardar.
 */
data class ProductForm(
    val productoId: String? = null,
    val nombre: String = "",
    val precio: String = "",
    val moneda: String = MONEDA_POR_DEFECTO,
    val categoria: String = "",
    val descripcion: String = "",
    val tiempoPreparacion: String = "5",
    val stockInicial: String = "20",
    val puntoReorden: String = "5",
    val destacado: Boolean = false,
    val etiqueta: String = "",
    /** Imagen ya subida a Cloudinary (al editar). */
    val imagenUrl: String? = null,
    /** Imagen elegida en el teléfono que todavía no se sube (Uri como texto). */
    val imagenLocal: String? = null,
    val errores: ProductFormErrors = ProductFormErrors(),
    val guardando: Boolean = false,
    val error: String? = null
) {
    val esNuevo: Boolean get() = productoId == null

    /** Lo que se ve como foto: la recién elegida o la que ya tenía. */
    val imagenVisible: String? get() = imagenLocal ?: imagenUrl

    companion object {
        fun nuevo(moneda: String = MONEDA_POR_DEFECTO) = ProductForm(moneda = moneda)

        fun desde(producto: Producto) = ProductForm(
            productoId = producto.id,
            nombre = producto.nombre,
            precio = formatearPrecio(producto.precio.monto),
            moneda = producto.precio.moneda,
            categoria = producto.categoria,
            descripcion = producto.descripcion,
            tiempoPreparacion = producto.tiempoPreparacionMin.toString(),
            stockInicial = producto.inventario.stock.toString(),
            puntoReorden = producto.inventario.puntoReorden.toString(),
            destacado = producto.destacado,
            etiqueta = producto.etiqueta,
            imagenUrl = producto.imagenUrl?.takeIf { it.isNotBlank() }
        )
    }
}

/** 35.0 -> "35", 35.5 -> "35.50" */
fun formatearPrecio(monto: Double): String =
    if (monto % 1.0 == 0.0) monto.toLong().toString() else String.format(Locale.US, "%.2f", monto)

/** Acepta coma o punto decimal y hasta 2 decimales. null si no es un precio válido. */
fun leerPrecio(texto: String): Double? {
    val limpio = texto.trim().replace(',', '.')
    if (!Regex("^\\d+(\\.\\d{1,2})?$").matches(limpio)) return null
    return limpio.toDoubleOrNull()?.takeIf { it > 0 }
}

fun ProductForm.validar(): ProductFormErrors {
    val tiempo = tiempoPreparacion.trim().toIntOrNull()
    val stock = stockInicial.trim().toIntOrNull()
    val reorden = puntoReorden.trim().toIntOrNull()
    return ProductFormErrors(
        nombre = if (nombre.isBlank()) R.string.vendor_form_error_name else null,
        precio = if (leerPrecio(precio) == null) R.string.vendor_form_error_price else null,
        categoria = if (categoria.isBlank()) R.string.vendor_form_error_category else null,
        tiempo = if (tiempo == null || tiempo !in TIEMPO_PREPARACION_MIN..TIEMPO_PREPARACION_MAX)
            R.string.vendor_form_error_time else null,
        // El inventario solo se captura al crear; al editar se usa "Reponer"
        stock = if (esNuevo && (stock == null || stock < 0)) R.string.vendor_form_error_stock else null,
        puntoReorden = if (esNuevo && (reorden == null || reorden < 0)) R.string.vendor_form_error_stock else null
    )
}

/** Datos para el servidor. Llamar solo después de [validar] sin errores. */
fun ProductForm.aInput(categoriaFinal: String, imagenFinal: String?): ProductoInput = ProductoInput(
    nombre = TextoBusqueda.limpiar(nombre),
    precio = leerPrecio(precio) ?: 0.0,
    moneda = moneda,
    categoria = categoriaFinal,
    descripcion = descripcion.trim(),
    tiempoPreparacionMin = tiempoPreparacion.trim().toInt(),
    destacado = destacado,
    etiqueta = etiqueta.trim(),
    imagenUrl = imagenFinal,
    stockInicial = stockInicial.trim().toIntOrNull() ?: 0,
    puntoReorden = puntoReorden.trim().toIntOrNull() ?: 0
)

/** Estado de la pestaña Menú del vendedor. */
data class VendorMenuUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val productos: List<Producto> = emptyList(),
    val busqueda: String = "",
    /** Productos con un cambio de disponibilidad en camino. */
    val enProceso: Set<String> = emptySet(),
    /** Formulario abierto, o null si no se está creando ni editando. */
    val formulario: ProductForm? = null,
    @StringRes val mensaje: Int? = null
) {
    val categorias: List<CategoriaProductos>
        get() {
            val buscado = TextoBusqueda.normalizar(busqueda)
            val filtrados = if (buscado.isEmpty()) productos else productos.filter {
                buscado in TextoBusqueda.normalizar(it.nombre) || buscado in TextoBusqueda.normalizar(it.categoria)
            }
            return agruparPorCategoria(filtrados)
        }

    val nombresCategorias: List<String> get() = categoriasExistentes(productos)

    /** Moneda de los productos que ya existen, para crear los nuevos igual. */
    val moneda: String get() = productos.firstOrNull()?.precio?.moneda ?: MONEDA_POR_DEFECTO
}
