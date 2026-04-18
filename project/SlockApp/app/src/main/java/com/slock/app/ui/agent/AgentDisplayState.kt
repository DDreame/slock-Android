package com.slock.app.ui.agent

import androidx.compose.ui.graphics.Color
import com.slock.app.ui.theme.Lime
import com.slock.app.ui.theme.Yellow
import com.slock.app.ui.theme.Pink

enum class AgentDisplayState(
    val dotColor: Color,
    val statusText: String,
    val isActive: Boolean
) {
    ONLINE(Lime, "Online", true),
    THINKING(Yellow, "Thinking...", true),
    WORKING(Lime, "Working...", true),
    ERROR(Pink, "Error", true),
    OFFLINE(Color(0xFFCCCCCC), "Hibernating", false);
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
