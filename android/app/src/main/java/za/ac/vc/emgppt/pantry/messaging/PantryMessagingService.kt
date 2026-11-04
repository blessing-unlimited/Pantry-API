package za.ac.vc.emgppt.pantry.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Firebase Cloud Messaging payloads and shows them on the pantry
 * channel, including when the app is in the foreground (Firebase, 2026).
 */
class PantryMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.i("Pantry", "FCM onNewToken")
        PantryNotifications.upload(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: getString(za.ac.vc.emgppt.pantry.R.string.app_name)
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        Log.i("Pantry", "FCM message received id=${message.messageId}")
        PantryNotifications.show(this, title, body)
    }
}
