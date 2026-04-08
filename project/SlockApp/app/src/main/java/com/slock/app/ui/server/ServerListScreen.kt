package com.slock.app.ui.server

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import com.slock.app.data.model.Server

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    state: ServerUiState,
    onCreateServer: (name: String, slug: String) -> Unit,
    onDeleteServer: (serverId: String) -> Unit,
    onServerClick: (serverId: String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToAgents: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Servers") },
                actions = {
                    IconButton(onClick = onNavigateToAgents) { Icon(Icons.Default.SmartToy, contentDescription = "Agents") }
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "Logout") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Create") }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
                state.servers.isEmpty() -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No servers yet", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("Create your first server", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showCreateDialog = true }) { Text("Create Server") }
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.servers) { server ->
                        ServerCard(server, onClick = { onServerClick(server.id) }, onDelete = { onDeleteServer(server.id) })
                    }
                }
            }
        }
        
        if (showCreateDialog) CreateServerDialog(onDismiss = { showCreateDialog = false }, onCreate = { name, slug ->
            onCreateServer(name, slug)
            showCreateDialog = false
        })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerCard(server: Server, onClick: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = { if (server.role == "owner") showMenu = true }
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) { Text(server.name.take(2).uppercase(), fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(server.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Text("@${server.slug}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            Box {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(server.role.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete Server", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateServerDialog(onDismiss: () -> Unit, onCreate: (name: String, slug: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    
    LaunchedEffect(name) { slug = name.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Server Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = slug, onValueChange = { slug = it }, label = { Text("URL Slug") }, prefix = { Text("slock.ai/s/") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank() && slug.isNotBlank()) onCreate(name, slug) }, enabled = name.isNotBlank() && slug.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
