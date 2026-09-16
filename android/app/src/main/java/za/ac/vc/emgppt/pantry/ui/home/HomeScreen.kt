package za.ac.vc.emgppt.pantry.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import za.ac.vc.emgppt.pantry.data.ServiceLocator
import za.ac.vc.emgppt.pantry.data.api.RecipeSummaryDto
import za.ac.vc.emgppt.pantry.ui.main.MainUiState
import za.ac.vc.emgppt.pantry.ui.theme.Basil
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta

@Composable
fun HomeScreen(
    state: MainUiState,
    onRecipeClick: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val name = ServiceLocator.session.displayName ?: "chef"
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Hi, $name", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${state.recipes.count { it.pantryNeedCount == 0 }} recipes you can cook from your pantry right now."
            )
        }
        item {
            Text("Your pantry", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.pantry.take(12), key = { it.id }) { item ->
                    Card {
                        Text(item.ingredientName, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        }
        item { Text("Recommended for you", style = MaterialTheme.typography.titleMedium) }
        items(state.recipes, key = { it.id }) { recipe ->
            RecipeCard(recipe, onClick = { onRecipeClick(recipe.id) })
        }
    }
}

@Composable
fun RecipeCard(recipe: RecipeSummaryDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = recipe.thumbnailUrl,
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                val ready = recipe.pantryNeedCount == 0
                Text(
                    if (ready) "All ingredients ready" else "Missing: ${recipe.missingIngredients.take(3).joinToString()}",
                    color = if (ready) Basil else Terracotta
                )
                Text("Uses ${recipe.pantryHaveCount}/${recipe.pantryHaveCount + recipe.pantryNeedCount} pantry items · ${recipe.matchPercent}% match")
            }
        }
    }
}
