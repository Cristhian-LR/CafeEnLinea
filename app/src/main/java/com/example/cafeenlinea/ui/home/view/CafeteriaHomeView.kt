package com.example.cafeenlinea.ui.home.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.ui.cart.viewmodel.CartViewModel
import com.example.cafeenlinea.ui.home.model.CafeteriaHomeUiState
import com.example.cafeenlinea.ui.home.model.CafeteriaListItemUiState
import com.example.cafeenlinea.ui.home.model.Extra
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
    var selected by remember { mutableStateOf<Pair<DetailTarget, List<Extra>>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.cafeterias.isEmpty() -> {
                    CafeLoading()
                }

                uiState.error != null && uiState.cafeterias.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = viewModel::cargarCafeterias) { Text(stringResource(R.string.action_retry)) }
                    }
                }

                uiState.emptyMessage != null -> {
                    Text(
                        text = uiState.emptyMessage!!,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.cafeterias, key = { it.id }) { item ->
                            CafeteriaExpandableCard(
                                item = item,
                                visibleMenu = viewModel.menuVisible(item),
                                cartViewModel = cartViewModel,
                                onToggle = { viewModel.toggleExpand(item.id) },
                                onRetry = { viewModel.reintentarMenu(item.id) },
                                onSearch = { query -> viewModel.onSearchQueryChange(item.id, query) },
                                onSectionSelected = { name -> viewModel.onSectionSelected(item.id, name) },
                                onSelectProduct = { target, extras -> selected = target to extras }
                            )
                        }
                    }
                }
            }
        }
    }

    selected?.let { (target, extras) ->
        ProductDetailSheet(
            target = target,
            extras = extras,
            onDismiss = { selected = null },
            onAdd = { item ->
                cartViewModel.addItem(item)
                selected = null
            }
        )
    }
}

@Composable
private fun CafeteriaExpandableCard(
    item: CafeteriaListItemUiState,
    visibleMenu: CafeteriaHomeUiState?,
    cartViewModel: CartViewModel,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
    onSearch: (String) -> Unit,
    onSectionSelected: (String?) -> Unit,
    onSelectProduct: (DetailTarget, List<Extra>) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (item.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (item.expanded) {
                when {
                    item.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) { CafeLoading(size = 48.dp) }
                    }

                    item.error != null -> {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(item.error, color = MaterialTheme.colorScheme.error)
                            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                        }
                    }

                    item.isEmpty -> {
                        Text(
                            text = stringResource(R.string.home_menu_empty),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item.menu != null && visibleMenu != null -> {
                        CafeteriaMenuContent(
                            menu = visibleMenu,
                            cartViewModel = cartViewModel,
                            onSearch = onSearch,
                            onSectionSelected = onSectionSelected,
                            onSelectProduct = onSelectProduct
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CafeteriaMenuContent(
    menu: CafeteriaHomeUiState,
    cartViewModel: CartViewModel,
    onSearch: (String) -> Unit,
    onSectionSelected: (String?) -> Unit,
    onSelectProduct: (DetailTarget, List<Extra>) -> Unit
) {
    val extrasDeMenu = menu.extras
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        menu.shop?.let { ShopHeader(it) }

        Column {
            OutlinedTextField(
                value = menu.searchQuery,
                onValueChange = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menu.sectionNames) { name ->
                    FilterChip(
                        selected = menu.selectedSection == name,
                        onClick = { onSectionSelected(name) },
                        label = { Text(name) }
                    )
                }
            }
        }

        if (menu.noResults) {
            Text(
                text = stringResource(R.string.home_no_results),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (menu.featured.isNotEmpty()) {
            SectionTitle(stringResource(R.string.home_section_featured))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menu.featured) { featured ->
                    FeaturedCard(
                        item = featured,
                        onClick = {
                            onSelectProduct(
                                DetailTarget(
                                    cartId = "featured_${featured.productId.ifBlank { featured.id.toString() }}",
                                    name = featured.name,
                                    description = "",
                                    badge = featured.badge,
                                    sectionNote = "",
                                    price = featured.price,
                                    priceText = featured.priceText,
                                    image = featured.image,
                                    productId = featured.productId
                                ),
                                extrasDeMenu
                            )
                        }
                    )
                }
            }
        }

        menu.sections.forEach { section ->
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                section.products.forEach { product ->
                    MenuItemCard(
                        product = product,
                        onClick = {
                            onSelectProduct(
                                DetailTarget(
                                    cartId = "product_${product.productId.ifBlank { "${section.name}_${product.id}" }}",
                                    name = product.name,
                                    description = product.description,
                                    badge = product.badge,
                                    sectionNote = section.note,
                                    price = product.price,
                                    priceText = product.priceText,
                                    image = product.image,
                                    productId = product.productId
                                ),
                                extrasDeMenu
                            )
                        }
                    )
                }
            }
        }

        if (menu.combos.isNotEmpty()) {
            SectionTitle(stringResource(R.string.home_section_combos))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menu.combos) { ComboCard(it, cartViewModel = cartViewModel) }
            }
            if (menu.combosNote.isNotBlank()) {
                Text(
                    text = menu.combosNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (menu.extras.isNotEmpty()) {
            SectionTitle(stringResource(R.string.home_section_extras))
            menu.extras.forEach { extra ->
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