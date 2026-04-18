package com.slock.app.ui.release

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReleaseNoteEntry(
    val version: String,
    val date: String,
    val highlights: List<String>
)

data class ReleaseNotesUiState(
    val notes: List<ReleaseNoteEntry> = staticReleaseNotes
)

class ReleaseNotesViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReleaseNotesUiState())
    val state: StateFlow<ReleaseNotesUiState> = _state.asStateFlow()
}

private val staticReleaseNotes = listOf(
    ReleaseNoteEntry(
        version = "0.5.0",
        date = "2026-04-18",
        highlights = listOf(
            "Agent Reset UI — reset agents from AgentDetail with confirmation dialog",
            "GFM Task List rendering — checkbox-style task items in messages",
            "Settings Theme semantic cleanup — clarified non-configurable theme label",
            "DM retry dead-loop fix — resolved infinite retry when serverId is null"
        )
    ),
    ReleaseNoteEntry(
        version = "0.4.0",
        date = "2026-04-10",
        highlights = listOf(
            "Agent activity log with real-time socket updates",
            "Machine management — view connected machines from AgentDetail",
            "Thread entry and reply support in message list",
            "Neo Brutalism design system refinements"
        )
    ),
    ReleaseNoteEntry(
        version = "0.3.0",
        date = "2026-03-28",
        highlights = listOf(
            "Agent start/stop controls on AgentDetail screen",
            "Direct message channels with creation flow",
            "Saved channels feature in Settings",
            "Message attachment support with image preview"
        )
    ),
    ReleaseNoteEntry(
        version = "0.2.0",
        date = "2026-03-15",
        highlights = listOf(
            "Channel list with unread indicators",
            "Message list with Markdown rendering",
            "Agent list per server",
            "User profile and member list views"
        )
    ),
    ReleaseNoteEntry(
        version = "0.1.0",
        date = "2026-03-01",
        highlights = listOf(
            "Initial release",
            "Authentication — login, register, forgot password",
            "Server connection and channel browsing",
            "Basic message sending and receiving"
        )
    )
)
