package za.ac.vc.emgppt.pantry.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import za.ac.vc.emgppt.pantry.ui.theme.BasilSoft
import za.ac.vc.emgppt.pantry.ui.theme.TerracottaDeep
import za.ac.vc.emgppt.pantry.ui.theme.Basil
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Ivory
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .background(Cream)
            .padding(contentPadding)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val displayName = ServiceLocator.session.displayName ?: "Chef"
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(BasilSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Basil
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(displayName, style = MaterialTheme.typography.titleLarge)
                Text("Home Chef • Level 4", color = Terracotta, style = MaterialTheme.typography.labelMedium)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("XP to Level 5", style = MaterialTheme.typography.labelSmall)
                Text("850 / 1000", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { 0.85f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Basil,
                trackColor = BasilSoft,
                strokeCap = StrokeCap.Round
            )
        }

        Text(ServiceLocator.session.email ?: "No email linked", style = MaterialTheme.typography.bodySmall)

        Text("Your Achievements", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Badge("3-Day Streak 🔥", Terracotta)
            Badge("Master Chef 🍳", Basil)
            Badge("Waste Fighter ♻️", Color(0xFF2E7D32))
        }

        Text("Dietary preferences", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DietOptions.forEach { option ->
                FilterChip(
                    selected = diets.contains(option),
                    onClick = {
                        diets = if (diets.contains(option)) diets - option else diets + option
                    },
                    label = { Text(option, style = MaterialTheme.typography.labelLarge) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Basil,
                        selectedLabelColor = Ivory
                    )
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
                    label = { Text(option, style = MaterialTheme.typography.labelLarge) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Basil,
                        selectedLabelColor = Ivory
                    )
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
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
        ) { Text("Save settings", style = MaterialTheme.typography.labelLarge) }

        Text("Your Achievements", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Badge("3-Day Streak 🔥", Terracotta)
            Badge("Master Chef 🍳", Basil)
            Badge("Waste Fighter ♻️", Color(0xFF2E7D32))
        }

        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}

@Composable
private fun Badge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun RowWithSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
