package com.slock.app.ui.agent

import androidx.compose.ui.graphics.Color
import com.slock.app.ui.theme.Lime
import com.slock.app.ui.theme.Yellow
import com.slock.app.ui.theme.Pink

enum class AgentDisplayState(
    val dotColor: Color,
    val statusText: String,
    val isActive: Boolean,
    val toggleLabel: String
) {
    ONLINE(Lime, "Online", true, "Stop"),
    THINKING(Yellow, "Thinking...", true, "Stop"),
    WORKING(Lime, "Working...", true, "Stop"),
    ERROR(Pink, "Error", true, "Stop"),
    OFFLINE(Color(0xFFCCCCCC), "Hibernating", false, "Start");
}

fun resolveDisplayState(
    status: String?,
    activity: String?
): AgentDisplayState {
    if (status != "active") return AgentDisplayState.OFFLINE
    if (activity.isNullOrBlank()) return AgentDisplayState.ONLINE
    val lower = activity.lowercase()
    return when {
        lower.contains("error") -> AgentDisplayState.ERROR
        lower.contains("thinking") || lower.contains("planning") -> AgentDisplayState.THINKING
        else -> AgentDisplayState.WORKING
    }
}
