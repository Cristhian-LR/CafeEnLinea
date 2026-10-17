package com.example.cafeenlinea.ui.home.network

import com.example.cafeenlinea.ui.home.model.CafeteriaHomeResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

/**
 * Endpoint del home de la cafetería.
 *
 * Base URL: https://androidbasics-auth-api.onrender.com
 *
 * Mismo estilo que
 * [com.jorgeromo.androidbasicsclass.ui.firstpartialpdm1.pickarooHome.network.PickarooService].
 */
interface CafeteriaService {

    /**
     * `GET /api/home/cafeteria/` — público, sin token.
     *
     * @return [CafeteriaHomeResponse] con `shop`, `featured`, `sections`, `combos`,
     *   `combosNote` y `extras` (HTTP 200).
     */
    @GET("api/home/cafeteria/")
    suspend fun getCafeteriaHome(): CafeteriaHomeResponse
}

/**
 * Cliente Retrofit para [CafeteriaService]. Timeouts de 60 s por el cold-start de
 * Render (free tier). Mismo criterio que `PickarooRetrofitClient`.
 */
object CafeteriaRetrofitClient {

    private const val BASE_URL = "https://androidbasics-auth-api.onrender.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val cafeteriaService: CafeteriaService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CafeteriaService::class.java)
}