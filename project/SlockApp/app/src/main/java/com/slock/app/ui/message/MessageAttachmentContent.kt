package com.slock.app.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.slock.app.data.model.Attachment
import com.slock.app.ui.theme.Black
import com.slock.app.ui.theme.Cream
import com.slock.app.ui.theme.TextMuted
import com.slock.app.ui.theme.TextSecondary
import com.slock.app.ui.theme.White

@Composable
fun MessageAttachmentContent(
    attachments: List<Attachment>,
    modifier: Modifier = Modifier,
    onImageClick: ((String) -> Unit)? = null
) {
    val buckets = remember(attachments) { bucketMessageAttachments(attachments) }
    if (buckets.imageAttachments.isEmpty() && buckets.fileAttachments.isEmpty()) return

    val uriHandler = LocalUriHandler.current
    val resolvedImageClick: (String) -> Unit = remember(onImageClick, uriHandler) {
        onImageClick ?: { uri -> uriHandler.openUri(uri) }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        buckets.imageAttachments.forEach { attachment ->
            val url = attachment.url.orEmpty()
            if (url.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = attachment.name,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .clip(RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .clickable { resolvedImageClick(url) }
                )
            }
        }

        buckets.fileAttachments.forEach { attachment ->
            FileAttachmentCard(
                attachment = attachment,
                onOpen = { url -> uriHandler.openUri(url) }
            )
        }
    }
}

@Composable
private fun FileAttachmentCard(
    attachment: Attachment,
    onOpen: (String) -> Unit
) {
    val url = attachment.url.orEmpty()
    val isOpenable = url.isNotBlank()
    val typeLabel = attachment.type.orEmpty().ifBlank {
        attachment.name
            ?.substringAfterLast('.', missingDelimiterValue = "FILE")
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }
            ?: "FILE"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream)
            .border(1.5.dp, Black, RectangleShape)
            .clickable(enabled = isOpenable) { onOpen(url) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(White)
                .border(1.5.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "FILE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.name.orEmpty().ifBlank { "Unnamed attachment" },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = if (isOpenable) "OPEN" else "UNAVAILABLE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isOpenable) Black else TextSecondary
        )
    }
}
