package com.slock.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NeoColorScheme = lightColorScheme(
    primary = Yellow,
    onPrimary = Black,
    secondary = Cyan,
    onSecondary = Black,
    tertiary = Pink,
    onTertiary = Black,
    background = Cream,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = White,
    outline = BorderBlack,
    surfaceVariant = Cream
)

@Composable
fun SlockAppTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = NeoColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
