package com.example.cafeenlinea.ui.home.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Etiqueta de color reutilizable (badge nutricional, precio, etc.). Vive suelta en
 * el paquete `view` porque la usan [FeaturedCard], [MenuItemCard] y [ComboCard].
 *
 * @param text Texto del badge. Si viene vacío, no dibuja nada.
 * @param highlighted true para el color primario (precio), false para el
 *   secundario (etiqueta informativa).
 */
@Composable
fun CafeteriaBadge(
    text: String,
    highlighted: Boolean = false
) {
    if (text.isBlank()) return
    Surface(
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}