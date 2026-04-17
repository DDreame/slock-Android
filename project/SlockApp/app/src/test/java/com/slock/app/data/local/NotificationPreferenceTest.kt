package com.slock.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPreferenceTest {

    @Test
    fun fromStorageValue_returnsMatchingPreference() {
        assertEquals(
            NotificationPreference.MENTIONS_ONLY,
            NotificationPreference.fromStorageValue("mentions_only")
        )
        assertEquals(
            NotificationPreference.ALL_MESSAGES,
            NotificationPreference.fromStorageValue("all_messages")
        )
        assertEquals(
            NotificationPreference.MUTE,
            NotificationPreference.fromStorageValue("mute")
        )
    }

    @Test
    fun fromStorageValue_defaultsToAllMessagesForUnknownOrMissingValues() {
        assertEquals(
            NotificationPreference.ALL_MESSAGES,
            NotificationPreference.fromStorageValue(null)
        )
        assertEquals(
            NotificationPreference.ALL_MESSAGES,
            NotificationPreference.fromStorageValue("unexpected")
        )
    }
}
