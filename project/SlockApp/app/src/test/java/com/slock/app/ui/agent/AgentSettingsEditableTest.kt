package com.slock.app.ui.agent

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AgentSettingsEditableTest {

    private val source: String = listOf(
        File("src/main/java/com/slock/app/ui/agent/AgentListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/agent/AgentListScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `AgentSettingsSheet onSave signature includes name description prompt`() {
        val sheetSignature = source.substringAfter("private fun AgentSettingsSheet(")
            .substringBefore(") {")
        assertTrue(
            "onSave must accept name parameter",
            sheetSignature.contains("name: String?")
        )
        assertTrue(
            "onSave must accept description parameter",
            sheetSignature.contains("description: String?")
        )
        assertTrue(
            "onSave must accept prompt parameter",
            sheetSignature.contains("prompt: String?")
        )
    }

    @Test
    fun `AgentSettingsSheet has editable name field`() {
        val sheetBody = source.substringAfter("private fun AgentSettingsSheet(")
            .substringBefore("NeoLabel(\"RUNTIME\")")
        assertTrue(
            "AgentSettingsSheet must have NeoTextField for name",
            sheetBody.contains("draftName") && sheetBody.contains("NeoLabel(\"NAME\")")
        )
    }

    @Test
    fun `AgentSettingsSheet has editable description field`() {
        val sheetBody = source.substringAfter("private fun AgentSettingsSheet(")
            .substringBefore("NeoLabel(\"RUNTIME\")")
        assertTrue(
            "AgentSettingsSheet must have NeoTextField for description",
            sheetBody.contains("draftDescription") && sheetBody.contains("NeoLabel(\"ROLE DESCRIPTION\")")
        )
    }

    @Test
    fun `AgentSettingsSheet has editable prompt field`() {
        val sheetBody = source.substringAfter("private fun AgentSettingsSheet(")
            .substringBefore("NeoLabel(\"RUNTIME\")")
        assertTrue(
            "AgentSettingsSheet must have NeoTextField for prompt",
            sheetBody.contains("draftPrompt") && sheetBody.contains("NeoLabel(\"SYSTEM PROMPT\")")
        )
    }

    @Test
    fun `AgentSettingsSheet model remains read-only`() {
        val sheetBody = source.substringAfter("private fun AgentSettingsSheet(")
            .substringBefore("NeoLabel(\"RUNTIME\")")
        assertTrue(
            "MODEL must remain as read-only SettingsRow",
            sheetBody.contains("SettingsRow(label = \"MODEL\")")
        )
        assertFalse(
            "MODEL must not have a NeoTextField",
            sheetBody.contains("NeoLabel(\"MODEL\")")
        )
    }

    @Test
    fun `call site passes name description prompt to onUpdateAgent`() {
        val callSite = source.substringAfter("AgentSettingsSheet(")
            .substringBefore("onDelete = {")
        assertTrue(
            "onSave must pass name to onUpdateAgent",
            callSite.contains("name,")
        )
        assertTrue(
            "onSave must pass description to onUpdateAgent",
            callSite.contains("description,")
        )
        assertTrue(
            "onSave must pass prompt to onUpdateAgent",
            callSite.contains("prompt,")
        )
        assertFalse(
            "onSave must not pass null for name/description/prompt",
            callSite.contains("null,\n                    null,\n                    null,")
        )
    }

    @Test
    fun `SAVE CONFIG sends draftName draftDescription draftPrompt`() {
        val saveBlock = source.substringAfter("text = \"SAVE CONFIG\"")
            .substringBefore("containerColor = Yellow")
        assertTrue(
            "SAVE CONFIG must include draftName",
            saveBlock.contains("draftName")
        )
        assertTrue(
            "SAVE CONFIG must include draftDescription",
            saveBlock.contains("draftDescription")
        )
        assertTrue(
            "SAVE CONFIG must include draftPrompt",
            saveBlock.contains("draftPrompt")
        )
    }
}
