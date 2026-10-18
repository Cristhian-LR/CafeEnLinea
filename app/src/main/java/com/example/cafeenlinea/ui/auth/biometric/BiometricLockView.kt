package com.example.cafeenlinea.ui.auth.biometric

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cafeenlinea.R
import com.example.cafeenlinea.ui.theme.GreenDark
import com.example.cafeenlinea.ui.theme.GreenDarkest
import com.example.cafeenlinea.ui.theme.GreenLight

private val ErrorRed = Color(0xFFC1443A)

/**
 * Candado que aparece al abrir la app cuando hay sesión y la biometría está
 * activa. Lanza el diálogo de huella/rostro en cuanto se muestra.
 *
 * - Éxito → [onUnlocked] (ir al Home).
 * - "Usar contraseña" → [onUsePassword] (ir al Login).
 * - Cancelar o error → se queda aquí con el mensaje, y desde aquí puede
 *   reintentar o entrar con contraseña.
 */
@Composable
fun BiometricLockView(
    onUnlocked: () -> Unit,
    onUsePassword: () -> Unit
) {
    val activity = LocalContext.current.findFragmentActivity()
    var error by remember { mutableStateOf<String?>(null) }

    fun authenticate() {
        if (activity == null) {
            onUsePassword()
            return
        }
        error = null
        showBiometricPrompt(
            activity = activity,
            title = "Entrar a Café en Línea",
            subtitle = "Usa tu huella o rostro"
        ) { result ->
            when (result) {
                BiometricResult.Success -> onUnlocked()
                BiometricResult.UsePassword -> onUsePassword()
                is BiometricResult.Error -> error = result.message
            }
        }
    }

    LaunchedEffect(Unit) { authenticate() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.cafe_express),
            contentDescription = "Logo de Café en Línea",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Café en Línea",
            color = GreenDarkest,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Confirma que eres tú para continuar",
            color = GreenLight,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Filled.Fingerprint,
            contentDescription = null,
            tint = GreenDark,
            modifier = Modifier.size(64.dp)
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error!!,
                color = ErrorRed,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { authenticate() },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenDark,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Usar huella o rostro", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onUsePassword) {
            Text("Entrar con contraseña", color = GreenDark)
        }
    }
}
