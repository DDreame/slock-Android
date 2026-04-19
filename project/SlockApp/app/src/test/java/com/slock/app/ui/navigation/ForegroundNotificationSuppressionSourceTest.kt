package com.slock.app.ui.navigation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundNotificationSuppressionSourceTest {

    private fun readSource(vararg candidates: String): String {
        return candidates.map(::File).first { it.exists() }.readText()
    }

    private val activitySource = readSource(
        "src/main/java/com/slock/app/MainActivity.kt",
        "app/src/main/java/com/slock/app/MainActivity.kt"
    )

    private val navHostSource = readSource(
        "src/main/java/com/slock/app/ui/navigation/NavHost.kt",
        "app/src/main/java/com/slock/app/ui/navigation/NavHost.kt"
    )

    @Test
    fun `message route reports visible channel lifecycle to notification tracker`() {
        assertTrue(
            "MainActivity must pass lifecycleTracker into SlockNavHost",
            activitySource.contains("lifecycleTracker = lifecycleTracker")
        )

        val messageRouteBlock = navHostSource
            .substringAfter("Routes.MESSAGES")
            .substringBefore("MessageListScreen(")

        assertTrue(
            "Messages route must use DisposableEffect(channelId) for visible-channel tracking",
            messageRouteBlock.contains("DisposableEffect(channelId)")
        )
        assertTrue(
            "Messages route must set visible channel on entry",
            messageRouteBlock.contains("lifecycleTracker.onChannelScreenVisible(channelId)")
        )
        assertTrue(
            "Messages route must clear visible channel on dispose",
            messageRouteBlock.contains("lifecycleTracker.onChannelScreenHidden(channelId)")
        )
    }
}
