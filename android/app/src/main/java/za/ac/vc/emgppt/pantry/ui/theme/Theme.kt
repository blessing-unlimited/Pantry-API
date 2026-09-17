package za.ac.vc.emgppt.pantry.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ColorScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = TerracottaSoft,
    onPrimaryContainer = TerracottaDeep,
    secondary = Basil,
    onSecondary = Color.White,
    secondaryContainer = BasilSoft,
    onSecondaryContainer = Basil,
    background = Cream,
    onBackground = Espresso,
    surface = Ivory,
    onSurface = Espresso,
    surfaceVariant = Color(0xFFF0E6D8),
    onSurfaceVariant = Espresso,
    outline = Champagne,
    error = Danger
)

private val PantryShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PantryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        shapes = PantryShapes,
        content = content
    )
}
