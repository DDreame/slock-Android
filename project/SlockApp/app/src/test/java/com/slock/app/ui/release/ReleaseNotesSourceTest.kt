package com.slock.app.ui.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseNotesSourceTest {

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/release/ReleaseNotesScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/release/ReleaseNotesScreen.kt")
    ).first { it.exists() }.readText()

    private val vmSource: String = listOf(
        File("src/main/java/com/slock/app/ui/release/ReleaseNotesViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/release/ReleaseNotesViewModel.kt")
    ).first { it.exists() }.readText()

    private val navSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    private val settingsSource: String = listOf(
        File("src/main/java/com/slock/app/ui/settings/SettingsScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/settings/SettingsScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `ReleaseNoteEntry has version date and highlights`() {
        assertTrue("Must have version field", vmSource.contains("val version: String"))
        assertTrue("Must have date field", vmSource.contains("val date: String"))
        assertTrue("Must have highlights field", vmSource.contains("val highlights: List<String>"))
    }

    @Test
    fun `static release notes list is not empty`() {
        assertTrue("Must define static release notes", vmSource.contains("staticReleaseNotes"))
    }

    @Test
    fun `ReleaseNotesScreen accepts onNavigateBack`() {
        assertTrue(
            "Screen must accept onNavigateBack callback",
            screenSource.contains("onNavigateBack")
        )
    }

    @Test
    fun `screen shows Release Notes header`() {
        assertTrue(
            "Screen must show Release Notes title",
            screenSource.contains("\"Release Notes\"")
        )
    }

    @Test
    fun `screen handles empty state`() {
        assertTrue(
            "Screen must handle empty notes list",
            screenSource.contains("No release notes available")
        )
    }

    @Test
    fun `NavHost defines RELEASE_NOTES route`() {
        assertTrue(
            "Routes must define RELEASE_NOTES",
            navSource.contains("RELEASE_NOTES")
        )
    }

    @Test
    fun `NavHost wires ReleaseNotesScreen`() {
        assertTrue(
            "NavHost must wire ReleaseNotesScreen composable",
            navSource.contains("ReleaseNotesScreen")
        )
    }

    @Test
    fun `Settings has onOpenReleaseNotes callback`() {
        assertTrue(
            "SettingsScreen must accept onOpenReleaseNotes",
            settingsSource.contains("onOpenReleaseNotes")
        )
    }

    @Test
    fun `Settings About section has RELEASE NOTES button`() {
        assertTrue(
            "Settings About section must have RELEASE NOTES button",
            settingsSource.contains("\"RELEASE NOTES\"")
        )
    }

    @Test
    fun `ViewModel provides state`() {
        val vm = ReleaseNotesViewModel()
        val state = vm.state.value
        assertTrue("Default state should have notes", state.notes.isNotEmpty())
    }

    @Test
    fun `static notes have version and date`() {
        val vm = ReleaseNotesViewModel()
        val firstNote = vm.state.value.notes.first()
        assertTrue("First note version should not be blank", firstNote.version.isNotBlank())
        assertTrue("First note date should not be blank", firstNote.date.isNotBlank())
        assertTrue("First note should have highlights", firstNote.highlights.isNotEmpty())
    }
}
