package com.slock.app.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsThemeSourceTest {

    private val source: String = listOf(
        File("src/main/java/com/slock/app/ui/settings/SettingsScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/settings/SettingsScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `header description does not claim theme control`() {
        assertFalse(
            "Header should not list theme as a controllable item",
            source.contains("notifications, account details, theme,")
        )
    }

    @Test
    fun `theme subtitle indicates built-in and not configurable`() {
        assertFalse(
            "Theme subtitle should not say 'Current app theme' (implies alternatives exist)",
            source.contains("\"Current app theme\"")
        )
        assertTrue(
            "Theme subtitle should say 'Built-in app theme'",
            source.contains("\"Built-in app theme\"")
        )
    }

    @Test
    fun `theme section has no interactive controls`() {
        val themeBlock = source.substringAfter("SettingsSection(title = \"Theme\")")
            .substringBefore("SettingsSection(title = \"About\")")
        assertFalse(
            "Theme section should not contain NeoButton (no interactive controls)",
            themeBlock.contains("NeoButton")
        )
        assertFalse(
            "Theme section should not contain clickable modifier",
            themeBlock.contains(".clickable")
        )
    }

    @Test
    fun `notifications section still exists`() {
        assertTrue(
            "Notifications section must still exist",
            source.contains("SettingsSection(title = \"Notifications\")")
        )
    }

    @Test
    fun `account section still exists`() {
        assertTrue(
            "Account section must still exist",
            source.contains("SettingsSection(title = \"Account\")")
        )
    }

    @Test
    fun `about section still exists`() {
        assertTrue(
            "About section must still exist",
            source.contains("SettingsSection(title = \"About\")")
        )
    }
}
