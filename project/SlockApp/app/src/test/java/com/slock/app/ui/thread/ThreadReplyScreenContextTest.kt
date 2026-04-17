package com.slock.app.ui.thread

import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadReplyScreenContextTest {

    @Test
    fun `resolveThreadHeaderContext returns null for blank context`() {
        assertEquals(null, resolveThreadHeaderContext(""))
    }

    @Test
    fun `resolveThreadHeaderContext returns trimmed context`() {
        assertEquals("Acme Server · Threads", resolveThreadHeaderContext("  Acme Server · Threads  "))
    }
}
