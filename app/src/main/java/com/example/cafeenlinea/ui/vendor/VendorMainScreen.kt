package com.example.cafeenlinea.ui.vendor

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cafeenlinea.R
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.profile.data.CloudinaryUploader
import com.example.cafeenlinea.ui.profile.view.ProfileView
import com.example.cafeenlinea.ui.vendor.data.ApiVendorRepository
import com.example.cafeenlinea.ui.vendor.model.ColumnaPedido
import com.example.cafeenlinea.ui.vendor.view.VendorInventoryView
import com.example.cafeenlinea.ui.vendor.view.VendorMenuView
import com.example.cafeenlinea.ui.vendor.view.VendorOrdersView
import com.example.cafeenlinea.ui.vendor.view.VendorSummaryView
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorInventoryViewModel
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorMenuViewModel
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorOrdersViewModel
import com.example.cafeenlinea.ui.vendor.viewmodel.VendorSummaryViewModel
import kotlinx.coroutines.delay

/** Cada cuánto se revisa si llegaron pedidos nuevos. */
private const val AUTO_REFRESH_MS = 15_000L

private object PanelRoutes {
    const val ORDERS = "panel_orders"
    const val MENU = "panel_menu"
    const val INVENTORY = "panel_inventory"
    const val SUMMARY = "panel_summary"
    const val EXTRA = "panel_extra"
}

/** La quinta pestaña del panel: "Perfil" para el vendedor, "Negocio" para el dueño. */
data class PanelExtraTab(
    @StringRes val label: Int,
    val icon: ImageVector,
    val content: @Composable () -> Unit
)

private data class PanelTab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Pantalla del vendedor tras el login, distinta a la de los clientes. Pestañas:
 * Pedidos (la principal), Menú, Stock, Resumen y Perfil.
 */
@Composable
fun VendorMainScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val cafeteriaId = remember { SessionPreferences(context).cafeteriaId().orEmpty() }
    CafeteriaPanel(
        cafeteriaId = cafeteriaId,
        extraTab = PanelExtraTab(R.string.tab_profile, Icons.Filled.Person) { ProfileView(onLogout = onLogout) }
    )
}

/**
 * Pedidos, menú, stock y resumen de UNA cafetería, más una pestaña extra. Lo
 * usan el vendedor (su cafetería) y el dueño (la que elige arriba).
 *
 * Los ViewModel se crean aquí, uno por cafetería, para que su estado sobreviva
 * al cambiar de pestaña y se renueve al cambiar de cafetería.
 */
@Composable
fun CafeteriaPanel(
    cafeteriaId: String,
    extraTab: PanelExtraTab,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext

    val ordersViewModel: VendorOrdersViewModel = viewModel(
        key = "orders_$cafeteriaId",
        factory = viewModelFactory { initializer { VendorOrdersViewModel(cafeteriaId, ApiVendorRepository) } }
    )
    val menuViewModel: VendorMenuViewModel = viewModel(
        key = "menu_$cafeteriaId",
        factory = viewModelFactory {
            initializer {
                VendorMenuViewModel(cafeteriaId, ApiVendorRepository) { uri ->
                    CloudinaryUploader.uploadProductImage(appContext, Uri.parse(uri), cafeteriaId)
                }
            }
        }
    )
    val inventoryViewModel: VendorInventoryViewModel = viewModel(
        key = "inventory_$cafeteriaId",
        factory = viewModelFactory { initializer { VendorInventoryViewModel(cafeteriaId, ApiVendorRepository) } }
    )
    val summaryViewModel: VendorSummaryViewModel = viewModel(
        key = "summary_$cafeteriaId",
        factory = viewModelFactory { initializer { VendorSummaryViewModel(cafeteriaId, ApiVendorRepository) } }
    )

    val ordersState by ordersViewModel.uiState.collectAsState()
    val nuevos = ordersState.cuantos(ColumnaPedido.NUEVOS)

    // Revisa si llegaron pedidos en cualquier pestaña, para mostrar el aviso en la barra
    LaunchedEffect(ordersViewModel) {
        while (true) {
            delay(AUTO_REFRESH_MS)
            ordersViewModel.refreshSilently()
        }
    }

    val tabs = listOf(
        PanelTab(PanelRoutes.ORDERS, R.string.vendor_tab_orders, Icons.AutoMirrored.Filled.ReceiptLong),
        PanelTab(PanelRoutes.MENU, R.string.vendor_tab_menu, Icons.Filled.RestaurantMenu),
        // Etiqueta corta: con 5 pestañas, "Inventario" no cabe en una línea con letra grande
        PanelTab(PanelRoutes.INVENTORY, R.string.vendor_tab_inventory_short, Icons.Filled.Inventory2),
        PanelTab(PanelRoutes.SUMMARY, R.string.vendor_tab_summary, Icons.Filled.Insights),
        PanelTab(PanelRoutes.EXTRA, extraTab.label, extraTab.icon)
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val current = backStackEntry?.destination
                tabs.forEach { tab ->
                    val label = stringResource(tab.label)
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigateToTab(tab.route)
                            // Al volver a una pestaña se ve lo último (otro vendedor pudo cambiar algo)
                            when (tab.route) {
                                PanelRoutes.INVENTORY -> inventoryViewModel.refresh()
                                PanelRoutes.SUMMARY -> summaryViewModel.refresh()
                            }
                        },
                        icon = {
                            // Aviso de pedidos nuevos aunque esté en otra pestaña
                            if (tab.route == PanelRoutes.ORDERS && nuevos > 0) {
                                BadgedBox(badge = { Badge { Text(nuevos.toString()) } }) {
                                    Icon(tab.icon, contentDescription = label)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = label)
                            }
                        },
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PanelRoutes.ORDERS,
            // Cada pestaña tiene su propio TopAppBar: no se repite el espacio de la barra de estado
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            composable(PanelRoutes.ORDERS) { VendorOrdersView(ordersViewModel) }
            composable(PanelRoutes.MENU) { VendorMenuView(menuViewModel) }
            composable(PanelRoutes.INVENTORY) { VendorInventoryView(inventoryViewModel) }
            composable(PanelRoutes.SUMMARY) { VendorSummaryView(summaryViewModel) }
            composable(PanelRoutes.EXTRA) { extraTab.content() }
        }
    }
}
