package za.ac.vc.emgppt.pantry.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import za.ac.vc.emgppt.pantry.ui.main.MainViewModel
import za.ac.vc.emgppt.pantry.ui.theme.Basil
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta

@Composable
fun RecipeDetailScreen(
    recipeId: String,
    viewModel: MainViewModel,
    onStartCooking: () -> Unit
) {
    val uiState by viewModel.state.collectAsState()
    val detail = uiState.recipeDetail
    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }

    if (detail == null || detail.id != recipeId) {
        Text("Loading recipe…", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = detail.thumbnailUrl,
            contentDescription = detail.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(220.dp).clip(CircleShape)
        )
        Text(detail.title, style = MaterialTheme.typography.headlineSmall)
        Text(
            listOfNotNull(detail.category, detail.area).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = Terracotta
        )
        Text(
            "Pantry match ${detail.matchPercent}%  (${detail.pantryHaveCount} have / ${detail.pantryNeedCount} missing)",
            style = MaterialTheme.typography.bodyLarge
        )
        Text("Ingredients", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        detail.ingredients.forEach { ingredient ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${ingredient.name}  ${ingredient.measure}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (ingredient.inPantry) "In pantry" else "Missing",
                    color = if (ingredient.inPantry) Basil else Terracotta,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        OutlinedButton(
            onClick = { /* Blessing: push missing items to a shopping list */ },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Add missing (${detail.pantryNeedCount}) — Blessing")
        }
        Button(
            onClick = onStartCooking,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
        ) {
            Text("Start cooking", style = MaterialTheme.typography.labelLarge)
        }
        Text("Instructions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        Text(detail.instructions, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    }
}
