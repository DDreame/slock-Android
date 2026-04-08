package com.slock.app.ui.agent

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
import com.slock.app.data.model.Agent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(
    state: AgentUiState,
    onCreateAgent: (name: String, description: String, prompt: String, model: String) -> Unit,
    onStartAgent: (agentId: String) -> Unit,
    onStopAgent: (agentId: String) -> Unit,
    onResetAgent: (agentId: String) -> Unit,
    onDeleteAgent: (agentId: String) -> Unit,
    onAgentClick: (agentId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Agents") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Create") } }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.agents.isEmpty() -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No agents yet", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("Create an AI agent", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showCreateDialog = true }) { Text("Create Agent") }
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.agents) { agent ->
                        AgentCard(
                            agent = agent,
                            onClick = onAgentClick,
                            onStart = onStartAgent,
                            onStop = onStopAgent,
                            onReset = onResetAgent,
                            onDelete = onDeleteAgent,
                            activity = state.agentActivities[agent.id]
                        )
                    }
                }
            }
        }
        
        if (showCreateDialog) CreateAgentDialog(onDismiss = { showCreateDialog = false }, onCreate = { name, desc, prompt, model ->
            onCreateAgent(name, desc, prompt, model)
            showCreateDialog = false
        })
    }
}

@Composable
fun AgentCard(
    agent: Agent,
    onClick: (String) -> Unit,
    onStart: (String) -> Unit,
    onStop: (String) -> Unit,
    onReset: (String) -> Unit,
    onDelete: (String) -> Unit,
    activity: String? = null
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick(agent.id) }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.medium, color = if (agent.status == "running") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                        Box(contentAlignment = Alignment.Center) { Icon(if (agent.status == "running") Icons.Default.PlayArrow else Icons.Default.SmartToy, null) }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(agent.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Text(agent.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, maxLines = 1)
                    }
                }
                Surface(shape = MaterialTheme.shapes.small, color = if (agent.status == "running") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                    Text(agent.status.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(agent.model, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
            }
            if (!activity.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(activity, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, maxLines = 2)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onDelete(agent.id) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Delete") }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { onReset(agent.id) }) { Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Reset") }
                Spacer(Modifier.width(4.dp))
                if (agent.status == "running") OutlinedButton(onClick = { onStop(agent.id) }) { Icon(Icons.Default.Stop, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Stop") }
                else Button(onClick = { onStart(agent.id) }) { Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Start") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAgentDialog(onDismiss: () -> Unit, onCreate: (name: String, desc: String, prompt: String, model: String) -> Unit) {
    val modelOptions = listOf("claude-sonnet-4-20250514", "claude-haiku-4-5-20251001", "claude-opus-4-20250514")
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(modelOptions[0]) }
    var modelExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create AI Agent") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("System Prompt") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                    modelOptions.forEach { model ->
                        DropdownMenuItem(text = { Text(model) }, onClick = { selectedModel = model; modelExpanded = false })
                    }
                }
            }
        }},
        confirmButton = { Button(onClick = { if (name.isNotBlank() && prompt.isNotBlank()) onCreate(name, desc, prompt, selectedModel) }, enabled = name.isNotBlank() && prompt.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
