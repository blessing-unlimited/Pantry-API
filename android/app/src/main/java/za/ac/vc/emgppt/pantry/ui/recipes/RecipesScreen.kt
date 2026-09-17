package za.ac.vc.emgppt.pantry.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.ac.vc.emgppt.pantry.ui.home.RecipeCard
import za.ac.vc.emgppt.pantry.ui.main.MainUiState
import za.ac.vc.emgppt.pantry.ui.main.MainViewModel
import za.ac.vc.emgppt.pantry.ui.theme.Cream

@Composable
fun RecipesScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    onRecipeClick: (String) -> Unit,
    contentPadding: PaddingValues
) {
    var query by remember { mutableStateOf(state.searchQuery) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(contentPadding)
            .padding(20.dp)
    ) {
        Text("Recipes", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.searchRecipes(it)
            },
            label = { Text("Search recipes") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        Text(
            "Sorted by pantry match",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.recipes, key = { it.id }) { recipe ->
                RecipeCard(recipe) { onRecipeClick(recipe.id) }
            }
        }
    }
}
