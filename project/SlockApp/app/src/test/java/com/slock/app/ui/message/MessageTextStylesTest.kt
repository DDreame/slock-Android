package com.slock.app.ui.message

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.slock.app.ui.theme.MessageTextStyles
import com.slock.app.ui.theme.TextSecondary
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTextStylesTest {

    private val typography = Typography()

    @Test
    fun `body style uses bodyLarge at 16sp`() {
        val style = MessageTextStyles.bodyStyle(typography)
        assertEquals(typography.bodyLarge.fontSize, style.fontSize)
        assertEquals(16.sp, style.fontSize)
    }

    @Test
    fun `body style uses 24sp lineHeight`() {
        val style = MessageTextStyles.bodyStyle(typography)
        assertEquals(24.sp, style.lineHeight)
    }

    @Test
    fun `timestamp style uses bodySmall at 12sp`() {
        val style = MessageTextStyles.timestampStyle(typography)
        assertEquals(typography.bodySmall.fontSize, style.fontSize)
        assertEquals(12.sp, style.fontSize)
    }

    @Test
    fun `timestamp style uses monospace font`() {
        val style = MessageTextStyles.timestampStyle(typography)
        assertEquals(FontFamily.Monospace, style.fontFamily)
    }

    @Test
    fun `timestamp color is TextSecondary`() {
        assertEquals(TextSecondary, MessageTextStyles.timestampColor)
    }

    @Test
    fun `agent badge style uses labelSmall at 11sp`() {
        val style = MessageTextStyles.agentBadgeStyle(typography)
        assertEquals(typography.labelSmall.fontSize, style.fontSize)
        assertEquals(11.sp, style.fontSize)
    }

    @Test
    fun `agent badge style uses bold weight`() {
        val style = MessageTextStyles.agentBadgeStyle(typography)
        assertEquals(FontWeight.Bold, style.fontWeight)
    }
}
