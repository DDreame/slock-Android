package com.slock.app.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures uncaught exceptions and saves the crash log to internal storage
 * so it can be reported on the next app launch.
 */
object CrashHandler {

    private const val CRASH_LOG_FILE = "last_crash.log"

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * Install the global uncaught exception handler.
     * Call once from Application.onCreate().
     */
    fun install(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(context, thread, throwable)
            } catch (_: Exception) {
                // Last resort — don't let crash handler itself crash
            }
            // Chain to default handler (shows system crash dialog / kills process)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) { null }

        val report = buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Time: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine()
            appendLine("=== Device Info ===")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App: ${packageInfo?.versionName ?: "unknown"}")
            appendLine()
            appendLine("=== Stack Trace ===")
            append(stackTrace)
        }

        getCrashFile(context).writeText(report)
    }

    /**
     * Check if a crash log exists from a previous session.
     */
    fun hasCrashLog(context: Context): Boolean {
        return getCrashFile(context).exists()
    }

    /**
     * Read the crash log content.
     */
    fun readCrashLog(context: Context): String? {
        val file = getCrashFile(context)
        return if (file.exists()) file.readText() else null
    }

    /**
     * Delete the crash log after it has been sent or dismissed.
     */
    fun clearCrashLog(context: Context) {
        getCrashFile(context).delete()
    }

    private fun getCrashFile(context: Context): File {
        return File(context.filesDir, CRASH_LOG_FILE)
    }
}
