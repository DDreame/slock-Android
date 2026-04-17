package com.slock.app.integration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.slock.app.data.local.ActiveServerHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TestHiltActivity : ComponentActivity() {

    @Inject
    lateinit var activeServerHolder: ActiveServerHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Hilt Activity Injected")
        }
    }
}
