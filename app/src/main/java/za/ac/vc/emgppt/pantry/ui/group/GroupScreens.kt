package za.ac.vc.emgppt.pantry.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import za.ac.vc.emgppt.pantry.data.api.RecipeDetailDto
import za.ac.vc.emgppt.pantry.ui.theme.Basil
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta

/**
 * Lesedi: Hands-free cook mode implementation.
 * Features: Dark theme, one step at a time, FLAG_KEEP_SCREEN_ON,
 * swipe navigation, and a per-step timer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CookModeScreen(recipe: RecipeDetailDto?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val steps = recipe?.steps.orEmpty().ifEmpty { listOf("No steps were returned for this recipe.") }
    val pagerState = rememberPagerState(pageCount = { steps.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1C))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Cook mode", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text("Step ${pagerState.currentPage + 1} of ${steps.size}", color = Color(0xFFB5B5B5))
            }
            StepTimer()
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    steps[page],
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                enabled = pagerState.currentPage > 0
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
            }

            if (pagerState.currentPage == steps.size - 1) {
                Button(
                    onClick = {
                        Toast.makeText(context, "XP Awarded! Well cooked!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Basil)
                ) {
                    Text("Finish Cooking")
                }
            } else {
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    enabled = pagerState.currentPage < steps.size - 1
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

@Composable
fun StepTimer() {
    var timeLeft by remember { mutableIntStateOf(300) } // Default 5 minutes
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        if (timeLeft == 0) isRunning = false
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
        CircularProgressIndicator(
            progress = { timeLeft / 300f },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF4CAF50),
            trackColor = Color.DarkGray,
            strokeWidth = 6.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
            Row {
                IconButton(onClick = { isRunning = !isRunning }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (isRunning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

