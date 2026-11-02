package com.example.cafeenlinea.fcm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun SolicitarPermisoFCM() {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("FCM_PERMISOS", "Permiso de notificaciones concedido")
        } else {
            Log.w("FCM_PERMISOS", "Permiso de notificaciones denegado")
        }
    }

    LaunchedEffect(Unit) {
        // 1. Pedir permiso si estás en Android 13 (API 33) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Obtener e imprimir el token de FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_TOKEN", "========================================")
                Log.d("FCM_TOKEN", "TOKEN: ${task.result}")
                Log.d("FCM_TOKEN", "========================================")
            } else {
                Log.w("FCM_TOKEN", "Error al obtener el token", task.exception)
            }
        }
    }
}