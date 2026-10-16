package tj.osonaloqa.chat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = GreenContainer,
    onPrimaryContainer = PrimaryDarkGreen,
    secondary = PrimaryDarkGreen,
    secondaryContainer = GreenContainer,
    onSecondaryContainer = PrimaryDarkGreen,
    tertiary = AccentLive,
    background = SurfaceWhite,
    onBackground = TextMain,
    surface = SurfaceWhite,
    onSurface = TextMain,
    surfaceVariant = SurfaceSecond,
    onSurfaceVariant = TextSecond,
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF062312),
    primaryContainer = DarkOutgoing,
    onPrimaryContainer = Color(0xFFB8F1CC),
    secondary = DarkPrimary,
    background = DarkBackground,
    onBackground = Color(0xFFE2EFE7),
    surface = DarkSurface,
    onSurface = Color(0xFFE2EFE7),
    surfaceVariant = DarkSurface,
    onSurfaceVariant = Color(0xFF9DB2A5),
    error = ErrorRed,
)

@Composable
fun OsonAloqaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = OsonTypography,
        shapes = OsonShapes,
        content = content,
    )
}
