package com.example.cafeenlinea.ui.vendor.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.TextoBusqueda
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.ui.vendor.model.ProductForm
import com.example.cafeenlinea.ui.vendor.model.sugerirCategorias

/**
 * Formulario para crear o editar un producto, en una hoja que sube desde abajo.
 * La categoría se escribe libremente y se sugieren las que ya tiene el menú.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductFormSheet(
    form: ProductForm,
    categorias: List<String>,
    onChange: ((ProductForm) -> ProductForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        // Mientras se guarda no se puede cerrar deslizando
        confirmValueChange = { !form.guardando }
    )
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onChange { it.copy(imagenLocal = uri.toString()) }
    }
    val enabled = !form.guardando
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(if (form.esNuevo) R.string.vendor_form_new else R.string.vendor_form_edit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(16.dp))

            // --- FOTO ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductThumb(form.imagenVisible, Modifier.size(88.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    OutlinedButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = enabled
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                if (form.imagenVisible == null) R.string.vendor_form_photo_add
                                else R.string.vendor_form_photo_change
                            )
                        )
                    }
                    if (form.imagenVisible != null) {
                        TextButton(
                            onClick = { onChange { it.copy(imagenLocal = null, imagenUrl = null) } },
                            enabled = enabled
                        ) { Text(stringResource(R.string.vendor_form_photo_remove), color = colors.error) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            FormField(
                value = form.nombre,
                onValueChange = { v -> onChange { it.copy(nombre = v, errores = it.errores.copy(nombre = null)) } },
                label = stringResource(R.string.vendor_form_name),
                error = form.errores.nombre,
                enabled = enabled,
                capitalization = KeyboardCapitalization.Sentences
            )

            FormField(
                value = form.precio,
                onValueChange = { v ->
                    onChange { it.copy(precio = v.filter { c -> c.isDigit() || c == '.' || c == ',' }, errores = it.errores.copy(precio = null)) }
                },
                label = stringResource(R.string.vendor_form_price, form.moneda),
                error = form.errores.precio,
                enabled = enabled,
                keyboardType = KeyboardType.Decimal
            )

            // --- CATEGORÍA con sugerencias ---
            val sugerencias = sugerirCategorias(categorias, form.categoria)
            val esNueva = form.categoria.isNotBlank() &&
                categorias.none { TextoBusqueda.normalizar(it) == TextoBusqueda.normalizar(form.categoria) }
            FormField(
                value = form.categoria,
                onValueChange = { v -> onChange { it.copy(categoria = v, errores = it.errores.copy(categoria = null)) } },
                label = stringResource(R.string.vendor_form_category),
                error = form.errores.categoria,
                hint = stringResource(
                    if (esNueva && sugerencias.isEmpty()) R.string.vendor_form_category_new
                    else R.string.vendor_form_category_hint
                ),
                enabled = enabled,
                capitalization = KeyboardCapitalization.Sentences
            )
            if (sugerencias.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    sugerencias.forEach { categoria ->
                        AssistChip(
                            onClick = { onChange { it.copy(categoria = categoria, errores = it.errores.copy(categoria = null)) } },
                            label = { Text(categoria) },
                            enabled = enabled
                        )
                    }
                }
            }

            FormField(
                value = form.descripcion,
                onValueChange = { v -> onChange { it.copy(descripcion = v) } },
                label = stringResource(R.string.vendor_form_description),
                enabled = enabled,
                singleLine = false,
                capitalization = KeyboardCapitalization.Sentences
            )

            FormField(
                value = form.tiempoPreparacion,
                onValueChange = { v ->
                    onChange { it.copy(tiempoPreparacion = v.filter(Char::isDigit).take(3), errores = it.errores.copy(tiempo = null)) }
                },
                label = stringResource(R.string.vendor_form_time),
                error = form.errores.tiempo,
                enabled = enabled,
                keyboardType = KeyboardType.Number
            )

            // El inventario inicial solo al crear; al editar se cambia desde Inventario
            if (form.esNuevo) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        value = form.stockInicial,
                        onValueChange = { v ->
                            onChange { it.copy(stockInicial = v.filter(Char::isDigit).take(4), errores = it.errores.copy(stock = null)) }
                        },
                        label = stringResource(R.string.vendor_form_stock),
                        error = form.errores.stock,
                        enabled = enabled,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    FormField(
                        value = form.puntoReorden,
                        onValueChange = { v ->
                            onChange { it.copy(puntoReorden = v.filter(Char::isDigit).take(4), errores = it.errores.copy(puntoReorden = null)) }
                        },
                        label = stringResource(R.string.vendor_form_reorder),
                        error = form.errores.puntoReorden,
                        enabled = enabled,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    stringResource(R.string.vendor_form_stock_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            FormField(
                value = form.etiqueta,
                onValueChange = { v -> onChange { it.copy(etiqueta = v.take(20)) } },
                label = stringResource(R.string.vendor_form_tag),
                hint = stringResource(R.string.vendor_form_tag_hint),
                enabled = enabled,
                capitalization = KeyboardCapitalization.Sentences
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.vendor_form_featured),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = form.destacado,
                    onCheckedChange = { v -> onChange { it.copy(destacado = v) } },
                    enabled = enabled
                )
            }

            form.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = colors.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                enabled = enabled,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (form.guardando) CafeLoading(size = 28.dp)
                else Text(stringResource(R.string.vendor_form_save), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: Int? = null,
    hint: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        isError = error != null,
        supportingText = when {
            error != null -> { { Text(stringResource(error)) } }
            hint != null -> { { Text(hint) } }
            else -> null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    )
}
