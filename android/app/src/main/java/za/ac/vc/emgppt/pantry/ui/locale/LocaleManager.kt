package za.ac.vc.emgppt.pantry.ui.locale

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies English, isiZulu, or Afrikaans for the whole app. Per-app locales
 * are the supported API from Android 13 backward through AppCompat
 * (Android Developers, 2026).
 */
object LocaleManager {
    fun apply(languageCode: String?) {
        val code = when (languageCode?.lowercase()) {
            "zu" -> "zu"
            "af" -> "af"
            else -> "en"
        }
        val locales = LocaleListCompat.forLanguageTags(code)
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.toLanguageTags() == locales.toLanguageTags()) {
            return
        }
        Log.i("Pantry", "Applying locale $code")
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
