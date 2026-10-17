package com.example.cafeenlinea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cafeenlinea.ui.navigation.AppNavHost
import com.example.cafeenlinea.ui.theme.CafeEnLineaTheme

/** Entry point activity. Hosts [AppNavHost] inside [CafeEnLineaTheme]. */

class MainActivity : ComponentActivity() {
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