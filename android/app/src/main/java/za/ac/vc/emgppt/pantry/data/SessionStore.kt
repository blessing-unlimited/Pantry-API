package za.ac.vc.emgppt.pantry.data

import android.content.Context
import android.util.Log

/**
 * Lightweight session persistence for the SSO token. EncryptedSharedPreferences
 * can replace this in the final POE (Android Developers, 2024).
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("pantry_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
            Log.i("Pantry", "Session token ${if (value == null) "cleared" else "saved"}")
        }

    var displayName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) {
            prefs.edit().putString(KEY_NAME, value).apply()
        }

    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) {
            prefs.edit().putString(KEY_EMAIL, value).apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
    }
}
