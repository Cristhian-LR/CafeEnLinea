package com.example.cafeenlinea.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cafeenlinea.ui.cart.view.CartView
import com.example.cafeenlinea.ui.cart.viewmodel.CartViewModel
import com.example.cafeenlinea.ui.home.view.CafeteriaHomeView
import com.example.cafeenlinea.ui.profile.view.ProfileView

private object MainRoutes {
    const val HOME = "main_home"
    const val CART = "main_cart"
    const val PROFILE = "main_profile"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(MainRoutes.HOME, "Home", Icons.Filled.Home),
    BottomTab(MainRoutes.CART, "Carrito", Icons.Filled.ShoppingCart),
    BottomTab(MainRoutes.PROFILE, "Perfil", Icons.Filled.Person)
)

/**
 * Pantalla contenedora tras el login: barra inferior (Home / Carrito / Perfil)
 * con su propio NavHost anidado. El [CartViewModel] se crea aquí (una sola
 * vez) y se comparte entre Home y Carrito para que el estado sobreviva al
 * cambiar de pestaña.
 *
 * @param onLogout Se propaga hasta ProfileView; navega de vuelta a Login.
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val innerNavController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            innerNavController.navigate(tab.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
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
                CartView(cartViewModel = cartViewModel)
            }
            composable(MainRoutes.PROFILE) {
                ProfileView(onLogout = onLogout)
            }
        }
    }
}