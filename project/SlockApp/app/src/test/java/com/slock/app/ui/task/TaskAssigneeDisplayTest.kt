package com.slock.app.ui.task

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.Member
import com.slock.app.data.model.Task
import com.slock.app.data.model.User
import com.slock.app.data.repository.ServerRepository
import com.slock.app.data.repository.TaskRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TaskAssigneeDisplayTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepository: TaskRepository = mock()
    private val serverRepository: ServerRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val secureTokenStorage: SecureTokenStorage = mock()
    private val activeServerHolder = ActiveServerHolder(secureTokenStorage)

    @Test
    fun `TaskViewModel loadTasks populates memberNames from server members`() = runTest {
        whenever(taskRepository.getTasks(any(), any())).thenReturn(
            Result.success(listOf(
                Task(id = "t1", assigneeId = "user-abc-123", title = "Test task")
            ))
        )
        whenever(serverRepository.getServerMembers(any())).thenReturn(
            Result.success(listOf(
                Member(id = "m1", userId = "user-abc-123", user = User(name = "Alice"))
            ))
        )

        activeServerHolder.serverId = "srv1"
        val vm = TaskViewModel(taskRepository, serverRepository, activeServerHolder)
        vm.loadTasks("ch1")
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Alice", state.memberNames["user-abc-123"])
    }

    @Test
    fun `TaskViewModel memberNames falls back to displayName when user is null`() = runTest {
        whenever(taskRepository.getTasks(any(), any())).thenReturn(Result.success(emptyList()))
        whenever(serverRepository.getServerMembers(any())).thenReturn(
            Result.success(listOf(
                Member(id = "m2", userId = "user-xyz", user = null, displayName = "Bob Display")
            ))
        )

        activeServerHolder.serverId = "srv1"
        val vm = TaskViewModel(taskRepository, serverRepository, activeServerHolder)
        vm.loadTasks("ch1")
        advanceUntilIdle()

        assertEquals("Bob Display", vm.state.value.memberNames["user-xyz"])
    }

    @Test
    fun `TaskViewModel memberNames empty when server members fetch fails`() = runTest {
        whenever(taskRepository.getTasks(any(), any())).thenReturn(Result.success(emptyList()))
        whenever(serverRepository.getServerMembers(any())).thenReturn(
            Result.failure(RuntimeException("network error"))
        )

        activeServerHolder.serverId = "srv1"
        val vm = TaskViewModel(taskRepository, serverRepository, activeServerHolder)
        vm.loadTasks("ch1")
        advanceUntilIdle()

        assertTrue(vm.state.value.memberNames.isEmpty())
    }

    @Test
    fun `ServerTasksViewModel loadAllTasks populates memberNames`() = runTest {
        whenever(taskRepository.getServerTasks(any())).thenReturn(
            Result.success(listOf(
                Task(id = "t1", assigneeId = "user-abc-123", title = "Server task")
            ))
        )
        whenever(serverRepository.getServerMembers(any())).thenReturn(
            Result.success(listOf(
                Member(id = "m1", userId = "user-abc-123", user = User(name = "Charlie"))
            ))
        )
        whenever(socketIOManager.events).thenReturn(emptyFlow())

        val vm = ServerTasksViewModel(taskRepository, serverRepository, socketIOManager, activeServerHolder)
        vm.loadAllTasks("srv1")
        advanceUntilIdle()

        assertEquals("Charlie", vm.state.value.memberNames["user-abc-123"])
    }
}

class TaskAssigneeSourceTest {

    private fun taskListSource(): String = listOf(
        java.io.File("src/main/java/com/slock/app/ui/task/TaskListScreen.kt"),
        java.io.File("app/src/main/java/com/slock/app/ui/task/TaskListScreen.kt")
    ).first { it.exists() }.readText()

    private fun serverTasksSource(): String = listOf(
        java.io.File("src/main/java/com/slock/app/ui/task/ServerTasksScreen.kt"),
        java.io.File("app/src/main/java/com/slock/app/ui/task/ServerTasksScreen.kt")
    ).first { it.exists() }.readText()

    private fun taskViewModelSource(): String = listOf(
        java.io.File("src/main/java/com/slock/app/ui/task/TaskViewModel.kt"),
        java.io.File("app/src/main/java/com/slock/app/ui/task/TaskViewModel.kt")
    ).first { it.exists() }.readText()

    private fun serverTasksViewModelSource(): String = listOf(
        java.io.File("src/main/java/com/slock/app/ui/task/ServerTasksViewModel.kt"),
        java.io.File("app/src/main/java/com/slock/app/ui/task/ServerTasksViewModel.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `NeoTaskCard uses memberNames for assignee display`() {
        val source = taskListSource()
        val cardBlock = source.substringAfter("private fun NeoTaskCard(")
            .substringBefore("private fun CreateTaskSheet(")
        assertTrue(
            "NeoTaskCard must use memberNames for display initial",
            cardBlock.contains("memberNames[assignee]")
        )
    }

    @Test
    fun `ServerTaskCard uses memberNames for assignee display`() {
        val source = serverTasksSource()
        val cardBlock = source.substringAfter("private fun ServerTaskCard(")
        assertTrue(
            "ServerTaskCard must use memberNames for display initial",
            cardBlock.contains("memberNames[assignee]")
        )
    }

    @Test
    fun `TaskViewModel injects ServerRepository`() {
        val source = taskViewModelSource()
        assertTrue(
            "TaskViewModel must inject ServerRepository",
            source.contains("serverRepository: ServerRepository")
        )
    }

    @Test
    fun `TaskViewModel calls getServerMembers`() {
        val source = taskViewModelSource()
        assertTrue(
            "TaskViewModel must call getServerMembers to load member names",
            source.contains("getServerMembers")
        )
    }

    @Test
    fun `ServerTasksViewModel injects ServerRepository`() {
        val source = serverTasksViewModelSource()
        assertTrue(
            "ServerTasksViewModel must inject ServerRepository",
            source.contains("serverRepository: ServerRepository")
        )
    }

    @Test
    fun `ServerTasksViewModel calls getServerMembers`() {
        val source = serverTasksViewModelSource()
        assertTrue(
            "ServerTasksViewModel must call getServerMembers to load member names",
            source.contains("getServerMembers")
        )
    }

    @Test
    fun `TaskUiState has memberNames field`() {
        val source = taskViewModelSource()
        assertTrue(
            "TaskUiState must have memberNames field",
            source.contains("memberNames: Map<String, String>")
        )
    }

    @Test
    fun `ServerTasksUiState has memberNames field`() {
        val source = serverTasksViewModelSource()
        assertTrue(
            "ServerTasksUiState must have memberNames field",
            source.contains("memberNames: Map<String, String>")
        )
    }
}
