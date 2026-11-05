package za.ac.vc.emgppt.pantry.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.ac.vc.emgppt.pantry.data.api.UserSettings
import za.ac.vc.emgppt.pantry.ui.main.MainViewModel
import za.ac.vc.emgppt.pantry.ui.theme.Basil
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Ivory
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta

private val DietOptions = listOf("vegetarian", "vegan", "halal", "gluten-free", "dairy-free", "no restriction")
private val CuisineOptions = listOf("Italian", "Asian", "Mediterranean", "Comfort Food")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    var diets by remember { mutableStateOf(setOf<String>()) }
    var cuisines by remember { mutableStateOf(setOf<String>()) }
    var allergens by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome to Pantry", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Let's customize your experience. These can be changed in settings later.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text("Dietary preferences", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DietOptions.forEach { option ->
                FilterChip(
                    selected = diets.contains(option),
                    onClick = {
                        diets = if (diets.contains(option)) diets - option else diets + option
                    },
                    label = { Text(option) },
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
                    label = { Text(option) },
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

        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = {
                viewModel.saveSettings(
                    UserSettings(
                        dietaryPreferences = diets.toList(),
                        cuisinePreferences = cuisines.toList(),
                        allergens = allergens.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                )
                onComplete()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
        ) {
            Text("Continue", style = MaterialTheme.typography.labelLarge)
        }
        
        OutlinedButton(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Skip for now")
        }
    }
}
