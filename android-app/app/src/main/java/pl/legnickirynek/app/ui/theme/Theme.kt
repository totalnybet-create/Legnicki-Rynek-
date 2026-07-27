package pl.legnickirynek.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LegnicaNavy = Color(0xFF062A55)
val LegnicaCoral = Color(0xFFFF6B5E)
val LegnicaBackground = Color(0xFFF5F6F8)
val LegnicaSurface = Color(0xFFFFFFFF)
val LegnicaText = Color(0xFF172033)
val LegnicaMuted = Color(0xFF667085)

private val LightColors = lightColorScheme(
    primary = LegnicaCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410003),
    secondary = LegnicaNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E4FF),
    onSecondaryContainer = Color(0xFF001C38),
    background = LegnicaBackground,
    onBackground = LegnicaText,
    surface = LegnicaSurface,
    onSurface = LegnicaText,
    surfaceVariant = Color(0xFFE9EDF3),
    onSurfaceVariant = LegnicaMuted,
    outline = Color(0xFF77818F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AC),
    onPrimary = Color(0xFF690007),
    primaryContainer = Color(0xFF93000D),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFA5C8FF),
    onSecondary = Color(0xFF00315B),
    secondaryContainer = Color(0xFF004881),
    onSecondaryContainer = Color(0xFFD4E4FF),
    background = Color(0xFF0E141C),
    onBackground = Color(0xFFE7EDF6),
    surface = Color(0xFF151D27),
    onSurface = Color(0xFFE7EDF6),
    surfaceVariant = Color(0xFF202A36),
    onSurfaceVariant = Color(0xFFC0CAD8),
    outline = Color(0xFF8A95A5)
)

@Composable
fun LegnickiRynekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
