package com.slock.app.ui.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentDisplayStateTest {

    @Test
    fun `stopped agent returns OFFLINE regardless of activity`() {
        assertEquals(AgentDisplayState.OFFLINE, resolveDisplayState("stopped", "Thinking"))
        assertEquals(AgentDisplayState.OFFLINE, resolveDisplayState("stopped", null))
        assertEquals(AgentDisplayState.OFFLINE, resolveDisplayState(null, "Working"))
    }

    @Test
    fun `active agent with no activity returns ONLINE`() {
        assertEquals(AgentDisplayState.ONLINE, resolveDisplayState("active", null))
        assertEquals(AgentDisplayState.ONLINE, resolveDisplayState("active", ""))
        assertEquals(AgentDisplayState.ONLINE, resolveDisplayState("active", "   "))
    }

    @Test
    fun `active agent with thinking activity returns THINKING`() {
        assertEquals(AgentDisplayState.THINKING, resolveDisplayState("active", "Thinking"))
        assertEquals(AgentDisplayState.THINKING, resolveDisplayState("active", "thinking"))
        assertEquals(AgentDisplayState.THINKING, resolveDisplayState("active", "Planning next step"))
        assertEquals(AgentDisplayState.THINKING, resolveDisplayState("active", "planning"))
    }

    @Test
    fun `active agent with error activity returns ERROR`() {
        assertEquals(AgentDisplayState.ERROR, resolveDisplayState("active", "Error occurred"))
        assertEquals(AgentDisplayState.ERROR, resolveDisplayState("active", "error"))
        assertEquals(AgentDisplayState.ERROR, resolveDisplayState("active", "Tool error: timeout"))
    }

    @Test
    fun `active agent with other activity returns WORKING`() {
        assertEquals(AgentDisplayState.WORKING, resolveDisplayState("active", "Processing messages"))
        assertEquals(AgentDisplayState.WORKING, resolveDisplayState("active", "Tool call"))
        assertEquals(AgentDisplayState.WORKING, resolveDisplayState("active", "Reading file"))
    }

    @Test
    fun `OFFLINE state isActive is false`() {
        assertFalse(AgentDisplayState.OFFLINE.isActive)
    }

    @Test
    fun `all active states have isActive true`() {
        assertTrue(AgentDisplayState.ONLINE.isActive)
        assertTrue(AgentDisplayState.THINKING.isActive)
        assertTrue(AgentDisplayState.WORKING.isActive)
        assertTrue(AgentDisplayState.ERROR.isActive)
    }

    @Test
    fun `status text matches expected values`() {
        assertEquals("Online", AgentDisplayState.ONLINE.statusText)
        assertEquals("Thinking...", AgentDisplayState.THINKING.statusText)
        assertEquals("Working...", AgentDisplayState.WORKING.statusText)
        assertEquals("Error", AgentDisplayState.ERROR.statusText)
        assertEquals("Hibernating", AgentDisplayState.OFFLINE.statusText)
    }
}
