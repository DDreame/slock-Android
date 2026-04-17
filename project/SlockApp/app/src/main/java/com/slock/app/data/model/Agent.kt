package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

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
    @SerializedName("machineId")
    val machineId: String? = null,
    @SerializedName("machineName")
    val machineName: String? = null
)

data class CreateAgentRequest(
    val name: String,
    val description: String,
    val prompt: String,
    val model: String = "claude-sonnet-4-20250514",
    val avatar: String? = null
)

data class UpdateAgentRequest(
    val name: String? = null,
    val description: String? = null,
    val prompt: String? = null
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
