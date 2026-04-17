package com.slock.app

import android.app.Application
import com.slock.app.service.AppLifecycleTracker
import com.slock.app.util.CrashHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SlockApp : Application() {

    @Inject lateinit var lifecycleTracker: AppLifecycleTracker

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        lifecycleTracker.init()
    }
}
