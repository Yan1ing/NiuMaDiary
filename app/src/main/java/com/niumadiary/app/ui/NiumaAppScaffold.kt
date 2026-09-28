package com.niumadiary.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.niumadiary.app.R
import com.niumadiary.app.ui.theme.SunsetCoral
import com.niumadiary.app.ui.theme.SunsetCream
import com.niumadiary.app.ui.theme.SunsetInk

enum class NiumaDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    TODAY("today", R.string.nav_today, R.drawable.ic_today),
    CALENDAR("calendar", R.string.nav_calendar, R.drawable.ic_calendar),
    STATS("stats", R.string.nav_stats, R.drawable.ic_stats),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_settings),
}

@Composable
fun NiumaAppScaffold(
    currentDestination: NiumaDestination,
    onDestinationSelected: (NiumaDestination) -> Unit,
    content: @Composable (bottomPadding: PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (!isImeVisible) {
                NavigationBar(
                    modifier = Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SunsetInk.copy(alpha = 0.78f),
                                SunsetInk.copy(alpha = 0.96f),
                            ),
                        ),
                    ),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    NiumaDestination.entries.forEach { destination ->
                        val label = stringResource(destination.labelRes)
                        NavigationBarItem(
                            selected = currentDestination == destination,
                            onClick = { onDestinationSelected(destination) },
                            icon = {
                                Icon(
                                    painter = painterResource(destination.iconRes),
                                    contentDescription = label,
                                )
                            },
                            label = { Text(text = label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SunsetCream,
                                selectedTextColor = SunsetCream,
                                indicatorColor = SunsetCoral.copy(alpha = 0.38f),
                                unselectedIconColor = SunsetCream.copy(alpha = 0.64f),
                                unselectedTextColor = SunsetCream.copy(alpha = 0.64f),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        content(PaddingValues(bottom = innerPadding.calculateBottomPadding()))
    }
}
