package za.ac.vc.emgppt.pantry.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.ac.vc.emgppt.pantry.data.ServiceLocator
import za.ac.vc.emgppt.pantry.data.api.UserSettings
import za.ac.vc.emgppt.pantry.ui.main.MainUiState
import za.ac.vc.emgppt.pantry.ui.main.MainViewModel

private val DietOptions = listOf("vegetarian", "vegan", "halal", "gluten-free", "dairy-free", "no restriction")
private val CuisineOptions = listOf("Italian", "Asian", "Mediterranean", "Comfort Food")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    onSignOut: () -> Unit,
    contentPadding: PaddingValues
) {
    var diets by remember(state.settings) { mutableStateOf(state.settings.dietaryPreferences.toSet()) }
    var cuisines by remember(state.settings) { mutableStateOf(state.settings.cuisinePreferences.toSet()) }
    var allergens by remember(state.settings) { mutableStateOf(state.settings.allergens.joinToString(", ")) }
    var notifications by remember(state.settings) { mutableStateOf(state.settings.notificationsEnabled) }
    var maxMissing by remember(state.settings) { mutableStateOf(state.settings.maxMissingIngredients.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Your kitchen journey", style = MaterialTheme.typography.headlineSmall)
        Text(ServiceLocator.session.email ?: ServiceLocator.session.displayName ?: "Signed in")
        Text("Dietary preferences", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DietOptions.forEach { option ->
                FilterChip(
                    selected = diets.contains(option),
                    onClick = {
                        diets = if (diets.contains(option)) diets - option else diets + option
                    },
                    label = { Text(option) }
                )
            }
        }
        Text("Cuisine bias", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CuisineOptions.forEach { option ->
                FilterChip(
                    selected = cuisines.contains(option),
                    onClick = {
                        cuisines = if (cuisines.contains(option)) cuisines - option else cuisines + option
                    },
                    label = { Text(option) }
                )
            }
        }
        OutlinedTextField(
            value = allergens,
            onValueChange = { allergens = it },
            label = { Text("Allergens (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = maxMissing,
            onValueChange = { maxMissing = it },
            label = { Text("Max missing ingredients") },
            modifier = Modifier.fillMaxWidth()
        )
        RowWithSwitch("Notifications (stored now, used in final POE)", notifications) { notifications = it }
        Button(
            onClick = {
                val max = maxMissing.toIntOrNull()
                if (max == null || max !in 0..20) {
                    viewModel.saveSettings(state.settings)
                    return@Button
                }
                viewModel.saveSettings(
                    UserSettings(
                        languageCode = "en",
                        dietaryPreferences = diets.toList(),
                        cuisinePreferences = cuisines.toList(),
                        allergens = allergens.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        notificationsEnabled = notifications,
                        maxMissingIngredients = max
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save settings") }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        Text(
            "Gamification badges and streaks are assigned to Lesedi for the next commit.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun RowWithSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
