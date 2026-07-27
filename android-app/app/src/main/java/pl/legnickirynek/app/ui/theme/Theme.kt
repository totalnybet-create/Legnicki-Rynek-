package pl.legnickirynek.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LegnicaNavy = Color(0xFF062A55)
val LegnicaCoral = Color(0xFFFF6B5E)
val LegnicaBackground = Color(0xFFF5F6F8)
val LegnicaSurface = Color(0xFFFFFFFF)
val LegnicaText = Color(0xFF172033)
val LegnicaMuted = Color(0xFF667085)

private val LegnickiRynekColors = lightColorScheme(
    primary = LegnicaCoral,
    onPrimary = Color.White,
    secondary = LegnicaNavy,
    onSecondary = Color.White,
    background = LegnicaBackground,
    onBackground = LegnicaText,
    surface = LegnicaSurface,
    onSurface = LegnicaText
)

@Composable
fun LegnickiRynekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LegnickiRynekColors,
        content = content
    )
}
