package com.slock.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkNavigationTest {

    @Test
    fun `cold start logged in with deep link navigates to HOME then CHANNEL`() {
        val result = resolveSplashNavigation(
            isLoggedIn = true,
            deepLinkChannelId = "ch-123",
            deepLinkChannelName = "general"
        )

        assertEquals(Routes.HOME, result.target)
        assertNotNull(result.deepLinkRoute)
        assertTrue(result.deepLinkRoute!!.startsWith("channel/ch-123/messages"))
        assertTrue(result.deepLinkRoute!!.contains("name=general"))
    }

    @Test
    fun `cold start logged in without deep link navigates to HOME only`() {
        val result = resolveSplashNavigation(
            isLoggedIn = true,
            deepLinkChannelId = null,
            deepLinkChannelName = null
        )

        assertEquals(Routes.HOME, result.target)
        assertNull(result.deepLinkRoute)
    }

    @Test
    fun `cold start not logged in with deep link navigates to LOGIN and ignores deep link`() {
        val result = resolveSplashNavigation(
            isLoggedIn = false,
            deepLinkChannelId = "ch-123",
            deepLinkChannelName = "general"
        )

        assertEquals(Routes.LOGIN, result.target)
        assertNull(result.deepLinkRoute)
    }

    @Test
    fun `cold start not logged in without deep link navigates to LOGIN`() {
        val result = resolveSplashNavigation(
            isLoggedIn = false,
            deepLinkChannelId = null,
            deepLinkChannelName = null
        )

        assertEquals(Routes.LOGIN, result.target)
        assertNull(result.deepLinkRoute)
    }

    @Test
    fun `cold start with blank deep link channel ID is treated as no deep link`() {
        val result = resolveSplashNavigation(
            isLoggedIn = true,
            deepLinkChannelId = "",
            deepLinkChannelName = "test"
        )

        assertEquals(Routes.HOME, result.target)
        assertNull(result.deepLinkRoute)
    }

    @Test
    fun `deep link route encodes channel name with spaces`() {
        val result = resolveSplashNavigation(
            isLoggedIn = true,
            deepLinkChannelId = "dm-1",
            deepLinkChannelName = "Agent Bot"
        )

        assertNotNull(result.deepLinkRoute)
        assertEquals("channel/dm-1/messages?name=Agent%20Bot", result.deepLinkRoute)
    }

    @Test
    fun `warm start deep link fires when splash is done and channel ID is present`() {
        assertTrue(shouldHandleWarmStartDeepLink(isSplashDone = true, deepLinkChannelId = "ch-123"))
    }

    @Test
    fun `warm start deep link does NOT fire when splash is not done`() {
        assertFalse(shouldHandleWarmStartDeepLink(isSplashDone = false, deepLinkChannelId = "ch-123"))
    }

    @Test
    fun `warm start deep link does NOT fire when channel ID is null`() {
        assertFalse(shouldHandleWarmStartDeepLink(isSplashDone = true, deepLinkChannelId = null))
    }

    @Test
    fun `warm start deep link does NOT fire when channel ID is blank`() {
        assertFalse(shouldHandleWarmStartDeepLink(isSplashDone = true, deepLinkChannelId = ""))
    }
}
