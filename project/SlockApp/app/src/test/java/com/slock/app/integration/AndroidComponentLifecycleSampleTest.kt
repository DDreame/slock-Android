package com.slock.app.integration

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidComponentLifecycleSampleTest {

    @Test
    fun `buildActivity creates and resumes a real ComponentActivity`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java)
        val activity = controller.create().start().resume().get()
        assertNotNull(activity)
        assertTrue(
            "Activity must reach RESUMED state",
            activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
    }

    @Test
    fun `buildActivity supports full lifecycle through pause and stop`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java)
        val activity = controller.create().start().resume().get()
        assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))

        controller.pause()
        assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))

        controller.stop()
        assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
    }

    class MinimalTestService : Service() {
        var onCreateCalled = false
        var onStartCommandCalled = false

        override fun onCreate() {
            super.onCreate()
            onCreateCalled = true
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            onStartCommandCalled = true
            return START_NOT_STICKY
        }

        override fun onBind(intent: Intent?): IBinder? = null
    }

    @Test
    fun `buildService creates and starts a real Service`() {
        val controller = Robolectric.buildService(MinimalTestService::class.java)
        val service = controller.create().get()
        assertNotNull(service)
        assertTrue("Service.onCreate must be called", service.onCreateCalled)
    }

    @Test
    fun `buildService supports startCommand lifecycle`() {
        val controller = Robolectric.buildService(MinimalTestService::class.java)
        val service = controller.create().startCommand(0, 0).get()
        assertTrue("Service.onStartCommand must be called", service.onStartCommandCalled)
    }
}
