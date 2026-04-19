package com.slock.app.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.slock.app.resolveDeepLinkFromIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SocketNotificationPhase1Test {

    @Test
    fun `foreground same channel notification is suppressed`() {
        assertTrue(
            shouldSuppressForegroundChannelNotification(
                isAppInForeground = true,
                currentVisibleChannelId = "channel-1",
                incomingChannelId = "channel-1"
            )
        )
    }

    @Test
    fun `foreground different channel notification is not suppressed`() {
        assertFalse(
            shouldSuppressForegroundChannelNotification(
                isAppInForeground = true,
                currentVisibleChannelId = "channel-1",
                incomingChannelId = "channel-2"
            )
        )
    }

    @Test
    fun `DM notification deep link intent preserves channel extras`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = buildNotificationDeepLinkIntent(
            context = context,
            channelId = "dm-1",
            channelName = "Alice"
        )

        assertEquals("channel", intent.getStringExtra("navigate_to"))
        assertEquals("dm-1", intent.getStringExtra("channelId"))
        assertEquals("Alice", intent.getStringExtra("channelName"))

        val deepLink = resolveDeepLinkFromIntent(
            intent.getStringExtra("channelId"),
            intent.getStringExtra("channelName")
        )
        assertNotNull(deepLink)
        assertEquals("dm-1", deepLink!!.first)
        assertEquals("Alice", deepLink.second)
    }
}
