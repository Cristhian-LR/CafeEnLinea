package com.example.cafeenlinea.ui.profile.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Términos y condiciones. Texto de ejemplo (placeholder): reemplazar con el
 * texto real del negocio/equipo antes de entregar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsView(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Términos y condiciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Text(
            text = """
                Última actualización: octubre 2026

                1. Uso del servicio
                CafeEnLinea permite consultar el menú de la cafetería y armar un pedido dentro de la plataforma. El uso de la app implica la aceptación de estos términos.

                2. Pedidos
                Los precios mostrados pueden cambiar sin previo aviso. La disponibilidad de los productos depende del inventario de la cafetería al momento de confirmar el pedido.

                3. Cuenta de usuario
                Eres responsable de mantener la confidencialidad de tu usuario y contraseña, así como de toda actividad realizada desde tu cuenta.

                4. Privacidad
                La información que proporcionas (nombre, usuario) se usa únicamente para identificarte dentro de la app y gestionar tus pedidos.

                5. Soporte
                Para dudas, quejas o aclaraciones, puedes contactarnos desde la sección de Soporte en tu perfil.

                6. Cambios en estos términos
                Estos términos pueden actualizarse periódicamente. Te notificaremos los cambios relevantes dentro de la app.
            """.trimIndent(),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}