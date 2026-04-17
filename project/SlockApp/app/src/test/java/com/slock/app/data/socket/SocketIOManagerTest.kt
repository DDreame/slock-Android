package com.slock.app.data.socket

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocketIOManagerTest {

    @Test
    fun `reconnect required when existing socket server changes`() {
        assertTrue(
            shouldReconnectForServerChange(
                currentServerId = "server-a",
                requestedServerId = "server-b",
                hasSocket = true
            )
        )
    }

    @Test
    fun `reconnect not required when existing socket server stays the same`() {
        assertFalse(
            shouldReconnectForServerChange(
                currentServerId = "server-a",
                requestedServerId = "server-a",
                hasSocket = true
            )
        )
    }

    @Test
    fun `reconnect not required when there is no socket session`() {
        assertFalse(
            shouldReconnectForServerChange(
                currentServerId = "server-a",
                requestedServerId = "server-b",
                hasSocket = false
            )
        )
    }
}
