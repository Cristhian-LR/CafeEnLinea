package com.example.cafeenlinea.ui.auth.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.ui.auth.model.PASSWORD_MIN_LENGTH
import com.example.cafeenlinea.ui.auth.viewmodel.BusinessRegisterViewModel

/**
 * "Registra tu cafetería": datos de la cafetería y de la cuenta de su dueño.
 * La cafetería queda en revisión y no la ven los alumnos hasta que se aprueba.
 */
@Composable
fun BusinessRegisterScreenView(
    viewModel: BusinessRegisterViewModel,
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val enabled = !uiState.isLoading

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) onRegistered()
    }

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus()
        viewModel.register()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = colors.onBackground)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.business_title),
                color = colors.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.business_subtitle), color = colors.onSurfaceVariant, fontSize = 14.sp)

            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = colors.secondaryContainer) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.business_review_note),
                        color = colors.onSecondaryContainer,
                        fontSize = 13.sp
                    )
                }
            }

            // --- LA CAFETERÍA ---
            SectionLabel(stringResource(R.string.business_section_cafeteria))
            AuthField(
                value = uiState.nombreNegocio,
                onValueChange = { v -> viewModel.actualizar { it.copy(nombreNegocio = v, negocioError = null) } },
                label = stringResource(R.string.business_name_label),
                error = uiState.negocioError?.let { stringResource(it) },
                hint = stringResource(R.string.business_name_hint),
                enabled = enabled,
                capitalization = KeyboardCapitalization.Words
            )
            AuthField(
                value = uiState.nombreCafeteria,
                onValueChange = { v -> viewModel.actualizar { it.copy(nombreCafeteria = v) } },
                label = stringResource(R.string.business_cafeteria_label),
                hint = stringResource(R.string.business_cafeteria_hint),
                enabled = enabled,
                capitalization = KeyboardCapitalization.Words
            )
            EscuelaField(
                seleccion = uiState.escuela,
                enabled = enabled,
                onTextChanged = viewModel::onEscuelaTextoChanged,
                onSuggestionSelected = viewModel::onEscuelaSugeridaSelected,
                onCiudadChanged = viewModel::onCiudadChanged,
                onRetryCatalog = viewModel::loadEscuelas,
                onDone = { focusManager.clearFocus() },
                label = stringResource(R.string.business_school_label)
            )
            Spacer(Modifier.height(8.dp))
            AuthField(
                value = uiState.ubicacion,
                onValueChange = { v -> viewModel.actualizar { it.copy(ubicacion = v) } },
                label = stringResource(R.string.business_location_label),
                hint = stringResource(R.string.business_location_hint),
                enabled = enabled,
                capitalization = KeyboardCapitalization.Sentences
            )

            // --- TU CUENTA ---
            SectionLabel(stringResource(R.string.business_section_account))
            val cuenta = uiState.cuentaErrores
            AuthField(
                value = uiState.nombre,
                onValueChange = { v -> viewModel.actualizar { it.copy(nombre = v, cuentaErrores = it.cuentaErrores.copy(nombre = null)) } },
                label = stringResource(R.string.business_owner_label),
                error = cuenta.nombre?.let { stringResource(it) },
                enabled = enabled,
                capitalization = KeyboardCapitalization.Words
            )
            AuthField(
                value = uiState.correo,
                onValueChange = { v ->
                    viewModel.actualizar { it.copy(correo = v, correoDuplicado = false, cuentaErrores = it.cuentaErrores.copy(correo = null)) }
                },
                label = stringResource(R.string.login_email_label),
                error = when {
                    uiState.correoDuplicado -> stringResource(R.string.register_error_email_taken)
                    cuenta.correo != null -> stringResource(cuenta.correo)
                    else -> null
                },
                enabled = enabled,
                keyboardType = KeyboardType.Email
            )
            AuthField(
                value = uiState.password,
                onValueChange = { v -> viewModel.actualizar { it.copy(password = v, cuentaErrores = it.cuentaErrores.copy(password = null)) } },
                label = stringResource(R.string.login_password_label),
                error = cuenta.password?.let { stringResource(it, PASSWORD_MIN_LENGTH) },
                hint = stringResource(R.string.register_password_hint, PASSWORD_MIN_LENGTH),
                enabled = enabled,
                keyboardType = KeyboardType.Password,
                password = true,
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible }
            )
            AuthField(
                value = uiState.confirmPassword,
                onValueChange = { v ->
                    viewModel.actualizar { it.copy(confirmPassword = v, cuentaErrores = it.cuentaErrores.copy(confirmPassword = null)) }
                },
                label = stringResource(R.string.register_confirm_password_label),
                error = cuenta.confirmPassword?.let { stringResource(it) },
                enabled = enabled,
                keyboardType = KeyboardType.Password,
                password = true,
                passwordVisible = passwordVisible,
                imeAction = ImeAction.Done,
                onDone = ::submit
            )

            uiState.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = colors.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = ::submit,
                enabled = uiState.isButtonEnabled,
                shape = RoundedCornerShape(10.dp),
                colors = authButtonColors(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState.isLoading) CafeLoading(size = 28.dp)
                else Text(stringResource(R.string.business_submit), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
    Spacer(Modifier.height(8.dp))
}

/** Campo de texto con los colores del login; muestra el error o la ayuda debajo. */
@Composable
internal fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    error: String? = null,
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    password: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    val oculto = password && !passwordVisible
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = when {
            error != null -> { { FieldError(error) } }
            hint != null -> { { FieldHint(hint) } }
            else -> null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            autoCorrectEnabled = !password && keyboardType != KeyboardType.Email,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        textStyle = if (oculto) LocalTextStyle.current.copy(letterSpacing = PasswordDotSpacing) else LocalTextStyle.current,
        visualTransformation = if (oculto) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = onTogglePassword?.let {
            {
                IconButton(onClick = it) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.cd_hide_password else R.string.cd_show_password
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(10.dp),
        colors = authFieldColors(),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
}
