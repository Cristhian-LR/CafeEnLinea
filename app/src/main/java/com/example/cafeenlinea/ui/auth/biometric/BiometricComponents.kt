package com.example.cafeenlinea.ui.auth.biometric

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import androidx.compose.ui.res.stringResource
import com.example.cafeenlinea.R

/**
 * Pregunta, justo después del primer login con contraseña, si se quiere entrar
 * con huella o rostro las próximas veces. Cualquiera de las dos respuestas
 * marca la pregunta como hecha para no repetirla.
 */
@Composable
fun BiometricOfferDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        icon = { Icon(Icons.Filled.Fingerprint, contentDescription = null) },
        title = { Text(stringResource(R.string.biometric_offer_title)) },
        text = {
            Text(stringResource(R.string.biometric_offer_text))
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(R.string.biometric_offer_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text(stringResource(R.string.biometric_offer_decline)) }
        }
    )
}

/**
 * Fila del Perfil para activar/desactivar la biometría. Si el teléfono no tiene
 * huella ni rostro registrados no dibuja nada.
 *
 * Para activarla se pide la huella primero: así solo el dueño puede encenderla.
 * Para desactivarla basta con tocar el switch.
 */
@Composable
fun BiometricSettingRow() {
    val context = LocalContext.current
    if (!canUseBiometrics(context)) return

    val sessionPreferences = remember { SessionPreferences(context) }
    var enabled by remember { mutableStateOf(sessionPreferences.isBiometricEnabled()) }

    fun onToggle(checked: Boolean) {
        if (!checked) {
            sessionPreferences.setBiometricEnabled(false)
            enabled = false
            return
        }
        val activity = context.findFragmentActivity() ?: return
        showBiometricPrompt(
            activity = activity,
            title = context.getString(R.string.biometric_enable_title),
            subtitle = context.getString(R.string.biometric_enable_subtitle)
        ) { result ->
            if (result == BiometricResult.Success) {
                sessionPreferences.setBiometricEnabled(true)
                enabled = true
            } else if (result is BiometricResult.Error) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(R.string.biometric_setting_label),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = enabled, onCheckedChange = { onToggle(it) })
    }
}
