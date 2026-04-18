package com.slock.app.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.slock.app.data.local.entity.*
import com.slock.app.data.model.*

private val gson = Gson()

// Server
fun Server.toEntity() = ServerEntity(
    id = id.orEmpty(), name = name, slug = slug, role = role, createdAt = createdAt
)

fun ServerEntity.toModel() = Server(
    id = id, name = name.orEmpty(), slug = slug.orEmpty(),
    role = role.orEmpty(), createdAt = createdAt.orEmpty()
)

// Channel
fun Channel.toEntity(serverId: String) = ChannelEntity(
    id = id.orEmpty(), serverId = serverId, name = name, type = type, seq = seq, createdAt = createdAt
)

fun ChannelEntity.toModel() = Channel(
    id = id, serverId = serverId.orEmpty(), name = name.orEmpty(),
    type = type.orEmpty(), seq = seq, createdAt = createdAt.orEmpty()
)

// Message
fun Message.toEntity() = MessageEntity(
    id = id.orEmpty(), channelId = channelId, content = content,
    senderId = senderId, senderType = senderType, senderName = senderName,
    seq = seq, createdAt = createdAt, updatedAt = updatedAt,
    threadId = threadChannelId, taskNumber = taskNumber, taskStatus = taskStatus,
    replyCount = replyCount,
    attachments = try { gson.toJson(attachments) } catch (_: Exception) { "[]" },
    reactions = try { gson.toJson(reactions) } catch (_: Exception) { "[]" }
)

fun MessageEntity.toModel() = Message(
    id = id, channelId = channelId.orEmpty(), content = content.orEmpty(),
    senderId = senderId.orEmpty(), senderName = senderName.orEmpty(),
    senderType = senderType.orEmpty(),
    seq = seq, createdAt = createdAt.orEmpty(), updatedAt = updatedAt,
    threadChannelId = threadId, taskNumber = taskNumber, taskStatus = taskStatus,
    replyCount = replyCount,
    attachments = try {
        val type = object : TypeToken<List<Attachment>>() {}.type
        gson.fromJson<List<Attachment>>(attachments ?: "[]", type) ?: emptyList()
    } catch (_: Exception) { emptyList() },
    reactions = try {
        val type = object : TypeToken<List<MessageReactionPayload>>() {}.type
        gson.fromJson<List<MessageReactionPayload>>(reactions ?: "[]", type) ?: emptyList()
    } catch (_: Exception) { emptyList() }
)

// Agent
fun Agent.toEntity(serverId: String) = AgentEntity(
    id = id.orEmpty(), serverId = serverId, name = name, description = description,
    prompt = prompt, model = model, avatar = avatar, status = status,
    activity = activity, activityDetail = activityDetail, createdAt = createdAt
)

fun AgentEntity.toModel() = Agent(
    id = id, name = name.orEmpty(), description = description,
    prompt = prompt, model = model.orEmpty(), avatar = avatar,
    status = status.orEmpty(), activity = activity,
    activityDetail = activityDetail, createdAt = createdAt.orEmpty()
)

// Task
fun Task.toEntity() = TaskEntity(
    id = id.orEmpty(), channelId = channelId, title = title, description = description,
    status = status, createdBy = createdBy, assigneeId = assigneeId,
    messageId = messageId, createdAt = createdAt, updatedAt = updatedAt
)

fun TaskEntity.toModel() = Task(
    id = id, channelId = channelId.orEmpty(), title = title.orEmpty(),
    description = description, status = status.orEmpty(),
    createdBy = createdBy.orEmpty(), assigneeId = assigneeId,
    messageId = messageId, createdAt = createdAt.orEmpty(), updatedAt = updatedAt
)
