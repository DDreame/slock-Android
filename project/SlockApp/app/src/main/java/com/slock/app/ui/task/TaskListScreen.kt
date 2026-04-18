package com.slock.app.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Task
import com.slock.app.ui.theme.*

private data class StatusColumn(
    val key: String,
    val label: String,
    val color: Color,
    val icon: String
)

private val STATUS_COLUMNS = listOf(
    StatusColumn("todo", "TODO", Cream, "\uD83D\uDCCB"),
    StatusColumn("in_progress", "IN PROGRESS", Cyan, "\u26A1"),
    StatusColumn("in_review", "IN REVIEW", Lavender, "\uD83D\uDD0D"),
    StatusColumn("done", "DONE", Lime, "\u2705")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    channelId: String,
    state: TaskUiState,
    onCreateTask: (title: String, description: String?) -> Unit,
    onUpdateStatus: (taskId: String, status: String) -> Unit,
    onDeleteTask: (taskId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Yellow header
        TaskHeader(
            onBack = onNavigateBack,
            onCreateClick = { showCreateDialog = true }
        )

        // Status filter chips
        StatusFilterBar(
            selectedStatus = selectedStatus,
            taskCounts = STATUS_COLUMNS.associate { col ->
                col.key to state.tasks.count { it.status == col.key }
            },
            onSelectStatus = { status ->
                selectedStatus = if (selectedStatus == status) null else status
            }
        )

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    NeoSkeletonCardList()
                }
                state.tasks.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "\uD83D\uDCDD", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No tasks yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Black
                        )
                        Text(
                            "Create your first task",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        NeoButton(
                            text = "CREATE TASK",
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                    }
                }
                else -> {
                    val filteredColumns = if (selectedStatus != null) {
                        STATUS_COLUMNS.filter { it.key == selectedStatus }
                    } else {
                        STATUS_COLUMNS
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        filteredColumns.forEach { column ->
                            val tasksInColumn = state.tasks.filter { it.status == column.key }

                            item {
                                KanbanColumnHeader(
                                    column = column,
                                    count = tasksInColumn.size
                                )
                            }

                            if (tasksInColumn.isEmpty()) {
                                item {
                                    EmptyColumnPlaceholder(column = column)
                                }
                            }

                            items(tasksInColumn) { task ->
                                NeoTaskCard(
                                    task = task,
                                    columnColor = column.color,
                                    memberNames = state.memberNames,
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

                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Create task bottom sheet
    if (showCreateDialog) {
        CreateTaskSheet(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, description ->
                onCreateTask(title, description)
                showCreateDialog = false
            }
        )
    }
}

// Yellow header
@Composable
private fun TaskHeader(onBack: () -> Unit, onCreateClick: () -> Unit) {
    Surface(color = Yellow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            NeoPressableBox(onClick = onCreateClick) {
                Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Black)
            }
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

// Status filter chips
@Composable
private fun StatusFilterBar(
    selectedStatus: String?,
    taskCounts: Map<String, Int>,
    onSelectStatus: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        STATUS_COLUMNS.forEach { column ->
            val isSelected = selectedStatus == column.key
            val count = taskCounts[column.key] ?: 0
            Box(
                modifier = Modifier
                    .background(if (isSelected) column.color else White)
                    .border(2.dp, Black, RectangleShape)
                    .clickable { onSelectStatus(column.key) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${column.label} ($count)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// Kanban column header
@Composable
private fun KanbanColumnHeader(column: StatusColumn, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(column.color)
                .border(1.dp, Black, CircleShape)
        )
        Text(
            text = column.icon,
            fontSize = 14.sp
        )
        Text(
            text = "${column.label} ($count)",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextMuted
        )
        Divider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color(0xFFDDDDDD)
        )
    }
}

// Empty column placeholder
@Composable
private fun EmptyColumnPlaceholder(column: StatusColumn) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(column.color.copy(alpha = 0.3f))
            .border(1.5.dp, Color(0xFFCCCCCC), RectangleShape)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No ${column.label.lowercase()} tasks",
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

// Task card
@Composable
private fun NeoTaskCard(
    task: Task,
    columnColor: Color,
    memberNames: Map<String, String> = emptyMap(),
    onAdvance: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.status == "done"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
    ) {
        // Color strip at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(columnColor)
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
                        .background(columnColor)
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
                    val displayInitial = (memberNames[assignee] ?: assignee).take(1).uppercase()
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Cyan)
                            .border(1.5.dp, Black, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayInitial,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isDone) {
                        // Advance button
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
                    // Delete button
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

// Create task bottom sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskSheet(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
            Text(
                text = "Create Task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            NeoLabel("TITLE")
            NeoTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Task title"
            )

            Spacer(modifier = Modifier.height(14.dp))

            NeoLabel("DESCRIPTION")
            NeoTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Optional description"
            )

            Spacer(modifier = Modifier.height(16.dp))

            NeoButton(
                text = "CREATE TASK",
                onClick = { if (title.isNotBlank()) onCreate(title, description.ifBlank { null }) },
                enabled = title.isNotBlank()
            )
        }
    }
}
