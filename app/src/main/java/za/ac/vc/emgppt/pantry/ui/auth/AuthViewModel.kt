package za.ac.vc.emgppt.pantry.ui.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import za.ac.vc.emgppt.pantry.BuildConfig
import za.ac.vc.emgppt.pantry.R
import za.ac.vc.emgppt.pantry.data.ServiceLocator
import za.ac.vc.emgppt.pantry.data.api.SsoRequest

data class AuthUiState(
    val loading: Boolean = false,
    val signedIn: Boolean = false,
    val isNewUser: Boolean = false,
    val error: String? = null,
    val displayName: String? = null,
)

/**
 * Google SSO is the production path (Firebase, 2026). Debug builds may fall
 * back to the API development token so the prototype can be demonstrated on
 * an emulator before google-services.json is added.
 */
class AuthViewModel : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    init {
        restoreSession()
    }

    fun restoreSession() {
        viewModelScope.launch {
            val token = ServiceLocator.session.token
            if (token.isNullOrBlank()) {
                Log.d("Pantry", "No saved session")
                return@launch
            }
            try {
                val me = ServiceLocator.api.me()
                if (me.success && me.data != null) {
                    _state.value = AuthUiState(
                        signedIn = true,
                        isNewUser = !me.data.onboardingCompleted,
                        displayName = me.data.displayName ?: ServiceLocator.session.displayName
                    )
                    Log.i("Pantry", "Restored session for ${me.data.userId}, onboarding=${!me.data.onboardingCompleted}")
                } else {
                    ServiceLocator.session.clear()
                }
            } catch (ex: Exception) {
                Log.w("Pantry", "Session restore failed: ${ex.message}")
            }
        }
    }

    fun googleSignInOptions(activity: Activity): GoogleSignInOptions {
        val webClientId = activity.getString(R.string.default_web_client_id)
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
        if (!webClientId.startsWith("REPLACE")) {
            builder.requestIdToken(webClientId)
        }
        return builder.build()
    }

    fun completeGoogleSignIn(activity: Activity, data: android.content.Intent?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    if (BuildConfig.ALLOW_DEV_SSO) {
                        Log.w("Pantry", "Google account had no ID token; using development SSO")
                        signInWithToken("dev-token", account.displayName, account.email)
                    } else {
                        _state.value = _state.value.copy(loading = false, error = "Google did not return an ID token.")
                    }
                    return@launch
                }
                signInWithToken(idToken, account.displayName, account.email)
            } catch (ex: Exception) {
                Log.e("Pantry", "Google SSO failed", ex)
                if (BuildConfig.ALLOW_DEV_SSO) {
                    signInWithToken("dev-token", "Sihle", "sihle@pantry.local")
                } else {
                    _state.value = _state.value.copy(loading = false, error = ex.message ?: "Google sign-in failed.")
                }
            }
        }
    }

    fun signInWithDevelopmentSso() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            signInWithToken("dev-token", "Sihle", "sihle@pantry.local")
        }
    }

    private suspend fun signInWithToken(idToken: String, name: String?, email: String?) {
        try {
            val response = ServiceLocator.api.sso(SsoRequest(idToken, name, email))
            if (!response.success || response.data == null) {
                _state.value = _state.value.copy(loading = false, error = response.error ?: "SSO failed.")
                return
            }
            ServiceLocator.session.token = idToken
            ServiceLocator.session.displayName = response.data.displayName
            ServiceLocator.session.email = response.data.email
            Log.i("Pantry", "SSO success newUser=${response.data.isNewUser} id=${response.data.userId}")
            _state.value = AuthUiState(
                signedIn = true,
                isNewUser = response.data.isNewUser,
                displayName = response.data.displayName
            )
        } catch (ex: Exception) {
            Log.e("Pantry", "SSO API call failed", ex)
            _state.value = _state.value.copy(loading = false, error = "Cannot reach the Pantry API. Is it running on localhost:5080?")
        }
    }

    fun signOut() {
        ServiceLocator.session.clear()
        _state.value = AuthUiState()
        Log.i("Pantry", "Signed out")
    }
}
