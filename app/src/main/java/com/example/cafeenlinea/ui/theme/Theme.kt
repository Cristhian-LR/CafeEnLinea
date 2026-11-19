package com.example.cafeenlinea.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = GreenDarkest,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenLightest,
    secondary = GreenLightest,
    onSecondary = GreenDarkest,
    secondaryContainer = GreenMedium,
    onSecondaryContainer = GreenLightest,
    tertiary = GreenLightest,
    onTertiary = GreenDarkest,
    tertiaryContainer = GreenDarkest,
    onTertiaryContainer = GreenLightest,
    background = GreenBlack,
    onBackground = GreenLightest,
    surface = GreenBlack,
    onSurface = GreenLightest,
    surfaceVariant = GreenDarkest,
    onSurfaceVariant = GreenLight,
    surfaceContainerLowest = GreenBlack,
    surfaceContainerLow = GreenDarkest,
    surfaceContainer = GreenDarkest,
    surfaceContainerHigh = GreenDarkest,
    surfaceContainerHighest = GreenDark,
    outline = GreenMedium,
    outlineVariant = GreenDark
)

private val LightColorScheme = lightColorScheme(
    primary = GreenDark,
    onPrimary = Color.White,
    primaryContainer = GreenLightest,
    onPrimaryContainer = GreenDarkest,
    secondary = GreenMedium,
    onSecondary = Color.White,
    secondaryContainer = GreenLight,
    onSecondaryContainer = GreenDarkest,
    tertiary = GreenDarkest,
    onTertiary = Color.White,
    tertiaryContainer = GreenLight,
    onTertiaryContainer = GreenDarkest,
    background = GreenWhite,
    onBackground = GreenDarkest,
    surface = GreenWhite,
    onSurface = GreenDarkest,
    surfaceVariant = GreenLightest,
    onSurfaceVariant = GreenMedium,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = GreenWhite,
    surfaceContainer = GreenLightest,
    surfaceContainerHigh = GreenLightest,
    surfaceContainerHighest = GreenLightest,
    outline = GreenLight,
    outlineVariant = GreenLightest
)

// Modo claro de las pantallas de adentro (inicio, pedidos, carrito, perfil):
// verde salvia con fondos crema y neutros cálidos.
private val SoftLightColorScheme = lightColorScheme(
    primary = SageDark,
    onPrimary = Color.White,
    primaryContainer = SageMist,
    onPrimaryContainer = SageDeep,
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = Taupe,
    onSecondaryContainer = SageDeep,
    tertiary = WarmGray,
    onTertiary = Color.White,
    tertiaryContainer = Cream,
    onTertiaryContainer = SageDeep,
    background = CreamLight,
    onBackground = SageDeep,
    surface = CreamLight,
    onSurface = SageDeep,
    surfaceVariant = Sand,
    onSurfaceVariant = WarmGray,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = CreamLight,
    surfaceContainer = Cream,
    surfaceContainerHigh = Cream,
    surfaceContainerHighest = Cream,
    outline = Stone,
    outlineVariant = Taupe
)

// Modo oscuro de las pantallas de adentro: la misma paleta salvia + crema.
private val SoftDarkColorScheme = darkColorScheme(
    primary = SageLight,
    onPrimary = Night,
    primaryContainer = SageNight,
    onPrimaryContainer = SageMist,
    secondary = Khaki,
    onSecondary = Night,
    secondaryContainer = SageNight,     // chips seleccionados y botón activo de la barra
    onSecondaryContainer = CreamSoft,
    tertiary = Taupe,
    onTertiary = Night,
    tertiaryContainer = Umber,
    onTertiaryContainer = CreamSoft,
    background = Night,
    onBackground = CreamSoft,
    surface = Night,
    onSurface = CreamSoft,
    surfaceVariant = NightHigh,
    onSurfaceVariant = TaupeMuted,
    surfaceContainerLowest = NightLowest,
    surfaceContainerLow = NightLow,
    surfaceContainer = NightBar,
    surfaceContainerHigh = NightCard,
    surfaceContainerHighest = NightCard,
    outline = StoneDark,
    outlineVariant = Umber
)

/**
 * @param softLight usa la paleta salvia + crema (pantallas de adentro), en claro y en oscuro.
 * El onboarding pasa false para conservar sus colores originales.
 */
@Composable
fun CafeEnLineaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    softLight: Boolean = true,
    // Dynamic color (Android 12+) is off so the app always uses the green palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme && softLight -> SoftDarkColorScheme
        darkTheme -> DarkColorScheme
        softLight -> SoftLightColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}