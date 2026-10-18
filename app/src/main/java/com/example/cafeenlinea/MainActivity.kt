package com.example.cafeenlinea

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
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
        enableEdgeToEdge()
        setContent {
            CafeEnLineaTheme {
                AppNavHost()
            }
        }
    }
}