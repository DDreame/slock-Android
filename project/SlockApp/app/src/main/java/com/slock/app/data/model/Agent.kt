package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

data class Agent(
    val id: String,
    val name: String,
    val description: String,
    val prompt: String,
    val model: String = "gpt-4",
    val avatar: String? = null,
    val status: String = "stopped",
    @SerializedName("createdAt")
    val createdAt: String
)

data class CreateAgentRequest(
    val name: String,
    val description: String,
    val prompt: String,
    val model: String = "gpt-4",
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
    val id: String,
    val name: String,
    @SerializedName("serverId")
    val serverId: String,
    val status: String,
    val key: String? = null
)
