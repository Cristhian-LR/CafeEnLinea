package com.example.cafeenlinea

import com.example.cafeenlinea.data.remote.FirmaCloudinary
import com.example.cafeenlinea.data.remote.TipoRecursoImagen
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProfileImageSignatureTest {

    private val gson = Gson()

    @Test
    fun `deserializa FirmaCloudinary para foto de perfil`() {
        val json = """
            {
                "signature": "sig_perfil_12345",
                "timestamp": 1731690000,
                "apiKey": "key_cloud_777",
                "cloudName": "cafe_cloud",
                "folder": "perfiles/usr_abc123",
                "uploadPreset": "cafe_signed_preset"
            }
        """.trimIndent()

        val firma = gson.fromJson(json, FirmaCloudinary::class.java)

        assertNotNull(firma)
        assertEquals("sig_perfil_12345", firma.signature)
        assertEquals(1731690000, firma.timestamp)
        assertEquals("key_cloud_777", firma.apiKey)
        assertEquals("cafe_cloud", firma.cloudName)
        assertEquals("perfiles/usr_abc123", firma.folder)
        assertEquals("cafe_signed_preset", firma.uploadPreset)
    }

    @Test
    fun `tipo recurso imagen PERFIL coincide con SDL GraphQL`() {
        assertEquals("PERFIL", TipoRecursoImagen.PERFIL.name)
    }

    @Test
    fun `parsea respuesta data de solicitarFirmaSubida para perfil`() {
        val responseJson = """
            {
                "data": {
                    "solicitarFirmaSubida": {
                        "signature": "firmada_123",
                        "timestamp": 1731691111,
                        "apiKey": "api_key_888",
                        "cloudName": "mi_cloud",
                        "folder": "perfiles/66000000aaaaaaaaaaaaaaaa",
                        "uploadPreset": "cafe_signed_preset"
                    }
                }
            }
        """.trimIndent()

        val jsonObject = gson.fromJson(responseJson, JsonObject::class.java)
        val dataObject = jsonObject.getAsJsonObject("data")
        val firmaJson = dataObject.get("solicitarFirmaSubida")

        val firma = gson.fromJson(firmaJson, FirmaCloudinary::class.java)
        assertEquals("firmada_123", firma.signature)
        assertEquals(1731691111, firma.timestamp)
        assertEquals("api_key_888", firma.apiKey)
        assertEquals("mi_cloud", firma.cloudName)
        assertEquals("perfiles/66000000aaaaaaaaaaaaaaaa", firma.folder)
        assertEquals("cafe_signed_preset", firma.uploadPreset)
    }
}
