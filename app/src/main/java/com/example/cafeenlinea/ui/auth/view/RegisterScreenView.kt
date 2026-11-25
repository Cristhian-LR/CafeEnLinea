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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafeenlinea.R
import com.example.cafeenlinea.common.components.CafeLoading
import com.example.cafeenlinea.ui.auth.model.PASSWORD_MIN_LENGTH
import com.example.cafeenlinea.ui.auth.viewmodel.RegisterViewModel

/**
 * Crear cuenta: nombre, correo, contraseña (dos veces) y escuela. La escuela se
 * escribe a mano con sugerencias del catálogo; si es nueva se pide su ciudad.
 * Al terminar, el usuario ya queda con la sesión iniciada.
 */
@Composable
fun RegisterScreenView(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit,
    onRegisterBusiness: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
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
            .background(colors.background)
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
                    tint = colors.onBackground
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.register_title),
                color = colors.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.register_subtitle),
                color = colors.onSurfaceVariant,
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
                colors = authFieldColors(),
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
                colors = authFieldColors(),
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
                        FieldHint(stringResource(R.string.register_password_hint, PASSWORD_MIN_LENGTH))
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
                            tint = colors.onSurfaceVariant
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = authFieldColors(),
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
                keyboardOptions = passwordKeyboard,
                textStyle = passwordTextStyle,
                visualTransformation = passwordTransformation,
                shape = RoundedCornerShape(10.dp),
                colors = authFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- ESCUELA (con sugerencias) y CIUDAD si es nueva ---
            EscuelaField(
                seleccion = uiState.escuela,
                enabled = enabled,
                onTextChanged = viewModel::onEscuelaTextoChanged,
                onSuggestionSelected = viewModel::onEscuelaSugeridaSelected,
                onCiudadChanged = viewModel::onCiudadChanged,
                onRetryCatalog = viewModel::loadEscuelas,
                onDone = ::submit
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = uiState.error!!, color = colors.error, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÓN CREAR CUENTA ---
            Button(
                onClick = ::submit,
                enabled = uiState.isButtonEnabled,
                shape = RoundedCornerShape(10.dp),
                colors = authButtonColors(),
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
                Text(stringResource(R.string.register_have_account), color = colors.onSurfaceVariant, fontSize = 14.sp)
                TextButton(onClick = onBack, enabled = enabled) {
                    Text(stringResource(R.string.register_go_login), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }

            // Los dueños de cafeterías se registran por otro camino (como Uber Eats o Rappi Aliados)
            HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
            TextButton(
                onClick = onRegisterBusiness,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = colors.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.register_go_business), color = colors.primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
internal fun FieldError(message: String) {
    Text(text = message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
}

@Composable
internal fun FieldHint(message: String) {
    Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
}
