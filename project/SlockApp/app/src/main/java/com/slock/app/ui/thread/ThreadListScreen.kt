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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.ui.theme.*

@Composable
fun ThreadListScreen(
    state: ThreadListUiState,
    onThreadClick: (threadChannelId: String, parentMessage: com.slock.app.data.model.Message, channelName: String) -> Unit,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit = {},
    onTabSelected: (ThreadInboxTab) -> Unit = {},
    onMarkDone: (threadChannelId: String) -> Unit = {},
    onUndoDone: (threadChannelId: String) -> Unit = {},
    onUnfollow: (threadChannelId: String) -> Unit = {},
    showHeader: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        if (showHeader) {
            ThreadListHeader(onBack = onNavigateBack)
        }

        ThreadInboxTabStrip(
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> {
                    NeoSkeletonCardList()
                }
                state.error != null -> {
                    NeoErrorState(
                        message = "讨论加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = onRetry,
                        logContext = state.error
                    )
                }
                state.threads.isEmpty() -> {
                    val emptyMessage = when (state.selectedTab) {
                        ThreadInboxTab.FOLLOWING -> "No followed threads"
                        ThreadInboxTab.ALL -> "No threads yet"
                        ThreadInboxTab.DONE -> "No done threads"
                    }
                    val emptyHint = when (state.selectedTab) {
                        ThreadInboxTab.FOLLOWING -> "Threads you follow will appear here"
                        ThreadInboxTab.ALL -> "Threads will appear here when conversations start"
                        ThreadInboxTab.DONE -> "Threads you mark as done will appear here"
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "\uD83E\uDDF5", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(emptyMessage, style = MaterialTheme.typography.titleMedium, color = Black)
                        Text(emptyHint, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
                                isDoneTab = state.selectedTab == ThreadInboxTab.DONE,
                                onClick = {
                                    onThreadClick(
                                        thread.threadChannelId,
                                        thread.parentMessage,
                                        thread.channelName
                                    )
                                },
                                onMarkDone = { onMarkDone(thread.threadChannelId) },
                                onUndoDone = { onUndoDone(thread.threadChannelId) },
                                onUnfollow = { onUnfollow(thread.threadChannelId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadInboxTabStrip(
    selectedTab: ThreadInboxTab,
    onTabSelected: (ThreadInboxTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("threadInboxTabStrip"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThreadInboxTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val label = when (tab) {
                ThreadInboxTab.FOLLOWING -> "Following"
                ThreadInboxTab.ALL -> "All"
                ThreadInboxTab.DONE -> "Done"
            }
            Box(
                modifier = Modifier
                    .background(if (isSelected) Black else White)
                    .border(2.dp, Black, RectangleShape)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("tab_$label")
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) White else Black
                )
            }
        }
    }
}

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

@Composable
private fun ThreadCard(
    thread: ThreadItem,
    isDoneTab: Boolean,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
    onUndoDone: () -> Unit,
    onUnfollow: () -> Unit
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
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(if (isDoneTab) Lime else Lavender)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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

                Text(
                    text = formatThreadTime(thread.lastActivity),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThreadIndicatorChip(
                        text = threadReplySummary(thread.replyCount),
                        testTag = "threadReplyCountChip"
                    )
                    if (thread.unreadCount > 0) {
                        ThreadIndicatorChip(
                            text = threadUnreadSummary(thread.unreadCount),
                            backgroundColor = Orange,
                            testTag = "threadUnreadBadge"
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isDoneTab) {
                        Box(
                            modifier = Modifier
                                .background(Lime)
                                .border(1.dp, Black, RectangleShape)
                                .clickable(onClick = onUndoDone)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("undoDoneButton")
                        ) {
                            Text(text = "UNDO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Black)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Lime)
                                .border(1.dp, Black, RectangleShape)
                                .clickable(onClick = onMarkDone)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("markDoneButton")
                        ) {
                            Text(text = "DONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Black)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEEEEEE))
                                .border(1.dp, Black, RectangleShape)
                                .clickable(onClick = onUnfollow)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("unfollowButton")
                        ) {
                            Text(text = "UNFOLLOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadIndicatorChip(
    text: String,
    backgroundColor: Color = Lavender,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .border(1.dp, Black, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "\uD83D\uDCAC", fontSize = 11.sp)
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
    }
}

private fun threadReplySummary(replyCount: Int): String {
    return when (replyCount) {
        1 -> "1 reply"
        0 -> "0 replies"
        else -> "$replyCount replies"
    }
}

private fun threadUnreadSummary(unreadCount: Int): String {
    return when {
        unreadCount > 99 -> "99+ new"
        unreadCount == 1 -> "1 new"
        else -> "$unreadCount new"
    }
}

private fun formatThreadTime(isoTime: String): String {
    if (isoTime.isBlank()) return ""
    val timePart = isoTime.split("T").getOrNull(1)?.take(5) ?: ""
    val datePart = isoTime.split("T").getOrNull(0) ?: ""
    return if (datePart.isNotBlank() && timePart.isNotBlank()) {
        "$datePart $timePart"
    } else {
        timePart
    }
}
