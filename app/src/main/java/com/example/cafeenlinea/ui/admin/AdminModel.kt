package com.example.cafeenlinea.ui.admin

import androidx.annotation.StringRes
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.data.remote.Negocio
import com.example.cafeenlinea.ui.owner.model.EstadoNegocio

/** Lo que necesita la pantalla del administrador del servidor (interfaz para poder probarla). */
interface AdminRepository {
    suspend fun negocios(estado: String): List<Negocio>
    suspend fun aprobar(negocioId: String): Negocio
    suspend fun rechazar(negocioId: String, motivo: String): Negocio
}

object ApiAdminRepository : AdminRepository {
    override suspend fun negocios(estado: String) = ApiClient.negociosPorEstado(estado)
    override suspend fun aprobar(negocioId: String) = ApiClient.aprobarNegocio(negocioId)
    override suspend fun rechazar(negocioId: String, motivo: String) = ApiClient.rechazarNegocio(negocioId, motivo)
}

/** Pestañas de la pantalla: una por estado del negocio. */
enum class FiltroNegocios(val estado: String, @StringRes val titulo: Int) {
    EN_REVISION(EstadoNegocio.EN_REVISION, R.string.admin_tab_pending),
    APROBADOS(EstadoNegocio.APROBADO, R.string.admin_tab_approved),
    RECHAZADOS(EstadoNegocio.RECHAZADO, R.string.admin_tab_rejected)
}

/** Mismo máximo que valida el servidor. */
const val MOTIVO_RECHAZO_MAX = 300

data class AdminUiState(
    val filtro: FiltroNegocios = FiltroNegocios.EN_REVISION,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val negocios: List<Negocio> = emptyList(),
    /** Negocios con una aprobación o rechazo en camino. */
    val enProceso: Set<String> = emptySet(),
    /** Negocio al que se le está escribiendo el motivo del rechazo. */
    val rechazando: Negocio? = null,
    val motivo: String = "",
    @StringRes val motivoError: Int? = null,
    @StringRes val mensaje: Int? = null
) {
    /** Se puede aprobar lo que está en revisión o se rechazó antes. */
    val puedeAprobar: Boolean get() = filtro != FiltroNegocios.APROBADOS

    /** Se puede rechazar lo que está en revisión o dar de baja lo aprobado. */
    val puedeRechazar: Boolean get() = filtro != FiltroNegocios.RECHAZADOS
}
