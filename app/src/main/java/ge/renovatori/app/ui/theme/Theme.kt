package ge.renovatori.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// The web uses stock Bootstrap colors; primary is Bootstrap's #0d6efd.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0D6EFD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E6FF),
    onPrimaryContainer = Color(0xFF001A43),
    error = Color(0xFFDC3545)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EA8FE),
    onPrimary = Color(0xFF002E6B),
    primaryContainer = Color(0xFF0B4FB5),
    onPrimaryContainer = Color(0xFFD8E6FF),
    error = Color(0xFFEA868F)
)

/** App theme; follows the system light/dark setting (the web footer's "სისტემური" default). */
@Composable
fun RenovatoriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

/** Bootstrap `text-success` green, light/dark variant picked from the active scheme. */
val ColorScheme.success: Color
    get() = if (background.luminance() < 0.5f) Color(0xFF75B798) else Color(0xFF198754)
