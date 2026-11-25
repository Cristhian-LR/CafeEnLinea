package com.example.cafeenlinea.ui.owner.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.data.remote.CafeteriaNegocio
import com.example.cafeenlinea.ui.auth.model.PASSWORD_MIN_LENGTH
import com.example.cafeenlinea.ui.auth.view.AuthField
import com.example.cafeenlinea.ui.auth.view.EscuelaField
import com.example.cafeenlinea.ui.auth.view.authButtonColors
import com.example.cafeenlinea.ui.auth.view.authFieldColors
import com.example.cafeenlinea.ui.owner.model.CafeteriaForm
import com.example.cafeenlinea.ui.owner.model.VendedorForm
import com.example.cafeenlinea.ui.owner.viewmodel.OwnerViewModel

/** Hoja para dar de alta a un vendedor: el dueño crea su cuenta y se la pasa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VendedorFormSheet(
    form: VendedorForm,
    cafeterias: List<CafeteriaNegocio>,
    onChange: ((VendedorForm) -> VendedorForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val enabled = !form.guardando
    var passwordVisible by remember { mutableStateOf(false) }
    FormSheet(
        title = stringResource(R.string.owner_add_vendor),
        subtitle = stringResource(R.string.owner_vendor_note),
        guardando = form.guardando,
        error = form.error,
        saveText = stringResource(R.string.owner_create_account),
        onSave = onSave,
        onDismiss = onDismiss
    ) {
        CafeteriaPicker(
            cafeterias = cafeterias,
            selectedId = form.cafeteriaId,
            error = form.cafeteriaError?.let { stringResource(it) },
            enabled = enabled,
            onSelect = { id -> onChange { it.copy(cafeteriaId = id, cafeteriaError = null) } }
        )
        Spacer(Modifier.height(8.dp))
        AuthField(
            value = form.nombre,
            onValueChange = { v -> onChange { it.copy(nombre = v, errores = it.errores.copy(nombre = null)) } },
            label = stringResource(R.string.register_name_label),
            error = form.errores.nombre?.let { stringResource(it) },
            enabled = enabled,
            capitalization = KeyboardCapitalization.Words
        )
        AuthField(
            value = form.correo,
            onValueChange = { v -> onChange { it.copy(correo = v, correoDuplicado = false, errores = it.errores.copy(correo = null)) } },
            label = stringResource(R.string.login_email_label),
            error = when {
                form.correoDuplicado -> stringResource(R.string.register_error_email_taken)
                form.errores.correo != null -> stringResource(form.errores.correo)
                else -> null
            },
            enabled = enabled,
            keyboardType = KeyboardType.Email
        )
        AuthField(
            value = form.password,
            onValueChange = { v -> onChange { it.copy(password = v, errores = it.errores.copy(password = null)) } },
            label = stringResource(R.string.owner_vendor_password),
            error = form.errores.password?.let { stringResource(it, PASSWORD_MIN_LENGTH) },
            hint = stringResource(R.string.register_password_hint, PASSWORD_MIN_LENGTH),
            enabled = enabled,
            keyboardType = KeyboardType.Password,
            password = true,
            passwordVisible = passwordVisible,
            onTogglePassword = { passwordVisible = !passwordVisible },
            imeAction = ImeAction.Done,
            onDone = onSave
        )
    }
}

/** Hoja para abrir otra cafetería del negocio, en cualquier escuela. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CafeteriaFormSheet(form: CafeteriaForm, viewModel: OwnerViewModel) {
    val enabled = !form.guardando
    FormSheet(
        title = stringResource(R.string.owner_add_cafeteria),
        subtitle = stringResource(R.string.owner_cafeteria_note),
        guardando = form.guardando,
        error = form.error,
        saveText = stringResource(R.string.vendor_form_save),
        onSave = viewModel::guardarCafeteria,
        onDismiss = viewModel::cerrarCafeteria
    ) {
        AuthField(
            value = form.nombre,
            onValueChange = { v -> viewModel.actualizarCafeteria { it.copy(nombre = v, nombreError = null) } },
            label = stringResource(R.string.business_cafeteria_label),
            error = form.nombreError?.let { stringResource(it) },
            enabled = enabled,
            capitalization = KeyboardCapitalization.Words
        )
        EscuelaField(
            seleccion = form.escuela,
            enabled = enabled,
            onTextChanged = viewModel::onEscuelaTextoChanged,
            onSuggestionSelected = viewModel::onEscuelaSugeridaSelected,
            onCiudadChanged = viewModel::onCiudadChanged,
            onRetryCatalog = viewModel::cargarEscuelas,
            onDone = {},
            label = stringResource(R.string.business_school_label)
        )
        Spacer(Modifier.height(8.dp))
        AuthField(
            value = form.ubicacion,
            onValueChange = { v -> viewModel.actualizarCafeteria { it.copy(ubicacion = v) } },
            label = stringResource(R.string.business_location_label),
            hint = stringResource(R.string.business_location_hint),
            enabled = enabled,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormSheet(
    title: String,
    subtitle: String,
    guardando: Boolean,
    error: String?,
    saveText: String,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !guardando }
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            content()
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                enabled = !guardando,
                shape = RoundedCornerShape(10.dp),
                colors = authButtonColors(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (guardando) CafeLoading(size = 28.dp) else Text(saveText, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Lista para elegir en cuál cafetería atenderá el vendedor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CafeteriaPicker(
    cafeterias: List<CafeteriaNegocio>,
    selectedId: String?,
    error: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val seleccionada = cafeterias.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = seleccionada?.let { c -> listOfNotNull(c.nombre, c.escuela?.nombre).joinToString(" · ") }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(R.string.owner_vendor_cafeteria)) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(10.dp),
            colors = authFieldColors(),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            cafeterias.forEach { cafeteria ->
                DropdownMenuItem(
                    text = { Text(listOfNotNull(cafeteria.nombre, cafeteria.escuela?.nombre).joinToString(" · ")) },
                    onClick = {
                        expanded = false
                        onSelect(cafeteria.id)
                    }
                )
            }
        }
    }
}
