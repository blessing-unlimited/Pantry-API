package za.ac.vc.emgppt.pantry.data.blob

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Stores a scan photo as an object in Firebase Storage and returns a download
 * URL. The URL is then saved with the pantry item in Realtime Database through
 * the custom API (Firebase, 2026).
 */
object ScanImageStore {
    suspend fun upload(context: Context, userId: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) {
            return null
        }
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.w("Pantry", "Blob upload skipped: Firebase is not configured on this build")
            return null
        }
        val safeUser = userId.ifBlank { "anonymous" }.replace("/", "_")
        val path = "users/$safeUser/scans/${UUID.randomUUID()}.jpg"
        return try {
            val ref = FirebaseStorage.getInstance().reference.child(path)
            ref.putBytes(bytes).await()
            val url = ref.downloadUrl.await().toString()
            Log.i("Pantry", "Blob stored at $path")
            url
        } catch (ex: Exception) {
            Log.e("Pantry", "Blob upload failed", ex)
            null
        }
    }
}
