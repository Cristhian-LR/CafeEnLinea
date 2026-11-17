package com.example.cafeenlinea.ui.profile.data

import android.content.Context
import android.net.Uri
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.FirmaCloudinary
import com.example.cafeenlinea.data.remote.TipoRecursoImagen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gestiona la subida de fotografías de perfil a Cloudinary utilizando el sistema
 * de firmas seguras generado por el backend cafe_online.
 *
 * El API Secret permanece exclusivamente en el servidor. Android solicita primero
 * la firma mediante la mutación GraphQL `solicitarFirmaSubida(tipo: PERFIL)` y
 * posteriormente sube el archivo de forma directa a Cloudinary.
 */
object CloudinaryUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Sube un archivo de imagen directamente a Cloudinary utilizando los parámetros
     * y firma digital autorizados por el backend.
     *
     * @param context Contexto para resolver el [Uri].
     * @param imageUri Uri de la fotografía a subir.
     * @param firma Datos de autorización generados por el backend.
     * @return La URL pública segura (secure_url) devuelta por Cloudinary.
     * @throws IOException si falla la lectura local o Cloudinary rechaza la subida.
     */
    suspend fun uploadImageSigned(
        context: Context,
        imageUri: Uri,
        firma: FirmaCloudinary
    ): String = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
        val extension = when {
            mimeType.contains("png", ignoreCase = true) -> ".png"
            mimeType.contains("webp", ignoreCase = true) -> ".webp"
            else -> ".jpg"
        }

        val tempFile = File.createTempFile("profile_upload_", extension, context.cacheDir)
        try {
            contentResolver.openInputStream(imageUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("No se pudo leer la imagen seleccionada.")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", firma.apiKey)
                .addFormDataPart("timestamp", firma.timestamp.toString())
                .addFormDataPart("signature", firma.signature)
                .addFormDataPart("folder", firma.folder)
                .addFormDataPart("upload_preset", firma.uploadPreset)
                .addFormDataPart(
                    "file",
                    tempFile.name,
                    tempFile.asRequestBody(mimeType.toMediaType())
                )
                .build()

            val uploadUrl = "https://api.cloudinary.com/v1_1/${firma.cloudName}/image/upload"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString.isNullOrBlank()) {
                    val errorMessage = bodyString?.let {
                        runCatching { JSONObject(it).optJSONObject("error")?.optString("message") }.getOrNull()
                    } ?: "Error al subir imagen a Cloudinary (HTTP ${response.code})"
                    throw IOException(errorMessage)
                }

                val json = JSONObject(bodyString)
                val secureUrl = json.optString("secure_url")
                if (secureUrl.isNullOrBlank()) {
                    throw IOException("Cloudinary no devolvió una URL válida para la imagen.")
                }
                secureUrl
            }
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Solicita la firma de perfil al backend y sube la fotografía a la carpeta perfiles/<usuarioId>.
     */
    suspend fun uploadProfileImage(
        context: Context,
        imageUri: Uri
    ): String {
        val firma = ApiClient.solicitarFirmaSubida(
            tipo = TipoRecursoImagen.PERFIL
        )
        return uploadImageSigned(context, imageUri, firma)
    }
}
