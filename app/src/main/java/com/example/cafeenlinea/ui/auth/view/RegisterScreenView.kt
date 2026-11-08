package com.example.cafeenlinea.ui.auth.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.ui.auth.viewmodel.RegisterViewModel
import com.example.cafeenlinea.ui.auth.viewmodel.PASSWORD_MIN_LENGTH

private val BgWhite = Color(0xFFFFFFFF)
private val Ink = Color(0xFF1B3A2B)
private val Brand = Color(0xFF2D5A3D)
private val Muted = Color(0xFF8DB596)
private val HairlineBorder = Color(0xFFE3E7E4)
private val ErrorRed = Color(0xFFC1443A)

@Composable
private fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Brand,
    unfocusedBorderColor = HairlineBorder,
    focusedLabelColor = Brand,
    cursorColor = Brand,
    errorBorderColor = ErrorRed
)

/**
 * Crear cuenta: nombre, correo, contraseña (dos veces), escuela y cafetería.
 * Al terminar, el usuario ya queda con la sesión iniciada.
 */
@Composable
fun RegisterScreenView(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val enabled = !uiState.isLoading

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) onRegisterSuccess()
    }

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus()
        viewModel.register()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Ink
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.register_title),
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.register_subtitle),
                color = Muted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- NOMBRE ---
            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = viewModel::onNombreChanged,
                label = { Text(stringResource(R.string.register_name_label)) },
                singleLine = true,
                enabled = enabled,
                isError = uiState.nombreError != null,
                supportingText = uiState.nombreError?.let { { FieldError(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- CORREO ---
            OutlinedTextField(
                value = uiState.correo,
                onValueChange = viewModel::onCorreoChanged,
                label = { Text(stringResource(R.string.login_email_label)) },
                singleLine = true,
                enabled = enabled,
                isError = uiState.correoError != null,
                supportingText = uiState.correoError?.let { { FieldError(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- CONTRASEÑA y CONFIRMACIÓN ---
            val passwordTextStyle = if (passwordVisible) {
                LocalTextStyle.current
            } else {
                LocalTextStyle.current.copy(letterSpacing = PasswordDotSpacing)
            }
            val passwordTransformation =
                if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
            val passwordKeyboard = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text(stringResource(R.string.login_password_label)) },
                singleLine = true,
                enabled = enabled,
                isError = uiState.passwordError != null,
                supportingText = {
                    val error = uiState.passwordError
                    if (error != null) {
                        FieldError(error)
                    } else {
                        Text(stringResource(R.string.register_password_hint, PASSWORD_MIN_LENGTH), color = Muted, fontSize = 12.sp)
                    }
                },
                keyboardOptions = passwordKeyboard,
                textStyle = passwordTextStyle,
                visualTransformation = passwordTransformation,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) R.string.cd_hide_password else R.string.cd_show_password
                            ),
                            tint = Muted
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChanged,
                label = { Text(stringResource(R.string.register_confirm_password_label)) },
                singleLine = true,
                enabled = enabled,
                isError = uiState.confirmPasswordError != null,
                supportingText = uiState.confirmPasswordError?.let { { FieldError(it) } },
                keyboardOptions = passwordKeyboard.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                textStyle = passwordTextStyle,
                visualTransformation = passwordTransformation,
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- ESCUELA y CAFETERÍA ---
            CatalogDropdown(
                label = stringResource(R.string.register_school_label),
                options = uiState.escuelas,
                selected = uiState.escuelaSeleccionada,
                optionLabel = { it.nombre },
                onSelected = viewModel::onEscuelaSelected,
                enabled = enabled && uiState.escuelas.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            CatalogDropdown(
                label = stringResource(R.string.register_cafeteria_label),
                options = uiState.cafeterias,
                selected = uiState.cafeteriaSeleccionada,
                optionLabel = { it.nombre },
                onSelected = viewModel::onCafeteriaSelected,
                enabled = enabled && uiState.cafeterias.isNotEmpty()
            )

            // Estado de la carga de escuelas/cafeterías
            when {
                uiState.isLoadingCatalogos -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.login_connecting), color = Muted, fontSize = 12.sp)
                }
                uiState.catalogosError != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = uiState.catalogosError!!,
                            color = ErrorRed,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::retryCatalogos) {
                            Text(stringResource(R.string.action_retry), color = Brand)
                        }
                    }
                }
                uiState.escuelaSeleccionada != null && uiState.cafeterias.isEmpty() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.register_no_cafeterias), color = Muted, fontSize = 12.sp)
                }
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = uiState.error!!, color = ErrorRed, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÓN CREAR CUENTA ---
            Button(
                onClick = ::submit,
                enabled = uiState.isButtonEnabled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Brand,
                    contentColor = Color.White,
                    disabledContainerColor = Brand.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CafeLoading(size = 28.dp)
                } else {
                    Text(stringResource(R.string.register_button), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.register_have_account), color = Muted, fontSize = 14.sp)
                TextButton(onClick = onBack, enabled = enabled) {
                    Text(stringResource(R.string.register_go_login), color = Brand, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun FieldError(message: String) {
    Text(text = message, color = ErrorRed, fontSize = 12.sp)
}

/** Lista desplegable de solo lectura para elegir escuela o cafetería. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CatalogDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.let(optionLabel).orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(10.dp),
            colors = fieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}
