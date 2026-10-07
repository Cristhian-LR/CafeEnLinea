package com.example.cafeenlinea.ui.profile.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Sube una imagen a Cloudinary usando un "unsigned upload preset" (no requiere
 * exponer el API secret en la app). Devuelve la URL pública (secure_url) de
 * la imagen ya subida, o null si falló.
 */
object CloudinaryUploader {

    private const val CLOUD_NAME = "dwwbf9a3b"
    private const val UPLOAD_PRESET = "fmssbqxt"
    private val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * @param context para resolver el Uri a un archivo local temporal.
     * @param imageUri la foto a subir (ya sea de galería o de la cámara).
     * @return la URL pública de Cloudinary, o null si la subida falló.
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // Copiamos el Uri a un archivo temporal, porque OkHttp necesita un File real.
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart(
                    "file",
                    tempFile.name,
                    tempFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                tempFile.delete()
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null
                JSONObject(bodyString).optString("secure_url").ifBlank { null }
            }
        } catch (e: Exception) {
            null
        }
    }
}