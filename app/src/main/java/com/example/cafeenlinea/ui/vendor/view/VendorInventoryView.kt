package com.example.cafeenlinea.ui.vendor.view

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorInventoryViewModel

/** Inventario: arriba lo que llegó a su punto de reorden, abajo el resto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorInventoryView(viewModel: VendorInventoryViewModel) {
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.vendor_tab_inventory)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading && !uiState.loaded -> VendorLoading()
                uiState.error != null && !uiState.loaded -> VendorError(uiState.error!!, viewModel::load)
                uiState.productos.isEmpty() -> VendorMessage(
                    icon = Icons.Filled.Inventory2,
                    title = stringResource(R.string.vendor_menu_empty),
                    body = stringResource(R.string.vendor_menu_empty_hint)
                )
                else -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
                        item {
                            SectionTitle(
                                stringResource(R.string.vendor_inventory_low),
                                Modifier.padding(bottom = 8.dp)
                            )
                        }
                        if (uiState.porReponer.isEmpty()) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.vendor_inventory_all_good),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                        items(uiState.porReponer, key = { "bajo_" + it.id }) { producto ->
                            StockRow(producto, bajo = true, enProceso = producto.id in uiState.enProceso) {
                                viewModel.abrirReponer(producto)
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        if (uiState.conStock.isNotEmpty()) {
                            item {
                                SectionTitle(
                                    stringResource(R.string.vendor_inventory_ok),
                                    Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(uiState.conStock, key = { "ok_" + it.id }) { producto ->
                                StockRow(producto, bajo = false, enProceso = producto.id in uiState.enProceso) {
                                    viewModel.abrirReponer(producto)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.reponiendo?.let { producto ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarReponer,
            title = { Text(stringResource(R.string.vendor_restock_title, producto.nombre)) },
            text = {
                Column {
                    Text(stringResource(R.string.vendor_restock_current, producto.inventario.stock))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.cantidad,
                        onValueChange = viewModel::onCantidadChanged,
                        label = { Text(stringResource(R.string.vendor_restock_quantity)) },
                        singleLine = true,
                        isError = uiState.cantidadError != null,
                        supportingText = uiState.cantidadError?.let { { Text(stringResource(it)) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarReponer) {
                    Text(stringResource(R.string.vendor_inventory_restock))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cerrarReponer) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun StockRow(producto: Producto, bajo: Boolean, enProceso: Boolean, onRestock: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (bajo) colors.errorContainer else colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductThumb(producto.imagenUrl, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    producto.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (bajo) colors.onErrorContainer else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(
                        R.string.vendor_inventory_stock,
                        producto.inventario.stock,
                        producto.inventario.puntoReorden
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bajo) colors.onErrorContainer else colors.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onRestock, enabled = !enProceso) {
                if (enProceso) CafeLoading(size = 24.dp)
                else Text(stringResource(R.string.vendor_inventory_restock))
            }
        }
    }
}
