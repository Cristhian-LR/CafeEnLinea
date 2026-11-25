package com.example.cafeenlinea.ui.vendor.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.Producto
import com.example.cafeenlinea.ui.vendor.model.VendorMenuUiState
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorMenuViewModel

/**
 * Menú del vendedor: productos por categoría con interruptor de disponible/agotado
 * (lo más usado en apps como Rappi Aliados). Tocar un producto lo edita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorMenuView(viewModel: VendorMenuViewModel) {
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.vendor_tab_menu)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.loaded) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::nuevoProducto,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.vendor_menu_add)) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading && !uiState.loaded -> VendorLoading()
                uiState.error != null && !uiState.loaded -> VendorError(uiState.error!!, viewModel::load)
                uiState.productos.isEmpty() -> VendorMessage(
                    icon = Icons.Filled.Fastfood,
                    title = stringResource(R.string.vendor_menu_empty),
                    body = stringResource(R.string.vendor_menu_empty_hint),
                    actionText = stringResource(R.string.vendor_menu_add),
                    onAction = viewModel::nuevoProducto
                )
                else -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    MenuList(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }

    uiState.formulario?.let { form ->
        ProductFormSheet(
            form = form,
            categorias = uiState.nombresCategorias,
            onChange = viewModel::actualizarFormulario,
            onSave = viewModel::guardar,
            onDismiss = viewModel::cerrarFormulario
        )
    }
}

@Composable
private fun MenuList(uiState: VendorMenuUiState, viewModel: VendorMenuViewModel) {
    val categorias = uiState.categorias
    LazyColumn(
        // Espacio abajo para que el botón flotante no tape el último producto
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            OutlinedTextField(
                value = uiState.busqueda,
                onValueChange = viewModel::onBusquedaChanged,
                placeholder = { Text(stringResource(R.string.vendor_menu_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }

        if (categorias.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(320.dp)) {
                    VendorMessage(
                        icon = Icons.Filled.SearchOff,
                        title = stringResource(R.string.vendor_menu_no_results, uiState.busqueda.trim())
                    )
                }
            }
        }

        categorias.forEach { grupo ->
            item(key = "cat_" + grupo.categoria) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        grupo.categoria,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        pluralStringResource(R.plurals.vendor_menu_products_count, grupo.productos.size, grupo.productos.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(grupo.productos, key = { it.id }) { producto ->
                ProductRow(
                    producto = producto,
                    enProceso = producto.id in uiState.enProceso,
                    onToggle = { viewModel.toggleDisponible(producto) },
                    onClick = { viewModel.editar(producto) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProductRow(
    producto: Producto,
    enProceso: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val editar = stringResource(R.string.cd_edit_product, producto.nombre)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = editar }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Atenuado si está agotado, como lo ve el cliente
            val alpha = if (producto.disponible) 1f else 0.5f
            ProductThumb(producto.imagenUrl, Modifier.size(56.dp).alpha(alpha))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).alpha(alpha)) {
                Text(
                    producto.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    producto.precio.texto(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.vendor_menu_stock, producto.inventario.stock),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (producto.inventario.requiereReposicion) colors.error else colors.onSurfaceVariant
                    )
                    if (producto.destacado) {
                        Text(
                            " · " + stringResource(R.string.vendor_menu_featured),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = producto.disponible, onCheckedChange = { onToggle() }, enabled = !enProceso)
                Text(
                    stringResource(if (producto.disponible) R.string.vendor_menu_available else R.string.vendor_menu_paused),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (producto.disponible) colors.primary else colors.error
                )
            }
        }
    }
}

/** Foto del producto o un ícono si no tiene. */
@Composable
internal fun ProductThumb(url: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    if (url.isNullOrBlank()) {
        Box(
            modifier.clip(shape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Fastfood, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape)
        )
    }
}
