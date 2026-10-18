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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.ui.cart.viewmodel.CartViewModel
import com.example.cafeenlinea.ui.home.model.Shop
import com.example.cafeenlinea.ui.home.viewmodel.CafeteriaHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeteriaHomeView(
    onBack: () -> Unit = {},
    viewModel: CafeteriaHomeViewModel = viewModel(),
    cartViewModel: CartViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selected by remember { mutableStateOf<DetailTarget?>(null) }

    // C02: Snackbar si falla la recarga y se conserva el menú previo
    LaunchedEffect(uiState.error) {
        uiState.error?.let { mensaje ->
            if (uiState.shop != null || uiState.sections.isNotEmpty()) {
                snackbarHostState.showSnackbar(mensaje)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cafetería") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // C03: animación Lottie
                uiState.isLoading && uiState.shop == null -> {
                    CafeLoading()
                }

                uiState.error != null && uiState.shop == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = viewModel::loadCafeteriaHome) { Text("Reintentar") }
                    }
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshCafeteriaHome() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.shop?.let { shop ->
                                item { ShopHeader(shop) }
                            }

                            // C04: buscador y chips de sección
                            item {
                                Column {
                                    OutlinedTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = viewModel::onSearchQueryChange,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        placeholder = { Text("Buscar en el menú") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, contentDescription = null)
                                        },
                                        singleLine = true
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(uiState.sectionNames) { name ->
                                            FilterChip(
                                                selected = uiState.selectedSection == name,
                                                onClick = { viewModel.onSectionSelected(name) },
                                                label = { Text(name) }
                                            )
                                        }
                                    }
                                }
                            }

                            // C04: sin coincidencias
                            if (uiState.noResults) {
                                item {
                                    Text(
                                        text = "Sin resultados",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }

                            if (uiState.featured.isNotEmpty()) {
                                item { SectionTitle("Destacados") }
                                item {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(uiState.featured) { featured ->
                                            FeaturedCard(
                                                item = featured,
                                                onClick = {
                                                    selected = DetailTarget(
                                                        cartId = "featured_${featured.id}",
                                                        name = featured.name,
                                                        description = "",
                                                        badge = featured.badge,
                                                        sectionNote = "",
                                                        price = featured.price,
                                                        priceText = featured.priceText,
                                                        image = featured.image
                                                    )
                                                }
                                            )
                                        }
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
                                        onClick = {
                                            selected = DetailTarget(
                                                cartId = "product_${product.section}_${product.id}",
                                                name = product.name,
                                                description = product.description,
                                                badge = product.badge,
                                                sectionNote = section.note,
                                                price = product.price,
                                                priceText = product.priceText,
                                                image = product.image
                                            )
                                        },
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
                                        items(uiState.combos) {
                                            ComboCard(it, cartViewModel = cartViewModel)
                                        }
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
                                    Card(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
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
    }

    // C05: detalle del producto con extras
    selected?.let { target ->
        ProductDetailSheet(
            target = target,
            extras = uiState.extras,
            onDismiss = { selected = null },
            onAdd = { item ->
                cartViewModel.addItem(item)
                selected = null
            }
        )
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