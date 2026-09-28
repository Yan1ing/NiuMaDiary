package com.niumadiary.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.niumadiary.app.R
import com.niumadiary.app.ui.theme.SunsetCream
import com.niumadiary.app.ui.theme.SunsetInk

enum class SunsetBackdropStyle {
    TODAY,
    CALENDAR,
    STATS,
    SETTINGS,
}

@Composable
fun SunsetBackdrop(
    style: SunsetBackdropStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val imageRes = when (style) {
        SunsetBackdropStyle.TODAY -> R.drawable.sunset_background
        SunsetBackdropStyle.CALENDAR -> R.drawable.blue_hour_calendar
        SunsetBackdropStyle.STATS -> R.drawable.blue_hour_stats
        SunsetBackdropStyle.SETTINGS -> R.drawable.blue_hour_settings
    }
    Box(modifier = modifier) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (style == SunsetBackdropStyle.TODAY) 1f else 0.82f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when (style) {
                        SunsetBackdropStyle.TODAY -> Brush.verticalGradient(
                            0f to SunsetInk.copy(alpha = 0.20f),
                            0.38f to SunsetInk.copy(alpha = 0.06f),
                            1f to SunsetInk.copy(alpha = 0.64f),
                        )

                        SunsetBackdropStyle.CALENDAR -> Brush.verticalGradient(
                            0f to SunsetCream.copy(alpha = 0.28f),
                            0.48f to SunsetCream.copy(alpha = 0.54f),
                            1f to SunsetCream.copy(alpha = 0.78f),
                        )

                        SunsetBackdropStyle.STATS -> Brush.verticalGradient(
                            0f to SunsetCream.copy(alpha = 0.18f),
                            0.50f to SunsetCream.copy(alpha = 0.46f),
                            1f to SunsetCream.copy(alpha = 0.74f),
                        )

                        SunsetBackdropStyle.SETTINGS -> Brush.verticalGradient(
                            0f to SunsetCream.copy(alpha = 0.34f),
                            0.50f to SunsetCream.copy(alpha = 0.56f),
                            1f to SunsetCream.copy(alpha = 0.80f),
                        )
                    },
                ),
        )
        content()
    }
}
