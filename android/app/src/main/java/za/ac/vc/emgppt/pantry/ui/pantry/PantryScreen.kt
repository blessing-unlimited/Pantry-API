package za.ac.vc.emgppt.pantry.ui.pantry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import za.ac.vc.emgppt.pantry.ui.theme.Basil
import za.ac.vc.emgppt.pantry.ui.theme.BasilSoft
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Ivory
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import za.ac.vc.emgppt.pantry.R
import za.ac.vc.emgppt.pantry.ui.main.MainUiState
import za.ac.vc.emgppt.pantry.ui.main.MainViewModel

@Composable
fun PantryScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    onScanClick: () -> Unit,
    onListsClick: () -> Unit,
    contentPadding: PaddingValues
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(contentPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.pantry_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.pantry_hint), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (it.length >= 2) viewModel.searchIngredients(it)
            },
            label = { Text(stringResource(R.string.pantry_ingredient)) },
            modifier = Modifier.fillMaxWidth(),
            isError = name.isBlank() && state.message != null
        )
        if (state.ingredientSuggestions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                state.ingredientSuggestions.take(3).forEach { suggestion ->
                    TextButton(onClick = { name = suggestion.name }) { Text(suggestion.name) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text(stringResource(R.string.pantry_qty)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text(stringResource(R.string.pantry_unit)) },
                modifier = Modifier.weight(1f)
            )
        }
        Button(
            onClick = {
                viewModel.addPantryItem(name, quantity, unit)
                name = ""
                quantity = ""
                unit = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
        ) { Text(stringResource(R.string.pantry_save), style = MaterialTheme.typography.labelLarge) }
        OutlinedButton(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pantry_scan))
        }
        OutlinedButton(onClick = onListsClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pantry_lists))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.pantry, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Ivory),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(BasilSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.ingredientName.take(1).uppercase(), color = Basil, style = MaterialTheme.typography.titleMedium)
                            }
                            Column {
                                Text(item.ingredientName, style = MaterialTheme.typography.titleMedium)
                                val qty = listOfNotNull(item.quantity?.toString(), item.unit).joinToString(" ")
                                if (qty.isNotBlank()) Text(qty, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = { viewModel.deletePantryItem(item.id) }) { Text(stringResource(R.string.pantry_remove)) }
                    }
                }
            }
        }
    }
}
