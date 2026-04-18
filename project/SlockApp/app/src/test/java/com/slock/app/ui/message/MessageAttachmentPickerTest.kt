package com.slock.app.ui.message

import android.net.Uri
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.model.Message
import com.slock.app.data.model.UploadResponse
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class MessageAttachmentPickerStateTest {

    private fun fakeUri(rawValue: String): Uri {
        val uri: Uri = mock()
        whenever(uri.toString()).thenReturn(rawValue)
        return uri
    }

    @Test
    fun `isPendingAttachmentImage distinguishes image and file mime types`() {
        val imageAttachment = PendingAttachment(
            uri = fakeUri("file:///tmp/photo.png"),
            name = "photo.png",
            mimeType = "image/png",
            bytes = byteArrayOf(1)
        )
        val fileAttachment = PendingAttachment(
            uri = fakeUri("file:///tmp/report.pdf"),
            name = "report.pdf",
            mimeType = "application/pdf",
            bytes = byteArrayOf(2)
        )

        assertTrue(isPendingAttachmentImage(imageAttachment))
        assertFalse(isPendingAttachmentImage(fileAttachment))
    }

    @Test
    fun `pendingAttachmentTypeLabel prefers file extension and falls back to file`() {
        val pdfAttachment = PendingAttachment(
            uri = fakeUri("file:///tmp/report.pdf"),
            name = "report.pdf",
            mimeType = "application/pdf",
            bytes = byteArrayOf(1)
        )
        val unknownAttachment = PendingAttachment(
            uri = fakeUri("file:///tmp/blob"),
            name = "blob",
            mimeType = "application/octet-stream",
            bytes = byteArrayOf(2)
        )

        assertEquals("PDF", pendingAttachmentTypeLabel(pdfAttachment))
        assertEquals("FILE", pendingAttachmentTypeLabel(unknownAttachment))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MessageAttachmentPickerExecutionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository: MessageRepository = mock()
    private val channelRepository: ChannelRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val activeServerHolder: ActiveServerHolder = mock()
    private val presenceTracker = PresenceTracker()

    private fun fakeUri(rawValue: String): Uri {
        val uri: Uri = mock()
        whenever(uri.toString()).thenReturn(rawValue)
        return uri
    }

    private fun createViewModel(): MessageViewModel {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(messageRepository.isCachedMessagesFresh(any(), any())).thenReturn(false)
        return MessageViewModel(messageRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
    }

    @Test
    fun `sendMessage uploads file attachment with original mime and forwards attachment id`() = runTest {
        val attachmentBytes = "pdf".toByteArray()
        val pendingAttachment = PendingAttachment(
            uri = fakeUri("file:///tmp/report.pdf"),
            name = "report.pdf",
            mimeType = "application/pdf",
            bytes = attachmentBytes
        )
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.uploadFile("srv-1", "report.pdf", "application/pdf", attachmentBytes))
            .thenReturn(Result.success(UploadResponse(id = "att-1")))
        whenever(messageRepository.sendMessage("srv-1", "ch-1", "", listOf("att-1"), false, null))
            .thenReturn(Result.success(Message(id = "m-1", channelId = "ch-1", attachments = emptyList())))

        val viewModel = createViewModel()
        viewModel.loadMessages("ch-1")
        advanceUntilIdle()

        viewModel.addAttachment(pendingAttachment)
        viewModel.sendMessage("")
        advanceUntilIdle()

        verify(messageRepository).uploadFile("srv-1", "report.pdf", "application/pdf", attachmentBytes)
        verify(messageRepository).sendMessage("srv-1", "ch-1", "", listOf("att-1"), false, null)
    }

    @Test
    fun `sendMessage surfaces generic attachment error when all uploads fail without text`() = runTest {
        val pendingAttachment = PendingAttachment(
            uri = fakeUri("file:///tmp/report.pdf"),
            name = "report.pdf",
            mimeType = "application/pdf",
            bytes = "pdf".toByteArray()
        )
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.uploadFile(eq("srv-1"), eq("report.pdf"), eq("application/pdf"), any()))
            .thenReturn(Result.failure(IllegalStateException("upload failed")))

        val viewModel = createViewModel()
        viewModel.loadMessages("ch-1")
        advanceUntilIdle()

        viewModel.addAttachment(pendingAttachment)
        viewModel.sendMessage("")
        advanceUntilIdle()

        assertEquals("附件上传失败，消息未发送", viewModel.state.value.sendError)
        verify(messageRepository, never()).sendMessage(any(), any(), any(), anyOrNull(), any(), anyOrNull())
    }
}

class MessageAttachmentPickerStructuralTest {

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt")
    ).first { it.exists() }.readText()

    private val manifestSource: String = listOf(
        File("src/main/AndroidManifest.xml"),
        File("app/src/main/AndroidManifest.xml")
    ).first { it.exists() }.readText()

    private val filePathsSource: String = listOf(
        File("src/main/res/xml/file_paths.xml"),
        File("app/src/main/res/xml/file_paths.xml")
    ).first { it.exists() }.readText()

    @Test
    fun `compose bar exposes photo file and camera launchers`() {
        assertTrue(screenSource.contains("photoPicker.launch(\"image/*\")"))
        assertTrue(screenSource.contains("filePicker.launch(\"*/*\")"))
        assertTrue(screenSource.contains("ActivityResultContracts.TakePicture()"))
        assertTrue(screenSource.contains("createCameraCaptureTarget(context)"))
        assertTrue(screenSource.contains("cameraPicker.launch(captureTarget.uri)"))
    }

    @Test
    fun `attachment menu renders three entry labels`() {
        assertTrue(screenSource.contains("label = \"Photo\""))
        assertTrue(screenSource.contains("label = \"File\""))
        assertTrue(screenSource.contains("label = \"Camera\""))
    }

    @Test
    fun `pending attachment preview distinguishes image and file cards`() {
        assertTrue(screenSource.contains("isPendingAttachmentImage(attachment)"))
        assertTrue(screenSource.contains("PendingFileAttachmentCard(attachment = attachment)"))
    }

    @Test
    fun `manifest declares file provider for camera capture`() {
        assertTrue(manifestSource.contains("androidx.core.content.FileProvider"))
        assertTrue(manifestSource.contains("android:authorities=\"${'$'}{applicationId}.fileprovider\""))
        assertTrue(manifestSource.contains("@xml/file_paths"))
    }

    @Test
    fun `file provider paths expose cache dir for camera temp files`() {
        assertTrue(filePathsSource.contains("<cache-path"))
        assertTrue(filePathsSource.contains("path=\".\""))
    }
}
