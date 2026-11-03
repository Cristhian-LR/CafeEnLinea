package com.example.cafeenlinea.data.remote

import com.example.cafeenlinea.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Modelos (reflejan los tipos de tu schema.graphql)
// Si ya tienes clases con estos nombres en otro paquete, no hay conflicto
// mientras no importes las dos a la vez.
// ---------------------------------------------------------------------------
data class Dinero(val monto: Double, val moneda: String)
data class Usuario(val id: String, val nombre: String, val correo: String, val cafeteriaId: String)
data class Sesion(val token: String, val expiraEn: Int, val usuario: Usuario)
data class Escuela(val id: String, val clave: String, val nombre: String)
data class Cafeteria(val id: String, val nombre: String)
data class Inventario(val stock: Int, val requiereReposicion: Boolean)
data class Producto(
    val id: String,
    val nombre: String,
    val disponible: Boolean,
    val precio: Dinero,
    val inventario: Inventario,
    val descripcion: String,
    val categoria: String,
    val imagenUrl: String?,      // URL de Cloudinary; null si el producto no tiene imagen
    val destacado: Boolean,
    val etiqueta: String
)
data class ItemPedido(val nombreProducto: String, val cantidad: Int, val precioUnitario: Dinero? = null)
data class Pedido(val id: String, val estado: String, val total: Dinero? = null, val items: List<ItemPedido> = emptyList())

/** El servidor respondio con un error de negocio (ej. "Stock insuficiente", "Credenciales invalidas"). */
class ApiException(mensaje: String, val codigo: String? = null) : Exception(mensaje)

/** No hubo respuesta: sin internet, servidor dormido que no alcanzo a despertar, timeout... */
class SinConexionException :
    Exception("No se pudo conectar con el servidor. Revisa tu internet e intenta de nuevo.")

/** El token expiro o la sesion se cerro en Redis: hay que volver al login. */
class SesionExpiradaException : Exception("Tu sesion expiro. Inicia sesion de nuevo.")

// ---------------------------------------------------------------------------
// Retrofit: GraphQL es un unico POST a la raiz con { query, variables }
// ---------------------------------------------------------------------------
internal data class GraphQLRequest(val query: String, val variables: Map<String, Any?>? = null)

internal interface GraphQLService {
    @POST("/")
    suspend fun ejecutar(@Body cuerpo: GraphQLRequest): Response<JsonObject>
}

// ---------------------------------------------------------------------------
// Consultas (una sola linea cada una; "\$" es el simbolo $ de GraphQL en Kotlin)
// ---------------------------------------------------------------------------
private const val Q_LOGIN = "mutation(\$correo: String!, \$password: String!) { login(correo: \$correo, password: \$password) { token expiraEn usuario { id nombre correo cafeteriaId } } }"
private const val Q_LOGOUT = "mutation { logout }"
private const val Q_ME = "query { me { id nombre correo cafeteriaId } }"
private const val Q_ESCUELAS = "query { escuelas { id clave nombre } }"
private const val Q_CAFETERIAS = "query(\$escuelaId: ID!) { cafeteriasPorEscuela(escuelaId: \$escuelaId) { id nombre } }"
private const val Q_PRODUCTOS = "query(\$cafeteriaId: ID!) { productosPorCafeteria(cafeteriaId: \$cafeteriaId) { id nombre disponible precio { monto moneda } inventario { stock requiereReposicion } descripcion categoria imagenUrl destacado etiqueta } }"
private const val Q_CAFETERIA = "query(\$id: ID!) { cafeteria(id: \$id) { id nombre } }"
private const val Q_CREAR_PEDIDO = "mutation(\$usuarioId: ID!, \$cafeteriaId: ID!, \$items: [ItemPedidoInput!]!) { crearPedido(usuarioId: \$usuarioId, cafeteriaId: \$cafeteriaId, items: \$items) { id estado total { monto moneda } items { nombreProducto cantidad precioUnitario { monto moneda } } } }"
private const val Q_PEDIDOS = "query(\$usuarioId: ID!) { pedidosPorUsuario(usuarioId: \$usuarioId) { id estado total { monto moneda } items { nombreProducto cantidad } } }"

object ApiClient {

    /**
     * Token en memoria. Para que sobreviva al cerrar la app, guardalo cifrado
     * (EncryptedSharedPreferences o DataStore), no en SharedPreferences normal.
     */
    @Volatile
    var token: String? = null

    private val gson = Gson()

    // 90 s de margen: Render gratis se duerme y tarda ~1 min en despertar.
    private val http = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val t = token
            val peticion = if (t != null) {
                chain.request().newBuilder().header("Authorization", "Bearer $t").build()
            } else chain.request()
            chain.proceed(peticion)
        })
        .build()

    private val service: GraphQLService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(GraphQLService::class.java)

    // -------------------------------------------------------------------
    // Sesion
    // -------------------------------------------------------------------

    suspend fun login(correo: String, password: String): Sesion {
        // Tu backend rechaza con 401 CUALQUIER peticion con token invalido, incluso el login.
        token = null
        val sesion: Sesion = ejecutar("login", Q_LOGIN, mapOf("correo" to correo, "password" to password), sesionRequerida = false)
        token = sesion.token
        return sesion
    }

    suspend fun logout() {
        try {
            ejecutar<Boolean>("logout", Q_LOGOUT)
        } finally {
            token = null
        }
    }

    suspend fun me(): Usuario = ejecutar("me", Q_ME)

    // -------------------------------------------------------------------
    // Catalogos y productos
    // -------------------------------------------------------------------

    suspend fun escuelas(): List<Escuela> = ejecutar("escuelas", Q_ESCUELAS)

    suspend fun cafeteriasPorEscuela(escuelaId: String): List<Cafeteria> =
        ejecutar("cafeteriasPorEscuela", Q_CAFETERIAS, mapOf("escuelaId" to escuelaId))

    suspend fun cafeteria(id: String): Cafeteria =
        ejecutar("cafeteria", Q_CAFETERIA, mapOf("id" to id))

    suspend fun productos(cafeteriaId: String): List<Producto> =
        ejecutar("productosPorCafeteria", Q_PRODUCTOS, mapOf("cafeteriaId" to cafeteriaId))

    // -------------------------------------------------------------------
    // Pedidos
    // -------------------------------------------------------------------

    /** carrito: productoId -> cantidad */
    suspend fun crearPedido(usuarioId: String, cafeteriaId: String, carrito: Map<String, Int>): Pedido =
        ejecutar(
            "crearPedido", Q_CREAR_PEDIDO,
            mapOf(
                "usuarioId" to usuarioId,
                "cafeteriaId" to cafeteriaId,
                "items" to carrito.map { (id, cantidad) -> mapOf("productoId" to id, "cantidad" to cantidad) }
            )
        )

    suspend fun pedidosDelUsuario(usuarioId: String): List<Pedido> =
        ejecutar("pedidosPorUsuario", Q_PEDIDOS, mapOf("usuarioId" to usuarioId))

    // -------------------------------------------------------------------
    // Envia la consulta y traduce los fallos a excepciones claras para la UI
    // -------------------------------------------------------------------

    private suspend inline fun <reified T> ejecutar(
        campo: String,
        query: String,
        variables: Map<String, Any?> = emptyMap(),
        sesionRequerida: Boolean = true
    ): T {
        val respuesta = try {
            service.ejecutar(GraphQLRequest(query, variables))
        } catch (e: IOException) {
            throw SinConexionException()
        }

        if (respuesta.code() == 401 && sesionRequerida) {
            token = null
            throw SesionExpiradaException()
        }

        val cuerpo: JsonObject? = respuesta.body() ?: respuesta.errorBody()?.string()?.let {
            runCatching { gson.fromJson(it, JsonObject::class.java) }.getOrNull()
        }

        val errores = cuerpo?.getAsJsonArray("errors")
        if (errores != null && errores.size() > 0) {
            val primero = errores[0].asJsonObject
            val mensaje = primero.get("message")?.asString
            val codigo = primero.getAsJsonObject("extensions")?.get("code")?.asString
            throw ApiException(mensaje ?: "Error del servidor", codigo)
        }
        if (!respuesta.isSuccessful || cuerpo == null) {
            throw ApiException("El servidor respondio con error ${respuesta.code()}.")
        }

        val dato = cuerpo.getAsJsonObject("data")?.get(campo)
        if (dato == null || dato.isJsonNull) throw ApiException("El servidor devolvio una respuesta vacia.")

        return gson.fromJson(dato, object : TypeToken<T>() {}.type)
    }
}