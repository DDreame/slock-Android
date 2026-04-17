package com.slock.app.util

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader

object LogCollector {

    fun collectLogs(maxLines: Int = 200): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -t $maxLines")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logs = reader.readText()
            reader.close()
            process.waitFor()
            logs
        } catch (e: Exception) {
            "Failed to collect logs: ${e.message}"
        }
    }

    private fun collectDeviceInfo(context: Context): String {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) { null }
        return buildString {
            appendLine("=== Device Info ===")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App: ${packageInfo?.versionName ?: "unknown"}")
        }
    }

    fun buildReport(context: Context, errorContext: String? = null): String {
        return buildString {
            appendLine(collectDeviceInfo(context))
            if (errorContext != null) {
                appendLine("=== Error Context ===")
                appendLine(errorContext)
                appendLine()
            }
            appendLine("=== Recent Logs ===")
            append(collectLogs())
        }
    }

    fun shareReport(context: Context, errorContext: String? = null) {
        val report = buildReport(context, errorContext)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Slock App Log Report")
            putExtra(Intent.EXTRA_TEXT, report)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Send Log Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
