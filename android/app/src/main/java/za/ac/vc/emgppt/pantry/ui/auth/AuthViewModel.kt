package za.ac.vc.emgppt.pantry.ui.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import za.ac.vc.emgppt.pantry.BuildConfig
import za.ac.vc.emgppt.pantry.R
import za.ac.vc.emgppt.pantry.data.ServiceLocator
import za.ac.vc.emgppt.pantry.data.api.SsoRequest
import za.ac.vc.emgppt.pantry.messaging.PantryNotifications
import za.ac.vc.emgppt.pantry.security.BiometricGate

data class AuthUiState(
    val loading: Boolean = false,
    val signedIn: Boolean = false,
    val isNewUser: Boolean = false,
    val locked: Boolean = false,
    val offerBiometric: Boolean = false,
    val error: String? = null,
    val displayName: String? = null,
)

/**
 * Google SSO is the production path (Firebase, 2026). A saved session can
 * then be gated with BiometricPrompt (Android Developers, 2025). Debug builds
 * may fall back to the API development token.
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
            val locked = ServiceLocator.session.biometricEnabled
            _state.value = AuthUiState(
                signedIn = true,
                locked = locked,
                displayName = ServiceLocator.session.displayName
            )
            Log.i("Pantry", "Saved session found; biometricLock=$locked")
            if (!locked) {
                hydrate(token)
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
                        _state.value = _state.value.copy(
                            loading = false,
                            error = ServiceLocator.appContext.getString(R.string.google_token_missing)
                        )
                    }
                    return@launch
                }
                signInWithToken(idToken, account.displayName, account.email)
            } catch (ex: Exception) {
                Log.e("Pantry", "Google SSO failed", ex)
                if (BuildConfig.ALLOW_DEV_SSO) {
                    signInWithToken("dev-token", "Sihle", "sihle@pantry.local")
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = ex.message ?: ServiceLocator.appContext.getString(R.string.google_sign_in_failed)
                    )
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

    fun acceptBiometric() {
        ServiceLocator.session.biometricEnabled = true
        _state.value = _state.value.copy(offerBiometric = false, locked = false)
        Log.i("Pantry", "Biometric unlock enabled for this account")
    }

    fun skipBiometric() {
        _state.value = _state.value.copy(offerBiometric = false)
        Log.i("Pantry", "Biometric enrolment skipped")
    }

    fun unlock() {
        _state.value = _state.value.copy(locked = false)
        Log.i("Pantry", "Biometric gate opened")
        viewModelScope.launch { hydrate(ServiceLocator.session.token) }
    }

    fun signOut() {
        viewModelScope.launch {
            ServiceLocator.repository.clearUserData()
        }
        try {
            if (FirebaseApp.getApps(ServiceLocator.appContext).isNotEmpty()) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (ex: Exception) {
            Log.w("Pantry", "Firebase sign-out skipped: ${ex.message}")
        }
        ServiceLocator.session.clear()
        _state.value = AuthUiState()
        Log.i("Pantry", "Signed out")
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
            ServiceLocator.session.userId = response.data.userId
            Log.i("Pantry", "SSO success newUser=${response.data.isNewUser} id=${response.data.userId}")
            linkFirebase(idToken)
            PantryNotifications.registerToken()
            val offer = !ServiceLocator.session.biometricEnabled &&
                BiometricGate.canAuthenticate(ServiceLocator.appContext)
            _state.value = AuthUiState(
                signedIn = true,
                isNewUser = response.data.isNewUser,
                offerBiometric = offer,
                displayName = response.data.displayName
            )
        } catch (ex: Exception) {
            Log.e("Pantry", "SSO API call failed", ex)
            _state.value = _state.value.copy(
                loading = false,
                error = ServiceLocator.appContext.getString(R.string.cannot_reach_api)
            )
        }
    }

    private suspend fun hydrate(token: String?) {
        if (token.isNullOrBlank()) {
            return
        }
        try {
            val me = ServiceLocator.api.me()
            if (me.success && me.data != null) {
                ServiceLocator.session.userId = me.data.userId
                ServiceLocator.session.displayName = me.data.displayName ?: ServiceLocator.session.displayName
                _state.value = _state.value.copy(
                    signedIn = true,
                    locked = false,
                    isNewUser = !me.data.onboardingCompleted,
                    displayName = me.data.displayName ?: ServiceLocator.session.displayName
                )
                Log.i("Pantry", "Restored session for ${me.data.userId}")
                linkFirebase(token)
                PantryNotifications.registerToken()
            } else {
                ServiceLocator.session.clear()
                _state.value = AuthUiState()
            }
        } catch (ex: HttpException) {
            if (ex.code() == 401) {
                Log.w("Pantry", "Session rejected by the API")
                ServiceLocator.session.clear()
                _state.value = AuthUiState()
            } else {
                Log.w("Pantry", "Session restore kept local cache: ${ex.message}")
                _state.value = _state.value.copy(signedIn = true, locked = false)
            }
        } catch (ex: Exception) {
            Log.w("Pantry", "Session restore failed, continuing offline: ${ex.message}")
            _state.value = _state.value.copy(
                signedIn = true,
                locked = false,
                displayName = ServiceLocator.session.displayName
            )
        }
    }

    private suspend fun linkFirebase(idToken: String) {
        if (idToken == "dev-token") {
            return
        }
        if (FirebaseApp.getApps(ServiceLocator.appContext).isEmpty()) {
            Log.w("Pantry", "FirebaseApp is not initialised; Storage and FCM stay inactive")
            return
        }
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential).await()
            Log.i("Pantry", "Firebase Auth linked for blob storage")
        } catch (ex: Exception) {
            Log.w("Pantry", "Firebase Auth link failed: ${ex.message}")
        }
    }
}
