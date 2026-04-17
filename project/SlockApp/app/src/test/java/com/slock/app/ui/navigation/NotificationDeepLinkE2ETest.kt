package com.slock.app.ui.navigation

import com.slock.app.resolveDeepLinkFromIntent
import com.slock.app.service.resolveNotificationChannelName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WarmStartDeepLinkNavTest {

    @Test
    fun `resolveWarmStartDeepLinkNav returns correct route for channel`() {
        val action = resolveWarmStartDeepLinkNav("ch-123", "general")
        assertTrue(action.route.startsWith("channel/ch-123/messages"))
        assertTrue(action.route.contains("name=general"))
    }

    @Test
    fun `resolveWarmStartDeepLinkNav keeps HOME on back stack`() {
        val action = resolveWarmStartDeepLinkNav("ch-123", "general")
        assertEquals(Routes.HOME, action.popUpToRoute)
        assertEquals(false, action.inclusive)
    }

    @Test
    fun `resolveWarmStartDeepLinkNav uses singleTop`() {
        val action = resolveWarmStartDeepLinkNav("ch-123", null)
        assertTrue(action.singleTop)
    }

    @Test
    fun `resolveWarmStartDeepLinkNav defaults null channelName to empty`() {
        val action = resolveWarmStartDeepLinkNav("ch-123", null)
        assertTrue(action.route.contains("name="))
        assertTrue(action.route.startsWith("channel/ch-123/messages"))
    }

    @Test
    fun `resolveWarmStartDeepLinkNav encodes spaces in channel name`() {
        val action = resolveWarmStartDeepLinkNav("dm-1", "Agent Bot")
        assertTrue(action.route.contains("Agent%20Bot"))
    }
}

class NotificationChannelNameTest {

    @Test
    fun `DM notification uses sender name as channel name`() {
        assertEquals("Alice", resolveNotificationChannelName(isDm = true, senderName = "Alice"))
    }

    @Test
    fun `non-DM notification uses empty channel name`() {
        assertEquals("", resolveNotificationChannelName(isDm = false, senderName = "Alice"))
    }

    @Test
    fun `DM with blank sender name still returns it`() {
        assertEquals("", resolveNotificationChannelName(isDm = true, senderName = ""))
    }
}

class DeepLinkFromIntentTest {

    @Test
    fun `valid channelId returns pair with channelId and channelName`() {
        val result = resolveDeepLinkFromIntent("ch-123", "general")
        assertNotNull(result)
        assertEquals("ch-123", result!!.first)
        assertEquals("general", result.second)
    }

    @Test
    fun `null channelId returns null`() {
        assertNull(resolveDeepLinkFromIntent(null, "general"))
    }

    @Test
    fun `blank channelId returns null`() {
        assertNull(resolveDeepLinkFromIntent("", "general"))
        assertNull(resolveDeepLinkFromIntent("   ", null))
    }

    @Test
    fun `null channelName is passed through`() {
        val result = resolveDeepLinkFromIntent("ch-123", null)
        assertNotNull(result)
        assertEquals("ch-123", result!!.first)
        assertNull(result.second)
    }
}

class NotificationDeepLinkChainStructuralTest {

    private val serviceSource: String = listOf(
        File("src/main/java/com/slock/app/service/SocketNotificationService.kt"),
        File("app/src/main/java/com/slock/app/service/SocketNotificationService.kt")
    ).first { it.exists() }.readText()

    private val activitySource: String = listOf(
        File("src/main/java/com/slock/app/MainActivity.kt"),
        File("app/src/main/java/com/slock/app/MainActivity.kt")
    ).first { it.exists() }.readText()

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    // Step 1: Service builds intent with channelId and channelName
    @Test
    fun `service puts channelId extra into deep link intent`() {
        val intentBlock = serviceSource
            .substringAfter("Deep link intent")
            .substringBefore("PendingIntent")
        assertTrue(
            "Service must put channelId into intent extras",
            intentBlock.contains("putExtra(\"channelId\"")
        )
    }

    @Test
    fun `service puts channelName extra using resolveNotificationChannelName`() {
        val intentBlock = serviceSource
            .substringAfter("Deep link intent")
            .substringBefore("PendingIntent")
        assertTrue(
            "Service must use resolveNotificationChannelName for channelName extra",
            intentBlock.contains("resolveNotificationChannelName(")
        )
    }

    // Step 2: MainActivity extracts via resolveDeepLinkFromIntent
    @Test
    fun `activity handleDeepLink uses resolveDeepLinkFromIntent`() {
        val handleBlock = activitySource
            .substringAfter("handleDeepLink")
            .substringBefore("requestNotificationPermissionIfNeeded")
        assertTrue(
            "handleDeepLink must use resolveDeepLinkFromIntent helper",
            handleBlock.contains("resolveDeepLinkFromIntent(")
        )
    }

    @Test
    fun `activity passes deep link state to NavHost`() {
        assertTrue(
            "MainActivity must pass deepLinkChannelId to SlockNavHost",
            activitySource.contains("deepLinkChannelId = deepLinkChannelId")
        )
        assertTrue(
            "MainActivity must pass deepLinkChannelName to SlockNavHost",
            activitySource.contains("deepLinkChannelName = deepLinkChannelName")
        )
    }

    @Test
    fun `activity handles onNewIntent for warm start`() {
        assertTrue(
            "onNewIntent must call handleDeepLink for warm-start notification taps",
            activitySource.contains("override fun onNewIntent") && activitySource.contains("handleDeepLink(intent)")
        )
    }

    // Step 3: NavHost resolves navigation
    @Test
    fun `NavHost warm-start uses resolveWarmStartDeepLinkNav`() {
        assertTrue(
            "NavHost must use resolveWarmStartDeepLinkNav for warm-start deep link",
            navHostSource.contains("resolveWarmStartDeepLinkNav(deepLinkChannelId")
        )
    }

    @Test
    fun `NavHost calls onDeepLinkConsumed after navigation`() {
        val warmBlock = navHostSource
            .substringAfter("resolveWarmStartDeepLinkNav")
            .substringBefore("NavHost(")
        assertTrue(
            "NavHost must call onDeepLinkConsumed after processing deep link",
            warmBlock.contains("onDeepLinkConsumed()")
        )
    }

    @Test
    fun `NavHost cold start uses resolveSplashNavigation with deep link data`() {
        assertTrue(
            "NavHost splash must use resolveSplashNavigation with deep link params",
            navHostSource.contains("resolveSplashNavigation(state.isLoggedIn, deepLinkChannelId, deepLinkChannelName)")
        )
    }
}
