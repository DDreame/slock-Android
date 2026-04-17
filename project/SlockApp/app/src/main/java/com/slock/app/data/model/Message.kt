package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

data class Message(
    val id: String? = null,
    @SerializedName("channelId")
    val channelId: String? = null,
    val content: String? = null,
    @SerializedName("senderId")
    val senderId: String? = null,
    @SerializedName("senderName")
    val senderName: String? = null,
    @SerializedName("senderType")
    val senderType: String? = null,
    @SerializedName("messageType")
    val messageType: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val seq: Long = 0,
    @SerializedName("createdAt")
    val createdAt: String? = null,
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
    val id: String? = null,
    val name: String? = null,
    val url: String? = null,
    val type: String? = null
)

data class UploadResponse(
    val id: String? = null,
    val url: String? = null,
    val name: String? = null,
    val type: String? = null
)

data class SendMessageRequest(
    @SerializedName("channelId")
    val channelId: String,
    val content: String,
    @SerializedName("attachmentIds")
    val attachmentIds: List<String>? = null,
    @SerializedName("asTask")
    val asTask: Boolean = false,
    @SerializedName("parentMessageId")
    val parentMessageId: String? = null
)

data class Thread(
    val id: String? = null,
    @SerializedName("channelId")
    val channelId: String? = null,
    @SerializedName("parentMessageId")
    val parentMessageId: String? = null,
    @SerializedName("parentChannelId")
    val parentChannelId: String? = null
)

data class Task(
    val id: String? = null,
    @SerializedName("channelId")
    val channelId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("assigneeId")
    val assigneeId: String? = null,
    @SerializedName("messageId")
    val messageId: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
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
