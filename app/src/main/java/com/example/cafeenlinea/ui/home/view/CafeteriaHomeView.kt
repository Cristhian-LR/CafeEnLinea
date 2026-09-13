package com.example.cafeenlinea.ui.home.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeenlinea.ui.home.model.Shop
import com.example.cafeenlinea.ui.home.viewmodel.CafeteriaHomeViewModel

/**
 * Pantalla "home" de la cafetería. Solo dibuja: los datos vienen de
 * [CafeteriaHomeViewModel] (que llama a `GET /api/home/cafeteria/`).
 *
 * Estructura:
 * - Encabezado con nombre, lema y banner de promoción.
 * - "Destacados" → carrusel horizontal de [FeaturedCard].
 * - Una sección por categoría del menú (con su nota) → lista vertical de [MenuItemCard].
 * - "Combos" → carrusel horizontal de [ComboCard] + nota general.
 * - "Extras" → lista simple.
 *
 * @param onBack Se dispara al tocar la flecha de la barra superior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeteriaHomeView(
    onBack: () -> Unit = {},
    viewModel: CafeteriaHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cafetería") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                uiState.error != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::loadCafeteriaHome) { Text("Reintentar") }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.shop?.let { shop ->
                        item { ShopHeader(shop) }
                    }

                    if (uiState.featured.isNotEmpty()) {
                        item { SectionTitle("Destacados") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.featured) { FeaturedCard(it) }
                            }
                        }
                    }

                    uiState.sections.forEach { section ->
                        item(key = "sec_title_${section.name}") {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = section.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (section.note.isNotBlank()) {
                                    Text(
                                        text = section.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        items(
                            items = section.products,
                            key = { "sec_${section.name}_${it.id}" }
                        ) { product ->
                            MenuItemCard(
                                product = product,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    if (uiState.combos.isNotEmpty()) {
                        item { SectionTitle("Combos") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.combos) { ComboCard(it) }
                            }
                        }
                        if (uiState.combosNote.isNotBlank()) {
                            item {
                                Text(
                                    text = uiState.combosNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }

                    if (uiState.extras.isNotEmpty()) {
                        item { SectionTitle("Extras") }
                        items(uiState.extras, key = { "extra_${it.id}" }) { extra ->
                            Card(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(extra.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = extra.priceText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Encabezado con el nombre de la cafetería, su lema y el banner de promoción. */
@Composable
private fun ShopHeader(shop: Shop) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = shop.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = shop.tagline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (shop.promo.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp)
            ) {
                Text(
                    text = shop.promo,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Encabezado de sección dentro del scroll. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}