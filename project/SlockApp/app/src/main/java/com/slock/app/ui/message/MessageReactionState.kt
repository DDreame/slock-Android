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
    // TODO(task-89): map server-provided reaction payload from Message once the backend contract is confirmed.
    return reactionOverride ?: emptyList()
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
