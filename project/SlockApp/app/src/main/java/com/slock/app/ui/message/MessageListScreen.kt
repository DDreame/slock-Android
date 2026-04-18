package com.slock.app.ui.message

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.slock.app.data.model.Agent
import com.slock.app.data.model.Message
import com.slock.app.ui.theme.*
import java.io.File
import java.io.IOException
import java.util.Locale

internal fun messageListItemKey(message: Message): String {
    return message.id
        ?: "${message.seq}:${message.createdAt.orEmpty()}:${message.senderId.orEmpty()}"
}

internal fun buildMessagesById(messages: List<Message>): Map<String, Message> {
    return messages.mapNotNull { message ->
        message.id?.let { id -> id to message }
    }.toMap()
}

internal fun resolveQuotedMessage(parentMessageId: String?, messagesById: Map<String, Message>): Message? {
    return parentMessageId?.let(messagesById::get)
}

@Composable
fun MessageListScreen(
    channelName: String,
    contextLabel: String = "",
    state: MessageUiState,
    onSendMessage: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoad: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToThread: (threadChannelId: String, parentMessage: Message) -> Unit,
    onReplyTo: (Message) -> Unit = {},
    onClearReply: () -> Unit = {},
    onAddAttachment: (PendingAttachment) -> Unit = {},
    onRemoveAttachment: (Uri) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onDismissPreview: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onNextSearchResult: () -> Unit = {},
    onPreviousSearchResult: () -> Unit = {},
    onToggleReaction: (Message, String) -> Unit = { _, _ -> },
    onToggleSavedChannel: () -> Unit = {},
    onSavedChannelFeedbackShown: () -> Unit = {},
    onSendErrorShown: () -> Unit = {},
    channelAgents: List<Agent> = emptyList(),
    channelName_raw: String = "",
    onStopAllAgents: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { s, _ -> s() },
    onResumeAllAgents: (prompt: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, s, _ -> s() }
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showAgentBatchSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedChannelFeedbackMessage) {
        val message = state.savedChannelFeedbackMessage ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        onSavedChannelFeedbackShown()
    }

    LaunchedEffect(state.sendError) {
        val message = state.sendError ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        onSendErrorShown()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Channel Header
        ChannelHeader(
            channelName = "# $channelName",
            contextLabel = resolveChannelHeaderContext(contextLabel),
            onBack = onNavigateBack,
            onSearchClick = onToggleSearch,
            isSaved = state.isCurrentChannelSaved,
            isSavedStatusLoading = state.isSavedStatusLoading,
            onToggleSavedChannel = onToggleSavedChannel,
            hasAgents = channelAgents.isNotEmpty(),
            onAgentControlClick = { showAgentBatchSheet = true }
        )

        // Search bar (shown when search is active)
        if (state.isSearchActive) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
                matchCount = state.searchMatchIndices.size,
                currentMatch = state.currentSearchMatchPosition,
                onNext = onNextSearchResult,
                onPrevious = onPreviousSearchResult,
                onClose = onToggleSearch
            )
        }

        // Messages
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    NeoSkeletonMessageList()
                }
                state.error != null && state.messages.isEmpty() -> {
                    NeoErrorState(
                        message = "消息加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = { onRetryLoad() },
                        logContext = state.error
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
                    val listState = rememberLazyListState()
                    val messagesById = remember(state.messages) {
                        buildMessagesById(state.messages)
                    }

                    // Scroll to current search match
                    val targetIndex = if (state.isSearchActive && state.currentSearchMatchPosition >= 0 && state.searchMatchIndices.isNotEmpty()) {
                        state.searchMatchIndices[state.currentSearchMatchPosition]
                    } else -1
                    LaunchedEffect(targetIndex) {
                        if (targetIndex >= 0) {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }

                    val currentHighlightMessageIndex = if (targetIndex >= 0) targetIndex else -1

                    // Trigger load more when scrolled near the end (oldest messages)
                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val totalItems = listState.layoutInfo.totalItemsCount
                            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisibleItem >= totalItems - 5 && totalItems > 0
                        }
                    }
                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value && state.hasMoreMessages && !state.isLoadingMore) {
                            onLoadMore()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        reverseLayout = true
                    ) {
                        itemsIndexed(
                            items = state.messages,
                            key = { _, message -> messageListItemKey(message) }
                        ) { index, message ->
                            if (message.senderType.orEmpty() == "system") {
                                SystemMessageDivider(message.content.orEmpty())
                            } else {
                                val quotedMessage = resolveQuotedMessage(message.parentMessageId, messagesById)
                                val isCurrentMatch = index == currentHighlightMessageIndex
                                NeoMessage(
                                    message = message,
                                    quotedMessage = quotedMessage,
                                    isOnline = message.senderId.orEmpty() in state.onlineIds,
                                    reactions = resolveDisplayedReactions(
                                        message = message,
                                        reactionOverride = state.reactionOverridesByMessageId[message.id]
                                    ),
                                    highlightQuery = if (state.isSearchActive) state.searchQuery else "",
                                    isCurrentSearchMatch = isCurrentMatch,
                                    onThreadClick = if (message.threadChannelId != null) {
                                        { onNavigateToThread(message.threadChannelId!!, message) }
                                    } else null,
                                    onReply = { onReplyTo(message) },
                                    onToggleReaction = { emoji -> onToggleReaction(message, emoji) },
                                    onImageClick = onImageClick
                                )
                            }
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Black,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quote reply preview
        if (state.replyingTo != null) {
            ReplyPreviewBanner(
                message = state.replyingTo,
                onDismiss = onClearReply
            )
        }

        // Compose Bar
        NeoComposeBar(
            text = text,
            onTextChange = { text = it },
            onSend = {
                if ((text.isNotBlank() || state.pendingAttachments.isNotEmpty()) && !state.isSending) {
                    onSendMessage(text)
                    text = ""
                }
            },
            enabled = !state.isSending,
            placeholder = "Message #$channelName...",
            pendingAttachments = state.pendingAttachments,
            onAddAttachment = onAddAttachment,
            onRemoveAttachment = onRemoveAttachment,
            isUploading = state.isUploading
        )
    }

    if (showAgentBatchSheet) {
        AgentBatchControlSheet(
            agents = channelAgents,
            channelName = channelName_raw,
            onDismiss = { showAgentBatchSheet = false },
            onStopAll = { onSuccess ->
                onStopAllAgents(
                    { onSuccess() },
                    { error -> android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show() }
                )
            },
            onResumeAllWithCorrection = { prompt, onSuccess ->
                onResumeAllAgents(
                    prompt,
                    { onSuccess() },
                    { error -> android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show() }
                )
            },
            onKeepStopped = { showAgentBatchSheet = false }
        )
    }

    // Fullscreen image preview overlay
    state.previewImageUrl?.let { imageUrl ->
        ImagePreviewOverlay(
            imageUrl = imageUrl,
            onDismiss = onDismissPreview
        )
    }
}

internal fun resolveChannelHeaderContext(contextLabel: String): String? =
    contextLabel.trim().takeIf { it.isNotEmpty() }

private const val MaxAttachmentSizeBytes = 10 * 1024 * 1024

private data class CameraCaptureTarget(
    val file: File,
    val uri: Uri
)

internal fun isPendingAttachmentImage(attachment: PendingAttachment): Boolean =
    attachment.mimeType.startsWith("image/", ignoreCase = true)

internal fun pendingAttachmentTypeLabel(attachment: PendingAttachment): String {
    val extension = attachment.name.substringAfterLast('.', missingDelimiterValue = "")
        .trim()
        .uppercase(Locale.ROOT)
        .takeIf { it.isNotEmpty() }
    val subtype = attachment.mimeType.substringAfter('/', missingDelimiterValue = "")
        .substringBefore(';')
        .trim()
        .uppercase(Locale.ROOT)
        .takeIf { it.isNotEmpty() }

    if (isPendingAttachmentImage(attachment)) {
        return extension ?: subtype?.take(6) ?: "IMAGE"
    }

    return when {
        extension != null -> extension.take(6)
        subtype == null || subtype == "*" || subtype == "OCTET-STREAM" -> "FILE"
        else -> subtype.take(6)
    }
}

private fun fallbackExtensionForMimeType(mimeType: String): String {
    val subtype = mimeType.substringAfter('/', missingDelimiterValue = "")
        .substringBefore(';')
        .trim()

    return when {
        mimeType.startsWith("image/", ignoreCase = true) && subtype == "*" -> "jpg"
        subtype.isBlank() || subtype == "*" -> "bin"
        mimeType.startsWith("image/", ignoreCase = true) -> subtype
        subtype == "octet-stream" -> "bin"
        else -> subtype
    }
}

private fun resolveAttachmentName(context: Context, uri: Uri, mimeType: String, prefix: String = "attachment"): String {
    val resolver = context.contentResolver
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
    }

    val extension = fallbackExtensionForMimeType(mimeType)
    return "${prefix}_${System.currentTimeMillis()}.$extension"
}

private fun readPendingAttachment(
    context: Context,
    uri: Uri,
    fallbackMimeType: String,
    fallbackPrefix: String = "attachment"
): PendingAttachment? {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri)?.takeIf { it.isNotBlank() } ?: fallbackMimeType
    val name = resolveAttachmentName(context, uri, mimeType, fallbackPrefix)
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

    if (bytes.size > MaxAttachmentSizeBytes) {
        android.widget.Toast.makeText(context, "附件超过 10MB 限制: $name", android.widget.Toast.LENGTH_SHORT).show()
        return null
    }

    return PendingAttachment(uri = uri, name = name, mimeType = mimeType, bytes = bytes)
}

private fun createCameraCaptureTarget(context: Context): CameraCaptureTarget? {
    return try {
        val file = File.createTempFile(
            "camera_${System.currentTimeMillis()}_",
            ".jpg",
            context.cacheDir
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        CameraCaptureTarget(file = file, uri = uri)
    } catch (_: IOException) {
        android.widget.Toast.makeText(context, "无法启动相机", android.widget.Toast.LENGTH_SHORT).show()
        null
    } catch (_: IllegalArgumentException) {
        android.widget.Toast.makeText(context, "无法启动相机", android.widget.Toast.LENGTH_SHORT).show()
        null
    }
}

// Channel Header
@Composable
private fun ChannelHeader(
    channelName: String,
    contextLabel: String?,
    onBack: () -> Unit,
    onSearchClick: () -> Unit = {},
    isSaved: Boolean = false,
    isSavedStatusLoading: Boolean = false,
    onToggleSavedChannel: () -> Unit = {},
    hasAgents: Boolean = false,
    onAgentControlClick: () -> Unit = {}
) {
    Surface(color = White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            NeoPressableBox(onClick = onBack, backgroundColor = Cream) {
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
                if (contextLabel != null) {
                    Text(
                        text = contextLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasAgents) {
                    MiniIconButton(icon = "\uD83E\uDD16", onClick = onAgentControlClick)
                }
                MiniIconButton(
                    icon = when {
                        isSavedStatusLoading -> "…"
                        isSaved -> "★"
                        else -> "☆"
                    },
                    onClick = onToggleSavedChannel
                )
                MiniIconButton(icon = "\uD83D\uDD0D", onClick = onSearchClick)
            }
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

@Composable
private fun MiniIconButton(icon: String, onClick: () -> Unit = {}) {
    NeoPressableBox(onClick = onClick, size = 34.dp) {
        Text(text = icon, fontSize = 16.sp)
    }
}

// In-channel search bar
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    matchCount: Int,
    currentMatch: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: IllegalStateException) {}
    }

    Surface(color = Cream) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "搜索消息...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                singleLine = true,
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
                    .heightIn(min = 36.dp, max = 36.dp)
                    .neoShadowSmall()
                    .border(2.dp, Black, RectangleShape)
                    .focusRequester(focusRequester)
                    .focusable(),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            if (query.isNotBlank()) {
                Text(
                    text = if (matchCount > 0) "${currentMatch + 1}/$matchCount" else "0/0",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (matchCount > 0) Black else TextMuted
                )

                MiniIconButton(icon = "\u25B2", onClick = onPrevious)
                MiniIconButton(icon = "\u25BC", onClick = onNext)
            }

            MiniIconButton(icon = "\u2715", onClick = onClose)
        }
    }
    Divider(thickness = 2.dp, color = Black)
}

// Message Item
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NeoMessage(
    message: Message,
    quotedMessage: Message? = null,
    isOnline: Boolean = false,
    reactions: List<MessageReactionUiModel> = emptyList(),
    highlightQuery: String = "",
    isCurrentSearchMatch: Boolean = false,
    onThreadClick: (() -> Unit)? = null,
    onReply: () -> Unit = {},
    onToggleReaction: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val isAgent = message.isAgent
    val isPending = message.id.orEmpty().startsWith("pending-")
    val accentColor = if (isAgent) Orange else Cyan
    val alpha = if (isPending) 0.5f else 1f
    var showMenu by remember { mutableStateOf(false) }
    val copyTargets = remember(message) { buildMessageCopyTargets(message) }

    val messageBgColor = when {
        isCurrentSearchMatch -> Yellow.copy(alpha = 0.3f)
        isAgent -> Color(0xFFFFF5EB)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .background(messageBgColor)
            .height(IntrinsicSize.Min)
            .combinedClickable(
                onClick = { onThreadClick?.invoke() },
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

        MessageAvatar(
            name = message.senderName.orEmpty().ifEmpty { "Unknown" },
            isAgent = isAgent,
            isOnline = isOnline
        )

        // Content
        Column(modifier = Modifier.weight(1f)) {
            // Header: name + agent tag + time
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
                            style = MessageTextStyles.agentBadgeStyle(MaterialTheme.typography),
                            color = Black
                        )
                    }
                }
                Text(
                    text = message.createdAt.orEmpty().split("T").getOrNull(1)?.take(5) ?: "",
                    style = MessageTextStyles.timestampStyle(MaterialTheme.typography),
                    color = MessageTextStyles.timestampColor
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Inline quoted message
            if (quotedMessage != null) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .background(Color(0xFFF0F0F0))
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(Lavender)
                    )
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = quotedMessage.senderName.orEmpty().ifEmpty { "Unknown" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = Black.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = quotedMessage.content.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = Black.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Message content with markdown rendering
            NeoMessageContent(content = message.content.orEmpty(), highlightQuery = highlightQuery)

            if (message.attachments.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                MessageAttachmentContent(
                    attachments = message.attachments,
                    onImageClick = onImageClick
                )
            }

            if (reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                MessageReactionRow(
                    reactions = reactions,
                    onToggleReaction = onToggleReaction
                )
            }

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
            if (message.threadChannelId != null && message.replyCount > 0 && onThreadClick != null) {
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
                    val replyText = when (count) {
                        1 -> "1 reply"
                        0 -> "0 replies"
                        else -> "$count replies"
                    }
                    Text(
                        text = "\uD83D\uDCAC $replyText",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Black
                    )
                    val lastReplyLabel = formatMessageThreadTime(message.lastReplyAt)
                    if (lastReplyLabel.isNotBlank()) {
                        Text(
                            text = " \u00B7 $lastReplyLabel",
                            style = MessageTextStyles.timestampStyle(MaterialTheme.typography),
                            color = MessageTextStyles.timestampColor
                        )
                    }
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
            canCopyLink = copyTargets.link != null,
            reactions = reactions,
            onDismiss = { showMenu = false },
            onToggleReaction = { emoji -> showMenu = false; onToggleReaction(emoji) },
            onReplyThread = { showMenu = false; onThreadClick?.invoke() },
            onQuoteReply = { showMenu = false; onReply() },
            onCopyMarkdown = {
                clipboardManager.setText(AnnotatedString(copyTargets.markdown))
                showMenu = false
            },
            onCopyLink = {
                copyTargets.link?.let { clipboardManager.setText(AnnotatedString(it)) }
                showMenu = false
            }
        )
    }
}

private fun formatMessageThreadTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return ""
    val datePart = isoTime.substringBefore("T", "")
    val timePart = isoTime.substringAfter("T", "").take(5)
    return if (datePart.isNotBlank() && timePart.isNotBlank()) {
        "$datePart $timePart"
    } else {
        timePart
    }
}

@Composable
private fun MessageAvatar(
    name: String,
    isAgent: Boolean,
    isOnline: Boolean
) {
    Box {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(if (isAgent) Orange else Cyan)
                .border(2.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Black
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 1.dp, y = 1.dp)
                .size(8.dp)
                .background(if (isOnline) Lime else Color(0xFFCCCCCC))
                .border(1.5.dp, Black, RectangleShape)
        )
    }
}

// Long-press action bottom sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionSheet(
    hasThread: Boolean,
    canCopyLink: Boolean,
    reactions: List<MessageReactionUiModel>,
    onDismiss: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onReplyThread: () -> Unit,
    onQuoteReply: () -> Unit,
    onCopyMarkdown: () -> Unit,
    onCopyLink: () -> Unit
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
            QuickReactionPicker(
                reactions = reactions,
                onToggleReaction = onToggleReaction
            )
            Spacer(modifier = Modifier.height(4.dp))
            ActionSheetItem(
                icon = "\u21A9\uFE0F",
                label = "Quote Reply",
                onClick = onQuoteReply
            )
            if (hasThread) {
                ActionSheetItem(
                    icon = "\uD83D\uDCAC",
                    label = "Reply in Thread",
                    onClick = onReplyThread
                )
            }
            ActionSheetItem(
                icon = "\uD83D\uDCCB",
                label = "Copy Markdown",
                onClick = onCopyMarkdown
            )
            if (canCopyLink) {
                ActionSheetItem(
                    icon = "\uD83D\uDD17",
                    label = "Copy Link",
                    onClick = onCopyLink
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickReactionPicker(
    reactions: List<MessageReactionUiModel>,
    onToggleReaction: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Quick reactions",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Black,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickReactionOptions(reactions)) { emoji ->
                val selected = reactions.any { it.emoji == emoji && it.isSelected }
                ReactionChip(
                    emoji = emoji,
                    count = null,
                    isSelected = selected,
                    onClick = { onToggleReaction(emoji) }
                )
            }
        }
    }
}

@Composable
private fun MessageReactionRow(
    reactions: List<MessageReactionUiModel>,
    onToggleReaction: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(reactions) { reaction ->
            ReactionChip(
                emoji = reaction.emoji,
                count = reaction.count,
                isSelected = reaction.isSelected,
                onClick = { onToggleReaction(reaction.emoji) }
            )
        }
    }
}

@Composable
private fun ReactionChip(
    emoji: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Lime else White
    val textColor = Black
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .border(1.5.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = emoji, fontSize = 15.sp)
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
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
    enabled: Boolean = true,
    pendingAttachments: List<PendingAttachment> = emptyList(),
    onAddAttachment: (PendingAttachment) -> Unit = {},
    onRemoveAttachment: (Uri) -> Unit = {},
    isUploading: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var pendingCameraCapture by remember { mutableStateOf<CameraCaptureTarget?>(null) }
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            readPendingAttachment(context, uri, fallbackMimeType = "image/*", fallbackPrefix = "photo")
                ?.let(onAddAttachment)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            readPendingAttachment(context, uri, fallbackMimeType = "application/octet-stream", fallbackPrefix = "file")
                ?.let(onAddAttachment)
        }
    }

    val cameraPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val captureTarget = pendingCameraCapture
        pendingCameraCapture = null

        if (success && captureTarget != null) {
            readPendingAttachment(
                context = context,
                uri = captureTarget.uri,
                fallbackMimeType = "image/jpeg",
                fallbackPrefix = "camera"
            )?.let(onAddAttachment)
        } else {
            captureTarget?.file?.delete()
        }
    }

    Divider(thickness = 3.dp, color = Black)
    Surface(color = White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Pending attachments preview
            if (pendingAttachments.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pendingAttachments, key = { it.uri.toString() }) { attachment ->
                        PendingAttachmentPreview(
                            attachment = attachment,
                            onRemove = { onRemoveAttachment(attachment.uri) }
                        )
                    }
                }
                if (isUploading) {
                    Text(
                        text = "上传中...",
                        fontFamily = SpaceGrotesk,
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                    )
                }
            }

            if (showAttachmentMenu) {
                AttachmentMenu(
                    onPhotoClick = {
                        showAttachmentMenu = false
                        photoPicker.launch("image/*")
                    },
                    onFileClick = {
                        showAttachmentMenu = false
                        filePicker.launch("*/*")
                    },
                    onCameraClick = {
                        showAttachmentMenu = false
                        createCameraCaptureTarget(context)?.let { captureTarget ->
                            pendingCameraCapture = captureTarget
                            cameraPicker.launch(captureTarget.uri)
                        }
                    }
                )
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attach button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Cream)
                        .border(2.dp, Black, RectangleShape)
                        .clickable { showAttachmentMenu = !showAttachmentMenu },
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
                            if (isFocused) Modifier.neoShadow(4.dp, 4.dp, Cyan)
                            else Modifier.neoShadowSmall()
                        )
                        .border(2.dp, Black, RectangleShape)
                )

                // Send button
                val canSend = enabled && (text.isNotBlank() || pendingAttachments.isNotEmpty())
                val sendColor = if (canSend) Yellow else Color(0xFFD0D0D0)
                NeoPressableBox(
                    onClick = onSend,
                    enabled = canSend,
                    size = 40.dp,
                    backgroundColor = sendColor
                ) {
                    Text(text = "\u27A4", fontSize = 18.sp, color = if (canSend) Black else TextMuted)
                }
            }
        }
    }
}

@Composable
private fun AttachmentMenu(
    onPhotoClick: () -> Unit,
    onFileClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AttachmentMenuButton(
            label = "Photo",
            modifier = Modifier.weight(1f),
            onClick = onPhotoClick
        )
        AttachmentMenuButton(
            label = "File",
            modifier = Modifier.weight(1f),
            onClick = onFileClick
        )
        AttachmentMenuButton(
            label = "Camera",
            modifier = Modifier.weight(1f),
            onClick = onCameraClick
        )
    }
}

@Composable
private fun AttachmentMenuButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Cream)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Black
        )
    }
}

@Composable
private fun PendingAttachmentPreview(
    attachment: PendingAttachment,
    onRemove: () -> Unit
) {
    Box {
        if (isPendingAttachmentImage(attachment)) {
            PendingImageAttachmentPreview(attachment = attachment)
        } else {
            PendingFileAttachmentCard(attachment = attachment)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .size(20.dp)
                .background(Pink, RectangleShape)
                .border(1.5.dp, Color.Black, RectangleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2715", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PendingImageAttachmentPreview(attachment: PendingAttachment) {
    Box(modifier = Modifier.size(72.dp)) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .offset(3.dp, 3.dp)
                .background(Color.Black, RectangleShape)
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(attachment.uri)
                .crossfade(true)
                .build(),
            contentDescription = attachment.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(68.dp)
                .border(2.dp, Color.Black, RectangleShape)
                .clip(RectangleShape)
        )
    }
}

@Composable
private fun PendingFileAttachmentCard(attachment: PendingAttachment) {
    Row(
        modifier = Modifier
            .width(180.dp)
            .heightIn(min = 68.dp)
            .background(Cream)
            .border(2.dp, Black, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(White)
                .border(1.5.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pendingAttachmentTypeLabel(attachment),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Black,
                maxLines = 1
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.name.ifBlank { "Unnamed attachment" },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = attachment.mimeType.ifBlank { "FILE" },
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// System message divider (Style A: centered text with lines)
@Composable
private fun SystemMessageDivider(content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color(0xFFCCCCCC)
        )
        NeoMessageContent(
            content = content,
            modifier = Modifier.padding(horizontal = 12.dp),
            textColor = TextSecondary
        )
        Divider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color(0xFFCCCCCC)
        )
    }
}

// Reply preview banner above compose bar
@Composable
private fun ReplyPreviewBanner(message: Message, onDismiss: () -> Unit) {
    Surface(color = Color(0xFFF5F5F5)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .background(Cyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Replying to ${message.senderName.orEmpty().ifEmpty { "Unknown" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Black
                )
                Text(
                    text = message.content.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Cream)
                    .border(1.5.dp, Black, RectangleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\u2715", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Black)
            }
        }
    }
    Divider(thickness = 1.dp, color = Black.copy(alpha = 0.2f))
}

// Fullscreen Image Preview with pinch-to-zoom
@Composable
private fun ImagePreviewOverlay(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(onClick = onDismiss)
    ) {
        // Close button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
                .size(40.dp)
                .background(Color.White, RectangleShape)
                .border(2.dp, Color.Black, RectangleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2715", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Zoomable image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .clickable { /* consume click to prevent dismiss */ }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgentBatchControlSheet(
    agents: List<Agent>,
    channelName: String,
    onDismiss: () -> Unit,
    onStopAll: (onSuccess: () -> Unit) -> Unit,
    onResumeAllWithCorrection: (prompt: String, onSuccess: () -> Unit) -> Unit,
    onKeepStopped: () -> Unit
) {
    var phase by remember { mutableStateOf(AgentControlPhase.CONFIRM_STOP) }
    var correctionText by remember { mutableStateOf("") }
    val activeCount = agents.count { it.status == "active" }

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            when (phase) {
                AgentControlPhase.CONFIRM_STOP -> {
                    Text(
                        text = "Agent Control",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "This will immediately stop all running agents in #$channelName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    agents.forEach { agent -> AgentStatusRow(agent = agent) }

                    Spacer(modifier = Modifier.height(16.dp))

                    NeoButton(
                        text = "STOP ALL AGENTS",
                        onClick = {
                            onStopAll { phase = AgentControlPhase.STOPPED }
                        },
                        containerColor = Pink,
                        contentColor = Black,
                        enabled = activeCount > 0
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NeoButton(
                        text = "CANCEL",
                        onClick = onDismiss,
                        containerColor = Cream,
                        contentColor = Black
                    )
                }
                AgentControlPhase.STOPPED -> {
                    Text(
                        text = "All Agents Stopped",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Provide new guidance or corrections. All agents will see this when they resume.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    NeoTextField(
                        value = correctionText,
                        onValueChange = { correctionText = it },
                        placeholder = "e.g. Stop modifying the database schema \u2014 focus only on the frontend changes I described...",
                        focusHighlight = Orange,
                        modifier = Modifier.testTag("correctionTextField")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NeoButton(
                        text = "RESUME ALL",
                        onClick = {
                            if (correctionText.isNotBlank()) {
                                val prompt = buildSosPrompt(channelName, correctionText)
                                onResumeAllWithCorrection(prompt) { onDismiss() }
                            }
                        },
                        containerColor = Lime,
                        contentColor = Black,
                        enabled = correctionText.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NeoButton(
                        text = "KEEP STOPPED",
                        onClick = onKeepStopped,
                        containerColor = Cream,
                        contentColor = Black
                    )
                }
            }
        }
    }
}

internal enum class AgentControlPhase { CONFIRM_STOP, STOPPED }

internal fun buildSosPrompt(channelName: String, userText: String): String =
    "[SOS] The user has emergency-stopped all agents in #$channelName because they were going off-track. " +
        "Here is the user's correction and new guidance:\n\n$userText\n\n" +
        "Read this carefully, acknowledge the correction, and adjust your approach accordingly. " +
        "Use check_messages and read_history to understand the current state before taking any action."

@Composable
private fun AgentStatusRow(agent: Agent) {
    val isActive = agent.status == "active"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(if (isActive) Orange else Color(0xFFEEEEEE))
                .border(2.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = agent.name.orEmpty().take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Black
            )
        }
        Text(
            text = agent.name.orEmpty(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Black,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (isActive) Lime else Color(0xFFCCCCCC))
                .border(1.dp, Black, RectangleShape)
        )
        Text(
            text = if (isActive) "Active" else "Stopped",
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Black else TextMuted
        )
    }
}
