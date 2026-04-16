package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

data class Message(
    val id: String = "",
    @SerializedName("channelId")
    val channelId: String = "",
    val content: String = "",
    @SerializedName("senderId")
    val senderId: String = "",
    @SerializedName("senderName")
    val senderName: String = "",
    @SerializedName("senderType")
    val senderType: String = "",
    @SerializedName("messageType")
    val messageType: String = "",
    val attachments: List<Attachment> = emptyList(),
    val seq: Long = 0,
    @SerializedName("createdAt")
    val createdAt: String = "",
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("threadChannelId")
    val threadChannelId: String? = null,
    @SerializedName("parentMessageId")
    val parentMessageId: String? = null,
    @SerializedName("replyCount")
    val replyCount: Int = 0,
    @SerializedName("lastReplyAt")
    val lastReplyAt: String? = null,
    @SerializedName("taskNumber")
    val taskNumber: Int? = null,
    @SerializedName("taskStatus")
    val taskStatus: String? = null,
    @SerializedName("taskClaimedByName")
    val taskClaimedByName: String? = null
) {
    val isAgent: Boolean get() = senderType == "agent"
    val isTask: Boolean get() = taskNumber != null
}

data class Attachment(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val type: String = ""
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
    val channelId: String = "",
    val title: String = "",
    val description: String? = null,
    val status: String = "todo",
    @SerializedName("createdBy")
    val createdBy: String = "",
    @SerializedName("assigneeId")
    val assigneeId: String? = null,
    @SerializedName("messageId")
    val messageId: String? = null,
    @SerializedName("createdAt")
    val createdAt: String = "",
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
