package com.slock.app.ui.agent

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Agent
import com.slock.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(
    state: AgentUiState,
    onCreateAgent: (name: String, description: String, prompt: String, model: String) -> Unit,
    onStartAgent: (agentId: String) -> Unit,
    onStopAgent: (agentId: String) -> Unit,
    onResetAgent: (agentId: String) -> Unit,
    onDeleteAgent: (agentId: String) -> Unit,
    onUpdateAgent: (agentId: String, name: String?, description: String?, prompt: String?) -> Unit,
    onDmAgent: (agentId: String) -> Unit,
    onAgentClick: (agentId: String) -> Unit,
    onNavigateBack: () -> Unit,
    showHeader: Boolean = true
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettingsAgent by remember { mutableStateOf<Agent?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Orange header
        if (showHeader) {
            AgentHeader(
                onBack = onNavigateBack,
                onCreateClick = { showCreateDialog = true }
            )
        }

        // Quick actions bar
        QuickActionsBar(
            onResumeAll = { state.agents.filter { it.status != "active" }.forEach { onStartAgent(it.id) } },
            onStopAll = { state.agents.filter { it.status == "active" }.forEach { onStopAgent(it.id) } },
            onRefresh = { /* TODO: Refresh status */ }
        )

        // Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Black
                    )
                }
                state.error != null && state.agents.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "\u26A0", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Failed to load agents",
                            style = MaterialTheme.typography.titleMedium,
                            color = Black
                        )
                        Text(
                            state.error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pink,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                state.agents.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "\uD83E\uDD16", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No agents yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Black
                        )
                        Text(
                            "Create an AI agent to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        NeoButton(
                            text = "CREATE AGENT",
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                    }
                }
                else -> {
                    val activeAgents = state.agents.filter { it.status == "active" }
                    val inactiveAgents = state.agents.filter { it.status != "active" }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (activeAgents.isNotEmpty()) {
                            item {
                                SectionLabel("Active (${activeAgents.size})")
                            }
                            items(activeAgents) { agent ->
                                NeoAgentCard(
                                    agent = agent,
                                    activity = state.agentActivities[agent.id],
                                    onDm = { onDmAgent(agent.id) },
                                    onToggle = { onStopAgent(agent.id) },
                                    onConfig = { showSettingsAgent = agent }
                                )
                            }
                        }
                        if (inactiveAgents.isNotEmpty()) {
                            item {
                                SectionLabel("Inactive (${inactiveAgents.size})")
                            }
                            items(inactiveAgents) { agent ->
                                NeoAgentCard(
                                    agent = agent,
                                    activity = state.agentActivities[agent.id],
                                    onDm = { onDmAgent(agent.id) },
                                    onToggle = { onStartAgent(agent.id) },
                                    onConfig = { showSettingsAgent = agent }
                                )
                            }
                        }

                        // Machines section — hidden until backend API is available
                        // item {
                        //     SectionLabel("Machines")
                        //     MachineCard()
                        //     AddMachineCard()
                        // }
                    }
                }
            }
        }
    }

    // Create Agent Bottom Sheet
    if (showCreateDialog) {
        CreateAgentSheet(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, prompt, model ->
                onCreateAgent(name, desc, prompt, model)
                showCreateDialog = false
            }
        )
    }

    // Agent Settings Bottom Sheet
    showSettingsAgent?.let { agent ->
        AgentSettingsSheet(
            agent = agent,
            onDismiss = { showSettingsAgent = null },
            onSave = { _, _, _, _ ->
                // TODO: effort, wakeOnMessage, hibernateIdle not yet supported by API
                onUpdateAgent(agent.id, null, null, null)
                showSettingsAgent = null
            },
            onDelete = {
                onDeleteAgent(agent.id)
                showSettingsAgent = null
            }
        )
    }
}

// Orange header
@Composable
private fun AgentHeader(onBack: () -> Unit, onCreateClick: () -> Unit) {
    Surface(color = Orange) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .neoShadowSmall()
                        .background(White)
                        .border(2.dp, Black, RectangleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "\u2190", fontSize = 18.sp, color = Black)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Agents",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Black
                )
            }

            // Create button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .neoShadowSmall()
                    .background(White)
                    .border(2.dp, Black, RectangleShape)
                    .clickable(onClick = onCreateClick),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Black)
            }
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

// Quick actions horizontal scroll
@Composable
private fun QuickActionsBar(
    onResumeAll: () -> Unit,
    onStopAll: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(icon = "\u25B6", label = "Resume All", color = Lime, onClick = onResumeAll)
        QuickActionButton(icon = "\u25A0", label = "Stop All", color = Pink, onClick = onStopAll)
        QuickActionButton(icon = "\uD83D\uDD04", label = "Refresh Status", color = White, onClick = onRefresh)
    }
}

@Composable
private fun QuickActionButton(icon: String, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .neoShadowSmall()
            .background(color)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = icon, fontSize = 13.sp)
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
    }
}

// Section label
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold
        ),
        color = TextMuted,
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

// Agent card with status ring
@Composable
private fun NeoAgentCard(
    agent: Agent,
    activity: String?,
    onDm: () -> Unit,
    onToggle: () -> Unit,
    onConfig: () -> Unit
) {
    val isRunning = agent.status == "active"
    val isThinking = activity?.contains("Thinking") == true || activity == "thinking"
    val avatarColor = when {
        !isRunning -> Color(0xFFEEEEEE)
        agent.name.startsWith("Z", ignoreCase = true) -> Cyan
        agent.name.startsWith("X", ignoreCase = true) -> Orange
        agent.name.startsWith("J", ignoreCase = true) -> Lavender
        else -> Cyan
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .neoShadow()
            .background(White)
            .border(3.dp, Black, RectangleShape)
    ) {
        // Card header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with status ring
            Box(contentAlignment = Alignment.Center) {
                // Status ring
                StatusRing(
                    isActive = isRunning,
                    isThinking = isThinking,
                    modifier = Modifier.size(54.dp)
                )
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(avatarColor)
                        .border(2.dp, Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = agent.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Black
                    )
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = agent.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Black
                    )
                    // Model tag
                    Box(
                        modifier = Modifier
                            .background(Cream)
                            .border(1.dp, Black, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = getModelShortName(agent.model),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceMono,
                            color = Black
                        )
                    }
                }

                Text(
                    text = agent.description,
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Status label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isThinking -> Yellow
                                    isRunning -> Lime
                                    else -> Color(0xFFCCCCCC)
                                }
                            )
                            .border(1.dp, Black, CircleShape)
                    )
                    Text(
                        text = when {
                            isThinking -> "Thinking..."
                            isRunning -> "Working..."
                            else -> "Hibernating"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
            }
        }

        // Action buttons row
        Divider(thickness = 2.dp, color = Black)
        Row(modifier = Modifier.fillMaxWidth()) {
            AgentActionButton(
                icon = "\uD83D\uDCAC",
                label = "DM",
                onClick = onDm,
                modifier = Modifier.weight(1f)
            )
            AgentActionButton(
                icon = if (isRunning) "\u25A0" else "\u25B6",
                label = if (isRunning) "Stop" else "Wake",
                onClick = onToggle,
                modifier = Modifier.weight(1f),
                textColor = if (!isRunning) Color(0xFF22C55E) else Black
            )
            AgentActionButton(
                icon = "\u2699",
                label = "Config",
                onClick = onConfig,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Status ring with animation
@Composable
private fun StatusRing(
    isActive: Boolean,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    if (isThinking) {
        val infiniteTransition = rememberInfiniteTransition(label = "spin")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        Box(
            modifier = modifier
                .rotate(rotation)
                .drawBehind {
                    drawRect(
                        color = Color(0xFFFFD700),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    )
                }
        )
    } else if (isActive) {
        Box(
            modifier = modifier
                .border(3.dp, Lime, RectangleShape)
        )
    } else {
        Box(
            modifier = modifier
                .border(3.dp, Color(0xFFCCCCCC), RectangleShape)
        )
    }
}

@Composable
private fun AgentActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Black
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = textColor
        )
    }
}

// Machine card
@Composable
private fun MachineCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Cyan)
                    .border(2.dp, Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDCBB", fontSize = 16.sp)
            }
            Column {
                Text(
                    text = "Local Machine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Black
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Lime)
                            .border(1.dp, Black, CircleShape)
                    )
                    Text(
                        text = "Connected",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }
        // Command line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(Color(0xFF1A1A1A))
                .border(2.dp, Black, RectangleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$ slock connect --token=sk-***",
                fontFamily = SpaceMono,
                fontSize = 11.sp,
                color = Color(0xFFEEEEEE)
            )
        }
    }
}

// Add machine card (dashed border)
@Composable
private fun AddMachineCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .drawBehind {
                drawRect(
                    color = Color.Black,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                )
            }
            .clickable { /* TODO: Add machine */ }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Black)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add Machine",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Black
        )
        Text(
            text = "Run agents on your own computer",
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// Agent Settings Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentSettingsSheet(
    agent: Agent,
    onDismiss: () -> Unit,
    onSave: (model: String, effort: String, wakeOnMessage: Boolean, hibernateIdle: Boolean) -> Unit = { _, _, _, _ -> },
    onDelete: () -> Unit
) {
    val modelOptions = listOf("Opus", "Sonnet", "Haiku")
    val modelApiNames = listOf("claude-opus-4-20250514", "claude-sonnet-4-20250514", "claude-haiku-4-5-20251001")
    var selectedModel by remember { mutableStateOf(getModelShortName(agent.model)) }
    var selectedEffort by remember { mutableStateOf("High") }
    var wakeOnMessage by remember { mutableStateOf(true) }
    var hibernateIdle by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RectangleShape,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Black)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Agent Settings: ${agent.name}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Model
            SettingsRow(label = "MODEL") {
                Box(
                    modifier = Modifier
                        .background(White)
                        .border(2.dp, Black, RectangleShape)
                        .clickable {
                            val currentIndex = modelOptions.indexOf(selectedModel)
                            selectedModel = modelOptions[(currentIndex + 1) % modelOptions.size]
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedModel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
            }

            // Reasoning effort
            SettingsRow(label = "REASONING") {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Med", "High", "Extra").forEach { effort ->
                        Box(
                            modifier = Modifier
                                .background(if (effort == selectedEffort) Yellow else White)
                                .border(1.5.dp, Black, RectangleShape)
                                .clickable { selectedEffort = effort }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = effort.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Black
                            )
                        }
                    }
                }
            }

            // Wake on Message toggle
            SettingsRow(label = "WAKE ON MESSAGE") {
                NeoToggle(checked = wakeOnMessage, onToggle = { wakeOnMessage = !wakeOnMessage })
            }

            // Hibernate Idle toggle
            SettingsRow(label = "HIBERNATE IDLE") {
                NeoToggle(checked = hibernateIdle, onToggle = { hibernateIdle = !hibernateIdle })
            }

            // Role
            SettingsRow(label = "ROLE") {
                Text(text = agent.description, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            NeoButton(text = "SAVE CHANGES", onClick = {
                val modelApiName = modelApiNames.getOrElse(modelOptions.indexOf(selectedModel)) { agent.model }
                onSave(modelApiName, selectedEffort, wakeOnMessage, hibernateIdle)
                onDismiss()
            })

            Spacer(modifier = Modifier.height(8.dp))

            // Delete button
            NeoButton(
                text = "DELETE AGENT",
                onClick = onDelete,
                containerColor = Pink,
                contentColor = Black
            )
        }
    }
}

// Settings row
@Composable
private fun SettingsRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
        content()
    }
    Divider(color = Color(0xFFEEEEEE))
}

// Neo-Brutalism toggle switch
@Composable
private fun NeoToggle(checked: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .background(if (checked) Lime else Color(0xFFDDDDDD))
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onToggle)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .offset(x = if (checked) 23.dp else 3.dp, y = 2.dp)
                .background(White)
                .border(1.5.dp, Black, RectangleShape)
        )
    }
}

// Create Agent Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAgentSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, prompt: String, model: String) -> Unit
) {
    val modelOptions = listOf("claude-sonnet-4-20250514", "claude-haiku-4-5-20251001", "claude-opus-4-20250514")
    val modelLabels = listOf("Sonnet", "Haiku", "Opus")
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var selectedModelIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RectangleShape,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Black)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Create Agent",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            NeoLabel("NAME")
            NeoTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Agent name",
                focusHighlight = Orange
            )

            Spacer(modifier = Modifier.height(14.dp))

            NeoLabel("ROLE DESCRIPTION")
            NeoTextField(
                value = desc,
                onValueChange = { desc = it },
                placeholder = "Describe what this agent does...",
                focusHighlight = Orange
            )

            Spacer(modifier = Modifier.height(14.dp))

            NeoLabel("SYSTEM PROMPT")
            NeoTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = "Instructions for this agent...",
                focusHighlight = Orange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Model selector
            SettingsRow(label = "MODEL") {
                Box(
                    modifier = Modifier
                        .background(White)
                        .border(2.dp, Black, RectangleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable {
                            selectedModelIndex = (selectedModelIndex + 1) % modelOptions.size
                        }
                ) {
                    Text(
                        text = modelLabels[selectedModelIndex],
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NeoButton(
                text = "CREATE AGENT",
                onClick = {
                    if (name.isNotBlank()) onCreate(name, desc, prompt, modelOptions[selectedModelIndex])
                },
                enabled = name.isNotBlank()
            )
        }
    }
}

private fun getModelShortName(model: String): String = when {
    model.contains("opus", ignoreCase = true) -> "Opus"
    model.contains("sonnet", ignoreCase = true) -> "Sonnet"
    model.contains("haiku", ignoreCase = true) -> "Haiku"
    model.contains("gpt", ignoreCase = true) -> "GPT"
    model.contains("gemini", ignoreCase = true) -> "Gemini"
    else -> model.take(10)
}
