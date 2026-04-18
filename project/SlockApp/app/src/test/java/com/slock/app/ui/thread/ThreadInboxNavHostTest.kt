package com.slock.app.ui.thread

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThreadInboxNavHostTest {

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    private val homeBlock: String = navHostSource
        .substringAfter("threadsContent = {")
        .substringBefore("membersContent = {")

    private val threadListBlock: String = navHostSource
        .substringAfter("// Thread List Screen")
        .substringBefore("// Thread Reply Screen")

    private val threadReplyBlock: String = navHostSource
        .substringAfter("// Thread Reply Screen")
        .substringBefore("// Agent Detail Screen")

    @Test
    fun `Home threadsContent wires onTabSelected`() {
        assertTrue(
            "Home threadsContent must wire onTabSelected to selectTab",
            homeBlock.contains("onTabSelected") && homeBlock.contains("selectTab")
        )
    }

    @Test
    fun `Home threadsContent wires onMarkDone`() {
        assertTrue(
            "Home threadsContent must wire onMarkDone to markThreadDone",
            homeBlock.contains("onMarkDone") && homeBlock.contains("markThreadDone")
        )
    }

    @Test
    fun `Home threadsContent wires onUndoDone`() {
        assertTrue(
            "Home threadsContent must wire onUndoDone to undoThreadDone",
            homeBlock.contains("onUndoDone") && homeBlock.contains("undoThreadDone")
        )
    }

    @Test
    fun `Home threadsContent wires onUnfollow`() {
        assertTrue(
            "Home threadsContent must wire onUnfollow to unfollowThread",
            homeBlock.contains("onUnfollow") && homeBlock.contains("unfollowThread")
        )
    }

    @Test
    fun `Thread List standalone wires onTabSelected`() {
        assertTrue(
            "Thread List standalone must wire onTabSelected",
            threadListBlock.contains("onTabSelected") && threadListBlock.contains("selectTab")
        )
    }

    @Test
    fun `Thread List standalone wires onMarkDone`() {
        assertTrue(
            "Thread List standalone must wire onMarkDone",
            threadListBlock.contains("onMarkDone") && threadListBlock.contains("markThreadDone")
        )
    }

    @Test
    fun `Thread Reply wires onFollowThread`() {
        assertTrue(
            "Thread Reply must wire onFollowThread to viewModel followThread",
            threadReplyBlock.contains("onFollowThread") && threadReplyBlock.contains("followThread")
        )
    }

    @Test
    fun `Thread Reply wires onUnfollowThread`() {
        assertTrue(
            "Thread Reply must wire onUnfollowThread to viewModel unfollowThread",
            threadReplyBlock.contains("onUnfollowThread") && threadReplyBlock.contains("unfollowThread")
        )
    }

    @Test
    fun `Thread Reply wires onMarkDone`() {
        assertTrue(
            "Thread Reply must wire onMarkDone to viewModel markThreadDone",
            threadReplyBlock.contains("onMarkDone") && threadReplyBlock.contains("markThreadDone")
        )
    }

    @Test
    fun `Thread Reply pops back after markDone`() {
        assertTrue(
            "Thread Reply onMarkDone must pop back after marking done",
            threadReplyBlock.contains("popBackStack")
        )
    }
}
