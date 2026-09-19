package za.ac.vc.emgppt.pantry.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Plate-and-steam mark from Part 1 Figure 2.1: white plate, tomato / basil /
 * garlic dots, steam lines (Pantry Project Group, 2026).
 */
@Composable
fun PantryBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    showFooterBand: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h * 0.54f
        val plateR = w * 0.32f

        if (showFooterBand) {
            drawRect(
                color = TerracottaDeep,
                topLeft = Offset(0f, h * 0.78f),
                size = Size(w, h * 0.22f)
            )
        }

        drawCircle(color = Color.White, radius = plateR, center = Offset(cx, cy))
        drawCircle(color = GarlicCream, radius = plateR * 0.72f, center = Offset(cx, cy))
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = plateR * 0.72f,
            center = Offset(cx, cy),
            style = Stroke(width = plateR * 0.04f)
        )

        val dotR = plateR * 0.16f
        drawCircle(color = TomatoRed, radius = dotR, center = Offset(cx - plateR * 0.28f, cy + plateR * 0.04f))
        drawCircle(color = Basil, radius = dotR, center = Offset(cx, cy - plateR * 0.18f))
        drawCircle(color = GarlicCream, radius = dotR, center = Offset(cx + plateR * 0.28f, cy + plateR * 0.04f))
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = dotR,
            center = Offset(cx + plateR * 0.28f, cy + plateR * 0.04f),
            style = Stroke(width = 2f)
        )

        val steam = Stroke(width = w * 0.018f, cap = StrokeCap.Round)
        drawArc(
            color = Color.White,
            startAngle = 200f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.12f, h * 0.10f),
            size = Size(w * 0.08f, h * 0.18f),
            style = steam
        )
        drawArc(
            color = Color.White,
            startAngle = 200f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(cx + w * 0.02f, h * 0.08f),
            size = Size(w * 0.08f, h * 0.18f),
            style = steam
        )
    }
}
