package com.example.cafeenlinea.ui.vendor.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.data.remote.Dinero
import com.example.cafeenlinea.ui.vendor.model.isoAMillis
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** "$35.00" para pesos, "35.00 USD" para otras monedas (igual que en Pedidos del cliente). */
internal fun Dinero.texto(): String =
    if (moneda == "MXN") String.format(Locale.US, "$%.2f", monto)
    else String.format(Locale.US, "%.2f %s", monto, moneda)

internal fun dineroTexto(monto: Double, moneda: String = "MXN") = Dinero(monto, moneda).texto()

/** "Pedido #3F9A1C": los últimos 6 caracteres del id. */
internal fun numeroPedido(id: String): String = "#" + id.takeLast(6).uppercase()

/**
 * Hora local corta de una fecha ISO del servidor ("2026-11-24T18:25:00.000Z" -> "12:25 p.m.").
 * null si no se puede leer.
 */
internal fun horaCorta(iso: String?): String? =
    isoAMillis(iso)?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) }

/** "8:00" para la hora 8 del día (gráfica de horas pico). */
internal fun horaDelDia(hora: Int): String = String.format(Locale.US, "%d:00", hora)

@Composable
internal fun VendorLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CafeLoading(size = 120.dp)
    }
}

/** Mensaje centrado con ícono. Se puede desplazar para que funcione "deslizar para actualizar". */
@Composable
internal fun VendorMessage(
    icon: ImageVector,
    title: String,
    body: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onAction) { Text(actionText) }
        }
    }
}

@Composable
internal fun VendorError(message: String, onRetry: () -> Unit) {
    VendorMessage(
        icon = Icons.Filled.CloudOff,
        title = message,
        actionText = stringResource(R.string.action_retry),
        onAction = onRetry
    )
}

/** Título de sección dentro de una lista. */
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
