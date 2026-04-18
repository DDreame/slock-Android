package com.slock.app.ui.message

import com.slock.app.data.model.Message

data class MessageReactionUiModel(
    val emoji: String,
    val count: Int,
    val isSelected: Boolean
)

val DefaultQuickReactions = listOf("👍", "❤️", "😂", "🎉", "👀")

fun resolveDisplayedReactions(
    message: Message,
    reactionOverride: List<MessageReactionUiModel>?
): List<MessageReactionUiModel> {
    if (message.id.isNullOrBlank()) return emptyList()
    return reactionOverride ?: resolveServerReactions(message.reactions)
}

private fun resolveServerReactions(
    reactions: List<com.slock.app.data.model.MessageReactionPayload>
): List<MessageReactionUiModel> {
    val quickOrder = DefaultQuickReactions.withIndex().associate { it.value to it.index }
    return reactions.mapNotNull { reaction ->
        val emoji = reaction.emoji?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val isSelected = reaction.isSelected ?: reaction.selected ?: reaction.reacted ?: false
        val explicitCount = reaction.count ?: reaction.userIds?.size ?: reaction.users?.size ?: 0
        val normalizedCount = when {
            explicitCount > 0 -> explicitCount
            isSelected -> 1
            else -> 0
        }
        if (normalizedCount <= 0) return@mapNotNull null
        MessageReactionUiModel(
            emoji = emoji,
            count = normalizedCount,
            isSelected = isSelected
        )
    }.sortedWith(
        compareBy<MessageReactionUiModel> { quickOrder[it.emoji] ?: Int.MAX_VALUE }
            .thenBy { it.emoji }
    )
}

fun quickReactionOptions(reactions: List<MessageReactionUiModel>): List<String> =
    (DefaultQuickReactions + reactions.map { it.emoji }).distinct()

fun toggleLocalReaction(
    reactions: List<MessageReactionUiModel>,
    emoji: String
): List<MessageReactionUiModel> {
    val existing = reactions.firstOrNull { it.emoji == emoji }
    val updated = when {
        existing == null -> reactions + MessageReactionUiModel(emoji = emoji, count = 1, isSelected = true)
        existing.isSelected -> {
            val nextCount = (existing.count - 1).coerceAtLeast(0)
            reactions.mapNotNull { reaction ->
                when {
                    reaction.emoji != emoji -> reaction
                    nextCount == 0 -> null
                    else -> reaction.copy(count = nextCount, isSelected = false)
                }
            }
        }
        else -> reactions.map { reaction ->
            if (reaction.emoji == emoji) reaction.copy(count = reaction.count + 1, isSelected = true)
            else reaction
        }
    }

    val quickOrder = DefaultQuickReactions.withIndex().associate { it.value to it.index }
    return updated.sortedWith(
        compareBy<MessageReactionUiModel> { quickOrder[it.emoji] ?: Int.MAX_VALUE }
            .thenBy { it.emoji }
    )
}

fun updateReactionOverrides(
    currentOverrides: Map<String, List<MessageReactionUiModel>>,
    message: Message,
    emoji: String
): Map<String, List<MessageReactionUiModel>> {
    val messageId = message.id ?: return currentOverrides
    val current = resolveDisplayedReactions(message, currentOverrides[messageId])
    val updated = toggleLocalReaction(current, emoji)
    return currentOverrides.toMutableMap().apply {
        if (updated.isEmpty()) remove(messageId) else put(messageId, updated)
    }
}
