package com.example.cafeenlinea.ui.orders.view

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.data.remote.Dinero
import com.example.cafeenlinea.data.remote.Pedido
import com.example.cafeenlinea.ui.orders.model.OrderStatus
import com.example.cafeenlinea.ui.orders.model.creadoEnMillis
import com.example.cafeenlinea.ui.orders.model.estadoTexto
import com.example.cafeenlinea.ui.orders.model.numeroCorto
import com.example.cafeenlinea.ui.orders.viewmodel.OrdersViewModel
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Cada cuánto se actualiza solo el tab mientras hay pedidos en curso. */
private const val AUTO_REFRESH_MS = 15_000L

/**
 * Tab de Pedidos: "En curso" arriba e "Historial" abajo. Se puede deslizar hacia
 * abajo para actualizar y, mientras el tab está abierto, se actualiza solo.
 *
 * @param onGoToMenu Lo usa el botón de la pantalla vacía para ir al Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersView(
    viewModel: OrdersViewModel,
    onGoToMenu: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Solo corre mientras este tab está en pantalla; al cambiar de tab se detiene.
    LaunchedEffect(Unit) {
        while (true) {
            delay(AUTO_REFRESH_MS)
            viewModel.refreshSilently()
        }
    }

    // Si ya hay pedidos en pantalla, el error va al Snackbar y se conserva la lista
    LaunchedEffect(uiState.error) {
        val mensaje = uiState.error ?: return@LaunchedEffect
        if (uiState.loaded) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis pedidos") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && !uiState.loaded -> CafeLoading(size = 96.dp)

                !uiState.loaded && uiState.error != null -> CenteredMessage(
                    title = "No pudimos cargar tus pedidos",
                    body = uiState.error!!,
                    buttonText = "Reintentar",
                    onClick = viewModel::load,
                    primaryButton = true
                )

                else -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.isEmpty) {
                        // LazyColumn para que también se pueda deslizar con la lista vacía
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                CenteredMessage(
                                    title = "Aún no tienes pedidos",
                                    body = "Cuando confirmes un pedido desde el carrito, aquí verás cómo va.",
                                    buttonText = "Ver el menú",
                                    onClick = onGoToMenu,
                                    modifier = Modifier.padding(top = 96.dp)
                                )
                            }
                        }
                    } else {
                        OrdersList(enCurso = uiState.enCurso, historial = uiState.historial)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersList(enCurso: List<Pedido>, historial: List<Pedido>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (enCurso.isNotEmpty()) {
            item(key = "titulo_en_curso") { SectionTitle("En curso") }
            items(enCurso, key = { "curso_${it.id}" }) { OrderCard(it) }
        }
        if (historial.isNotEmpty()) {
            item(key = "titulo_historial") {
                SectionTitle("Historial", Modifier.padding(top = if (enCurso.isNotEmpty()) 12.dp else 0.dp))
            }
            items(historial, key = { "hist_${it.id}" }) { OrderCard(it) }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
private fun OrderCard(pedido: Pedido) {
    val status = OrderStatus.from(pedido.estado)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pedido ${pedido.numeroCorto()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    pedido.creadoEnMillis()?.let { millis ->
                        Text(
                            text = formatDate(millis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusChip(status = status, text = pedido.estadoTexto())
            }

            if (status.enCurso) StatusProgress(paso = status.paso)

            HorizontalDivider()

            pedido.items.forEach { item ->
                Text(
                    text = "${item.cantidad}× ${item.nombreProducto}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            pedido.total?.let { total ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatMoney(total),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus, text: String) {
    val colors = MaterialTheme.colorScheme
    val (fondo, texto) = when (status) {
        OrderStatus.PENDIENTE -> colors.secondaryContainer to colors.onSecondaryContainer
        OrderStatus.EN_PREPARACION -> colors.primaryContainer to colors.onPrimaryContainer
        OrderStatus.LISTO -> colors.primary to colors.onPrimary
        OrderStatus.ENTREGADO, OrderStatus.DESCONOCIDO -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = texto,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Barra de avance en 4 tramos: recibido → preparando → listo → entregado. */
@Composable
private fun StatusProgress(paso: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(OrderStatus.TOTAL_PASOS) { i ->
            val lleno = i < paso
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (lleno) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    body: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryButton: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        if (primaryButton) {
            Button(onClick = onClick) { Text(buttonText) }
        } else {
            OutlinedButton(onClick = onClick) { Text(buttonText) }
        }
    }
}

private fun formatDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        .format(Date(millis))

private fun formatMoney(d: Dinero): String =
    if (d.moneda == "MXN") String.format(Locale.US, "$%.2f", d.monto)
    else String.format(Locale.US, "%.2f %s", d.monto, d.moneda)
