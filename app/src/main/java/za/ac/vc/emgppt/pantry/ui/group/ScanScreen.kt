package za.ac.vc.emgppt.pantry.ui.group

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import za.ac.vc.emgppt.pantry.data.ServiceLocator
import za.ac.vc.emgppt.pantry.data.api.ConfirmFoodRequest
import za.ac.vc.emgppt.pantry.ui.main.MainViewModel
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta
import java.io.File

/**
 * CameraX / gallery capture that posts to Blessing's LogMeal identify
 * endpoint, then confirms a name before saving to the pantry
 * (Android Developers, 2024; LogMeal API, 2026).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanScreen(viewModel: MainViewModel, onSaved: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val imageCapture = remember { ImageCapture.Builder().build() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var imageId by remember { mutableStateOf(0L) }
    var foods by remember { mutableStateOf<List<String>>(emptyList()) }
    var selected by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }

    fun identify(part: MultipartBody.Part) {
        scope.launch {
            loading = true
            error = null
            try {
                val response = ServiceLocator.api.identifyFood(part)
                if (!response.success || response.data == null) {
                    error = response.error ?: "Food recognition failed. Set LogMeal:ApiToken on the API."
                    foods = emptyList()
                } else {
                    imageId = response.data.imageId
                    foods = response.data.foods
                    selected = response.data.topMatch?.name ?: response.data.foods.firstOrNull().orEmpty()
                    if (foods.isEmpty()) {
                        error = response.data.message ?: "No food items were recognized."
                    }
                }
            } catch (ex: Exception) {
                Log.e("Pantry", "identifyFood failed", ex)
                error = ex.message ?: "Could not reach the recognition API."
            } finally {
                loading = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) {
            error = "Could not read the selected image."
            return@rememberLauncherForActivityResult
        }
        val body = bytes.toRequestBody("image/jpeg".toMediaType())
        identify(MultipartBody.Part.createFormData("image", "gallery.jpg", body))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Scan ingredients", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Photograph food and confirm the name before it is added to your pantry. Recognition runs on the custom API (LogMeal), not on the phone.",
            style = MaterialTheme.typography.bodyLarge
        )

        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (ex: Exception) {
                                Log.e("Pantry", "Camera bind failed", ex)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            }
            Button(
                onClick = {
                    val file = File(context.cacheDir, "scan-${System.currentTimeMillis()}.jpg")
                    val output = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        output,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val body = file.asRequestBody("image/jpeg".toMediaType())
                                identify(MultipartBody.Part.createFormData("image", file.name, body))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                error = exception.message ?: "Camera capture failed."
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
            ) { Text("Capture photo") }
        } else {
            OutlinedButton(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Allow camera") }
        }

        OutlinedButton(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Choose from gallery") }

        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(color = Terracotta)
                Text("Identifying food…")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (foods.isNotEmpty()) {
            Text("What did we find?", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                foods.forEach { name ->
                    FilterChip(
                        selected = selected.equals(name, ignoreCase = true),
                        onClick = { selected = name },
                        label = { Text(name) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = customName,
            onValueChange = { customName = it },
            label = { Text("Or type a custom name") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Qty") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text("Unit") },
                modifier = Modifier.weight(1f)
            )
        }
        Button(
            onClick = {
                val name = customName.trim().ifBlank { selected.trim() }
                if (name.isEmpty()) {
                    error = "Select or type an ingredient name."
                    return@Button
                }
                scope.launch {
                    loading = true
                    error = null
                    try {
                        val allowCustom = customName.isNotBlank() &&
                            foods.none { it.equals(customName.trim(), ignoreCase = true) }
                        if (imageId != 0L) {
                            val confirmed = ServiceLocator.api.confirmFood(
                                ConfirmFoodRequest(
                                    imageId = imageId,
                                    selectedFood = name,
                                    allowCustomName = allowCustom || foods.isEmpty()
                                )
                            )
                            if (!confirmed.success) {
                                error = confirmed.error ?: "Could not confirm the food name."
                                loading = false
                                return@launch
                            }
                        }
                        viewModel.addPantryItem(name, quantity, unit)
                        onSaved()
                    } catch (ex: Exception) {
                        error = ex.message
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
        ) { Text("Save to pantry") }
    }
}
