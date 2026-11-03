package com.example.cafeenlinea

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.cafeenlinea.data.remote.ApiClient
import com.example.cafeenlinea.fcm.SolicitarPermisoFCM
import com.example.cafeenlinea.ui.auth.data.SessionPreferences
import com.example.cafeenlinea.ui.navigation.AppNavHost
import com.example.cafeenlinea.ui.theme.CafeEnLineaTheme

/**
 * Entry point activity. Hosts [AppNavHost] inside [CafeEnLineaTheme].
 *
 * Extiende [FragmentActivity] (que a su vez extiende ComponentActivity) porque
 * BiometricPrompt necesita un FragmentManager para mostrar su diálogo.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si hay una sesión guardada, cargamos su token para que ApiClient lo envíe.
        val session = SessionPreferences(this)
        if (session.isLoggedIn()) ApiClient.token = session.accessToken()

        enableEdgeToEdge()
        setContent {
            CafeEnLineaTheme {
                // Solicita el permiso POST_NOTIFICATIONS en Android 13+ e imprime el FCM Token en Logcat
                SolicitarPermisoFCM()

                AppNavHost()
            }
        }
    }
}