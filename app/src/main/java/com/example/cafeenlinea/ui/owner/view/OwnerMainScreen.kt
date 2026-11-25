package com.example.cafeenlinea.ui.owner.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeenlinea.R
import com.example.cafeenlinea.ui.auth.viewmodel.LogoutViewModel
import com.example.cafeenlinea.ui.owner.data.ApiOwnerRepository
import com.example.cafeenlinea.ui.owner.model.OwnerUiState
import com.example.cafeenlinea.ui.owner.viewmodel.OwnerViewModel
import com.example.cafeenlinea.ui.vendor.CafeteriaPanel
import com.example.cafeenlinea.ui.vendor.PanelExtraTab
import com.example.cafeenlinea.ui.vendor.view.VendorError
import com.example.cafeenlinea.ui.vendor.view.VendorLoading
import com.example.cafeenlinea.ui.vendor.view.VendorMessage

/**
 * Pantalla del dueño (JEFE). Arriba elige la cafetería y ve si su negocio sigue
 * en revisión; abajo, las mismas pestañas del vendedor más "Negocio" (sus
 * cafeterías y vendedores).
 */
@Composable
fun OwnerMainScreen(onLogout: () -> Unit) {
    val viewModel: OwnerViewModel = viewModel(
        factory = viewModelFactory { initializer { OwnerViewModel(ApiOwnerRepository) } }
    )
    val logoutViewModel: LogoutViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val cerrarSesion = { logoutViewModel.logout(onLogout) }

    val negocioTab = PanelExtraTab(R.string.owner_tab_business, Icons.Filled.Storefront) {
        OwnerBusinessTab(viewModel = viewModel, onLogout = onLogout)
    }

    val seleccionada = uiState.cafeteriaSeleccionada
    when {
        uiState.isLoading && !uiState.loaded -> Surface(Modifier.fillMaxSize()) { VendorLoading() }
        uiState.error != null && !uiState.loaded -> Surface(Modifier.fillMaxSize()) {
            VendorError(uiState.error!!, viewModel::load)
        }
        uiState.rechazado -> Surface(Modifier.fillMaxSize()) {
            VendorMessage(
                icon = Icons.Filled.Storefront,
                title = stringResource(R.string.owner_rejected_title),
                body = uiState.negocio?.motivoRechazo?.let { stringResource(R.string.owner_rejected_reason, it) }
                    ?: stringResource(R.string.owner_rejected_body),
                actionText = stringResource(R.string.profile_logout),
                onAction = cerrarSesion
            )
        }
        // Sin cafeterías (no debería pasar con el registro nuevo): solo la pestaña Negocio
        seleccionada == null -> Surface(Modifier.fillMaxSize().statusBarsPadding()) { negocioTab.content() }
        else -> Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            OwnerHeader(uiState = uiState, onSelect = viewModel::seleccionarCafeteria)
            CafeteriaPanel(
                cafeteriaId = seleccionada.id,
                extraTab = negocioTab,
                // El encabezado ya ocupa la barra de estado
                modifier = Modifier.weight(1f).consumeWindowInsets(WindowInsets.statusBars)
            )
        }
    }
}

/** Selector de cafetería y aviso de "en revisión". */
@Composable
private fun OwnerHeader(uiState: OwnerUiState, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val seleccionada = uiState.cafeteriaSeleccionada ?: return

    Surface(color = colors.surfaceContainer) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp)
            ) {
                TextButton(onClick = { expanded = true }, enabled = uiState.cafeterias.size > 1) {
                    Icon(Icons.Filled.Storefront, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            seleccionada.nombre,
                            color = colors.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        seleccionada.escuela?.let {
                            Text(
                                it.nombre,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (uiState.cafeterias.size > 1) {
                        Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.owner_choose_cafeteria), tint = colors.onSurface)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    uiState.cafeterias.forEach { cafeteria ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(cafeteria.nombre)
                                    cafeteria.escuela?.let {
                                        Text(it.nombre, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                                    }
                                }
                            },
                            onClick = {
                                expanded = false
                                onSelect(cafeteria.id)
                            }
                        )
                    }
                }
            }
            if (uiState.enRevision) {
                Surface(color = colors.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.HourglassTop, null, tint = colors.onTertiaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.owner_pending_banner),
                            color = colors.onTertiaryContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
