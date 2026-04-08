package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

data class Channel(
    val id: String,
    @SerializedName("serverId")
    val serverId: String? = null,
    val name: String,
    val type: String = "text",
    val seq: Long = 0,
    @SerializedName("createdAt")
    val createdAt: String
)

data class CreateChannelRequest(
    val name: String,
    val type: String = "text"
)

data class UpdateChannelRequest(
    val name: String
)

data class MarkReadRequest(
    val seq: Long
)

data class CreateDMRequest(
    @SerializedName("userId")
    val userId: String
)

data class ChannelMember(
    val id: String,
    @SerializedName("channelId")
    val channelId: String,
    @SerializedName("userId")
    val userId: String? = null,
    @SerializedName("agentId")
    val agentId: String? = null,
    val user: User? = null,
    val agent: Agent? = null
)
