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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Agent
import com.slock.app.data.model.DEFAULT_AGENT_REASONING_EFFORT_ID
import com.slock.app.data.model.DEFAULT_AGENT_REASONING_OPTIONS
import com.slock.app.data.model.DEFAULT_AGENT_MODEL_OPTIONS
import com.slock.app.data.model.DEFAULT_AGENT_RUNTIME_ID
import com.slock.app.data.model.DEFAULT_AGENT_RUNTIME_OPTIONS
import com.slock.app.data.model.supportsAgentReasoningEffort
import com.slock.app.ui.theme.*
import com.slock.app.util.LogCollector
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(
    state: AgentUiState,
    onCreateAgent: (
        name: String,
        description: String,
        prompt: String,
        model: String,
        runtime: String,
        reasoningEffort: String?,
        envVars: Map<String, String>?
    ) -> Unit,
    onStartAgent: (agentId: String) -> Unit,
    onStopAgent: (agentId: String) -> Unit,
    onResetAgent: (agentId: String) -> Unit,
    onDeleteAgent: (agentId: String) -> Unit,
    onUpdateAgent: (
        agentId: String,
        name: String?,
        description: String?,
        prompt: String?,
        runtime: String?,
        reasoningEffort: String?,
        envVars: Map<String, String>?
    ) -> Unit,
    onDmAgent: (agentId: String) -> Unit,
    onAgentClick: (agentId: String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToMachines: () -> Unit = {},
    onRetry: () -> Unit = {},
    showHeader: Boolean = true
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettingsAgent by remember { mutableStateOf<Agent?>(null) }
    var confirmStopAgent by remember { mutableStateOf<Agent?>(null) }
    var confirmDeleteAgent by remember { mutableStateOf<Agent?>(null) }
    var confirmStopAll by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            onResumeAll = {
                val count = state.agents.count { it.status != "active" }
                state.agents.filter { it.status != "active" }.forEach { onStartAgent(it.id.orEmpty()) }
                if (count > 0) Toast.makeText(context, "正在启动 $count 个 Agent", Toast.LENGTH_SHORT).show()
            },
            onStopAll = { confirmStopAll = true },
            onMachines = onNavigateToMachines
        )

        // Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> {
                    NeoSkeletonCardList()
                }
                state.error != null && state.agents.isEmpty() -> {
                    val context = LocalContext.current
                    NeoErrorState(
                        message = "Agent 加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = onRetry,
                        onSendLog = { LogCollector.shareReport(context, state.error) }
                    )
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
                                    activityInfo = state.agentActivities[agent.id.orEmpty()],
                                    onDm = { onDmAgent(agent.id.orEmpty()) },
                                    onToggle = { confirmStopAgent = agent },
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
                                    activityInfo = state.agentActivities[agent.id.orEmpty()],
                                    onDm = { onDmAgent(agent.id.orEmpty()) },
                                    onToggle = {
                                        onStartAgent(agent.id.orEmpty())
                                        Toast.makeText(context, "${agent.name.orEmpty()} 已启动", Toast.LENGTH_SHORT).show()
                                    },
                                    onConfig = { showSettingsAgent = agent }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Agent Bottom Sheet
    if (showCreateDialog) {
        CreateAgentSheet(
            availableModels = state.availableModels,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, prompt, model, runtime, reasoningEffort, envVars ->
                onCreateAgent(name, desc, prompt, model, runtime, reasoningEffort, envVars)
                showCreateDialog = false
            }
        )
    }

    // Agent Settings Bottom Sheet
    showSettingsAgent?.let { agent ->
        AgentSettingsSheet(
            agent = agent,
            onDismiss = { showSettingsAgent = null },
            onSave = { runtime, reasoningEffort, envVars ->
                onUpdateAgent(
                    agent.id.orEmpty(),
                    null,
                    null,
                    null,
                    runtime,
                    reasoningEffort,
                    envVars
                )
                showSettingsAgent = null
                Toast.makeText(context, "${agent.name.orEmpty()} 配置已保存", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                showSettingsAgent = null
                confirmDeleteAgent = agent
            }
        )
    }

    // Stop single agent confirmation
    confirmStopAgent?.let { agent ->
        NeoConfirmDialog(
            title = "Stop Agent",
            message = "确定要停止 ${agent.name.orEmpty()} 吗？停止后 Agent 将不再处理消息。",
            confirmText = "STOP",
            confirmColor = Pink,
            onConfirm = {
                onStopAgent(agent.id.orEmpty())
                confirmStopAgent = null
                Toast.makeText(context, "${agent.name.orEmpty()} 已停止", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { confirmStopAgent = null }
        )
    }

    // Delete agent confirmation
    confirmDeleteAgent?.let { agent ->
        NeoConfirmDialog(
            title = "Delete Agent",
            message = "确定要删除 ${agent.name.orEmpty()} 吗？此操作不可撤销，所有相关数据将被永久删除。",
            confirmText = "DELETE",
            confirmColor = Pink,
            onConfirm = {
                onDeleteAgent(agent.id.orEmpty())
                confirmDeleteAgent = null
                Toast.makeText(context, "${agent.name.orEmpty()} 已删除", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { confirmDeleteAgent = null }
        )
    }

    // Stop all agents confirmation
    if (confirmStopAll) {
        val activeCount = state.agents.count { it.status == "active" }
        NeoConfirmDialog(
            title = "Stop All Agents",
            message = "确定要停止所有 $activeCount 个活跃的 Agent 吗？停止后它们将不再处理消息。",
            confirmText = "STOP ALL",
            confirmColor = Pink,
            onConfirm = {
                state.agents.filter { it.status == "active" }.forEach { onStopAgent(it.id.orEmpty()) }
                confirmStopAll = false
                Toast.makeText(context, "已停止 $activeCount 个 Agent", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { confirmStopAll = false }
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
                NeoPressableBox(onClick = onBack) {
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
            NeoPressableBox(onClick = onCreateClick) {
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
    onMachines: () -> Unit = {}
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
        QuickActionButton(icon = "\uD83D\uDCBB", label = "Machines", color = Yellow, onClick = onMachines)
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
    activityInfo: AgentActivityInfo?,
    onDm: () -> Unit,
    onToggle: () -> Unit,
    onConfig: () -> Unit
) {
    val displayState = resolveDisplayState(agent.status, activityInfo?.activity)
    val isRunning = displayState.isActive
    val isThinking = displayState == AgentDisplayState.THINKING
    val avatarColor = when {
        !isRunning -> Color(0xFFEEEEEE)
        agent.name.orEmpty().startsWith("Z", ignoreCase = true) -> Cyan
        agent.name.orEmpty().startsWith("X", ignoreCase = true) -> Orange
        agent.name.orEmpty().startsWith("J", ignoreCase = true) -> Lavender
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
                        text = agent.name.orEmpty().take(1).uppercase(),
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
                        text = agent.name.orEmpty(),
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
                            text = getModelShortName(agent.model.orEmpty()),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceMono,
                            color = Black
                        )
                    }
                }

                Text(
                    text = agent.description ?: "",
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
                            .background(displayState.dotColor)
                            .border(1.dp, Black, CircleShape)
                    )
                    Text(
                        text = displayState.statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }

                if (isRunning && !activityInfo?.message.isNullOrBlank()) {
                    Text(
                        text = activityInfo!!.message!!,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
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
                label = displayState.toggleLabel,
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

// Agent Settings Bottom Sheet
data class AgentEnvVarDraft(
    val key: String = "",
    val value: String = ""
)

internal fun normalizeAgentEnvVars(drafts: List<AgentEnvVarDraft>): Map<String, String> {
    val normalized = linkedMapOf<String, String>()
    drafts.forEach { draft ->
        val trimmedKey = draft.key.trim()
        if (trimmedKey.isNotEmpty()) {
            normalized[trimmedKey] = draft.value
        }
    }
    return normalized
}

internal fun seedAgentEnvVarDrafts(envVars: Map<String, String>?): List<AgentEnvVarDraft> {
    return envVars.orEmpty()
        .toList()
        .sortedBy { it.first }
        .map { (key, value) -> AgentEnvVarDraft(key = key, value = value) }
}

internal fun resolveSelectedAgentRuntime(runtime: String?): String {
    val normalizedRuntime = runtime?.trim().orEmpty()
    return DEFAULT_AGENT_RUNTIME_OPTIONS.firstOrNull { it.id == normalizedRuntime }?.id
        ?: DEFAULT_AGENT_RUNTIME_ID
}

internal fun resolveSelectedReasoningEffort(runtime: String?, reasoningEffort: String?): String {
    val normalizedReasoningEffort = reasoningEffort?.trim().orEmpty()
    return if (
        supportsAgentReasoningEffort(runtime) &&
        DEFAULT_AGENT_REASONING_OPTIONS.any { it.id == normalizedReasoningEffort }
    ) {
        normalizedReasoningEffort
    } else {
        DEFAULT_AGENT_REASONING_EFFORT_ID
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentSettingsSheet(
    agent: Agent,
    onDismiss: () -> Unit,
    onSave: (runtime: String, reasoningEffort: String?, envVars: Map<String, String>?) -> Unit,
    onDelete: () -> Unit
) {
    var selectedRuntime by remember(agent.id, agent.runtime) {
        mutableStateOf(resolveSelectedAgentRuntime(agent.runtime))
    }
    var selectedReasoningEffort by remember(agent.id, agent.runtime, agent.reasoningEffort) {
        mutableStateOf(resolveSelectedReasoningEffort(agent.runtime, agent.reasoningEffort))
    }
    var envVarDrafts by remember(agent.id, agent.envVars) {
        mutableStateOf(seedAgentEnvVarDrafts(agent.envVars))
    }
    val runtimeSupportsReasoning = supportsAgentReasoningEffort(selectedRuntime)

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
                .verticalScroll(rememberScrollState())
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

            SettingsRow(label = "MODEL") {
                Box(
                    modifier = Modifier
                        .background(White)
                        .border(2.dp, Black, RectangleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = getModelShortName(agent.model.orEmpty()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
            }

            SettingsRow(label = "ROLE") {
                Text(
                    text = agent.description ?: "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            NeoLabel("RUNTIME")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DEFAULT_AGENT_RUNTIME_OPTIONS.forEach { runtimeOption ->
                    ConfigOptionChip(
                        label = runtimeOption.displayName,
                        isSelected = selectedRuntime == runtimeOption.id,
                        onClick = { selectedRuntime = runtimeOption.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (runtimeSupportsReasoning) {
                NeoLabel("REASONING EFFORT")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DEFAULT_AGENT_REASONING_OPTIONS.forEach { reasoningOption ->
                        ConfigOptionChip(
                            label = reasoningOption.label,
                            isSelected = selectedReasoningEffort == reasoningOption.id,
                            onClick = { selectedReasoningEffort = reasoningOption.id }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            EnvVarEditorSection(
                drafts = envVarDrafts,
                onAddDraft = { envVarDrafts = envVarDrafts + AgentEnvVarDraft() },
                onUpdateDraft = { index, key, value ->
                    envVarDrafts = envVarDrafts.mapIndexed { currentIndex, draft ->
                        if (currentIndex == index) {
                            AgentEnvVarDraft(key = key, value = value)
                        } else {
                            draft
                        }
                    }
                },
                onRemoveDraft = { index ->
                    envVarDrafts = envVarDrafts.filterIndexed { currentIndex, _ -> currentIndex != index }
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            NeoButton(
                text = "SAVE CONFIG",
                onClick = {
                    val normalizedEnvVars = normalizeAgentEnvVars(envVarDrafts)
                    onSave(
                        selectedRuntime,
                        if (runtimeSupportsReasoning) selectedReasoningEffort else null,
                        normalizedEnvVars.takeUnless { it.isEmpty() }
                    )
                },
                containerColor = Yellow,
                contentColor = Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            NeoButton(
                text = "DELETE AGENT",
                onClick = onDelete,
                containerColor = Pink,
                contentColor = Black
            )
        }
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAgentSheet(
    availableModels: List<String>,
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        desc: String,
        prompt: String,
        model: String,
        runtime: String,
        reasoningEffort: String?,
        envVars: Map<String, String>?
    ) -> Unit
) {
    val modelOptions = remember(availableModels) {
        availableModels.ifEmpty { DEFAULT_AGENT_MODEL_OPTIONS }
    }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(modelOptions.firstOrNull().orEmpty()) }
    var useCustomModel by remember { mutableStateOf(false) }
    var customModelId by remember { mutableStateOf("") }
    var selectedRuntime by remember { mutableStateOf(DEFAULT_AGENT_RUNTIME_ID) }
    var selectedReasoningEffort by remember { mutableStateOf(DEFAULT_AGENT_REASONING_EFFORT_ID) }
    var envVarDrafts by remember { mutableStateOf(emptyList<AgentEnvVarDraft>()) }

    LaunchedEffect(modelOptions) {
        if (selectedModel.isBlank() || selectedModel !in modelOptions) {
            selectedModel = modelOptions.firstOrNull().orEmpty()
        }
    }

    val resolvedModel = if (useCustomModel) customModelId.trim() else selectedModel.trim()
    val runtimeSupportsReasoning = supportsAgentReasoningEffort(selectedRuntime)

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
                .verticalScroll(rememberScrollState())
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

            NeoLabel("RUNTIME")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DEFAULT_AGENT_RUNTIME_OPTIONS.forEach { runtimeOption ->
                    ConfigOptionChip(
                        label = runtimeOption.displayName,
                        isSelected = selectedRuntime == runtimeOption.id,
                        onClick = { selectedRuntime = runtimeOption.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            NeoLabel("MODEL")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modelOptions.forEach { modelId ->
                    ConfigOptionChip(
                        label = getModelShortName(modelId),
                        isSelected = !useCustomModel && selectedModel == modelId,
                        onClick = {
                            selectedModel = modelId
                            useCustomModel = false
                        }
                    )
                }

                ConfigOptionChip(
                    label = "Custom",
                    isSelected = useCustomModel,
                    onClick = {
                        useCustomModel = true
                        if (customModelId.isBlank()) {
                            customModelId = selectedModel
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (useCustomModel) {
                NeoTextField(
                    value = customModelId,
                    onValueChange = { customModelId = it },
                    placeholder = "Custom model ID",
                    focusHighlight = Orange
                )
            } else if (selectedModel.isNotBlank()) {
                Text(
                    text = selectedModel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (runtimeSupportsReasoning) {
                NeoLabel("REASONING EFFORT")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DEFAULT_AGENT_REASONING_OPTIONS.forEach { reasoningOption ->
                        ConfigOptionChip(
                            label = reasoningOption.label,
                            isSelected = selectedReasoningEffort == reasoningOption.id,
                            onClick = { selectedReasoningEffort = reasoningOption.id }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            EnvVarEditorSection(
                drafts = envVarDrafts,
                onAddDraft = { envVarDrafts = envVarDrafts + AgentEnvVarDraft() },
                onUpdateDraft = { index, key, value ->
                    envVarDrafts = envVarDrafts.mapIndexed { currentIndex, draft ->
                        if (currentIndex == index) {
                            AgentEnvVarDraft(key = key, value = value)
                        } else {
                            draft
                        }
                    }
                },
                onRemoveDraft = { index ->
                    envVarDrafts = envVarDrafts.filterIndexed { currentIndex, _ -> currentIndex != index }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            NeoButton(
                text = "CREATE AGENT",
                onClick = {
                    if (name.isNotBlank() && resolvedModel.isNotBlank()) {
                        val normalizedEnvVars = normalizeAgentEnvVars(envVarDrafts)
                        onCreate(
                            name,
                            desc,
                            prompt,
                            resolvedModel,
                            selectedRuntime,
                            if (runtimeSupportsReasoning) selectedReasoningEffort else null,
                            normalizedEnvVars.takeUnless { it.isEmpty() }
                        )
                    }
                },
                enabled = name.isNotBlank() && resolvedModel.isNotBlank()
            )
        }
    }
}

@Composable
private fun ConfigOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(if (isSelected) Yellow else White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
    }
}

@Composable
private fun EnvVarEditorSection(
    drafts: List<AgentEnvVarDraft>,
    onAddDraft: () -> Unit,
    onUpdateDraft: (index: Int, key: String, value: String) -> Unit,
    onRemoveDraft: (index: Int) -> Unit
) {
    NeoLabel("ENV VARS")

    if (drafts.isEmpty()) {
        Text(
            text = "No environment variables configured.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }

    drafts.forEachIndexed { index, draft ->
        AgentEnvVarEditorRow(
            draft = draft,
            onKeyChange = { updatedKey -> onUpdateDraft(index, updatedKey, draft.value) },
            onValueChange = { updatedValue -> onUpdateDraft(index, draft.key, updatedValue) },
            onRemove = { onRemoveDraft(index) }
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    NeoButton(
        text = "ADD ENV VAR",
        onClick = onAddDraft,
        containerColor = White,
        contentColor = Black
    )
}

@Composable
private fun AgentEnvVarEditorRow(
    draft: AgentEnvVarDraft,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            NeoTextField(
                value = draft.key,
                onValueChange = onKeyChange,
                placeholder = "KEY",
                focusHighlight = Orange
            )

            Spacer(modifier = Modifier.height(8.dp))

            NeoTextField(
                value = draft.value,
                onValueChange = onValueChange,
                placeholder = "value",
                focusHighlight = Orange
            )
        }

        NeoPressableBox(onClick = onRemove) {
            Text(text = "×", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Black)
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
