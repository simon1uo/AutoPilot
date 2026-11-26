package io.github.simon1uo.autopilot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.simon1uo.autopilot.data.SettingsStore

/** 主题模式：dark / amoled / light / system */
object ThemeMode {
    const val DARK = "dark"
    const val AMOLED = "amoled"
    const val LIGHT = "light"
    const val SYSTEM = "system"
    val all = listOf(DARK, AMOLED, LIGHT, SYSTEM)
}

private fun schemeOf(c: AutoColors) = run {
    val base = if (c.isDark) darkColorScheme() else lightColorScheme()
    base.copy(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentDim,
        onPrimaryContainer = c.onAccent,
        secondary = c.textSecondary,
        onSecondary = c.onAccent,
        background = c.bg,
        onBackground = c.textPrimary,
        surface = c.bg,
        onSurface = c.textPrimary,
        surfaceVariant = c.cardHigh,
        onSurfaceVariant = c.textSecondary,
        surfaceContainer = c.card,
        surfaceContainerHigh = c.cardHigh,
        outline = c.hairline,
        outlineVariant = c.hairline,
        error = c.danger,
        onError = c.onAccent,
    )
}

/**
 * 根据主题模式解析生效配色。system 跟随系统在深/浅之间切换。
 */
fun resolveColors(mode: String, systemDark: Boolean): AutoColors = when (mode) {
    ThemeMode.AMOLED -> AmoledColors
    ThemeMode.LIGHT -> LightColors
    ThemeMode.DARK -> DarkColors
    ThemeMode.SYSTEM -> if (systemDark) DarkColors else LightColors
    else -> DarkColors
}

@Composable
fun AutoPilotTheme(
    mode: String = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val colors = resolveColors(mode, systemDark)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.bg.toArgb()
            window.navigationBarColor = colors.bg.toArgb()
            // 浅色主题下状态栏/导航栏图标用深色，深色/AMOLED 用浅色
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !colors.isDark
                isAppearanceLightNavigationBars = !colors.isDark
            }
        }
    }
    CompositionLocalProvider(LocalAutoColors provides colors) {
        MaterialTheme(colorScheme = schemeOf(colors), content = content)
    }
}
