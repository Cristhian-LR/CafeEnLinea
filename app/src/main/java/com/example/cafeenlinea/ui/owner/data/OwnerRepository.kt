package com.example.cafeenlinea.ui.owner.data

import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.CafeteriaNegocio
import com.example.cafeenlinea.data.remote.Escuela
import com.example.cafeenlinea.data.remote.Negocio
import com.example.cafeenlinea.data.remote.Usuario

/** Lo que necesita la pantalla del dueño del servidor (interfaz para poder probarla). */
interface OwnerRepository {
    suspend fun miNegocio(): Negocio
    suspend fun vendedores(): List<Usuario>
    suspend fun registrarVendedor(cafeteriaId: String, nombre: String, correo: String, password: String): Usuario
    suspend fun registrarCafeteria(
        nombre: String,
        ubicacion: String,
        escuelaId: String?,
        nombreEscuela: String,
        ciudadEscuela: String?
    ): CafeteriaNegocio
    suspend fun escuelas(): List<Escuela>
}

object ApiOwnerRepository : OwnerRepository {
    override suspend fun miNegocio() = ApiClient.miNegocio()
    override suspend fun vendedores() = ApiClient.misVendedores()

    override suspend fun registrarVendedor(cafeteriaId: String, nombre: String, correo: String, password: String) =
        ApiClient.registrarVendedor(cafeteriaId, nombre, correo, password)

    override suspend fun registrarCafeteria(
        nombre: String,
        ubicacion: String,
        escuelaId: String?,
        nombreEscuela: String,
        ciudadEscuela: String?
    ) = ApiClient.registrarCafeteria(nombre, ubicacion, escuelaId, nombreEscuela, ciudadEscuela)

    override suspend fun escuelas() = ApiClient.escuelas()
}
