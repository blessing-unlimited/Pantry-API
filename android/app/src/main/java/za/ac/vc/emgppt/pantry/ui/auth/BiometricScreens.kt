package za.ac.vc.emgppt.pantry.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import za.ac.vc.emgppt.pantry.R
import za.ac.vc.emgppt.pantry.security.BiometricGate
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta

@Composable
fun BiometricLockScreen(onUnlocked: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    var error by remember { mutableStateOf<String?>(null) }
    val title = stringResource(R.string.biometric_title)
    val subtitle = stringResource(R.string.biometric_subtitle)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.biometric_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.biometric_lock_body), style = MaterialTheme.typography.bodyLarge)
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                BiometricGate.authenticate(activity, title, subtitle) { ok, message ->
                    if (ok) {
                        onUnlocked()
                    } else {
                        error = message ?: activity.getString(R.string.biometric_failed)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
        ) {
            Text(stringResource(R.string.biometric_unlock))
        }
    }
}

@Composable
fun BiometricOfferScreen(onEnabled: () -> Unit, onSkip: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    var error by remember { mutableStateOf<String?>(null) }
    val title = stringResource(R.string.biometric_enable_title)
    val subtitle = stringResource(R.string.biometric_subtitle)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.biometric_enable_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.biometric_enable_body), style = MaterialTheme.typography.bodyLarge)
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (!BiometricGate.canAuthenticate(activity)) {
                    error = activity.getString(R.string.biometric_unavailable)
                    return@Button
                }
                BiometricGate.authenticate(activity, title, subtitle) { ok, message ->
                    if (ok) {
                        onEnabled()
                    } else {
                        error = message ?: activity.getString(R.string.biometric_failed)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
        ) {
            Text(stringResource(R.string.biometric_enable))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.biometric_not_now))
        }
    }
}
