package za.ac.vc.emgppt.pantry.security

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import za.ac.vc.emgppt.pantry.R

/**
 * Fingerprint or face unlock through BiometricPrompt. The app never receives
 * or stores the biometric sample itself (Android Developers, 2025).
 */
object BiometricGate {
    private const val ALLOWED =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun canAuthenticate(context: Context): Boolean {
        val result = BiometricManager.from(context).canAuthenticate(ALLOWED)
        val available = result == BiometricManager.BIOMETRIC_SUCCESS
        Log.i("Pantry", "Biometric canAuthenticate=$available code=$result")
        return available
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(activity.getString(R.string.biometric_cancel))
            .setAllowedAuthenticators(ALLOWED)
            .build()

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Log.i("Pantry", "Biometric authentication succeeded")
                    onResult(true, null)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.w("Pantry", "Biometric error $errorCode: $errString")
                    onResult(false, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    Log.w("Pantry", "Biometric did not match; prompt remains open")
                }
            }
        )
        prompt.authenticate(promptInfo)
    }
}
