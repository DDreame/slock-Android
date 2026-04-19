package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

data class Channel(
    val id: String? = null,
    @SerializedName("serverId")
    val serverId: String? = null,
    val name: String? = null,
    val type: String? = null,
    val seq: Long = 0,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    val members: List<ChannelMember>? = null
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
    @SerializedName("agentId")
    val agentId: String? = null,
    @SerializedName("userId")
    val userId: String? = null
)

data class SaveMessageRequest(
    @SerializedName("messageId")
    val messageId: String
)

data class SavedMessagesCheckRequest(
    @SerializedName("messageIds")
    val messageIds: List<String>
)

data class SavedMessagesCheckResponse(
    @SerializedName("savedIds")
    val savedIds: List<String> = emptyList()
)

data class ChannelMember(
    val id: String? = null,
    @SerializedName("channelId")
    val channelId: String? = null,
    @SerializedName("userId")
    val userId: String? = null,
    @SerializedName("agentId")
    val agentId: String? = null,
    val user: User? = null,
    val agent: Agent? = null
)

data class ResumeAllAgentsRequest(
    val prompt: String
)
