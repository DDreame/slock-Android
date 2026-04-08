package com.slock.app.ui.message

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
fun MessageListScreen(
    channelId: String,
    state: MessageUiState,
    onSendMessage: (String) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToThread: (threadChannelId: String, parentMessage: Message) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (text.isNotBlank()) { onSendMessage(text); text = "" } },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.messages.isEmpty() -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No messages yet", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("Be the first to say something!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), reverseLayout = true) {
                    items(state.messages) { message ->
                        MessageBubble(
                            message = message,
                            onThreadClick = if (message.threadId != null) {
                                { onNavigateToThread(message.threadId, message) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, onThreadClick: (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(message.userId, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                message.createdAt.split("T").getOrNull(1)?.take(5) ?: message.createdAt,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (message.threadId != null) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onThreadClick ?: {}, modifier = Modifier.padding(0.dp)) {
                    Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Thread", fontSize = 10.sp)
                }
            }
        }
    }
}
