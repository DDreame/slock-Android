package com.slock.app.data.local

import com.slock.app.data.local.entity.*
import com.slock.app.data.model.*

// Server
fun Server.toEntity() = ServerEntity(
    id = id, name = name, slug = slug, role = role, createdAt = createdAt
)

fun ServerEntity.toModel() = Server(
    id = id, name = name, slug = slug, role = role, createdAt = createdAt
)

// Channel
fun Channel.toEntity(serverId: String) = ChannelEntity(
    id = id, serverId = serverId, name = name, type = type, seq = seq, createdAt = createdAt
)

fun ChannelEntity.toModel() = Channel(
    id = id, serverId = serverId, name = name, type = type, seq = seq, createdAt = createdAt
)

// Message
fun Message.toEntity() = MessageEntity(
    id = id, channelId = channelId, content = content,
    senderId = senderId, senderType = senderType, senderName = senderName,
    seq = seq, createdAt = createdAt, updatedAt = updatedAt,
    threadId = threadChannelId, attachments = "[]"
)

fun MessageEntity.toModel() = Message(
    id = id, channelId = channelId, content = content,
    senderId = senderId, senderName = senderName, senderType = senderType,
    seq = seq, createdAt = createdAt, updatedAt = updatedAt,
    threadChannelId = threadId
)

// Agent
fun Agent.toEntity(serverId: String) = AgentEntity(
    id = id, serverId = serverId, name = name, description = description,
    prompt = prompt, model = model, avatar = avatar, status = status,
    activity = activity, activityDetail = activityDetail, createdAt = createdAt
)

fun AgentEntity.toModel() = Agent(
    id = id, name = name, description = description, prompt = prompt,
    model = model, avatar = avatar, status = status, activity = activity,
    activityDetail = activityDetail, createdAt = createdAt
)

// Task
fun Task.toEntity() = TaskEntity(
    id = id, channelId = channelId, title = title, description = description,
    status = status, createdBy = createdBy, assigneeId = assigneeId,
    messageId = messageId, createdAt = createdAt, updatedAt = updatedAt
)

fun TaskEntity.toModel() = Task(
    id = id, channelId = channelId, title = title, description = description,
    status = status, createdBy = createdBy, assigneeId = assigneeId,
    messageId = messageId, createdAt = createdAt, updatedAt = updatedAt
)
