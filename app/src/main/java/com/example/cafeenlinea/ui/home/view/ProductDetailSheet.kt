package com.example.cafeenlinea.ui.home.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cafeenlinea.ui.cart.model.CartItem
import com.example.cafeenlinea.ui.cart.model.buildCartItemId
import com.example.cafeenlinea.ui.cart.model.calculateLineTotal
import com.example.cafeenlinea.ui.home.model.Extra

/** Datos mínimos del producto o destacado que se muestra en el detalle. */
data class DetailTarget(
    val cartId: String,
    val name: String,
    val description: String,
    val badge: String,
    val sectionNote: String,
    val price: Double,
    val priceText: String,
    val image: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    target: DetailTarget,
    extras: List<Extra>,
    onDismiss: () -> Unit,
    onAdd: (CartItem) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var quantity by remember { mutableStateOf(1) }
    var note by remember { mutableStateOf("") }

    val selectedExtras = extras.filter { it.id in selectedIds }
    val total = calculateLineTotal(target.price, selectedExtras, quantity)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = target.image,
                contentDescription = target.name,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop
            )
            Text(target.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            CafeteriaBadge(target.badge)
            if (target.description.isNotBlank()) {
                Text(target.description, style = MaterialTheme.typography.bodyMedium)
            }
            if (target.sectionNote.isNotBlank()) {
                Text(
                    target.sectionNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (extras.isNotEmpty()) {
                Text("Extras", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                extras.forEach { extra ->
                    val checked = extra.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (checked) selectedIds - extra.id else selectedIds + extra.id
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(extra.name, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        Text(extra.priceText, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nota (ej. sin cebolla)") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { if (quantity > 1) quantity-- }) { Text("−") }
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                OutlinedButton(onClick = { quantity++ }) { Text("+") }
            }

            Button(
                onClick = {
                    onAdd(
                        CartItem(
                            id = buildCartItemId(target.cartId, selectedExtras),
                            name = target.name,
                            price = target.price,
                            priceText = target.priceText,
                            image = target.image,
                            quantity = quantity,
                            extras = selectedExtras,
                            note = note.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar al carrito · \$${"%.2f".format(total)}")
            }
        }
    }
}