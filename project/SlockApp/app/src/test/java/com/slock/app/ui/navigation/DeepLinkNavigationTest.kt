package com.slock.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
        assertTrue(result.deepLinkRoute!!.contains("context="))
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
        assertEquals("channel/dm-1/messages?name=Agent%20Bot&context=", result.deepLinkRoute)
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

class DeepLinkProductionOrderingTest {

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `warm-start deep link effect is gated by isSplashDone via shouldHandleWarmStartDeepLink`() {
        assertTrue(
            "Warm-start deep link must use shouldHandleWarmStartDeepLink gate",
            navHostSource.contains("shouldHandleWarmStartDeepLink(isSplashDone, deepLinkChannelId)")
        )
    }

    @Test
    fun `warm-start deep link effect has isSplashDone as LaunchedEffect key`() {
        assertTrue(
            "Warm-start deep link LaunchedEffect must include isSplashDone as key",
            navHostSource.contains("LaunchedEffect(deepLinkChannelId, isSplashDone)")
        )
    }

    @Test
    fun `splash composable uses resolveSplashNavigation for navigation decision`() {
        assertTrue(
            "Splash must delegate to resolveSplashNavigation",
            navHostSource.contains("resolveSplashNavigation(state.isLoggedIn, deepLinkChannelId, deepLinkChannelName)")
        )
    }

    @Test
    fun `splash navigates to primary target before deep link target`() {
        val primaryNav = navHostSource.indexOf("navController.navigate(result.target)")
        val deepLinkNav = navHostSource.indexOf("navController.navigate(result.deepLinkRoute)")
        assertTrue("Primary target navigation must exist in splash", primaryNav > 0)
        assertTrue("Deep link navigation must exist in splash", deepLinkNav > 0)
        assertTrue(
            "Primary target (HOME/LOGIN) must be navigated BEFORE deep link channel",
            primaryNav < deepLinkNav
        )
    }

    @Test
    fun `isSplashDone is set AFTER both navigation calls in splash`() {
        val splashBlock = navHostSource.substringAfter("composable(Routes.SPLASH)")
            .substringBefore("composable(Routes.LOGIN)")
        val deepLinkNav = splashBlock.indexOf("navigate(result.deepLinkRoute)")
        val splashDoneSet = splashBlock.indexOf("isSplashDone = true")
        assertTrue("isSplashDone assignment must exist in splash", splashDoneSet > 0)
        assertTrue(
            "isSplashDone must be set AFTER deep link navigation to prevent warm-start effect from firing prematurely",
            splashDoneSet > deepLinkNav
        )
    }

    @Test
    fun `deep link before splash composable is gated by isSplashDone`() {
        val beforeSplash = navHostSource.substringBefore("composable(Routes.SPLASH)")
        val hasDeepLinkNav = beforeSplash.contains("channel/\$deepLinkChannelId")
        if (hasDeepLinkNav) {
            assertTrue(
                "Any deep link navigation before splash must be gated by shouldHandleWarmStartDeepLink",
                beforeSplash.contains("shouldHandleWarmStartDeepLink(isSplashDone, deepLinkChannelId)")
            )
        }
    }
}
