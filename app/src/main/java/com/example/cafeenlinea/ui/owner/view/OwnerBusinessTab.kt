package com.example.cafeenlinea.ui.owner.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.CafeteriaNegocio
import com.example.cafeenlinea.data.remote.Usuario
import com.example.cafeenlinea.ui.owner.model.EstadoNegocio
import com.example.cafeenlinea.ui.owner.viewmodel.OwnerViewModel
import com.example.cafeenlinea.ui.profile.view.ProfileView
import com.example.cafeenlinea.ui.vendor.view.SectionTitle

/**
 * Pestaña "Negocio" del dueño: estado del negocio, sus cafeterías (y abrir otra),
 * sus vendedores (y dar de alta otro) y acceso a su perfil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OwnerBusinessTab(viewModel: OwnerViewModel, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var verPerfil by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (verPerfil) {
        BackHandler { verPerfil = false }
        ProfileView(onLogout = onLogout)
        return
    }

    val mensaje = uiState.mensaje?.let { stringResource(it) }
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.clearMensaje()
        }
    }

    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = { TopAppBar(title = { Text(uiState.negocio?.nombre ?: stringResource(R.string.owner_tab_business)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            item { EstadoCard(uiState.estado) }

            // --- Cafeterías ---
            item {
                SectionTitle(stringResource(R.string.owner_section_cafeterias), Modifier.padding(top = 16.dp))
            }
            items(uiState.cafeterias, key = { "caf_" + it.id }) { cafeteria ->
                CafeteriaRow(cafeteria, vendedores = uiState.vendedoresDe(cafeteria.id).size)
            }
            item {
                OutlinedButton(onClick = viewModel::nuevaCafeteria, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.owner_add_cafeteria))
                }
            }

            // --- Vendedores ---
            item {
                SectionTitle(stringResource(R.string.owner_section_vendors), Modifier.padding(top = 16.dp))
            }
            if (uiState.vendedores.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.owner_no_vendors),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            items(uiState.vendedores, key = { "ven_" + it.id }) { vendedor ->
                VendedorRow(vendedor, uiState.cafeterias.firstOrNull { it.id == vendedor.cafeteriaId }?.nombre)
            }
            item {
                OutlinedButton(
                    onClick = viewModel::nuevoVendedor,
                    enabled = uiState.cafeterias.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.owner_add_vendor))
                }
            }

            // --- Perfil ---
            item {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { verPerfil = true }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, null, tint = colors.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.owner_my_profile), color = colors.onSurface, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }

    uiState.vendedorForm?.let { form ->
        VendedorFormSheet(
            form = form,
            cafeterias = uiState.cafeterias,
            onChange = viewModel::actualizarVendedor,
            onSave = viewModel::guardarVendedor,
            onDismiss = viewModel::cerrarVendedor
        )
    }
    uiState.cafeteriaForm?.let { form ->
        CafeteriaFormSheet(
            form = form,
            viewModel = viewModel
        )
    }
}

@Composable
private fun EstadoCard(estado: String) {
    val colors = MaterialTheme.colorScheme
    val (titulo, cuerpo) = when (estado) {
        EstadoNegocio.EN_REVISION -> R.string.owner_status_pending to R.string.owner_status_pending_body
        else -> R.string.owner_status_approved to R.string.owner_status_approved_body
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (estado == EstadoNegocio.EN_REVISION) colors.tertiaryContainer else colors.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(titulo),
                fontWeight = FontWeight.SemiBold,
                color = if (estado == EstadoNegocio.EN_REVISION) colors.onTertiaryContainer else colors.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(cuerpo),
                style = MaterialTheme.typography.bodyMedium,
                color = if (estado == EstadoNegocio.EN_REVISION) colors.onTertiaryContainer else colors.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CafeteriaRow(cafeteria: CafeteriaNegocio, vendedores: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Storefront, null, tint = colors.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cafeteria.nombre, fontWeight = FontWeight.Medium, color = colors.onSurface)
                val lugar = listOfNotNull(cafeteria.escuela?.nombre, cafeteria.ubicacion?.takeIf { it.isNotBlank() })
                if (lugar.isNotEmpty()) {
                    Text(lugar.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Text(
                    stringResource(R.string.owner_vendor_count, vendedores),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
            Surface(shape = RoundedCornerShape(50), color = if (cafeteria.activa) colors.secondaryContainer else colors.surfaceVariant) {
                Text(
                    stringResource(if (cafeteria.activa) R.string.owner_cafeteria_visible else R.string.owner_cafeteria_hidden),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (cafeteria.activa) colors.onSecondaryContainer else colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun VendedorRow(vendedor: Usuario, cafeteria: String?) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Badge, null, tint = colors.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(vendedor.nombre, fontWeight = FontWeight.Medium, color = colors.onSurface)
                Text(vendedor.correo, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                cafeteria?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = colors.primary)
                }
            }
        }
    }
}
