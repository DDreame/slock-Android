package com.slock.app.ui.message

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageListHeaderContextTest {

    @Test
    fun `resolveChannelHeaderContext returns null for blank context`() {
        assertEquals(null, resolveChannelHeaderContext("  "))
    }

    @Test
    fun `resolveChannelHeaderContext returns trimmed context`() {
        assertEquals(
            "Acme Server · Search Results",
            resolveChannelHeaderContext("  Acme Server · Search Results  ")
        )
    }
}
