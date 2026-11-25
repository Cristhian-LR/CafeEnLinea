package com.example.cafeenlinea.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.data.remote.Negocio
import com.example.cafeenlinea.ui.auth.viewmodel.LogoutViewModel
import com.example.cafeenlinea.ui.vendor.model.isoAMillis
import com.example.cafeenlinea.ui.vendor.view.VendorError
import com.example.cafeenlinea.ui.vendor.view.VendorLoading
import com.example.cafeenlinea.ui.vendor.view.VendorMessage
import java.text.DateFormat
import java.util.Date

/**
 * Pantalla del administrador de la plataforma: revisa las cafeterías que se
 * registraron y las aprueba al cerrar el trato con su dueño (o las rechaza).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onLogout: () -> Unit) {
    val viewModel: AdminViewModel = viewModel(
        factory = viewModelFactory { initializer { AdminViewModel(ApiAdminRepository) } }
    )
    val logoutViewModel: LogoutViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val mensaje = uiState.mensaje?.let { stringResource(it) }
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.clearMensaje()
        }
    }
    LaunchedEffect(uiState.error, uiState.loaded) {
        val error = uiState.error
        if (error != null && uiState.loaded) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_title)) },
                actions = {
                    IconButton(onClick = { logoutViewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, stringResource(R.string.profile_logout))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = uiState.filtro.ordinal) {
                FiltroNegocios.entries.forEach { filtro ->
                    Tab(
                        selected = uiState.filtro == filtro,
                        onClick = { viewModel.selectFiltro(filtro) },
                        text = { Text(stringResource(filtro.titulo)) }
                    )
                }
            }
            when {
                uiState.isLoading && !uiState.loaded -> VendorLoading()
                uiState.error != null && !uiState.loaded -> VendorError(uiState.error!!, viewModel::load)
                else -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.negocios.isEmpty()) {
                        VendorMessage(
                            icon = Icons.Filled.CheckCircle,
                            title = stringResource(
                                if (uiState.filtro == FiltroNegocios.EN_REVISION) R.string.admin_empty_pending
                                else R.string.admin_empty
                            )
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.negocios, key = { it.id }) { negocio ->
                                NegocioCard(
                                    negocio = negocio,
                                    enProceso = negocio.id in uiState.enProceso,
                                    puedeAprobar = uiState.puedeAprobar,
                                    puedeRechazar = uiState.puedeRechazar,
                                    onApprove = { viewModel.aprobar(negocio) },
                                    onReject = { viewModel.abrirRechazo(negocio) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.rechazando?.let { negocio ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarRechazo,
            title = {
                Text(
                    stringResource(
                        if (uiState.filtro == FiltroNegocios.APROBADOS) R.string.admin_deactivate_title
                        else R.string.admin_reject_title,
                        negocio.nombre
                    )
                )
            },
            text = {
                Column {
                    Text(stringResource(R.string.admin_reject_body))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.motivo,
                        onValueChange = viewModel::onMotivoChanged,
                        label = { Text(stringResource(R.string.admin_reject_reason)) },
                        isError = uiState.motivoError != null,
                        supportingText = {
                            Text(uiState.motivoError?.let { stringResource(it) } ?: "${uiState.motivo.length}/$MOTIVO_RECHAZO_MAX")
                        },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarRechazo) {
                    Text(stringResource(R.string.admin_reject), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cerrarRechazo) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun NegocioCard(
    negocio: Negocio,
    enProceso: Boolean,
    puedeAprobar: Boolean,
    puedeRechazar: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storefront, null, tint = colors.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(negocio.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    isoAMillis(negocio.creadoEn)?.let {
                        Text(
                            stringResource(R.string.admin_registered_on, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            negocio.dueno?.let { dueno ->
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.admin_owner, dueno.nombre), color = colors.onSurface)
                Text(dueno.correo, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }

            negocio.cafeterias.orEmpty().forEach { cafeteria ->
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = colors.outlineVariant)
                Spacer(Modifier.height(10.dp))
                Text(cafeteria.nombre, fontWeight = FontWeight.Medium, color = colors.onSurface)
                val lugar = listOfNotNull(
                    cafeteria.escuela?.let { e -> listOf(e.nombre, e.ciudad).filter { it.isNotBlank() }.joinToString(", ") },
                    cafeteria.ubicacion?.takeIf { it.isNotBlank() }
                )
                if (lugar.isNotEmpty()) {
                    Text(lugar.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }

            negocio.motivoRechazo?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.admin_reason_label, it), style = MaterialTheme.typography.bodySmall, color = colors.error)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (puedeRechazar) {
                    TextButton(onClick = onReject, enabled = !enProceso) {
                        Text(
                            stringResource(if (puedeAprobar) R.string.admin_reject else R.string.admin_deactivate),
                            color = colors.error
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (puedeAprobar) {
                    Button(onClick = onApprove, enabled = !enProceso, shape = RoundedCornerShape(10.dp)) {
                        if (enProceso) CafeLoading(size = 24.dp)
                        else {
                            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.admin_approve))
                        }
                    }
                }
            }
        }
    }
}
