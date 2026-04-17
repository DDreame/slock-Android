package com.slock.app.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks online/offline presence of users and agents.
 * Updated by socket events (user:presence, agent:activity).
 */
@Singleton
class PresenceTracker @Inject constructor() {

    private val _onlineIds = MutableStateFlow<Set<String>>(emptySet())
    val onlineIds: StateFlow<Set<String>> = _onlineIds.asStateFlow()

    fun setOnline(id: String) {
        _onlineIds.value = _onlineIds.value + id
    }

    fun setOffline(id: String) {
        _onlineIds.value = _onlineIds.value - id
    }

    fun isOnline(id: String): Boolean = id in _onlineIds.value

    fun updateBulk(onlineUserIds: List<String>) {
        _onlineIds.value = onlineUserIds.toSet()
    }

    fun clear() {
        _onlineIds.value = emptySet()
    }
}
