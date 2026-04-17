package com.slock.app.ui.agent

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Agent
import com.slock.app.ui.theme.NeoPressableBox
import com.slock.app.ui.theme.NeoConfirmDialog
import com.slock.app.ui.theme.NeoSkeletonCardList
import com.slock.app.ui.theme.SpaceGrotesk
import com.slock.app.ui.theme.SpaceMono

private val NeoOrange = Color(0xFFFF9F43)
private val NeoCream = Color(0xFFFFF8E7)
private val NeoCyan = Color(0xFF00CFFF)
private val NeoLime = Color(0xFFA6FF00)
private val NeoPink = Color(0xFFFF6B9D)
private val NeoGold = Color(0xFFFFD700)

internal sealed interface AgentDetailContentState {
    data object Loading : AgentDetailContentState
    data class Error(val message: String) : AgentDetailContentState
    data class Content(val agent: Agent) : AgentDetailContentState
}

internal fun resolveAgentDetailContentState(state: AgentDetailUiState): AgentDetailContentState = when {
    state.agent != null -> AgentDetailContentState.Content(state.agent)
    state.error != null -> AgentDetailContentState.Error(state.error)
    else -> AgentDetailContentState.Loading
}

@Composable
fun AgentDetailScreen(
    state: AgentDetailUiState,
    onNavigateBack: () -> Unit = {},
    onStartAgent: () -> Unit = {},
    onStopAgent: () -> Unit = {},
    onDmClick: () -> Unit = {},
    onMachineClick: (machineId: String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val contentState = resolveAgentDetailContentState(state)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoCream)
    ) {
        // ── Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeoOrange)
                .padding(bottom = 3.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeoPressableBox(
                    onClick = onNavigateBack,
                    size = 36.dp,
                    backgroundColor = Color.White
                ) {
                    Text("←", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Agent Detail",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color.Black))

        when (contentState) {
            AgentDetailContentState.Loading -> {
                NeoSkeletonCardList(count = 3)
            }

            is AgentDetailContentState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(contentState.message, fontFamily = SpaceGrotesk, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        NeoPressableBox(onClick = onRetry, size = 40.dp, backgroundColor = NeoOrange) {
                            Text("↻", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is AgentDetailContentState.Content -> {
                val agent = contentState.agent
                var showStopConfirm by remember(agent.id) { mutableStateOf(false) }
                val agentName = agent.name.orEmpty().ifEmpty { "Agent" }
                val agentModel = agent.model.orEmpty()
                val isActive = agent.status == "active"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Hero Section ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .border(width = 0.dp, color = Color.Transparent, shape = RectangleShape)
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Avatar with shadow
                        Box(modifier = Modifier.size(59.dp)) {
                            // Shadow
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .offset(3.dp, 3.dp)
                                    .background(Color.Black, RectangleShape)
                            )
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(2.dp, Color.Black, RectangleShape)
                                    .background(NeoOrange, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    agentName.take(1).uppercase(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk
                                )
                            }
                        }
                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                agentName,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (agentModel.isNotEmpty()) {
                                val displayModel = agentModel
                                    .replace("claude-", "Claude ")
                                    .replace("-", " ")
                                    .split(" ")
                                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    displayModel,
                                    fontFamily = SpaceMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .border(1.dp, Color.Black, RectangleShape)
                                        .background(NeoCream)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .border(1.5.dp, Color.Black, RectangleShape)
                                        .background(if (isActive) NeoLime else Color.Gray)
                                )
                                Text(
                                    if (isActive) "Active" else "Stopped",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(3.dp).background(Color.Black))

                    // ── Action Buttons ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton("💬 DM", NeoCyan, Modifier.weight(1f), onDmClick)
                        if (isActive) {
                            ActionButton("⏹ Stop", NeoPink, Modifier.weight(1f)) { showStopConfirm = true }
                        } else {
                            ActionButton("▶ Start", NeoLime, Modifier.weight(1f), onStartAgent)
                        }
                    }

                    // Stop confirmation dialog
                    if (showStopConfirm) {
                        NeoConfirmDialog(
                            title = "Stop Agent",
                            message = "确定要停止 ${agent.name.orEmpty()} 吗？停止后 Agent 将不再处理消息。",
                            confirmText = "STOP",
                            confirmColor = NeoPink,
                            onConfirm = onStopAgent,
                            onDismiss = { showStopConfirm = false }
                        )
                    }

                    // ── Current Activity ──
                    val activity = state.latestActivity ?: agent.activity
                    if (activity != null) {
                        SectionTitle("Current Activity")
                        NeoCard(stripColor = NeoLime) {
                            Text(
                                "⚡ $activity",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            val detail = state.latestActivityDetail ?: agent.activityDetail
                            if (detail != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    detail,
                                    fontFamily = SpaceMono,
                                    fontSize = 12.sp,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // ── Running On ──
                    val machineId = agent.machineId
                    val machineName = agent.machineName
                    if (!machineId.isNullOrBlank()) {
                        SectionTitle("Running On")
                        NeoCard(stripColor = NeoGold) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMachineClick(machineId) },
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .border(2.dp, Color.Black, RectangleShape)
                                        .background(NeoGold, RectangleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("\uD83D\uDCBB", fontSize = 16.sp)
                                }
                                Column {
                                    Text(
                                        machineName.orEmpty().ifEmpty { "Machine" },
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "tap to view \u2192",
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 11.sp,
                                        color = Color.Black.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    // ── Details Card ──
                    SectionTitle("Details")
                    NeoCard(stripColor = NeoOrange) {
                        InfoRow("Model", agentModel.ifEmpty { "—" }, isMono = true)
                        InfoRow("Status", if (isActive) "Active" else "Stopped")
                        if (agent.createdAt.orEmpty().isNotEmpty()) {
                            InfoRow("Created", agent.createdAt.orEmpty().take(10), isMono = true)
                        }
                    }

                    // ── System Prompt ──
                    val prompt = agent.prompt
                    if (!prompt.isNullOrBlank()) {
                        SectionTitle("System Prompt")
                        PromptCard(prompt)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title.uppercase(),
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp)
    )
}

@Composable
private fun NeoCard(
    stripColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .offset(4.dp, 4.dp)
                .background(Color.Black, RectangleShape)
                .height(IntrinsicSize.Min)
        ) {
            // shadow placeholder — just needs to match size
            Column(Modifier.padding(12.dp).alpha(0f), content = content)
        }
        Column(
            modifier = modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RectangleShape)
                .background(Color.White, RectangleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(stripColor)
            )
            Column(Modifier.padding(12.dp, 14.dp), content = content)
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun InfoRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label.uppercase(),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            color = Color.Black.copy(alpha = 0.5f)
        )
        Text(
            value,
            fontFamily = if (isMono) SpaceMono else SpaceGrotesk,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isMono) 11.sp else 13.sp
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(3.dp, 3.dp)
                .background(Color.Black, RectangleShape)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RectangleShape)
                .background(bgColor, RectangleShape)
                .clickable(onClick = onClick)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PromptCard(prompt: String) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(4.dp, 4.dp)
                .background(Color.Black, RectangleShape)
                .height(IntrinsicSize.Min)
        ) {
            Column(Modifier.padding(12.dp).alpha(0f)) {
                Text(prompt, fontSize = 12.sp, maxLines = if (expanded) Int.MAX_VALUE else 5)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RectangleShape)
                .background(Color.White, RectangleShape)
                .clickable { expanded = !expanded }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(NeoOrange)
            )
            Column(
                modifier = Modifier
                    .padding(12.dp, 14.dp)
                    .animateContentSize()
            ) {
                Text(
                    prompt,
                    fontFamily = SpaceGrotesk,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = Color.Black.copy(alpha = 0.7f),
                    maxLines = if (expanded) Int.MAX_VALUE else 5,
                    overflow = TextOverflow.Ellipsis
                )
                if (!expanded) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "tap to expand ↓",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
