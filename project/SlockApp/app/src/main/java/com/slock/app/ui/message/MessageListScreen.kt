package com.slock.app.ui.message

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Message
import com.slock.app.ui.theme.*

@Composable
fun MessageListScreen(
    channelName: String,
    state: MessageUiState,
    onSendMessage: (String) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToThread: (threadChannelId: String, parentMessage: Message) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Channel Header
        ChannelHeader(
            channelName = "# $channelName",
            onBack = onNavigateBack
        )

        // Pinned message banner (stub — shown when pinned messages exist)
        // TODO: Wire to actual pinned message data
        PinnedBanner(
            pinnedText = null,
            onTap = { /* TODO: Navigate to pinned message */ }
        )

        // Messages
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Black
                    )
                }
                state.error != null -> {
                    NeoErrorState(
                        message = "消息加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = { onLoadMore() }
                    )
                }
                state.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "\uD83D\uDCAC", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Black
                        )
                        Text(
                            "Be the first to say something!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        reverseLayout = true
                    ) {
                        items(state.messages) { message ->
                            NeoMessage(
                                message = message,
                                onThreadClick = if (message.threadChannelId != null) {
                                    { onNavigateToThread(message.threadChannelId, message) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Compose Bar
        NeoComposeBar(
            text = text,
            onTextChange = { text = it },
            onSend = {
                if (text.isNotBlank() && !state.isSending) {
                    onSendMessage(text)
                    text = ""
                }
            },
            enabled = !state.isSending,
            placeholder = "Message #$channelName..."
        )
    }
}

// Channel Header
@Composable
private fun ChannelHeader(channelName: String, onBack: () -> Unit) {
    Surface(color = White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .neoShadowSmall()
                    .background(Cream)
                    .border(2.dp, Black, RectangleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\u2190", fontSize = 18.sp, color = Black)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Channel info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniIconButton(icon = "\uD83D\uDD0D")
                MiniIconButton(icon = "\u22EE")
            }
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

@Composable
private fun MiniIconButton(icon: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = 16.sp)
    }
}

// Message Item
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NeoMessage(
    message: Message,
    onThreadClick: (() -> Unit)? = null
) {
    val isAgent = message.isAgent
    val isPending = message.id.startsWith("pending-")
    val accentColor = if (isAgent) Orange else Cyan
    val alpha = if (isPending) 0.5f else 1f
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .height(IntrinsicSize.Min)
            .combinedClickable(
                onClick = { },
                onLongClick = { showMenu = true }
            ),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Left color bar for quick visual Human/Agent distinction
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(if (isAgent) Orange else Cyan)
                .border(2.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.senderName.ifEmpty { "Unknown" }.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Black
            )
        }

        // Content
        Column(modifier = Modifier.weight(1f)) {
            // Header: name + agent tag + time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = message.senderName.ifEmpty { "Unknown" },
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
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Message text
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF222222),
                lineHeight = 21.sp
            )

            // Sending indicator
            if (isPending) {
                Text(
                    text = "发送中...",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Task badge
            if (message.isTask) {
                Spacer(modifier = Modifier.height(6.dp))
                val (badgeColor, statusLabel) = when (message.taskStatus) {
                    "in_progress" -> Cyan to "In Progress"
                    "in_review" -> Lavender to "In Review"
                    "done" -> Lime to "Done"
                    else -> Orange to "Todo"
                }
                Row(
                    modifier = Modifier
                        .background(badgeColor)
                        .border(1.5.dp, Black, RectangleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "\uD83D\uDCCB #${message.taskNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Black
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Black
                    )
                    if (!message.taskClaimedByName.isNullOrBlank()) {
                        Text(
                            text = "@${message.taskClaimedByName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Thread preview
            if (message.threadChannelId != null && onThreadClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .background(Lavender)
                        .border(1.5.dp, Black, RectangleShape)
                        .clickable(onClick = onThreadClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = message.replyCount
                    val replyText = if (count > 0) {
                        "$count ${if (count == 1) "reply" else "replies"}"
                    } else {
                        "replies"
                    }
                    Text(
                        text = "\uD83D\uDCAC $replyText",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Black
                    )
                    Text(
                        text = " \u00B7 tap to view",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    // Long-press action bottom sheet
    if (showMenu) {
        val clipboardManager = LocalClipboardManager.current
        MessageActionSheet(
            hasThread = onThreadClick != null,
            onDismiss = { showMenu = false },
            onReplyThread = { showMenu = false; onThreadClick?.invoke() },
            onConvertToTask = { showMenu = false /* TODO: Convert to task */ },
            onPinMessage = { showMenu = false /* TODO: Pin message */ },
            onSaveMessage = { showMenu = false /* TODO: Save message */ },
            onCopy = {
                clipboardManager.setText(AnnotatedString(message.content))
                showMenu = false
            }
        )
    }
}

// Long-press action bottom sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionSheet(
    hasThread: Boolean,
    onDismiss: () -> Unit,
    onReplyThread: () -> Unit,
    onConvertToTask: () -> Unit,
    onPinMessage: () -> Unit,
    onSaveMessage: () -> Unit,
    onCopy: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RectangleShape,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Black)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            if (hasThread) {
                ActionSheetItem(
                    icon = "\uD83D\uDCAC",
                    label = "Reply in Thread",
                    onClick = onReplyThread
                )
            }
            ActionSheetItem(
                icon = "\uD83D\uDCDD",
                label = "Convert to Task",
                onClick = onConvertToTask
            )
            ActionSheetItem(
                icon = "\uD83D\uDCCC",
                label = "Pin Message",
                onClick = onPinMessage
            )
            ActionSheetItem(
                icon = "\uD83D\uDCBE",
                label = "Save Message",
                onClick = onSaveMessage
            )
            ActionSheetItem(
                icon = "\uD83D\uDCCB",
                label = "Copy Text",
                onClick = onCopy
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActionSheetItem(icon: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Black
        )
    }
}

// Pinned message banner
@Composable
private fun PinnedBanner(pinnedText: String?, onTap: () -> Unit) {
    if (pinnedText != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Yellow)
                .border(width = 0.dp, color = Color.Transparent)
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "\uD83D\uDCCC", fontSize = 14.sp)
            Text(
                text = "Pinned: $pinnedText",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "tap to view",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Divider(thickness = 2.dp, color = Black)
    }
}

// Compose Bar
@Composable
private fun NeoComposeBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String,
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
            // Attach button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Cream)
                    .border(2.dp, Black, RectangleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Black)
            }

            // Text input
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        text = placeholder,
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
                    .heightIn(min = 40.dp, max = 120.dp)
                    .onFocusChanged { isFocused = it.isFocused }
                    .then(
                        if (isFocused) Modifier.neoShadow(3.dp, 3.dp, Cyan)
                        else Modifier.neoShadowSmall()
                    )
                    .border(2.dp, Black, RectangleShape)
            )

            // Send button
            val sendColor = if (enabled) Yellow else Color(0xFFD0D0D0)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .neoShadowSmall()
                    .background(sendColor)
                    .border(2.dp, Black, RectangleShape)
                    .clickable(enabled = enabled, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\u27A4", fontSize = 18.sp, color = if (enabled) Black else TextMuted)
            }
        }
    }
}
