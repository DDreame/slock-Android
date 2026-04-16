package com.slock.app.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Message
import com.slock.app.ui.theme.*

@Composable
fun ThreadReplyScreen(
    channelName: String = "",
    state: ThreadReplyUiState,
    onSendReply: (String) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isFollowing by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Lavender Thread Header
        ThreadHeader(
            replyCount = state.replies.size,
            channelName = channelName,
            onBack = onNavigateBack
        )

        // Participant status bar
        ParticipantBar(
            participants = buildParticipantList(state),
            isFollowing = isFollowing,
            onToggleFollow = { isFollowing = !isFollowing }
        )

        // Scrollable content
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    NeoSkeletonMessageList()
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 12.dp),
                        reverseLayout = false
                    ) {
                        // Original message card
                        state.parentMessage?.let { parent ->
                            item {
                                OriginalMessageCard(message = parent)
                            }
                        }

                        // Replies divider
                        item {
                            RepliesDivider(count = state.replies.size)
                        }

                        // Reply items
                        items(state.replies) { reply ->
                            ThreadReply(message = reply)
                        }

                        if (state.replies.isEmpty() && !state.isLoading) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "\uD83D\uDCAC", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No replies yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Black
                                    )
                                    Text(
                                        "Be the first to reply!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compose bar with lavender focus
        ThreadComposeBar(
            text = text,
            onTextChange = { text = it },
            onSend = {
                if (text.isNotBlank() && !state.isSending) {
                    onSendReply(text)
                    text = ""
                }
            },
            enabled = !state.isSending
        )
    }
}

// Lavender thread header
@Composable
private fun ThreadHeader(
    replyCount: Int,
    channelName: String,
    onBack: () -> Unit
) {
    Surface(color = Lavender) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            NeoPressableBox(onClick = onBack) {
                Text(text = "\u2190", fontSize = 18.sp, color = Black)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Header info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Thread",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Black
                )
                Text(
                    text = "# $channelName \u00B7 $replyCount replies",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniActionButton(icon = "\uD83D\uDD17")
                MiniActionButton(icon = "\u22EE")
            }
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

@Composable
private fun MiniActionButton(icon: String, onClick: () -> Unit = {}) {
    NeoPressableBox(onClick = onClick, size = 34.dp) {
        Text(text = icon, fontSize = 15.sp)
    }
}

// Participant status bar
@Composable
private fun ParticipantBar(
    participants: List<Participant>,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Participant dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            participants.take(5).forEach { participant ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(if (participant.isAgent) Orange else Cyan)
                        .border(1.5.dp, Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.initial,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${participants.size} PARTICIPANTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = Black.copy(alpha = 0.6f)
            )
        }

        // Follow button
        Box(
            modifier = Modifier
                .background(if (isFollowing) Yellow else Cream)
                .border(1.5.dp, Black, RectangleShape)
                .clickable(onClick = onToggleFollow)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isFollowing) "\uD83D\uDD14 FOLLOWING" else "\uD83D\uDD14 FOLLOW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        }
    }
    Divider(thickness = 2.dp, color = Black)
}

// Original message card
@Composable
private fun OriginalMessageCard(message: Message) {
    val isAgent = message.isAgent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isAgent) Orange else Cyan)
                    .border(2.dp, Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.orEmpty().ifEmpty { "Unknown" }.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Black
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message.senderName.orEmpty().ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Black
                    )
                    if (isAgent) {
                        Box(
                            modifier = Modifier
                                .background(Orange)
                                .border(1.dp, Black, RectangleShape)
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "AGENT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Black
                            )
                        }
                    }
                    Text(
                        text = message.createdAt.orEmpty().split("T").getOrNull(1)?.take(5) ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = Black.copy(alpha = 0.4f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message text
                NeoMessageContent(content = message.content.orEmpty())

                Spacer(modifier = Modifier.height(10.dp))

                // Mark as resolved button
                Row(
                    modifier = Modifier
                        .background(Lime)
                        .border(1.5.dp, Black, RectangleShape)
                        .clickable { /* TODO: Mark as resolved */ }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "\u2610", fontSize = 14.sp, color = Black)
                    Text(
                        text = "Mark as resolved",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }
            }
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

// Replies divider
@Composable
private fun RepliesDivider(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$count REPLIES",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextMuted
        )
        Spacer(modifier = Modifier.width(10.dp))
        Divider(
            modifier = Modifier.weight(1f),
            thickness = 1.5.dp,
            color = Color(0xFFCCCCCC)
        )
    }
}

// Thread reply item (smaller avatar)
@Composable
private fun ThreadReply(message: Message) {
    val isAgent = message.isAgent
    val isPending = message.id.orEmpty().startsWith("pending-")
    val alpha = if (isPending) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Smaller avatar for replies
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(if (isAgent) Orange else Cyan)
                .border(2.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.senderName.orEmpty().ifEmpty { "Unknown" }.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Black
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = message.senderName.orEmpty().ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Black
                )
                if (isAgent) {
                    Box(
                        modifier = Modifier
                            .background(Orange)
                            .border(1.dp, Black, RectangleShape)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "AGENT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    }
                }
                Text(
                    text = message.createdAt.orEmpty().split("T").getOrNull(1)?.take(5) ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = Black.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            NeoMessageContent(content = message.content.orEmpty())

            if (isPending) {
                Text(
                    text = "发送中...",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// Compose bar with lavender focus highlight
@Composable
private fun ThreadComposeBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }

    Divider(thickness = 3.dp, color = Black)
    Surface(color = White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text input with lavender focus
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        text = "Reply in thread...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                singleLine = false,
                maxLines = 5,
                shape = RectangleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Black
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 100.dp)
                    .onFocusChanged { isFocused = it.isFocused }
                    .then(
                        if (isFocused) Modifier.neoShadow(4.dp, 4.dp, Lavender)
                        else Modifier.neoShadowSmall()
                    )
                    .border(2.dp, Black, RectangleShape)
            )

            // Send button
            val sendColor = if (enabled) Yellow else Color(0xFFD0D0D0)
            NeoPressableBox(
                onClick = onSend,
                enabled = enabled,
                size = 40.dp,
                backgroundColor = sendColor
            ) {
                Text(text = "\u27A4", fontSize = 18.sp, color = if (enabled) Black else TextMuted)
            }
        }
    }
}

// Helper data
private data class Participant(val initial: String, val isAgent: Boolean)

private fun buildParticipantList(state: ThreadReplyUiState): List<Participant> {
    val seen = mutableSetOf<String>()
    val participants = mutableListOf<Participant>()

    state.parentMessage?.let { msg ->
        if (seen.add(msg.senderId.orEmpty())) {
            participants.add(Participant(msg.senderName.orEmpty().ifEmpty { "?" }.take(1).uppercase(), msg.isAgent))
        }
    }

    state.replies.forEach { reply ->
        if (seen.add(reply.senderId.orEmpty())) {
            participants.add(Participant(reply.senderName.orEmpty().ifEmpty { "?" }.take(1).uppercase(), reply.isAgent))
        }
    }

    return participants
}
