package io.github.simon1uo.autopilot.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 应用配色。深色 / AMOLED / 浅色三套为「显式」配色；「跟随系统」在运行时决定具体用哪套。
 *
 * 各屏的取色统一通过 [LocalAutoColors]，从而随主题即时切换。
 * 为了兼容历史代码（大量直接引用 Ink / NeonGreen / TextPrimary 等），
 * 这里保留同名顶层常量作为「深色默认值」，新代码请改用 LocalAutoColors.current.xxx。
 */
data class AutoColors(
    val accent: Color,          // 主强调色
    val accentDim: Color,       // 强调色次级（按下/容器）
    val onAccent: Color,        // 强调色之上的文字
    val bg: Color,              // 页面背景
    val card: Color,            // 卡片
    val cardHigh: Color,        // 次级卡片 / 输入框
    val hairline: Color,        // 极细分割线 / 描边
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val danger: Color,
    val isDark: Boolean,        // 供状态栏图标明暗判断
)

// ===== 深色（Nothing 风）=====
val DarkColors = AutoColors(
    accent = Color(0xFFD6FF3F),
    accentDim = Color(0xFFB8E02E),
    onAccent = Color(0xFF0A0A0A),
    bg = Color(0xFF0A0A0A),
    card = Color(0xFF161616),
    cardHigh = Color(0xFF1F1F1F),
    hairline = Color(0xFF2A2A2A),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFF9A9A9A),
    textTertiary = Color(0xFF6B6B6B),
    danger = Color(0xFFFF5C5C),
    isDark = true,
)

// ===== 纯黑 AMOLED =====
val AmoledColors = AutoColors(
    accent = Color(0xFFD6FF3F),
    accentDim = Color(0xFFB8E02E),
    onAccent = Color(0xFF000000),
    bg = Color(0xFF000000),
    card = Color(0xFF0A0A0A),
    cardHigh = Color(0xFF141414),
    hairline = Color(0xFF232323),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFF9A9A9A),
    textTertiary = Color(0xFF6B6B6B),
    danger = Color(0xFFFF5C5C),
    isDark = true,
)

// ===== 浅色（近白底 + 深炭黑字 + 深绿强调，保证对比度）=====
val LightColors = AutoColors(
    accent = Color(0xFF4E6B00),        // 深绿，白底可读
    accentDim = Color(0xFF6E9000),
    onAccent = Color(0xFFFFFFFF),
    bg = Color(0xFFFAFAFA),
    card = Color(0xFFFFFFFF),
    cardHigh = Color(0xFFF1F2EE),
    hairline = Color(0xFFE2E3DE),
    textPrimary = Color(0xFF1A1C18),
    textSecondary = Color(0xFF5B5F57),
    textTertiary = Color(0xFF8A8F84),
    danger = Color(0xFFC62828),
    isDark = false,
)

/** 当前生效的配色；由 AutoPilotTheme 根据 themeMode 提供。 */
val LocalAutoColors = staticCompositionLocalOf { DarkColors }

// ===== 兼容旧引用的深色默认顶层常量（新代码请用 LocalAutoColors.current）=====
val NeonGreen = DarkColors.accent
val NeonGreenDim = DarkColors.accentDim
val Ink = DarkColors.bg
val InkElevated = DarkColors.card
val InkElevated2 = DarkColors.cardHigh
val Hairline = DarkColors.hairline
val TextPrimary = DarkColors.textPrimary
val TextSecondary = DarkColors.textSecondary
val TextTertiary = DarkColors.textTertiary
val DangerRed = DarkColors.danger
