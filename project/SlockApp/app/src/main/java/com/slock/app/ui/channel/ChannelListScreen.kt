package com.slock.app.ui.channel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Channel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    serverId: String,
    state: ChannelUiState,
    onCreateChannel: (name: String, type: String) -> Unit,
    onChannelClick: (channelId: String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAgents: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channels") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = onNavigateToAgents) { Icon(Icons.Default.SmartToy, contentDescription = "Agents") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (selectedTab == 0) showCreateDialog = true else onNavigateToAgents()
            }) { Icon(Icons.Default.Add, contentDescription = "Create") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Text") }, icon = { Icon(Icons.Default.Tag, null) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Agents") }, icon = { Icon(Icons.Default.SmartToy, null) })
            }

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> {
                    val channels = state.channels.filter { if (selectedTab == 0) it.type == "text" else it.type == "agent" }
                    if (channels.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(if (selectedTab == 0) Icons.Default.Tag else Icons.Default.SmartToy, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (selectedTab == 0) "No channels yet" else "No agent channels", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn { items(channels) { channel ->
                            ListItem(
                                headlineContent = { Text("# ${channel.name}", fontWeight = FontWeight.Medium) },
                                leadingContent = { Icon(if (channel.type == "agent") Icons.Default.SmartToy else Icons.Default.Tag, null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable { onChannelClick(channel.id) }
                            )
                            Divider()
                        }}
                    }
                }
            }
        }

        if (showCreateDialog) {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Channel") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Channel Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = { Button(onClick = { if (name.isNotBlank()) { onCreateChannel(name, if (selectedTab == 0) "text" else "agent"); showCreateDialog = false } }, enabled = name.isNotBlank()) { Text("Create") } },
                dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
            )
        }
    }
}
