package com.carmelitasoftworks.motointercom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Amber = Color(0xFFFFB300)
private val Cyan = Color(0xFF4DD0E1)
private val Red = Color(0xFFFF6B60)

/**
 * Dark, high-contrast scheme: readable in sunlight through a visor and usable
 * with gloves (CLAUDE.md §6). Dark is the only theme for now.
 */
private val HighContrastDarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A3300),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = Cyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFFB2EBF2),
    error = Red,
    onError = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFE0E0E0),
    surfaceContainer = Color(0xFF161616),
    outline = Color(0xFF9E9E9E),
)

@Composable
fun MotoIntercomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HighContrastDarkColors,
        content = content,
    )
}
