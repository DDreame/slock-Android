package com.slock.app.service

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleTracker @Inject constructor() : DefaultLifecycleObserver {

    @Volatile
    var isAppInForeground: Boolean = false
        private set

    @Volatile
    var currentVisibleChannelId: String? = null
        private set

    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun onChannelScreenVisible(channelId: String) {
        currentVisibleChannelId = channelId
    }

    fun onChannelScreenHidden(channelId: String? = null) {
        if (channelId == null || currentVisibleChannelId == channelId) {
            currentVisibleChannelId = null
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
    }
}
