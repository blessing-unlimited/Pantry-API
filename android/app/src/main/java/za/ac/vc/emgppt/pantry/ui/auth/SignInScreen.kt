package za.ac.vc.emgppt.pantry.ui.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import za.ac.vc.emgppt.pantry.BuildConfig
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Ivory
import za.ac.vc.emgppt.pantry.ui.theme.PantryBrandMark
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta

@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onSignedIn: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as Activity
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("Pantry", "Google SSO activity result=${result.resultCode}")
        viewModel.completeGoogleSignIn(activity, result.data)
    }

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onSignedIn()
    }

    Column(modifier = Modifier.fillMaxSize().background(Cream)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Terracotta, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .padding(top = 56.dp, bottom = 36.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PantryBrandMark(size = 112.dp)
            Spacer(Modifier.height(12.dp))
            Text("PANTRY", style = MaterialTheme.typography.headlineLarge, color = Ivory)
            Text(
                "Cook what's already yours",
                color = Ivory,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Sign in to match recipes to your kitchen.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            if (state.loading) {
                CircularProgressIndicator(color = Terracotta)
            } else {
                Button(
                    onClick = {
                        val client = GoogleSignIn.getClient(activity, viewModel.googleSignInOptions(activity))
                        launcher.launch(client.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Continue with Google", style = MaterialTheme.typography.labelLarge)
                }
                if (BuildConfig.ALLOW_DEV_SSO) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.signInWithDevelopmentSso() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Continue (local demo SSO)", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            state.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}
