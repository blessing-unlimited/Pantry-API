package za.ac.vc.emgppt.pantry.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.ac.vc.emgppt.pantry.data.api.ShoppingListDto
import za.ac.vc.emgppt.pantry.data.api.ShoppingListItemDto
import za.ac.vc.emgppt.pantry.ui.main.MainUiState
import za.ac.vc.emgppt.pantry.ui.main.MainViewModel
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Ivory
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta

@Composable
fun ShoppingListsScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    contentPadding: PaddingValues
) {
    LaunchedEffect(Unit) { viewModel.loadShoppingLists() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Shopping lists", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Generated from recipes you cannot fully cook yet. Tick items as you shop.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (state.shoppingLists.isEmpty()) {
            item { Text("No lists yet. Open a recipe and tap Add missing.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(state.shoppingLists, key = { it.id }) { list ->
            ShoppingListCard(list) { item, checked ->
                viewModel.toggleShoppingItem(list.id, item, checked)
            }
        }
    }
}

@Composable
private fun ShoppingListCard(
    list: ShoppingListDto,
    onToggle: (ShoppingListItemDto, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Ivory),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(list.name, style = MaterialTheme.typography.titleMedium)
            Text("${list.items.count { !it.isChecked }} still to buy", color = Terracotta, style = MaterialTheme.typography.bodySmall)
            list.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = item.isChecked, onCheckedChange = { onToggle(item, it) })
                    Column(modifier = Modifier.weight(1f)) {
                        val qty = listOfNotNull(item.quantity?.toString(), item.unit).joinToString(" ")
                        Text(
                            if (qty.isBlank()) item.ingredientName else "${item.ingredientName}  $qty",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (item.sourceRecipeTitles.isNotEmpty()) {
                            Text(item.sourceRecipeTitles.joinToString(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
