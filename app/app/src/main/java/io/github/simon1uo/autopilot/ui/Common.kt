package io.github.simon1uo.autopilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.simon1uo.autopilot.ui.theme.*

/** 极简区块标题 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalAutoColors.current.textTertiary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

/** 细线分割 */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalAutoColors.current.hairline))
}

/** 荧光绿圆角主按钮 */
@Composable
fun NeonButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LocalAutoColors.current.accent,
            contentColor = LocalAutoColors.current.onAccent,
            disabledContainerColor = LocalAutoColors.current.cardHigh,
            disabledContentColor = LocalAutoColors.current.textTertiary,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    }
}

/** 小圆角标签 */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    bg: androidx.compose.ui.graphics.Color? = null,
    fg: androidx.compose.ui.graphics.Color? = null,
) {
    val c = LocalAutoColors.current
    Box(
        modifier.clip(RoundedCornerShape(50)).background(bg ?: c.cardHigh).padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelSmall, color = fg ?: c.textSecondary) }
}