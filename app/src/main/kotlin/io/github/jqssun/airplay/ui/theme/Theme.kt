package io.github.jqssun.airplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// "surface" est transparent : les ListItem, barres, etc. laissent voir le fond (le verre est dessiné par Modifier.glass).
// Les surfaceContainer* restent quasi opaques pour que menus et boîtes de dialogue soient lisibles.
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF00204A),
    primaryContainer = Color(0xFF1E3A6E),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFFC9A8FF),
    background = Color(0xFF0A0F1F),
    onBackground = Color(0xFFF2F4FF),
    surface = Color.Transparent,
    onSurface = Color(0xFFF2F4FF),
    surfaceVariant = Color.White.copy(alpha = 0.10f),
    onSurfaceVariant = Color(0xFFC4C9E0),
    surfaceContainerLowest = Color(0xE60E1224),
    surfaceContainerLow = Color(0xE6141830),
    surfaceContainer = Color(0xE61A1F3A),
    surfaceContainerHigh = Color(0xEB222846),
    surfaceContainerHighest = Color(0xF02B3253),
    outline = Color.White.copy(alpha = 0.35f),
    outlineVariant = Color.White.copy(alpha = 0.15f),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2F62D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E4FF),
    onPrimaryContainer = Color(0xFF001B47),
    secondary = Color(0xFF7B3FD0),
    background = Color(0xFFEAF1FF),
    onBackground = Color(0xFF12162B),
    surface = Color.Transparent,
    onSurface = Color(0xFF12162B),
    surfaceVariant = Color.White.copy(alpha = 0.45f),
    onSurfaceVariant = Color(0xFF454A66),
    surfaceContainerLowest = Color(0xF2FFFFFF),
    surfaceContainerLow = Color(0xF2F7F9FF),
    surfaceContainer = Color(0xF2F0F3FF),
    surfaceContainerHigh = Color(0xF5EAEEFF),
    surfaceContainerHighest = Color(0xF8E3E8FB),
    outline = Color.Black.copy(alpha = 0.30f),
    outlineVariant = Color.Black.copy(alpha = 0.12f),
)

@Composable
fun AirPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val glass = if (darkTheme) DarkGlass else LightGlass

    CompositionLocalProvider(LocalGlass provides glass) {
        MaterialTheme(colorScheme = colorScheme) {
            GlassBackground {
                CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
                    content()
                }
            }
        }
    }
}
