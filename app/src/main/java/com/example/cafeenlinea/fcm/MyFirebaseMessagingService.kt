package com.example.cafeenlinea.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.cafeenlinea.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_SERVICE"
        const val CHANNEL_ID = "pedidos_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo FCM Token recibido: $token")
        // Aquí se enviará el token al backend cuando esté listo el endpoint
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje recibido de: ${remoteMessage.from}")

        // Manejo de mensajes de notificación
        remoteMessage.notification?.let {
            Log.d(TAG, "Notificación - Título: ${it.title}, Cuerpo: ${it.body}")
            mostrarNotificacion(it.title ?: getString(R.string.notification_default_title), it.body ?: "")
        }

        // Manejo de mensajes de datos (Data)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Payload de datos: ${remoteMessage.data}")
            if (remoteMessage.notification == null) {
                val titulo = remoteMessage.data["title"] ?: getString(R.string.notification_order_update_title)
                val mensaje = remoteMessage.data["body"] ?: getString(R.string.notification_order_update_body)
                mostrarNotificacion(titulo, mensaje)
            }
        }
    }

    private fun mostrarNotificacion(titulo: String, mensaje: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_cafe)
            .setColor(ContextCompat.getColor(this, R.color.cafe_ink))
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}