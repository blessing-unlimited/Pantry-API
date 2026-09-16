package za.ac.vc.emgppt.pantry.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    secondary = Basil,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
    error = Danger
)

@Composable
fun PantryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content
    )
}
