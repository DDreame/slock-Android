package com.slock.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SlockApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
