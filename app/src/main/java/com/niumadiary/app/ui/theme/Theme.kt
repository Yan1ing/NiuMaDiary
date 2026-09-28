package com.niumadiary.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SunsetCoral,
    onPrimary = SunsetInk,
    primaryContainer = PaperStrong,
    onPrimaryContainer = SunsetInk,
    secondary = SunsetMauve,
    onSecondary = SunsetInk,
    secondaryContainer = SunsetBlush.copy(alpha = 0.35f),
    onSecondaryContainer = SunsetInk,
    tertiary = SunsetLamp,
    onTertiary = SunsetInk,
    background = SunsetCream,
    onBackground = SunsetInk,
    surface = Paper,
    onSurface = SunsetInk,
    surfaceVariant = PaperStrong,
    onSurfaceVariant = InkMuted,
    outline = InkMuted,
    outlineVariant = BorderSoft,
)

private val DarkColors = darkColorScheme(
    primary = SunsetCoral,
    onPrimary = SunsetInk,
    primaryContainer = ColorTokens.DarkCoralContainer,
    onPrimaryContainer = SunsetCream,
    secondary = SunsetBlush,
    onSecondary = SunsetInk,
    tertiary = SunsetLamp,
    onTertiary = SunsetInk,
    background = SunsetInk,
    onBackground = SunsetCream,
    surface = NightSurface,
    onSurface = SunsetCream,
    surfaceVariant = NightSurfaceRaised,
    onSurfaceVariant = SunsetBlush,
    outline = SunsetMauve,
    outlineVariant = BorderSoft,
)

private object ColorTokens {
    val DarkCoralContainer = androidx.compose.ui.graphics.Color(0xFF653B38)
}

@Composable
fun NiumaDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NiumaTypography,
        content = content,
    )
}
