package com.slock.app.service

import com.slock.app.data.local.NotificationPreference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDecisionTest {

    @Test
    fun allMessages_notifiesForRegularMessages() {
        assertTrue(
            NotificationDecision.shouldNotify(
                preference = NotificationPreference.ALL_MESSAGES,
                isDm = false,
                isMentioned = false
            )
        )
    }

    @Test
    fun mentionsOnly_notifiesForDirectMessages() {
        assertTrue(
            NotificationDecision.shouldNotify(
                preference = NotificationPreference.MENTIONS_ONLY,
                isDm = true,
                isMentioned = false
            )
        )
    }

    @Test
    fun mentionsOnly_notifiesForMentionedMessages() {
        assertTrue(
            NotificationDecision.shouldNotify(
                preference = NotificationPreference.MENTIONS_ONLY,
                isDm = false,
                isMentioned = true
            )
        )
    }

    @Test
    fun mentionsOnly_skipsRegularMessages() {
        assertFalse(
            NotificationDecision.shouldNotify(
                preference = NotificationPreference.MENTIONS_ONLY,
                isDm = false,
                isMentioned = false
            )
        )
    }

    @Test
    fun mute_skipsEveryMessageType() {
        assertFalse(
            NotificationDecision.shouldNotify(
                preference = NotificationPreference.MUTE,
                isDm = true,
                isMentioned = true
            )
        )
        assertFalse(
            NotificationDecision.shouldNotify(
                preference = NotificationPreference.MUTE,
                isDm = false,
                isMentioned = false
            )
        )
    }
}
