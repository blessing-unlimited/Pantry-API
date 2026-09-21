package za.ac.vc.emgppt.pantry.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Terracotta),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PantryBrandMark(size = 168.dp)
        Spacer(Modifier.height(20.dp))
        Text("PANTRY", style = MaterialTheme.typography.displayLarge, color = Color.White)
        Text(
            "Cook what's already yours",
            color = Ivory,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(16.dp))
        ForkSpoonFlourish()
    }
}

/** Part 1 Figure 5.1 fork-and-spoon flourish under the splash tagline. */
@Composable
private fun ForkSpoonFlourish() {
    Canvas(modifier = Modifier.size(width = 168.dp, height = 28.dp)) {
        val ink = Color.White.copy(alpha = 0.92f)
        val stem = 3.4f
        val tine = 2.4f
        val forkX = size.width * 0.22f
        val spoonX = size.width * 0.78f
        val baseY = size.height * 0.92f

        drawLine(ink, Offset(forkX, baseY), Offset(forkX, size.height * 0.42f), stem, StrokeCap.Round)
        listOf(-7f, 0f, 7f).forEach { dx ->
            drawLine(
                ink,
                Offset(forkX + dx, size.height * 0.08f),
                Offset(forkX + dx, size.height * 0.44f),
                tine,
                StrokeCap.Round
            )
        }

        drawLine(ink, Offset(size.width * 0.38f, size.height * 0.52f), Offset(size.width * 0.62f, size.height * 0.52f), 2.2f, StrokeCap.Round)

        drawLine(ink, Offset(spoonX, baseY), Offset(spoonX, size.height * 0.46f), stem, StrokeCap.Round)
        drawOval(
            color = ink,
            topLeft = Offset(spoonX - 8.5f, size.height * 0.04f),
            size = Size(17f, 24f),
            style = Stroke(width = 2.8f, cap = StrokeCap.Round)
        )
    }
}
