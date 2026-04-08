package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

data class Message(
    val id: String,
    @SerializedName("channelId")
    val channelId: String,
    val content: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("agentId")
    val agentId: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val seq: Long = 0,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("threadId")
    val threadId: String? = null,
    @SerializedName("parentChannelId")
    val parentChannelId: String? = null
)

data class Attachment(
    val id: String,
    val name: String,
    val url: String,
    val type: String
)

data class SendMessageRequest(
    @SerializedName("channelId")
    val channelId: String,
    val content: String,
    @SerializedName("attachmentIds")
    val attachmentIds: List<String>? = null,
    @SerializedName("asTask")
    val asTask: Boolean = false
)

data class Thread(
    val id: String,
    @SerializedName("channelId")
    val channelId: String,
    @SerializedName("parentMessageId")
    val parentMessageId: String,
    @SerializedName("parentChannelId")
    val parentChannelId: String
)

data class Task(
    val id: String,
    @SerializedName("channelId")
    val channelId: String,
    val title: String,
    val description: String? = null,
    val status: String = "todo",
    @SerializedName("createdBy")
    val createdBy: String,
    @SerializedName("assigneeId")
    val assigneeId: String? = null,
    @SerializedName("messageId")
    val messageId: String? = null,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class CreateTaskRequest(
    @SerializedName("channelId")
    val channelId: String,
    val title: String,
    val description: String? = null,
    @SerializedName("assigneeId")
    val assigneeId: String? = null,
    @SerializedName("messageId")
    val messageId: String? = null
)

data class UpdateTaskStatusRequest(
    val status: String
)
