package com.slock.app.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadReplyScreen(
    state: ThreadReplyUiState,
    onSendReply: (String) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thread Reply") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Reply in thread...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { if (text.isNotBlank()) { onSendReply(text); text = "" } },
                        enabled = text.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Parent message
            state.parentMessage?.let { parent ->
                ParentMessageCard(message = parent)
                Divider()
            }

            // Replies
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.replies.isEmpty() -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No replies yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Be the first to reply!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), reverseLayout = true) {
                        items(state.replies) { reply ->
                            ReplyBubble(reply)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentMessageCard(message: Message) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Reply, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("Original Message", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(message.userId, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.widthIn(max = 280.dp)) {
                Text(message.content, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            Text(message.createdAt.split("T").getOrNull(1)?.take(5) ?: message.createdAt, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReplyBubble(message: Message) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(message.userId, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.widthIn(max = 280.dp)) {
            Text(message.content, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Text(message.createdAt.split("T").getOrNull(1)?.take(5) ?: message.createdAt, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
