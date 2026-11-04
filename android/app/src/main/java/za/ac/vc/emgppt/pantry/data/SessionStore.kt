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

    var userId: String?
        get() = prefs.getString(KEY_USER, null)
        set(value) {
            prefs.edit().putString(KEY_USER, value).apply()
        }

    /** True after the user enrols fingerprint or face unlock (Android Developers, 2025). */
    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) {
            prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()
            Log.i("Pantry", "Biometric enrolment set to $value")
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_USER = "userId"
        const val KEY_BIOMETRIC = "biometricEnabled"
    }
}
