package com.example.cafeenlinea.ui.auth.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.data.remote.Escuela
import com.example.cafeenlinea.ui.auth.model.ESCUELA_MIN_LENGTH
import com.example.cafeenlinea.ui.auth.model.EscuelaSeleccion

/**
 * Campo de escuela que se escribe libremente. Mientras se escribe, despliega las
 * escuelas del catálogo que se parecen; si es nueva, aparece el campo de ciudad.
 *
 * @param onDone Se llama al terminar de escribir (tecla "Listo" del teclado).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EscuelaField(
    seleccion: EscuelaSeleccion,
    enabled: Boolean,
    onTextChanged: (String) -> Unit,
    onSuggestionSelected: (Escuela) -> Unit,
    onCiudadChanged: (String) -> Unit,
    onRetryCatalog: () -> Unit,
    onDone: () -> Unit,
    label: String = stringResource(R.string.register_school_label)
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val sugerencias = seleccion.sugerencias
    val expanded = enabled && focused && !dismissed && sugerencias.isNotEmpty()
    val colors = MaterialTheme.colorScheme

    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { dismissed = !it }) {
            OutlinedTextField(
                value = seleccion.texto,
                onValueChange = {
                    dismissed = false
                    onTextChanged(it)
                },
                label = { Text(label) },
                singleLine = true,
                enabled = enabled,
                isError = seleccion.error != null,
                supportingText = {
                    val elegida = seleccion.seleccionada
                    when {
                        seleccion.error != null -> Text(
                            stringResource(seleccion.error, ESCUELA_MIN_LENGTH),
                            color = colors.error,
                            fontSize = 12.sp
                        )
                        elegida != null -> Text(
                            stringResource(R.string.register_school_found, elegida.ciudad.ifBlank { elegida.clave }),
                            color = colors.primary,
                            fontSize = 12.sp
                        )
                        seleccion.esNueva && seleccion.texto.trim().length >= ESCUELA_MIN_LENGTH -> Hint(
                            stringResource(R.string.register_school_new)
                        )
                        else -> Hint(stringResource(R.string.register_school_hint))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = if (seleccion.esNueva) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    // Si es nueva, el teclado pasa a la ciudad
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { onDone() }
                ),
                shape = RoundedCornerShape(10.dp),
                colors = authFieldColors(),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled)
                    .onFocusChanged { focused = it.isFocused }
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { dismissed = true }) {
                sugerencias.forEach { escuela ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(escuela.nombre, color = colors.onSurface)
                                if (escuela.ciudad.isNotBlank()) {
                                    Text(escuela.ciudad, color = colors.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                        },
                        onClick = {
                            dismissed = true
                            onSuggestionSelected(escuela)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(visible = seleccion.esNueva) {
            Column {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = seleccion.ciudad,
                    onValueChange = onCiudadChanged,
                    label = { Text(stringResource(R.string.register_city_label)) },
                    singleLine = true,
                    enabled = enabled,
                    isError = seleccion.ciudadError != null,
                    supportingText = seleccion.ciudadError?.let {
                        { Text(stringResource(it), color = colors.error, fontSize = 12.sp) }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    shape = RoundedCornerShape(10.dp),
                    colors = authFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // El catálogo solo da sugerencias: si falla, se puede seguir igual
        when {
            seleccion.cargando -> Hint(stringResource(R.string.login_connecting))
            seleccion.catalogoError != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    seleccion.catalogoError,
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRetryCatalog) {
                    Text(stringResource(R.string.action_retry), color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
}
