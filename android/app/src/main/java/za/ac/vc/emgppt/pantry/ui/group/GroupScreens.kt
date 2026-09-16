package za.ac.vc.emgppt.pantry.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import za.ac.vc.emgppt.pantry.data.api.RecipeDetailDto

/**
 * Lesedi: replace this scaffold with hands-free cook mode.
 * Required behaviour from Part 1: dark theme, one step at a time, FLAG_KEEP_SCREEN_ON,
 * swipe navigation, and an optional per-step timer (Android Developers, 2024).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CookModeScreen(recipe: RecipeDetailDto?) {
    val steps = recipe?.steps.orEmpty().ifEmpty { listOf("No steps were returned for this recipe.") }
    val pagerState = rememberPagerState(pageCount = { steps.size })
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1C))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Cook mode", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text("Step ${pagerState.currentPage + 1} of ${steps.size}", color = Color(0xFFB5B5B5))
        Text(
            "TODO (Lesedi): keep the screen awake, add a circular timer, and enlarge this type for arm's-length reading.",
            color = Color(0xFFB5B5B5),
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            Text(steps[page], color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * Blessing: CameraX preview + ingredient confirmation.
 * Part 1 specified live bounding boxes and a confirm-before-save step.
 */
@Composable
fun ScanScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ingredient scan", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Assigned to Blessing. Implement CameraX capture, optional ML Kit labelling, " +
                "and a confirmation list with quantity steppers before calling POST /api/v1/pantry-items."
        )
    }
}
