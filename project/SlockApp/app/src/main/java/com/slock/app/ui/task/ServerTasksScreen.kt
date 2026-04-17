package com.slock.app.ui.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import com.slock.app.data.model.Task
import com.slock.app.ui.theme.*
import com.slock.app.util.LogCollector

private data class TaskStatusGroup(
    val key: String,
    val label: String,
    val color: Color,
    val icon: String
)

private val STATUS_GROUPS = listOf(
    TaskStatusGroup("in_progress", "IN PROGRESS", Cyan, "\u26A1"),
    TaskStatusGroup("in_review", "IN REVIEW", Lavender, "\uD83D\uDD0D"),
    TaskStatusGroup("todo", "TODO", Cream, "\uD83D\uDCCB"),
    TaskStatusGroup("done", "DONE", Lime, "\u2705")
)

@Composable
fun ServerTasksScreen(
    state: ServerTasksUiState,
    onToggleGroup: (String) -> Unit,
    onUpdateStatus: (taskId: String, status: String) -> Unit,
    onDeleteTask: (taskId: String) -> Unit,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit = {},
    showHeader: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Yellow header
        if (showHeader) {
            ServerTasksHeader(onBack = onNavigateBack)
        }

        // Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && state.tasks.isEmpty() -> {
                    NeoSkeletonCardList()
                }
                state.error != null && state.tasks.isEmpty() -> {
                    val context = LocalContext.current
                    NeoErrorState(
                        message = "任务加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = onRetry,
                        onSendLog = { LogCollector.shareReport(context, state.error) }
                    )
                }
                state.tasks.isEmpty() && !state.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "\uD83D\uDCDD", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No tasks yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Black
                        )
                        Text(
                            "Tasks from all channels will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        STATUS_GROUPS.forEach { group ->
                            val tasksInGroup = state.tasks.filter { it.status == group.key }
                            val isCollapsed = group.key in state.collapsedGroups

                            item(key = "header_${group.key}") {
                                CollapsibleGroupHeader(
                                    group = group,
                                    count = tasksInGroup.size,
                                    isCollapsed = isCollapsed,
                                    onClick = { onToggleGroup(group.key) }
                                )
                            }

                            if (!isCollapsed) {
                                if (tasksInGroup.isEmpty()) {
                                    item(key = "empty_${group.key}") {
                                        EmptyGroupPlaceholder(group = group)
                                    }
                                } else {
                                    items(
                                        items = tasksInGroup,
                                        key = { it.id.orEmpty() }
                                    ) { task ->
                                        ServerTaskCard(
                                            task = task,
                                            groupColor = group.color,
                                            onAdvance = {
                                                val nextStatus = when (task.status) {
                                                    "todo" -> "in_progress"
                                                    "in_progress" -> "in_review"
                                                    "in_review" -> "done"
                                                    else -> null
                                                }
                                                nextStatus?.let { onUpdateStatus(task.id.orEmpty(), it) }
                                            },
                                            onDelete = { onDeleteTask(task.id.orEmpty()) }
                                        )
                                    }
                                }
                            }

                            item(key = "spacer_${group.key}") {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerTasksHeader(onBack: () -> Unit) {
    Surface(color = Yellow) {
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
                text = "Tasks",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Black
            )
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

@Composable
private fun CollapsibleGroupHeader(
    group: TaskStatusGroup,
    count: Int,
    isCollapsed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .neoShadowSmall()
            .background(group.color)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = group.icon, fontSize = 16.sp)
            Text(
                text = group.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Black
            )
            // Count badge
            Box(
                modifier = Modifier
                    .background(White)
                    .border(1.5.dp, Black, RectangleShape)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
            }
        }

        // Collapse indicator
        Text(
            text = if (isCollapsed) "\u25B6" else "\u25BC",
            fontSize = 12.sp,
            color = Black
        )
    }
}

@Composable
private fun EmptyGroupPlaceholder(group: TaskStatusGroup) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(group.color.copy(alpha = 0.3f))
            .border(1.5.dp, Color(0xFFCCCCCC), RectangleShape)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No ${group.label.lowercase()} tasks",
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun ServerTaskCard(
    task: Task,
    groupColor: Color,
    onAdvance: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.status == "done"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
    ) {
        // Color strip at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(groupColor)
        )

        Column(modifier = Modifier.padding(14.dp)) {
            // Title
            Text(
                text = task.title.orEmpty(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isDone) TextMuted else Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom row: status badge + assignee + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status badge
                Box(
                    modifier = Modifier
                        .background(groupColor)
                        .border(1.5.dp, Black, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.status.orEmpty().uppercase().replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }

                // Assignee avatar stub
                task.assigneeId?.let { assignee ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Cyan)
                            .border(1.5.dp, Black, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = assignee.take(1).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isDone) {
                        Box(
                            modifier = Modifier
                                .background(Yellow)
                                .border(1.5.dp, Black, RectangleShape)
                                .clickable(onClick = onAdvance)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "\u25B6",
                                fontSize = 12.sp,
                                color = Black
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Pink)
                            .border(1.5.dp, Black, RectangleShape)
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "\u2715",
                            fontSize = 12.sp,
                            color = Black
                        )
                    }
                }
            }
        }
    }
}
