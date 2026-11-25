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
data class Usuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val escuelaId: String? = null,
    val cafeteriaId: String?,
    val fotoPerfilUrl: String? = null,
    /** CLIENTE, VENDEDOR, JEFE, ADMIN_ESCUELA o ADMIN_PLATAFORMA. */
    val rol: String? = null
)
data class Sesion(val token: String, val expiraEn: Int, val usuario: Usuario)
data class Escuela(val id: String, val clave: String, val nombre: String, val ciudad: String = "")
data class Cafeteria(val id: String, val nombre: String)
data class Inventario(
    val stock: Int,
    val requiereReposicion: Boolean,
    /** Con este stock o menos, el producto aparece en "por reponer". */
    val puntoReorden: Int = 0,
    val cantidadReposicion: Int = 0
)
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
    val etiqueta: String,
    val tiempoPreparacionMin: Int = 5
)
data class ItemPedido(val nombreProducto: String, val cantidad: Int, val precioUnitario: Dinero? = null)
data class Pedido(
    val id: String,
    val estado: String,
    val total: Dinero? = null,
    val items: List<ItemPedido> = emptyList(),
    // Solo los piden las consultas del vendedor; en las demás llegan null
    val notas: String? = null,
    val horaRecogida: String? = null,
    val tiempoEstimadoMin: Int? = null,
    val creadoEn: String? = null,
    val historial: List<CambioEstado>? = null
)
/** Un cambio de estado del pedido, con su fecha ISO. */
data class CambioEstado(val estado: String, val fecha: String)

/** Cafetería vista por su dueño o por el administrador (incluye las que aún no ven los alumnos). */
data class CafeteriaNegocio(
    val id: String,
    val nombre: String,
    val ubicacion: String? = null,
    val activa: Boolean = false,
    val escuela: Escuela? = null
)

/**
 * Negocio de un dueño (JEFE). [estado]: EN_REVISION, APROBADO o RECHAZADO.
 * Mientras no esté aprobado, sus cafeterías no aparecen a los alumnos.
 */
data class Negocio(
    val id: String,
    val nombre: String,
    val estado: String,
    val motivoRechazo: String? = null,
    val creadoEn: String? = null,
    val dueno: Usuario? = null,
    val cafeterias: List<CafeteriaNegocio>? = null
)
data class RegistroNegocioPayload(val negocio: Negocio, val sesion: Sesion)

/** Datos del registro de una cafetería por su dueño. La escuela va por id o por nombre. */
data class RegistroCafeteriaInput(
    val nombreDueno: String,
    val correo: String,
    val password: String,
    val nombreNegocio: String,
    val nombreCafeteria: String,
    val ubicacion: String,
    val escuelaId: String?,
    val nombreEscuela: String,
    val ciudadEscuela: String?
)

/** Datos para crear o editar un producto desde la pantalla del vendedor. */
data class ProductoInput(
    val nombre: String,
    val precio: Double,
    val moneda: String,
    val categoria: String,
    val descripcion: String,
    val tiempoPreparacionMin: Int,
    val destacado: Boolean,
    val etiqueta: String,
    val imagenUrl: String?,
    /** Solo al crear: el servidor exige inventario inicial. */
    val stockInicial: Int,
    val puntoReorden: Int
)

enum class TipoRecursoImagen {
    PRODUCTO,
    PERFIL
}

data class FirmaCloudinary(
    val signature: String,
    val timestamp: Int,
    val apiKey: String,
    val cloudName: String,
    val folder: String,
    val uploadPreset: String
)

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
private const val Q_LOGIN = "mutation(\$correo: String!, \$password: String!) { login(correo: \$correo, password: \$password) { token expiraEn usuario { id nombre correo rol escuelaId cafeteriaId } } }"
private const val Q_REGISTRO = "mutation(\$nombre: String!, \$correo: String!, \$password: String!, \$escuelaId: ID!) { registro(nombre: \$nombre, correo: \$correo, password: \$password, escuelaId: \$escuelaId) { token expiraEn usuario { id nombre correo rol escuelaId cafeteriaId } } }"
private const val Q_REGISTRO_ESCUELA_NUEVA = "mutation(\$nombre: String!, \$correo: String!, \$password: String!, \$nombreEscuela: String!, \$ciudadEscuela: String) { registro(nombre: \$nombre, correo: \$correo, password: \$password, nombreEscuela: \$nombreEscuela, ciudadEscuela: \$ciudadEscuela) { token expiraEn usuario { id nombre correo rol escuelaId cafeteriaId } } }"
private const val Q_LOGOUT = "mutation { logout }"
private const val Q_ME = "query { me { id nombre correo rol escuelaId cafeteriaId fotoPerfilUrl } }"
private const val Q_ESCUELAS = "query { escuelas { id clave nombre ciudad } }"
private const val Q_CAFETERIAS = "query(\$escuelaId: ID!) { cafeteriasPorEscuela(escuelaId: \$escuelaId) { id nombre } }"
private const val Q_PRODUCTOS = "query(\$cafeteriaId: ID!) { productosPorCafeteria(cafeteriaId: \$cafeteriaId) { id nombre disponible precio { monto moneda } inventario { stock requiereReposicion } descripcion categoria imagenUrl destacado etiqueta } }"
private const val Q_CAFETERIA = "query(\$id: ID!) { cafeteria(id: \$id) { id nombre } }"
private const val Q_CREAR_PEDIDO = "mutation(\$usuarioId: ID!, \$cafeteriaId: ID!, \$items: [ItemPedidoInput!]!) { crearPedido(usuarioId: \$usuarioId, cafeteriaId: \$cafeteriaId, items: \$items) { id estado total { monto moneda } items { nombreProducto cantidad precioUnitario { monto moneda } } } }"
private const val Q_PEDIDOS = "query(\$usuarioId: ID!) { pedidosPorUsuario(usuarioId: \$usuarioId) { id estado total { monto moneda } items { nombreProducto cantidad } } }"
private const val M_SOLICITAR_FIRMA = "mutation(\$tipo: TipoRecursoImagen!, \$cafeteriaId: ID) { solicitarFirmaSubida(tipo: \$tipo, cafeteriaId: \$cafeteriaId) { signature timestamp apiKey cloudName folder uploadPreset } }"
private const val M_ACTUALIZAR_FOTO_PERFIL = "mutation(\$fotoUrl: String!) { actualizarFotoPerfil(fotoUrl: \$fotoUrl) { id nombre correo cafeteriaId fotoPerfilUrl } }"

// ---- Vendedor ----
private const val CAMPOS_PEDIDO_VENDEDOR = "id estado total { monto moneda } items { nombreProducto cantidad } notas horaRecogida tiempoEstimadoMin"
private const val CAMPOS_PRODUCTO_VENDEDOR = "id nombre disponible precio { monto moneda } inventario { stock requiereReposicion puntoReorden cantidadReposicion } descripcion categoria imagenUrl destacado etiqueta tiempoPreparacionMin"
private const val Q_COLA = "query(\$cafeteriaId: ID!) { colaCafeteria(cafeteriaId: \$cafeteriaId) { $CAMPOS_PEDIDO_VENDEDOR } }"
private const val M_ESTADO_PEDIDO = "mutation(\$pedidoId: ID!, \$nuevoEstado: String!, \$codigoRecogida: String) { actualizarEstadoPedido(pedidoId: \$pedidoId, nuevoEstado: \$nuevoEstado, codigoRecogida: \$codigoRecogida) { $CAMPOS_PEDIDO_VENDEDOR } }"
private const val Q_PRODUCTOS_VENDEDOR = "query(\$cafeteriaId: ID!) { productosPorCafeteria(cafeteriaId: \$cafeteriaId) { $CAMPOS_PRODUCTO_VENDEDOR } }"
private const val M_REGISTRAR_PRODUCTO = "mutation(\$nombre: String!, \$precio: DineroInput!, \$inventario: InventarioInput!, \$cafeteriaId: ID!, \$descripcion: String, \$categoria: String, \$imagenUrl: String, \$destacado: Boolean, \$etiqueta: String, \$tiempoPreparacionMin: Int) { registrarProducto(nombre: \$nombre, precio: \$precio, inventario: \$inventario, cafeteriaId: \$cafeteriaId, descripcion: \$descripcion, categoria: \$categoria, imagenUrl: \$imagenUrl, destacado: \$destacado, etiqueta: \$etiqueta, tiempoPreparacionMin: \$tiempoPreparacionMin) { $CAMPOS_PRODUCTO_VENDEDOR } }"
private const val M_ACTUALIZAR_PRODUCTO = "mutation(\$productoId: ID!, \$cambios: ProductoCambiosInput!) { actualizarProducto(productoId: \$productoId, cambios: \$cambios) { $CAMPOS_PRODUCTO_VENDEDOR } }"
private const val M_DISPONIBILIDAD = "mutation(\$productoId: ID!, \$disponible: Boolean!) { cambiarDisponibilidadProducto(productoId: \$productoId, disponible: \$disponible) { $CAMPOS_PRODUCTO_VENDEDOR } }"
private const val Q_POR_REPONER = "query(\$cafeteriaId: ID!) { productosPorReponer(cafeteriaId: \$cafeteriaId) { $CAMPOS_PRODUCTO_VENDEDOR } }"
private const val M_REPONER = "mutation(\$productoId: ID!, \$cantidad: Int) { reponerInventario(productoId: \$productoId, cantidad: \$cantidad) { $CAMPOS_PRODUCTO_VENDEDOR } }"
// ---- Negocios (dueño y administrador de la plataforma) ----
private const val CAMPOS_SESION = "token expiraEn usuario { id nombre correo rol escuelaId cafeteriaId }"
private const val CAMPOS_NEGOCIO = "id nombre estado motivoRechazo creadoEn dueno { id nombre correo cafeteriaId } cafeterias { id nombre ubicacion activa escuela { id clave nombre ciudad } }"
private const val CAMPOS_CAFETERIA_NEGOCIO = "id nombre ubicacion activa escuela { id clave nombre ciudad }"
private const val M_REGISTRAR_MI_CAFETERIA = "mutation(\$input: RegistroCafeteriaInput!) { registrarMiCafeteria(input: \$input) { negocio { $CAMPOS_NEGOCIO } sesion { $CAMPOS_SESION } } }"
private const val Q_MI_NEGOCIO = "query { miNegocio { $CAMPOS_NEGOCIO } }"
private const val Q_MIS_VENDEDORES = "query { misVendedores { id nombre correo cafeteriaId } }"
private const val M_REGISTRAR_VENDEDOR = "mutation(\$cafeteriaId: ID!, \$cuenta: CuentaInput!) { registrarVendedor(cafeteriaId: \$cafeteriaId, cuenta: \$cuenta) { id nombre correo cafeteriaId } }"
private const val M_REGISTRAR_CAFETERIA = "mutation(\$nombre: String!, \$ubicacion: String, \$escuelaId: ID, \$nombreEscuela: String, \$ciudadEscuela: String) { registrarCafeteria(nombre: \$nombre, ubicacion: \$ubicacion, escuelaId: \$escuelaId, nombreEscuela: \$nombreEscuela, ciudadEscuela: \$ciudadEscuela) { $CAMPOS_CAFETERIA_NEGOCIO } }"
private const val Q_NEGOCIOS_POR_ESTADO = "query(\$estado: EstadoNegocio!) { negociosPorEstado(estado: \$estado) { $CAMPOS_NEGOCIO } }"
private const val M_APROBAR_NEGOCIO = "mutation(\$negocioId: ID!) { aprobarNegocio(negocioId: \$negocioId) { $CAMPOS_NEGOCIO } }"
private const val M_RECHAZAR_NEGOCIO = "mutation(\$negocioId: ID!, \$motivo: String!) { rechazarNegocio(negocioId: \$negocioId, motivo: \$motivo) { $CAMPOS_NEGOCIO } }"

// El vendedor no tiene permiso de reportes (es del JEFE): el resumen se calcula con sus pedidos
private const val Q_PEDIDOS_CAFETERIA = "query(\$cafeteriaId: ID!) { pedidosPorCafeteria(cafeteriaId: \$cafeteriaId) { id estado creadoEn total { monto moneda } items { nombreProducto cantidad precioUnitario { monto moneda } } historial { estado fecha } } }"

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

    /**
     * Crea la cuenta y deja la sesion iniciada (el servidor responde igual que en login).
     * Con [escuelaId] usa una escuela del catalogo; sin el, el servidor busca la escuela
     * por [nombreEscuela] y, si no existe, la crea con [ciudadEscuela] y la agrega al catalogo.
     */
    suspend fun registro(
        nombre: String,
        correo: String,
        password: String,
        escuelaId: String?,
        nombreEscuela: String,
        ciudadEscuela: String?
    ): Sesion {
        token = null
        val cuenta = mapOf("nombre" to nombre, "correo" to correo, "password" to password)
        val sesion: Sesion = if (escuelaId != null) {
            ejecutar("registro", Q_REGISTRO, cuenta + ("escuelaId" to escuelaId), sesionRequerida = false)
        } else {
            ejecutar(
                "registro", Q_REGISTRO_ESCUELA_NUEVA,
                cuenta + mapOf("nombreEscuela" to nombreEscuela, "ciudadEscuela" to ciudadEscuela),
                sesionRequerida = false
            )
        }
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
    // Cloudinary e Imágenes
    // -------------------------------------------------------------------

    /**
     * Solicita una firma de subida autorizada al backend para Cloudinary.
     * Para perfil: tipo = TipoRecursoImagen.PERFIL (sube a perfiles/<usuarioId>).
     */
    suspend fun solicitarFirmaSubida(
        tipo: TipoRecursoImagen,
        cafeteriaId: String? = null
    ): FirmaCloudinary = ejecutar(
        campo = "solicitarFirmaSubida",
        query = M_SOLICITAR_FIRMA,
        variables = mapOf("tipo" to tipo.name, "cafeteriaId" to cafeteriaId)
    )

    /**
     * Persiste en el backend la URL de Cloudinary de la foto de perfil del usuario
     * de la sesion, para que se conserve entre dispositivos.
     */
    suspend fun actualizarFotoPerfil(fotoUrl: String): Usuario = ejecutar(
        campo = "actualizarFotoPerfil",
        query = M_ACTUALIZAR_FOTO_PERFIL,
        variables = mapOf("fotoUrl" to fotoUrl)
    )

    // -------------------------------------------------------------------
    // Vendedor: cola de pedidos, menu, inventario y reportes de su cafeteria
    // -------------------------------------------------------------------

    /** Pedidos activos (PENDIENTE, ACEPTADO, EN_PREPARACION, LISTO) por hora de recogida. */
    suspend fun colaCafeteria(cafeteriaId: String): List<Pedido> =
        ejecutar("colaCafeteria", Q_COLA, mapOf("cafeteriaId" to cafeteriaId))

    /** Para ENTREGADO el servidor exige el [codigoRecogida] que muestra el comprador. */
    suspend fun actualizarEstadoPedido(pedidoId: String, nuevoEstado: String, codigoRecogida: String? = null): Pedido =
        ejecutar(
            "actualizarEstadoPedido", M_ESTADO_PEDIDO,
            mapOf("pedidoId" to pedidoId, "nuevoEstado" to nuevoEstado, "codigoRecogida" to codigoRecogida)
        )

    /** Todos los productos de la cafeteria, también los pausados (a diferencia del menú del cliente). */
    suspend fun productosVendedor(cafeteriaId: String): List<Producto> =
        ejecutar("productosPorCafeteria", Q_PRODUCTOS_VENDEDOR, mapOf("cafeteriaId" to cafeteriaId))

    suspend fun registrarProducto(cafeteriaId: String, datos: ProductoInput): Producto =
        ejecutar(
            "registrarProducto", M_REGISTRAR_PRODUCTO,
            mapOf(
                "cafeteriaId" to cafeteriaId,
                "nombre" to datos.nombre,
                "precio" to mapOf("monto" to datos.precio, "moneda" to datos.moneda),
                "inventario" to mapOf(
                    "stock" to datos.stockInicial,
                    "puntoReorden" to datos.puntoReorden,
                    // Al reponer sin cantidad se suma esto; se toma el stock inicial como referencia
                    "cantidadReposicion" to datos.stockInicial.coerceAtLeast(1)
                ),
                "descripcion" to datos.descripcion,
                "categoria" to datos.categoria,
                "imagenUrl" to datos.imagenUrl,
                "destacado" to datos.destacado,
                "etiqueta" to datos.etiqueta,
                "tiempoPreparacionMin" to datos.tiempoPreparacionMin
            )
        )

    /** Edita un producto. El inventario no se cambia aquí (se usa [reponerInventario]). */
    suspend fun actualizarProducto(productoId: String, datos: ProductoInput): Producto =
        ejecutar(
            "actualizarProducto", M_ACTUALIZAR_PRODUCTO,
            mapOf(
                "productoId" to productoId,
                "cambios" to mapOf(
                    "nombre" to datos.nombre,
                    "precio" to mapOf("monto" to datos.precio, "moneda" to datos.moneda),
                    "descripcion" to datos.descripcion,
                    "categoria" to datos.categoria,
                    // "" quita la imagen; el servidor ignora null
                    "imagenUrl" to (datos.imagenUrl ?: ""),
                    "destacado" to datos.destacado,
                    "etiqueta" to datos.etiqueta,
                    "tiempoPreparacionMin" to datos.tiempoPreparacionMin
                )
            )
        )

    suspend fun cambiarDisponibilidadProducto(productoId: String, disponible: Boolean): Producto =
        ejecutar(
            "cambiarDisponibilidadProducto", M_DISPONIBILIDAD,
            mapOf("productoId" to productoId, "disponible" to disponible)
        )

    suspend fun productosPorReponer(cafeteriaId: String): List<Producto> =
        ejecutar("productosPorReponer", Q_POR_REPONER, mapOf("cafeteriaId" to cafeteriaId))

    /** Suma [cantidad] al stock; sin cantidad usa la cantidad de reposición del producto. */
    suspend fun reponerInventario(productoId: String, cantidad: Int?): Producto =
        ejecutar("reponerInventario", M_REPONER, mapOf("productoId" to productoId, "cantidad" to cantidad))

    /** Todos los pedidos de la cafetería (también entregados y cancelados), con su historial. */
    suspend fun pedidosCafeteria(cafeteriaId: String): List<Pedido> =
        ejecutar("pedidosPorCafeteria", Q_PEDIDOS_CAFETERIA, mapOf("cafeteriaId" to cafeteriaId))

    // -------------------------------------------------------------------
    // Negocios: alta de cafeterías por su dueño y aprobación de la plataforma
    // -------------------------------------------------------------------

    /** Crea el negocio (en revisión), su primera cafetería y la cuenta del dueño; deja la sesión iniciada. */
    suspend fun registrarMiCafeteria(datos: RegistroCafeteriaInput): Sesion {
        token = null
        val input = mapOf(
            "dueno" to mapOf("nombre" to datos.nombreDueno, "correo" to datos.correo, "password" to datos.password),
            "nombreNegocio" to datos.nombreNegocio,
            "nombreCafeteria" to datos.nombreCafeteria,
            "ubicacion" to datos.ubicacion,
            "escuelaId" to datos.escuelaId,
            "nombreEscuela" to datos.nombreEscuela.takeIf { datos.escuelaId == null },
            "ciudadEscuela" to datos.ciudadEscuela
        )
        val payload: RegistroNegocioPayload = ejecutar(
            "registrarMiCafeteria", M_REGISTRAR_MI_CAFETERIA, mapOf("input" to input), sesionRequerida = false
        )
        token = payload.sesion.token
        return payload.sesion
    }

    suspend fun miNegocio(): Negocio = ejecutar("miNegocio", Q_MI_NEGOCIO)

    suspend fun misVendedores(): List<Usuario> = ejecutar("misVendedores", Q_MIS_VENDEDORES)

    suspend fun registrarVendedor(cafeteriaId: String, nombre: String, correo: String, password: String): Usuario =
        ejecutar(
            "registrarVendedor", M_REGISTRAR_VENDEDOR,
            mapOf(
                "cafeteriaId" to cafeteriaId,
                "cuenta" to mapOf("nombre" to nombre, "correo" to correo, "password" to password)
            )
        )

    /** Abre otra cafetería del negocio. La escuela va por [escuelaId] o por [nombreEscuela] (se crea si es nueva). */
    suspend fun registrarCafeteria(
        nombre: String,
        ubicacion: String,
        escuelaId: String?,
        nombreEscuela: String,
        ciudadEscuela: String?
    ): CafeteriaNegocio = ejecutar(
        "registrarCafeteria", M_REGISTRAR_CAFETERIA,
        mapOf(
            "nombre" to nombre,
            "ubicacion" to ubicacion,
            "escuelaId" to escuelaId,
            "nombreEscuela" to nombreEscuela.takeIf { escuelaId == null },
            "ciudadEscuela" to ciudadEscuela
        )
    )

    suspend fun negociosPorEstado(estado: String): List<Negocio> =
        ejecutar("negociosPorEstado", Q_NEGOCIOS_POR_ESTADO, mapOf("estado" to estado))

    suspend fun aprobarNegocio(negocioId: String): Negocio =
        ejecutar("aprobarNegocio", M_APROBAR_NEGOCIO, mapOf("negocioId" to negocioId))

    suspend fun rechazarNegocio(negocioId: String, motivo: String): Negocio =
        ejecutar("rechazarNegocio", M_RECHAZAR_NEGOCIO, mapOf("negocioId" to negocioId, "motivo" to motivo))

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