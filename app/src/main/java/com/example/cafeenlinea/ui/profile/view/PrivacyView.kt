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
 * Aviso de privacidad. Texto de ejemplo (placeholder): reemplazar con el
 * texto real del negocio/equipo antes de entregar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyView(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aviso de privacidad") },
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

                1. Responsable del tratamiento de datos
                CafeEnLinea, como parte de un proyecto académico, es responsable del uso de los datos personales que se recaban a través de esta app.

                2. Datos que recabamos
                Al usar la app recabamos: nombre de usuario, y los pedidos que realizas dentro de la plataforma (productos, cantidades y totales).

                3. Finalidad
                Tus datos se usan únicamente para identificarte dentro de la app, gestionar tu sesión y mostrarte tu historial de pedidos.

                4. No compartimos tu información
                Tus datos no se venden, rentan ni comparten con terceros ajenos a este proyecto.

                5. Almacenamiento
                Tu sesión se guarda localmente en tu dispositivo para que no tengas que iniciar sesión cada vez que abres la app.

                6. Tus derechos
                Puedes cerrar sesión en cualquier momento desde la sección de Perfil, lo cual elimina tu sesión guardada en este dispositivo.

                7. Contacto
                Para dudas sobre el manejo de tus datos, puedes escribirnos desde la sección de Soporte en tu perfil.
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