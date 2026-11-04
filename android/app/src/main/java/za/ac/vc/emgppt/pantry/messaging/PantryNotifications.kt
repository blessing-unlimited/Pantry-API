package za.ac.vc.emgppt.pantry.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import za.ac.vc.emgppt.pantry.R
import za.ac.vc.emgppt.pantry.data.ServiceLocator
import za.ac.vc.emgppt.pantry.data.api.DeviceTokenRequest

/**
 * Notification channel and device-token registration for Firebase Cloud
 * Messaging (Firebase, 2026).
 */
object PantryNotifications {
    const val CHANNEL_ID = "pantry_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notifications_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = context.getString(R.string.notifications_channel_desc)
        manager.createNotificationChannel(channel)
        Log.i("Pantry", "Notification channel $CHANNEL_ID ready")
    }

    fun show(context: Context, title: String, body: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        Log.i("Pantry", "Notification displayed")
    }

    fun registerToken() {
        val context = ServiceLocator.appContext
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.w("Pantry", "FCM token skipped: Firebase is not configured on this build")
            return
        }
        if (ServiceLocator.session.token.isNullOrBlank()) {
            Log.d("Pantry", "FCM token deferred until the user is signed in")
            return
        }
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("Pantry", "FCM token request failed", task.exception)
                    return@addOnCompleteListener
                }
                upload(task.result)
            }
        } catch (ex: Exception) {
            Log.w("Pantry", "FCM unavailable: ${ex.message}")
        }
    }

    fun upload(token: String) {
        if (token.isBlank() || ServiceLocator.session.token.isNullOrBlank()) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ServiceLocator.api.registerDevice(DeviceTokenRequest(token))
                if (response.success) {
                    Log.i("Pantry", "FCM token registered with the API")
                } else {
                    Log.w("Pantry", "FCM token rejected: ${response.error}")
                }
            } catch (ex: Exception) {
                Log.w("Pantry", "FCM token upload failed: ${ex.message}")
            }
        }
    }
}
