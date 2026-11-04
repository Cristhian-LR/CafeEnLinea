package com.example.cafeenlinea.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.cart.view.CartView
import com.example.cafeenlinea.ui.cart.viewmodel.CartViewModel
import com.example.cafeenlinea.ui.home.view.CafeteriaHomeView
import com.example.cafeenlinea.ui.orders.view.OrdersView
import com.example.cafeenlinea.ui.orders.viewmodel.OrdersViewModel
import com.example.cafeenlinea.ui.profile.view.ProfileView

private object MainRoutes {
    const val HOME = "main_home"
    const val CART = "main_cart"
    const val ORDERS = "main_orders"
    const val PROFILE = "main_profile"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(MainRoutes.HOME, "Home", Icons.Filled.Home),
    BottomTab(MainRoutes.CART, "Carrito", Icons.Filled.ShoppingCart),
    BottomTab(MainRoutes.ORDERS, "Pedidos", Icons.AutoMirrored.Filled.ReceiptLong),
    BottomTab(MainRoutes.PROFILE, "Perfil", Icons.Filled.Person)
)

/** Cambia de tab conservando el estado de cada uno. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Pantalla contenedora tras el login: barra inferior (Home / Carrito / Pedidos / Perfil)
 * con su propio NavHost anidado. El [CartViewModel] y el [OrdersViewModel] se crean
 * aquí (una sola vez) para que su estado sobreviva al cambiar de pestaña.
 *
 * Al confirmar un pedido desde el Carrito, se recargan los pedidos y se abre el tab Pedidos.
 *
 * @param onLogout Se propaga hasta ProfileView; navega de vuelta a Login.
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val innerNavController = rememberNavController()
    val context = LocalContext.current
    val session = remember { SessionPreferences(context) }

    val cartViewModel: CartViewModel = viewModel()
    val ordersViewModel: OrdersViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                OrdersViewModel(
                    usuarioId = { session.userId() },
                    cargarPedidos = ApiClient::pedidosDelUsuario
                )
            }
        }
    )

    // Pedido recién creado: actualiza la lista y lleva al usuario a verlo
    val createdOrder by cartViewModel.createdOrder.collectAsState()
    LaunchedEffect(createdOrder) {
        if (createdOrder != null) {
            ordersViewModel.load()
            innerNavController.navigateToTab(MainRoutes.ORDERS)
            cartViewModel.consumeCreatedOrder()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { innerNavController.navigateToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = MainRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainRoutes.HOME) {
                CafeteriaHomeView(cartViewModel = cartViewModel)
            }
            composable(MainRoutes.CART) {
                CartView(
                    cartViewModel = cartViewModel,
                    onCheckout = {
                        cartViewModel.confirmOrder(session.userId(), session.cafeteriaId())
                    }
                )
            }
            composable(MainRoutes.ORDERS) {
                OrdersView(
                    viewModel = ordersViewModel,
                    onGoToMenu = { innerNavController.navigateToTab(MainRoutes.HOME) }
                )
            }
            composable(MainRoutes.PROFILE) {
                ProfileView(onLogout = onLogout)
            }
        }
    }
}
