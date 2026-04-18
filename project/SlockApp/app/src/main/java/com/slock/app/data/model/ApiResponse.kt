package com.slock.app.data.model

/**
 * Wrapper for GET /messages/channel/{id}
 * API returns: { "messages": [...], "historyLimited": bool }
 */
data class MessagesResponse(
    val messages: List<Message> = emptyList(),
    val historyLimited: Boolean = false
)

/**
 * Wrapper for GET /messages/search
 * API returns: { "results": [...], "hasMore": bool }
 */
data class SearchMessagesResponse(
    val results: List<Message> = emptyList(),
    val hasMore: Boolean = false
)

/**
 * Wrapper for GET /tasks/channel/{id}
 * API returns: { "tasks": [...] }
 */
data class TasksResponse(
    val tasks: List<Task> = emptyList()
)

/**
 * Wrapper for GET /channels/threads/followed
 * API returns: { "threads": [...] }
 */
data class FollowedThreadsResponse(
    val threads: List<ThreadSummary> = emptyList()
)

/**
 * Thread summary from followed threads API
 */
data class ThreadSummary(
    val threadChannelId: String? = null,
    val parentMessageId: String? = null,
    val parentChannelId: String? = null,
    val parentMessageSenderId: String? = null,
    val parentMessageSenderType: String? = null,
    val parentMessageSenderName: String? = null,
    val parentMessagePreview: String? = null,
    val replyCount: Int = 0,
    val lastReplyAt: String? = null,
    val unreadCount: Int = 0,
    val channelName: String? = null
)

data class FollowThreadRequest(
    val parentMessageId: String
)

data class ThreadChannelIdRequest(
    val threadChannelId: String
)
