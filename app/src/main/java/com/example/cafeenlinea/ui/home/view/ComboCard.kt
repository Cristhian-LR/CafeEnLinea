package com.example.cafeenlinea.ui.home.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cafeenlinea.ui.cart.model.CartItem
import com.example.cafeenlinea.ui.cart.viewmodel.CartViewModel
import com.example.cafeenlinea.ui.home.model.Combo

@Composable
fun ComboCard(
    combo: Combo,
    cartViewModel: CartViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(220.dp)
    ) {
        Column {
            AsyncImage(
                model = combo.image,
                contentDescription = combo.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = combo.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = combo.includes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = combo.priceText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        cartViewModel.addItem(
                            CartItem(
                                id = "combo_${combo.id}",
                                name = combo.name,
                                price = combo.price,
                                priceText = combo.priceText,
                                image = combo.image
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text("Agregar")
                }
            }
        }
    }
}