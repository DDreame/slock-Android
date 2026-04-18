package com.slock.app.ui.message

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.model.ConvertMessageToTaskRequest
import com.slock.app.data.model.Message
import com.slock.app.data.model.Task
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.TaskRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class MessageConvertToTaskTitleTest {

    @Test
    fun `prefillConvertTaskTitle uses first non blank line`() {
        val title = prefillConvertTaskTitle(
            Message(content = "\n  Check notification deep-link flow   \nPlease verify cold start")
        )

        assertEquals("Check notification deep-link flow", title)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MessageConvertToTaskExecutionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository: MessageRepository = mock()
    private val taskRepository: TaskRepository = mock()
    private val channelRepository: ChannelRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val activeServerHolder: ActiveServerHolder = mock()
    private val presenceTracker = PresenceTracker()

    private fun createViewModel(): MessageViewModel {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        runBlocking { whenever(messageRepository.isCachedMessagesFresh(any(), any())).thenReturn(false) }
        return MessageViewModel(messageRepository, taskRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
    }

    @Test
    fun `showConvertToTask seeds draft from source message`() = runTest {
        val vm = createViewModel()
        val source = Message(
            id = "msg-1",
            senderName = "DDreame",
            content = "Check notification deep-link flow"
        )

        vm.showConvertToTask(source, "general")

        val draft = vm.state.value.convertToTaskDraft
        assertNotNull(draft)
        assertEquals(source, draft?.sourceMessage)
        assertEquals("general", draft?.channelName)
        assertEquals("Check notification deep-link flow", draft?.title)
        assertEquals("todo", draft?.status)
    }

    @Test
    fun `submitConvertToTask posts convert request and updates selected status`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null)).thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50)).thenReturn(Result.success(emptyList()))

        val request = ConvertMessageToTaskRequest(
            messageId = "msg-1",
            title = "Check notification deep-link flow",
            status = "in_review",
            channelId = "ch-1"
        )
        whenever(taskRepository.convertMessageToTask(eq("srv-1"), eq(request))).thenReturn(
            Result.success(Task(id = "task-1", channelId = "ch-1", messageId = "msg-1", status = "todo"))
        )
        whenever(taskRepository.updateTaskStatus("srv-1", "task-1", "in_review")).thenReturn(
            Result.success(Task(id = "task-1", channelId = "ch-1", messageId = "msg-1", status = "in_review"))
        )

        val vm = createViewModel()
        vm.loadMessages("ch-1")
        advanceUntilIdle()

        vm.showConvertToTask(
            Message(id = "msg-1", senderName = "DDreame", content = "Check notification deep-link flow"),
            "general"
        )
        vm.updateConvertToTaskStatus("in_review")
        vm.submitConvertToTask()
        advanceUntilIdle()

        verify(taskRepository).convertMessageToTask("srv-1", request)
        verify(taskRepository).updateTaskStatus("srv-1", "task-1", "in_review")
        assertNull(vm.state.value.convertToTaskDraft)
        assertEquals("Task created from message", vm.state.value.convertTaskFeedbackMessage)
    }

    @Test
    fun `submitConvertToTask keeps draft open when convert fails`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null)).thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50)).thenReturn(Result.success(emptyList()))

        val request = ConvertMessageToTaskRequest(
            messageId = "msg-2",
            title = "Message to convert",
            status = "todo",
            channelId = "ch-1"
        )
        whenever(taskRepository.convertMessageToTask(eq("srv-1"), eq(request))).thenReturn(
            Result.failure(IllegalStateException("convert failed"))
        )

        val vm = createViewModel()
        vm.loadMessages("ch-1")
        advanceUntilIdle()

        vm.showConvertToTask(
            Message(id = "msg-2", senderName = "DDreame", content = "Message to convert"),
            "general"
        )
        vm.submitConvertToTask()
        advanceUntilIdle()

        verify(taskRepository).convertMessageToTask("srv-1", request)
        verify(taskRepository, never()).updateTaskStatus(any(), any(), any())
        assertNotNull(vm.state.value.convertToTaskDraft)
        assertFalse(vm.state.value.convertToTaskDraft?.isSubmitting ?: true)
        assertEquals("convert failed", vm.state.value.convertTaskFeedbackMessage)
    }
}

class MessageConvertToTaskStructuralTest {

    private val messageListSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt")
    ).first { it.exists() }.readText()

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    private val apiSource: String = listOf(
        File("src/main/java/com/slock/app/data/api/ApiService.kt"),
        File("app/src/main/java/com/slock/app/data/api/ApiService.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `message action sheet exposes Convert to Task entry`() {
        assertTrue(messageListSource.contains("label = \"Convert to Task\""))
        assertTrue(messageListSource.contains("canConvertToTask = !message.isTask"))
    }

    @Test
    fun `convert task sheet renders source status and channel sections`() {
        assertTrue(messageListSource.contains("NeoLabel(\"SOURCE MESSAGE\")"))
        assertTrue(messageListSource.contains("NeoLabel(\"STATUS\")"))
        assertTrue(messageListSource.contains("NeoLabel(\"CHANNEL\")"))
        assertTrue(messageListSource.contains("ConvertToTaskSheet("))
    }

    @Test
    fun `NavHost wires convert task callbacks to MessageViewModel`() {
        val messageBlock = navHostSource
            .substringAfter("MessageListScreen(")
            .substringBefore("composable(Routes.SAVED_CHANNELS)")
        assertTrue(messageBlock.contains("onShowConvertToTask = viewModel::showConvertToTask"))
        assertTrue(messageBlock.contains("onDismissConvertToTask = viewModel::dismissConvertToTask"))
        assertTrue(messageBlock.contains("onSubmitConvertToTask = viewModel::submitConvertToTask"))
    }

    @Test
    fun `ApiService includes tasks convert message endpoint`() {
        assertTrue(apiSource.contains("@POST(\"tasks/convert-message\")"))
        assertTrue(apiSource.contains("convertMessageToTask"))
    }
}
