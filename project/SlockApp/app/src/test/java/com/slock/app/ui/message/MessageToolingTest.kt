package com.slock.app.ui.message

import com.slock.app.data.model.Attachment
import com.slock.app.data.model.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageToolingTest {

    @Test
    fun `buildMessageCopyTargets keeps original markdown and first markdown link`() {
        val message = Message(
            id = "msg-1",
            content = "Check [docs](https://docs.slock.ai) and [site](https://slock.ai)",
            senderName = "J2"
        )

        val targets = buildMessageCopyTargets(message)

        assertEquals("Check [docs](https://docs.slock.ai) and [site](https://slock.ai)", targets.markdown)
        assertEquals("https://docs.slock.ai", targets.link)
    }

    @Test
    fun `buildMessageCopyTargets falls back to first raw url`() {
        val message = Message(
            id = "msg-2",
            content = "Spec: https://example.com/spec, backup https://example.com/other"
        )

        val targets = buildMessageCopyTargets(message)

        assertEquals("https://example.com/spec", targets.link)
    }

    @Test
    fun `buildMessageCopyTargets returns null link when content has none`() {
        val message = Message(id = "msg-3", content = "No links here")

        val targets = buildMessageCopyTargets(message)

        assertNull(targets.link)
    }

    @Test
    fun `bucketMessageAttachments separates image and file attachments`() {
        val buckets = bucketMessageAttachments(
            listOf(
                Attachment(id = "1", name = "photo.png", url = "https://cdn.slock.ai/photo.png", type = "image/png"),
                Attachment(id = "2", name = "report.pdf", url = "https://cdn.slock.ai/report.pdf", type = "application/pdf"),
                Attachment(id = "3", name = "archive.zip", url = "https://cdn.slock.ai/archive.zip", type = "application/zip")
            )
        )

        assertEquals(listOf("photo.png"), buckets.imageAttachments.map { it.name })
        assertEquals(listOf("report.pdf", "archive.zip"), buckets.fileAttachments.map { it.name })
        assertTrue(buckets.fileAttachments.all { !isImageAttachment(it) })
    }
}
