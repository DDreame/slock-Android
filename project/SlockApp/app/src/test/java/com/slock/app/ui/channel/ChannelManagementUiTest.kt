package com.slock.app.ui.channel

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChannelManagementOverflowMenuTest {

    private val homeScreenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/home/HomeScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/home/HomeScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `ChannelItem has overflow menu with DropdownMenu`() {
        val channelItemBlock = homeScreenSource
            .substringAfter("private fun ChannelItem(")
            .substringBefore("private fun formatPreviewTime(")
        assertTrue(
            "ChannelItem must contain a DropdownMenu for overflow actions",
            channelItemBlock.contains("DropdownMenu(")
        )
    }

    @Test
    fun `overflow menu has Edit entry with callback`() {
        val channelItemBlock = homeScreenSource
            .substringAfter("DropdownMenu(")
            .substringBefore("private fun formatPreviewTime(")
        assertTrue(
            "DropdownMenu must have Edit menu item",
            channelItemBlock.contains("\"Edit\"")
        )
        assertTrue(
            "Edit menu item must call onEdit",
            channelItemBlock.contains("onEdit()")
        )
    }

    @Test
    fun `overflow menu has Leave entry with callback`() {
        val channelItemBlock = homeScreenSource
            .substringAfter("DropdownMenu(")
            .substringBefore("private fun formatPreviewTime(")
        assertTrue(
            "DropdownMenu must have Leave menu item",
            channelItemBlock.contains("\"Leave\"")
        )
        assertTrue(
            "Leave menu item must call onLeave",
            channelItemBlock.contains("onLeave()")
        )
    }

    @Test
    fun `overflow menu has Delete entry with callback`() {
        val channelItemBlock = homeScreenSource
            .substringAfter("DropdownMenu(")
            .substringBefore("private fun formatPreviewTime(")
        assertTrue(
            "DropdownMenu must have Delete menu item",
            channelItemBlock.contains("\"Delete\"")
        )
        assertTrue(
            "Delete menu item must call onDelete",
            channelItemBlock.contains("onDelete()")
        )
    }

    @Test
    fun `ChannelItem accepts onEdit onDelete onLeave callbacks`() {
        val signature = homeScreenSource
            .substringAfter("private fun ChannelItem(")
            .substringBefore(") {")
        assertTrue("ChannelItem must accept onEdit callback", signature.contains("onEdit"))
        assertTrue("ChannelItem must accept onDelete callback", signature.contains("onDelete"))
        assertTrue("ChannelItem must accept onLeave callback", signature.contains("onLeave"))
    }

    @Test
    fun `ChannelListContent passes management callbacks to ChannelItem`() {
        val listContentBlock = homeScreenSource
            .substringAfter("private fun ChannelListContent(")
            .substringBefore("private fun NeoTopBar(")
        assertTrue(
            "ChannelListContent must pass onEdit to ChannelItem",
            listContentBlock.contains("onEdit = {") || listContentBlock.contains("onEdit =")
        )
        assertTrue(
            "ChannelListContent must pass onDelete to ChannelItem",
            listContentBlock.contains("onDelete = {") || listContentBlock.contains("onDelete =")
        )
        assertTrue(
            "ChannelListContent must pass onLeave to ChannelItem",
            listContentBlock.contains("onLeave = {") || listContentBlock.contains("onLeave =")
        )
    }
}

class ChannelManagementDialogTest {

    private val homeScreenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/home/HomeScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/home/HomeScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `EditChannelNeoDialog exists and accepts currentName`() {
        assertTrue(
            "HomeScreen must contain EditChannelNeoDialog composable",
            homeScreenSource.contains("fun EditChannelNeoDialog(")
        )
        val signature = homeScreenSource
            .substringAfter("fun EditChannelNeoDialog(")
            .substringBefore(") {")
        assertTrue(
            "EditChannelNeoDialog must accept currentName parameter",
            signature.contains("currentName: String")
        )
    }

    @Test
    fun `EditChannelNeoDialog calls onSave with new name`() {
        val dialogBlock = homeScreenSource
            .substringAfter("fun EditChannelNeoDialog(")
            .substringBefore("fun ConfirmActionNeoDialog(")
        assertTrue(
            "EditChannelNeoDialog must call onSave callback",
            dialogBlock.contains("onSave(")
        )
    }

    @Test
    fun `EditChannelNeoDialog SAVE button only enabled when name changed`() {
        val dialogBlock = homeScreenSource
            .substringAfter("fun EditChannelNeoDialog(")
            .substringBefore("fun ConfirmActionNeoDialog(")
        assertTrue(
            "SAVE button must be disabled when name equals currentName",
            dialogBlock.contains("name != currentName")
        )
    }

    @Test
    fun `ConfirmActionNeoDialog exists with title message and confirm callback`() {
        assertTrue(
            "HomeScreen must contain ConfirmActionNeoDialog composable",
            homeScreenSource.contains("fun ConfirmActionNeoDialog(")
        )
        val signature = homeScreenSource
            .substringAfter("fun ConfirmActionNeoDialog(")
            .substringBefore(") {")
        assertTrue("Must accept title", signature.contains("title: String"))
        assertTrue("Must accept message", signature.contains("message: String"))
        assertTrue("Must accept onConfirm", signature.contains("onConfirm"))
    }

    @Test
    fun `HomeScreen shows EditChannelNeoDialog when editingChannel is set`() {
        val homeScreenBlock = homeScreenSource
            .substringAfter("fun HomeScreen(")
            .substringBefore("private fun ChannelsTabContent(")
        assertTrue(
            "HomeScreen must show EditChannelNeoDialog via editingChannel state",
            homeScreenBlock.contains("editingChannel?.let") && homeScreenBlock.contains("EditChannelNeoDialog(")
        )
    }

    @Test
    fun `HomeScreen shows delete confirmation dialog when deletingChannelId is set`() {
        val homeScreenBlock = homeScreenSource
            .substringAfter("fun HomeScreen(")
            .substringBefore("private fun ChannelsTabContent(")
        assertTrue(
            "HomeScreen must show ConfirmActionNeoDialog for delete via deletingChannelId state",
            homeScreenBlock.contains("deletingChannelId?.let") && homeScreenBlock.contains("\"Delete Channel\"")
        )
    }

    @Test
    fun `HomeScreen shows leave confirmation dialog when leavingChannelId is set`() {
        val homeScreenBlock = homeScreenSource
            .substringAfter("fun HomeScreen(")
            .substringBefore("private fun ChannelsTabContent(")
        assertTrue(
            "HomeScreen must show ConfirmActionNeoDialog for leave via leavingChannelId state",
            homeScreenBlock.contains("leavingChannelId?.let") && homeScreenBlock.contains("\"Leave Channel\"")
        )
    }
}

class ChannelManagementNavHostWiringTest {

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `NavHost wires onEditChannel to channelViewModel updateChannel`() {
        val homeBlock = navHostSource
            .substringAfter("HomeScreen(")
            .substringBefore("composable(Routes.SETTINGS)")
        assertTrue(
            "NavHost must wire onEditChannel to channelViewModel::updateChannel",
            homeBlock.contains("onEditChannel = channelViewModel::updateChannel")
        )
    }

    @Test
    fun `NavHost wires onDeleteChannel to channelViewModel deleteChannel`() {
        val homeBlock = navHostSource
            .substringAfter("HomeScreen(")
            .substringBefore("composable(Routes.SETTINGS)")
        assertTrue(
            "NavHost must wire onDeleteChannel to channelViewModel::deleteChannel",
            homeBlock.contains("onDeleteChannel = channelViewModel::deleteChannel")
        )
    }

    @Test
    fun `NavHost wires onLeaveChannel to channelViewModel leaveChannel`() {
        val homeBlock = navHostSource
            .substringAfter("HomeScreen(")
            .substringBefore("composable(Routes.SETTINGS)")
        assertTrue(
            "NavHost must wire onLeaveChannel to channelViewModel::leaveChannel",
            homeBlock.contains("onLeaveChannel = channelViewModel::leaveChannel")
        )
    }

    @Test
    fun `NavHost consumes actionFeedbackMessage via Toast`() {
        val homeComposable = navHostSource
            .substringAfter("composable(Routes.HOME)")
            .substringBefore("composable(Routes.SETTINGS)")
        assertTrue(
            "NavHost HOME must observe actionFeedbackMessage",
            homeComposable.contains("actionFeedbackMessage")
        )
        assertTrue(
            "NavHost HOME must show Toast for action feedback",
            homeComposable.contains("Toast.makeText")
        )
        assertTrue(
            "NavHost HOME must call consumeActionFeedback after showing Toast",
            homeComposable.contains("consumeActionFeedback()")
        )
    }

    @Test
    fun `HomeScreen accepts onEditChannel onDeleteChannel onLeaveChannel callbacks`() {
        val homeScreenSource = listOf(
            File("src/main/java/com/slock/app/ui/home/HomeScreen.kt"),
            File("app/src/main/java/com/slock/app/ui/home/HomeScreen.kt")
        ).first { it.exists() }.readText()

        val signature = homeScreenSource
            .substringAfter("fun HomeScreen(")
            .substringBefore(") {")
        assertTrue("HomeScreen must accept onEditChannel", signature.contains("onEditChannel"))
        assertTrue("HomeScreen must accept onDeleteChannel", signature.contains("onDeleteChannel"))
        assertTrue("HomeScreen must accept onLeaveChannel", signature.contains("onLeaveChannel"))
    }
}
