package com.example.cafeenlinea.ui.vendor.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.ui.vendor.model.PedidosPorHora
import com.example.cafeenlinea.ui.vendor.model.ResumenVendedor
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorSummaryViewModel
import java.util.Locale

/** Resumen: pedidos y ventas de hoy, horas pico, lo más vendido y tiempos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorSummaryView(viewModel: VendorSummaryViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.vendor_tab_summary)) }) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading && !uiState.loaded -> VendorLoading()
                uiState.error != null && !uiState.loaded -> VendorError(uiState.error!!, viewModel::load)
                else -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Si falló al actualizar, se sigue viendo lo anterior con el aviso arriba
                        uiState.error?.let { SectionNote(it) }
                        TodayCard(uiState.resumen)
                        TopSellersCard(uiState.resumen)
                        TimesCard(uiState.resumen)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(title)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SectionNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TodayCard(resumen: ResumenVendedor) {
    SummaryCard(stringResource(R.string.vendor_summary_today)) {
        Row(Modifier.fillMaxWidth()) {
            Stat(stringResource(R.string.vendor_summary_orders), resumen.pedidosHoy.toString(), Modifier.weight(1f))
            Stat(stringResource(R.string.vendor_summary_delivered), resumen.entregadosHoy.toString(), Modifier.weight(1f))
            Stat(
                stringResource(R.string.vendor_summary_income),
                dineroTexto(resumen.ventasHoy, resumen.moneda),
                Modifier.weight(1.3f)
            )
        }
        Spacer(Modifier.height(12.dp))
        val pico = resumen.horaPico
        if (pico == null) {
            SectionNote(stringResource(R.string.vendor_summary_no_sales))
        } else {
            Text(
                stringResource(R.string.vendor_summary_peak, horaDelDia(pico.hora), pico.pedidos),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            HourBars(resumen.porHora)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Barras de pedidos por hora; la hora pico va en el color fuerte. */
@Composable
private fun HourBars(porHora: List<PedidosPorHora>) {
    if (porHora.isEmpty()) return
    val maximo = porHora.maxOf { it.pedidos }.coerceAtLeast(1)
    val colors = MaterialTheme.colorScheme

    SectionNote(stringResource(R.string.vendor_summary_by_hour))
    Spacer(Modifier.height(8.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().height(120.dp)
    ) {
        porHora.forEach { (hora, pedidos) ->
            val descripcion = "${horaDelDia(hora)}: $pedidos"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { contentDescription = descripcion }
            ) {
                if (pedidos > 0) {
                    Text(pedidos.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((pedidos.toFloat() / maximo * 0.75f).coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (pedidos == maximo) colors.primary else colors.primary.copy(alpha = 0.45f))
                )
                Text(hora.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TopSellersCard(resumen: ResumenVendedor) {
    SummaryCard(stringResource(R.string.vendor_summary_top)) {
        val lista = resumen.masVendidos
        if (lista.isEmpty()) {
            SectionNote(stringResource(R.string.vendor_summary_top_empty))
            return@SummaryCard
        }
        val maximo = lista.maxOf { it.unidades }.coerceAtLeast(1)
        lista.forEachIndexed { index, producto ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}. ${producto.nombre}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (producto.ingresos > 0) {
                    Text(
                        dineroTexto(producto.ingresos, resumen.moneda),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(producto.unidades.toFloat() / maximo)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.vendor_summary_units, producto.unidades),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimesCard(resumen: ResumenVendedor) {
    SummaryCard(stringResource(R.string.vendor_summary_times)) {
        if (resumen.pedidosMedidos == 0 && resumen.preparacionPromedioMin == null) {
            SectionNote(stringResource(R.string.vendor_summary_no_data))
            return@SummaryCard
        }
        Row(Modifier.fillMaxWidth()) {
            Stat(stringResource(R.string.vendor_summary_prep), minutos(resumen.preparacionPromedioMin), Modifier.weight(1f))
            Stat(stringResource(R.string.vendor_summary_total_time), minutos(resumen.entregaPromedioMin), Modifier.weight(1f))
        }
        if (resumen.pedidosMedidos > 0) {
            Spacer(Modifier.height(8.dp))
            SectionNote(stringResource(R.string.vendor_summary_measured, resumen.pedidosMedidos))
        }
    }
}

@Composable
private fun minutos(valor: Double?): String =
    if (valor == null) "—"
    else stringResource(R.string.vendor_summary_minutes, String.format(Locale.getDefault(), "%.0f", valor))
