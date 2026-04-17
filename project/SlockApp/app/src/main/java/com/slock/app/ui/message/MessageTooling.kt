package com.slock.app.ui.message

import com.slock.app.data.model.Attachment
import com.slock.app.data.model.Message

private val markdownLinkPattern = Regex("""\[[^\]]+\]\((https?://[^)\s]+)\)""")
private val rawUrlPattern = Regex("""https?://[^\s]+""")

data class MessageCopyTargets(
    val markdown: String,
    val link: String?
)

data class MessageAttachmentBuckets(
    val imageAttachments: List<Attachment>,
    val fileAttachments: List<Attachment>
)

fun buildMessageCopyTargets(message: Message): MessageCopyTargets {
    val markdown = message.content.orEmpty()
    return MessageCopyTargets(
        markdown = markdown,
        link = extractFirstCopyableLink(markdown)
    )
}

fun extractFirstCopyableLink(content: String): String? {
    markdownLinkPattern.find(content)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }

    return rawUrlPattern.find(content)
        ?.value
        ?.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
        ?.takeIf { it.isNotEmpty() }
}

fun bucketMessageAttachments(attachments: List<Attachment>): MessageAttachmentBuckets {
    val imageAttachments = attachments.filter(::isImageAttachment)
    return MessageAttachmentBuckets(
        imageAttachments = imageAttachments,
        fileAttachments = attachments.filterNot(::isImageAttachment)
    )
}

internal fun isImageAttachment(attachment: Attachment): Boolean {
    val type = attachment.type.orEmpty()
    val url = attachment.url.orEmpty()
    val name = attachment.name.orEmpty()

    return type.startsWith("image/") ||
        url.endsWith(".png", ignoreCase = true) ||
        url.endsWith(".jpg", ignoreCase = true) ||
        url.endsWith(".jpeg", ignoreCase = true) ||
        url.endsWith(".gif", ignoreCase = true) ||
        url.endsWith(".webp", ignoreCase = true) ||
        name.endsWith(".png", ignoreCase = true) ||
        name.endsWith(".jpg", ignoreCase = true) ||
        name.endsWith(".jpeg", ignoreCase = true) ||
        name.endsWith(".gif", ignoreCase = true) ||
        name.endsWith(".webp", ignoreCase = true)
}
