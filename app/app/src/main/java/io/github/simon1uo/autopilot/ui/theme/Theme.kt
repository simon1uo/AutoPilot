package io.github.simon1uo.autopilot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Ink,
    primaryContainer = NeonGreenDim,
    onPrimaryContainer = Ink,
    secondary = TextSecondary,
    onSecondary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Ink,
    onSurface = TextPrimary,
    surfaceVariant = InkElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = InkElevated,
    surfaceContainerHigh = InkElevated2,
    outline = Hairline,
    outlineVariant = Hairline,
    error = DangerRed,
    onError = Ink,
)

// 极简黑白风始终以深色呈现（Nothing 风），light 也返回深色保证观感一致
private val LightScheme = DarkScheme

@Composable
fun AutoPilotTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Ink.toArgb()
            window.navigationBarColor = Ink.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
