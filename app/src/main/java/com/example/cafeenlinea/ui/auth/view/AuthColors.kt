package com.example.cafeenlinea.ui.auth.view

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

/**
 * Colores de login y registro tomados del tema (paleta salvia + crema), para que
 * las dos pantallas se vean como el resto de la app en modo claro y oscuro.
 */
@Composable
internal fun authFieldColors(): TextFieldColors {
    val colors = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.outlineVariant,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onSurfaceVariant,
        focusedTextColor = colors.onBackground,
        unfocusedTextColor = colors.onBackground,
        cursorColor = colors.primary,
        errorBorderColor = colors.error,
        errorLabelColor = colors.error,
        errorCursorColor = colors.error
    )
}

@Composable
internal fun authButtonColors(): ButtonColors {
    val colors = MaterialTheme.colorScheme
    return ButtonDefaults.buttonColors(
        containerColor = colors.primary,
        contentColor = colors.onPrimary,
        disabledContainerColor = colors.primary.copy(alpha = 0.4f),
        disabledContentColor = colors.onPrimary.copy(alpha = 0.7f)
    )
}
