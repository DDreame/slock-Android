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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.ui.theme.*
import com.slock.app.util.LogCollector

@Composable
fun ThreadListScreen(
    state: ThreadListUiState,
    onThreadClick: (threadChannelId: String, parentMessage: com.slock.app.data.model.Message, channelName: String) -> Unit,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit = {},
    showHeader: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Lavender header
        if (showHeader) {
            ThreadListHeader(onBack = onNavigateBack)
        }

        // Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> {
                    NeoSkeletonCardList()
                }
                state.error != null -> {
                    val context = LocalContext.current
                    NeoErrorState(
                        message = "讨论加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = onRetry,
                        onSendLog = { LogCollector.shareReport(context, state.error) }
                    )
                }
                state.threads.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "\uD83E\uDDF5", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No threads yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Black
                        )
                        Text(
                            "Threads will appear here when conversations start",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.threads) { thread ->
                            ThreadCard(
                                thread = thread,
                                onClick = {
                                    onThreadClick(
                                        thread.threadChannelId,
                                        thread.parentMessage,
                                        thread.channelName
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Lavender header
@Composable
private fun ThreadListHeader(onBack: () -> Unit) {
    Surface(color = Lavender) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            NeoPressableBox(onClick = onBack) {
                Text(text = "\u2190", fontSize = 18.sp, color = Black)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Threads",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Black
            )
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

// Thread card with left Lavender accent
@Composable
private fun ThreadCard(
    thread: ThreadItem,
    onClick: () -> Unit
) {
    val isAgent = thread.parentMessage.isAgent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
    ) {
        // Left Lavender accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Lavender)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            // Channel tag + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Channel name tag
                Box(
                    modifier = Modifier
                        .background(Lavender)
                        .border(1.dp, Black, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "# ${thread.channelName}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }

                // Time
                Text(
                    text = formatThreadTime(thread.lastActivity),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Author + content
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Lavender)
                        .border(2.dp, Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = thread.parentMessage.senderName.orEmpty().ifEmpty { "Unknown" }.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Black
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Author name with agent badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = thread.parentMessage.senderName.orEmpty().ifEmpty { "Unknown" },
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
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Message preview
                    Text(
                        text = thread.parentMessage.content.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF444444),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: reply count + tap hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .background(Lavender)
                        .border(1.dp, Black, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "\uD83D\uDCAC", fontSize = 11.sp)
                    Text(
                        text = "View thread",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
            }
        }
    }
}

private fun formatThreadTime(isoTime: String): String {
    if (isoTime.isBlank()) return ""
    // Extract time portion from ISO string
    val timePart = isoTime.split("T").getOrNull(1)?.take(5) ?: ""
    val datePart = isoTime.split("T").getOrNull(0) ?: ""
    return if (datePart.isNotBlank() && timePart.isNotBlank()) {
        "$datePart $timePart"
    } else {
        timePart
    }
}
