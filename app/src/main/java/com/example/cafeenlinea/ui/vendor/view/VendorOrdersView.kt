package com.example.cafeenlinea.ui.vendor.view

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.ui.vendor.model.ColumnaPedido
import com.example.cafeenlinea.ui.vendor.model.FlujoPedido
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorOrdersViewModel

/**
 * Pestaña principal del vendedor: los pedidos que llegan, en columnas
 * Nuevos / Preparando / Listos. Cada tarjeta avanza su pedido un paso.
 * La lista se actualiza sola desde [com.example.cafeenlinea.ui.vendor.VendorMainScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorOrdersView(viewModel: VendorOrdersViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var rechazando by remember { mutableStateOf<Pedido?>(null) }

    val mensaje = uiState.mensaje?.let { stringResource(it) }
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.clearMensaje()
        }
    }
    // Errores de una acción (la lista ya está en pantalla): snackbar
    LaunchedEffect(uiState.error, uiState.loaded) {
        val error = uiState.error
        if (error != null && uiState.loaded) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.vendor_tab_orders)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = uiState.columna.ordinal) {
                ColumnaPedido.entries.forEach { columna ->
                    val cuantos = uiState.cuantos(columna)
                    Tab(
                        selected = uiState.columna == columna,
                        onClick = { viewModel.selectColumna(columna) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(columna.titulo))
                                if (cuantos > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(
                                        containerColor = if (columna == ColumnaPedido.NUEVOS)
                                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    ) { Text(cuantos.toString()) }
                                }
                            }
                        }
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
                    val visibles = uiState.visibles
                    if (visibles.isEmpty()) {
                        VendorMessage(
                            icon = when (uiState.columna) {
                                ColumnaPedido.NUEVOS -> Icons.Filled.NotificationsActive
                                ColumnaPedido.PREPARANDO -> Icons.Filled.Restaurant
                                ColumnaPedido.LISTOS -> Icons.Filled.CheckCircle
                            },
                            title = stringResource(
                                when (uiState.columna) {
                                    ColumnaPedido.NUEVOS -> R.string.vendor_orders_empty_new
                                    ColumnaPedido.PREPARANDO -> R.string.vendor_orders_empty_preparing
                                    ColumnaPedido.LISTOS -> R.string.vendor_orders_empty_ready
                                }
                            ),
                            body = stringResource(R.string.vendor_orders_empty_hint)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(visibles, key = { it.id }) { pedido ->
                                OrderCard(
                                    pedido = pedido,
                                    enProceso = pedido.id in uiState.enProceso,
                                    onAdvance = { viewModel.avanzar(pedido) },
                                    onReject = { rechazando = pedido }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    rechazando?.let { pedido ->
        AlertDialog(
            onDismissRequest = { rechazando = null },
            title = { Text(stringResource(R.string.vendor_reject_title, numeroPedido(pedido.id))) },
            text = { Text(stringResource(R.string.vendor_reject_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rechazar(pedido)
                    rechazando = null
                }) { Text(stringResource(R.string.vendor_action_reject), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { rechazando = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    uiState.entregando?.let { pedido ->
        val enProceso = pedido.id in uiState.enProceso
        val codigoInvalido = stringResource(R.string.vendor_deliver_code_invalid)
        AlertDialog(
            onDismissRequest = { if (!enProceso) viewModel.cancelarEntrega() },
            title = { Text(stringResource(R.string.vendor_deliver_title, numeroPedido(pedido.id))) },
            text = {
                Column {
                    Text(stringResource(R.string.vendor_deliver_body))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.codigo,
                        onValueChange = viewModel::onCodigoChanged,
                        label = { Text(stringResource(R.string.vendor_deliver_code_label)) },
                        singleLine = true,
                        enabled = !enProceso,
                        isError = uiState.codigoError != null,
                        supportingText = uiState.codigoError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        textStyle = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmarEntrega(codigoInvalido) },
                    enabled = !enProceso && uiState.codigo.length == 4
                ) {
                    if (enProceso) CafeLoading(size = 24.dp)
                    else Text(stringResource(R.string.vendor_action_deliver))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarEntrega, enabled = !enProceso) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun OrderCard(
    pedido: Pedido,
    enProceso: Boolean,
    onAdvance: () -> Unit,
    onReject: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    numeroPedido(pedido.id),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                FlujoPedido.etiqueta(pedido.estado)?.let { etiqueta ->
                    Surface(shape = RoundedCornerShape(50), color = colors.secondaryContainer) {
                        Text(
                            stringResource(etiqueta),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            horaCorta(pedido.horaRecogida)?.let { hora ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.vendor_order_pickup, hora),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = colors.outlineVariant)
            Spacer(Modifier.height(10.dp))

            pedido.items.forEach { item ->
                Text(
                    "${item.cantidad} × ${item.nombreProducto}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface
                )
            }

            pedido.notas?.takeIf { it.isNotBlank() }?.let { notas ->
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = colors.tertiaryContainer) {
                    Text(
                        stringResource(R.string.vendor_order_notes, notas),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            pedido.total?.let { total ->
                Spacer(Modifier.height(8.dp))
                Text(
                    total.texto(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (FlujoPedido.sePuedeRechazar(pedido.estado)) {
                    TextButton(onClick = onReject, enabled = !enProceso) {
                        Text(stringResource(R.string.vendor_action_reject), color = colors.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                FlujoPedido.accion(pedido.estado)?.let { accion ->
                    Button(onClick = onAdvance, enabled = !enProceso, shape = RoundedCornerShape(10.dp)) {
                        if (enProceso) CafeLoading(size = 24.dp)
                        else Text(stringResource(accion))
                    }
                }
            }
        }
    }
}
