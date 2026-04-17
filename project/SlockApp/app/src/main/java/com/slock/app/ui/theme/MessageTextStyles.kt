package com.slock.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MessageTextStyles {

    fun bodyStyle(typography: Typography): TextStyle =
        typography.bodyLarge.copy(lineHeight = 24.sp)

    fun timestampStyle(typography: Typography): TextStyle =
        typography.bodySmall.copy(fontFamily = FontFamily.Monospace)

    val timestampColor: Color = TextSecondary

    fun agentBadgeStyle(typography: Typography): TextStyle =
        typography.labelSmall.copy(fontWeight = FontWeight.Bold)
}
