package com.example.cafeenlinea.data.remote

/** Traduce un fallo de [ApiClient] a un mensaje para el usuario. [accion] completa "No se pudo ...". */
fun mensajeDeError(e: Exception, accion: String): String = when (e) {
    is SinConexionException ->
        "No se pudo conectar. Si el servidor estaba iniciando, vuelve a intentar."
    is SesionExpiradaException ->
        "Tu sesión expiró. Cierra sesión desde Perfil e inicia de nuevo."
    is ApiException -> e.message ?: "Error del servidor"
    else -> "No se pudo $accion. Intenta de nuevo."
}
