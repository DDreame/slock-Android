package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

const val DEFAULT_AGENT_MODEL_ID = "claude-sonnet-4-20250514"
const val DEFAULT_AGENT_RUNTIME_ID = "claude"
const val DEFAULT_AGENT_REASONING_EFFORT_ID = "medium"

val DEFAULT_AGENT_MODEL_OPTIONS = listOf(
    DEFAULT_AGENT_MODEL_ID,
    "claude-haiku-4-5-20251001",
    "claude-opus-4-20250514"
)

data class AgentRuntimeOption(
    val id: String,
    val displayName: String
)

data class AgentReasoningOption(
    val id: String,
    val label: String
)

val DEFAULT_AGENT_RUNTIME_OPTIONS = listOf(
    AgentRuntimeOption(id = "claude", displayName = "Claude Code"),
    AgentRuntimeOption(id = "codex", displayName = "Codex CLI"),
    AgentRuntimeOption(id = "kimi", displayName = "Kimi CLI"),
    AgentRuntimeOption(id = "copilot", displayName = "Copilot CLI"),
    AgentRuntimeOption(id = "cursor", displayName = "Cursor CLI"),
    AgentRuntimeOption(id = "gemini", displayName = "Gemini CLI")
)

val DEFAULT_AGENT_REASONING_OPTIONS = listOf(
    AgentReasoningOption(id = "low", label = "Low"),
    AgentReasoningOption(id = "medium", label = "Medium"),
    AgentReasoningOption(id = "high", label = "High"),
    AgentReasoningOption(id = "xhigh", label = "Extra High")
)

val AGENT_RUNTIMES_SUPPORTING_REASONING = setOf("codex", "copilot")

fun supportsAgentReasoningEffort(runtime: String?): Boolean {
    val normalizedRuntime = runtime?.trim().orEmpty()
    return normalizedRuntime in AGENT_RUNTIMES_SUPPORTING_REASONING
}

data class Agent(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val prompt: String? = null,
    val model: String? = null,
    val avatar: String? = null,
    val status: String? = null,
    val activity: String? = null,
    @SerializedName("activityDetail")
    val activityDetail: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    val runtime: String? = null,
    @SerializedName("reasoningEffort")
    val reasoningEffort: String? = null,
    @SerializedName("envVars")
    val envVars: Map<String, String>? = null,
    @SerializedName("machineId")
    val machineId: String? = null,
    @SerializedName("machineName")
    val machineName: String? = null
)

data class CreateAgentRequest(
    val name: String,
    val description: String,
    val prompt: String,
    val model: String = DEFAULT_AGENT_MODEL_ID,
    val runtime: String? = null,
    @SerializedName("reasoningEffort")
    val reasoningEffort: String? = null,
    @SerializedName("envVars")
    val envVars: Map<String, String>? = null,
    val avatar: String? = null
)

data class UpdateAgentRequest(
    val name: String? = null,
    val description: String? = null,
    val prompt: String? = null,
    val runtime: String? = null,
    @SerializedName("reasoningEffort")
    val reasoningEffort: String? = null,
    @SerializedName("envVars")
    val envVars: Map<String, String>? = null
)

data class ResetAgentRequest(
    val mode: String = "full"
)

data class Machine(
    val id: String? = null,
    val name: String? = null,
    @SerializedName("serverId")
    val serverId: String? = null,
    val status: String? = null,
    val key: String? = null,
    val uptime: String? = null,
    @SerializedName("lastSeen")
    val lastSeen: String? = null,
    val meta: List<String>? = null,
    @SerializedName("runningAgents")
    val runningAgents: List<MachineAgent>? = null
)

data class MachineAgent(
    val id: String? = null,
    val name: String? = null,
    val status: String? = null
)

data class ActivityLogEntry(
    val timestamp: String? = null,
    val activity: String? = null,
    val detail: String? = null,
    val entry: Any? = null
)
