package com.slock.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.service.SocketNotificationService
import com.slock.app.ui.navigation.SlockNavHost
import com.slock.app.ui.theme.SlockAppTheme
import com.slock.app.util.CrashHandler
import com.slock.app.util.LogCollector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var secureTokenStorage: SecureTokenStorage

    private var deepLinkChannelId by mutableStateOf<String?>(null)
    private var deepLinkChannelName by mutableStateOf<String?>(null)
    private var showCrashDialog by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, service still runs — notifications just won't show */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        if (secureTokenStorage.hasTokens()) {
            SocketNotificationService.start(this)
        }

        handleDeepLink(intent)

        // Check for crash log from previous session
        if (CrashHandler.hasCrashLog(this)) {
            showCrashDialog = true
        }

        setContent {
            SlockAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SlockNavHost(
                        deepLinkChannelId = deepLinkChannelId,
                        deepLinkChannelName = deepLinkChannelName,
                        onDeepLinkConsumed = {
                            deepLinkChannelId = null
                            deepLinkChannelName = null
                        }
                    )

                    if (showCrashDialog) {
                        CrashReportDialog(
                            onSend = {
                                showCrashDialog = false
                                sendCrashReport()
                            },
                            onDismiss = {
                                showCrashDialog = false
                                CrashHandler.clearCrashLog(this@MainActivity)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun sendCrashReport() {
        val crashLog = CrashHandler.readCrashLog(this)
        val report = buildString {
            if (crashLog != null) {
                appendLine(crashLog)
                appendLine()
            }
            appendLine("=== Recent Logs ===")
            append(LogCollector.collectLogs())
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Slock App Crash Report")
            putExtra(Intent.EXTRA_TEXT, report)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(Intent.createChooser(intent, "Send Crash Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        CrashHandler.clearCrashLog(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val result = resolveDeepLinkFromIntent(
            intent?.getStringExtra("channelId"),
            intent?.getStringExtra("channelName")
        )
        if (result != null) {
            deepLinkChannelId = result.first
            deepLinkChannelName = result.second
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

internal fun resolveDeepLinkFromIntent(channelId: String?, channelName: String?): Pair<String, String?>? {
    if (channelId.isNullOrBlank()) return null
    return channelId to channelName
}
