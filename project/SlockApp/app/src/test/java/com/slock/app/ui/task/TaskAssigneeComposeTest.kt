package com.slock.app.ui.task

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.slock.app.data.model.Task
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskAssigneeComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `TaskListScreen shows member name initial instead of UUID initial`() {
        val task = Task(
            id = "t1",
            assigneeId = "zz-uuid-999",
            title = "Fix the widget",
            status = "in_progress"
        )
        val memberNames = mapOf("zz-uuid-999" to "Alice")

        composeTestRule.setContent {
            TaskListScreen(
                channelId = "ch1",
                state = TaskUiState(
                    tasks = listOf(task),
                    memberNames = memberNames
                ),
                onCreateTask = { _, _ -> },
                onUpdateStatus = { _, _ -> },
                onDeleteTask = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Fix the widget").assertIsDisplayed()
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun `TaskListScreen falls back to UUID initial when memberNames is empty`() {
        val task = Task(
            id = "t2",
            assigneeId = "zz-uuid-999",
            title = "Fallback task",
            status = "todo"
        )

        composeTestRule.setContent {
            TaskListScreen(
                channelId = "ch1",
                state = TaskUiState(
                    tasks = listOf(task),
                    memberNames = emptyMap()
                ),
                onCreateTask = { _, _ -> },
                onUpdateStatus = { _, _ -> },
                onDeleteTask = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Fallback task").assertIsDisplayed()
        composeTestRule.onNodeWithText("Z").assertIsDisplayed()
    }

    @Test
    fun `ServerTasksScreen shows member name initial instead of UUID initial`() {
        val task = Task(
            id = "t3",
            assigneeId = "zz-uuid-888",
            title = "Server widget fix",
            status = "in_progress"
        )
        val memberNames = mapOf("zz-uuid-888" to "Bob")

        composeTestRule.setContent {
            ServerTasksScreen(
                state = ServerTasksUiState(
                    tasks = listOf(task),
                    memberNames = memberNames,
                    collapsedGroups = emptySet()
                ),
                onToggleGroup = {},
                onUpdateStatus = { _, _ -> },
                onDeleteTask = {},
                onNavigateBack = {},
                showHeader = false
            )
        }

        composeTestRule.onNodeWithText("Server widget fix").assertIsDisplayed()
        composeTestRule.onNodeWithText("B").assertIsDisplayed()
    }

    @Test
    fun `ServerTasksScreen falls back to UUID initial when memberNames is empty`() {
        val task = Task(
            id = "t4",
            assigneeId = "zz-uuid-888",
            title = "Server fallback",
            status = "in_review"
        )

        composeTestRule.setContent {
            ServerTasksScreen(
                state = ServerTasksUiState(
                    tasks = listOf(task),
                    memberNames = emptyMap(),
                    collapsedGroups = emptySet()
                ),
                onToggleGroup = {},
                onUpdateStatus = { _, _ -> },
                onDeleteTask = {},
                onNavigateBack = {},
                showHeader = false
            )
        }

        composeTestRule.onNodeWithText("Server fallback").assertIsDisplayed()
        composeTestRule.onNodeWithText("Z").assertIsDisplayed()
    }
}
