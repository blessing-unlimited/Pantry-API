package za.ac.vc.emgppt.pantry.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import za.ac.vc.emgppt.pantry.data.ServiceLocator
import za.ac.vc.emgppt.pantry.data.api.RecipeSummaryDto
import za.ac.vc.emgppt.pantry.ui.main.MainUiState
import za.ac.vc.emgppt.pantry.ui.theme.Basil
import za.ac.vc.emgppt.pantry.ui.theme.BasilSoft
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Ivory
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
            .background(Cream)
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Hi, $name", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${state.recipes.count { it.pantryNeedCount == 0 }} recipes you can cook from your pantry right now.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        item {
            Text("Your pantry", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.pantry.take(12), key = { it.id }) { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(BasilSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                item.ingredientName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Basil
                            )
                        }
                        Text(
                            item.ingredientName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        item { Text("Recommended for you", style = MaterialTheme.typography.titleMedium) }
        items(state.recipes, key = { it.id }) { recipe ->
            RecipeCard(recipe, onClick = { onRecipeClick(recipe.id) })
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun RecipeCard(recipe: RecipeSummaryDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Ivory),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = recipe.thumbnailUrl,
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp).clip(CircleShape)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                val ready = recipe.pantryNeedCount == 0
                val have = recipe.pantryHaveCount
                val total = have + recipe.pantryNeedCount
                Text(
                    if (ready) "All ingredients ready" else "Uses $have/$total pantry items",
                    color = Basil,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!ready) {
                    Text(
                        "Missing: ${recipe.missingIngredients.take(3).joinToString()}",
                        color = Terracotta,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                val meta = listOfNotNull(recipe.category, recipe.area).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.labelMedium, color = Terracotta)
                }
            }
        }
    }
}
