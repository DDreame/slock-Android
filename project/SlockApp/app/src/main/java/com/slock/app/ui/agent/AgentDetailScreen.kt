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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.slock.app.data.model.ActivityLogEntry
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
    data class Content(val agent: com.slock.app.data.model.Agent) : AgentDetailContentState
}

internal fun resolveAgentDetailContentState(state: AgentDetailUiState): AgentDetailContentState = when {
    state.agent != null -> AgentDetailContentState.Content(state.agent)
    state.error != null -> AgentDetailContentState.Error(state.error)
    else -> AgentDetailContentState.Loading
}

@Composable
fun AgentDetailScreen(
    state: AgentDetailUiState,
    contextLabel: String = "",
    onNavigateBack: () -> Unit = {},
    onStartAgent: () -> Unit = {},
    onStopAgent: () -> Unit = {},
    onDmClick: () -> Unit = {},
    onMachineClick: (machineId: String) -> Unit = {},
    onSelectTab: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onResetAgent: () -> Unit = {},
    onConsumeResetFeedback: () -> Unit = {}
) {
    val contentState = resolveAgentDetailContentState(state)
    val headerContextLabel = resolveAgentDetailHeaderContext(contextLabel)

    val context = LocalContext.current
    LaunchedEffect(state.resetFeedbackMessage) {
        state.resetFeedbackMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onConsumeResetFeedback()
        }
    }

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
                    Text("\u2190", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                if (headerContextLabel != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Agent Detail",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            headerContextLabel,
                            fontFamily = SpaceMono,
                            fontSize = 11.sp,
                            color = Color.Black.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        "Agent Detail",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
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
                            Text("\u21BB", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is AgentDetailContentState.Content -> {
                val agent = contentState.agent
                var showStopConfirm by remember(agent.id) { mutableStateOf(false) }
                var showResetConfirm by remember(agent.id) { mutableStateOf(false) }
                val agentName = agent.name.orEmpty().ifEmpty { "Agent" }
                val agentModel = agent.model.orEmpty()
                val isActive = agent.status == "active"
                val detailActivity = state.latestActivity ?: agent.activity
                val displayState = resolveDisplayState(agent.status, detailActivity)

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
                        Box(modifier = Modifier.size(59.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .offset(3.dp, 3.dp)
                                    .background(Color.Black, RectangleShape)
                            )
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
                                        .background(displayState.dotColor)
                                )
                                Text(
                                    displayState.statusText,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(3.dp).background(Color.Black))

                    // ── Tab Row ──
                    val tabs = listOf("OVERVIEW", "ACTIVITY LOG")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val selected = state.selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectTab(index) }
                                    .background(if (selected) NeoOrange.copy(alpha = 0.15f) else Color.White)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = if (selected) Color.Black else Color.Black.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

                    // ── Tab Content ──
                    if (state.selectedTab == 0) {
                        OverviewContent(state, agent, agentModel, isActive, displayState, onDmClick, onStartAgent, onMachineClick, onStopClick = { showStopConfirm = true }, onResetClick = { showResetConfirm = true })
                    } else {
                        ActivityLogContent(state)
                    }

                    Spacer(Modifier.height(24.dp))
                }

                if (showStopConfirm) {
                    NeoConfirmDialog(
                        title = "Stop Agent",
                        message = "\u786E\u5B9A\u8981\u505C\u6B62 ${agent.name.orEmpty()} \u5417\uFF1F\u505C\u6B62\u540E Agent \u5C06\u4E0D\u518D\u5904\u7406\u6D88\u606F\u3002",
                        confirmText = "STOP",
                        confirmColor = NeoPink,
                        onConfirm = onStopAgent,
                        onDismiss = { showStopConfirm = false }
                    )
                }

                if (showResetConfirm) {
                    NeoConfirmDialog(
                        title = "Reset Agent",
                        message = "\u786E\u5B9A\u8981\u91CD\u7F6E ${agent.name.orEmpty()} \u5417\uFF1F\u8FD9\u5C06\u6E05\u9664 Agent \u7684\u5F53\u524D\u72B6\u6001\u548C\u4F1A\u8BDD\u8BB0\u5F55\u3002",
                        confirmText = "RESET",
                        confirmColor = NeoOrange,
                        onConfirm = {
                            onResetAgent()
                            showResetConfirm = false
                        },
                        onDismiss = { showResetConfirm = false }
                    )
                }
            }
        }
    }
}

internal fun resolveAgentDetailHeaderContext(contextLabel: String): String? =
    contextLabel.trim().takeIf { it.isNotEmpty() }

@Composable
private fun OverviewContent(
    state: AgentDetailUiState,
    agent: com.slock.app.data.model.Agent,
    agentModel: String,
    isActive: Boolean,
    displayState: AgentDisplayState,
    onDmClick: () -> Unit,
    onStartAgent: () -> Unit,
    onMachineClick: (String) -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton("DM", NeoCyan, Modifier.weight(1f), onDmClick)
        if (isActive) {
            ActionButton(displayState.toggleLabel, NeoPink, Modifier.weight(1f), onStopClick)
        } else {
            ActionButton(displayState.toggleLabel, NeoLime, Modifier.weight(1f), onStartAgent)
        }
        ActionButton("Reset", NeoOrange, Modifier.weight(1f), onResetClick)
    }

    val activity = state.latestActivity ?: agent.activity
    if (isActive && activity != null) {
        SectionTitle("Current Activity")
        NeoCard(stripColor = NeoLime) {
            Text(
                activity,
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

    SectionTitle("Details")
    NeoCard(stripColor = NeoOrange) {
        InfoRow("Model", agentModel.ifEmpty { "\u2014" }, isMono = true)
        InfoRow("Status", if (isActive) "Active" else "Stopped")
        if (agent.createdAt.orEmpty().isNotEmpty()) {
            InfoRow("Created", agent.createdAt.orEmpty().take(10), isMono = true)
        }
    }

    val prompt = agent.prompt
    if (!prompt.isNullOrBlank()) {
        SectionTitle("System Prompt")
        PromptCard(prompt)
    }
}

@Composable
private fun ActivityLogContent(state: AgentDetailUiState) {
    if (state.isLoadingLog && state.activityLog.isEmpty()) {
        NeoSkeletonCardList(count = 3)
        return
    }

    if (state.activityLog.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No activity yet",
                fontFamily = SpaceGrotesk,
                fontSize = 14.sp,
                color = Color.Black.copy(alpha = 0.4f)
            )
        }
        return
    }

    SectionTitle("Recent Activity")
    state.activityLog.forEach { entry ->
        ActivityLogItem(entry)
    }
}

@Composable
private fun ActivityLogItem(entry: ActivityLogEntry) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(3.dp, 3.dp)
                .background(Color.Black, RectangleShape)
                .height(IntrinsicSize.Min)
        ) {
            Column(Modifier.padding(10.dp).alpha(0f)) {
                Text(entry.activity.orEmpty(), fontSize = 12.sp)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color.Black, RectangleShape)
                .background(Color.White, RectangleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(NeoLime)
            )
            Column(Modifier.padding(10.dp, 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        entry.activity.orEmpty().ifEmpty { "Activity" },
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val ts = entry.timestamp
                    if (!ts.isNullOrBlank()) {
                        Text(
                            formatTimestamp(ts),
                            fontFamily = SpaceMono,
                            fontSize = 9.sp,
                            color = Color.Black.copy(alpha = 0.4f)
                        )
                    }
                }
                val detail = entry.detail
                if (!detail.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        detail,
                        fontFamily = SpaceMono,
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.6f),
                        lineHeight = 16.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(iso: String): String {
    return try {
        val instant = java.time.Instant.parse(iso)
        val local = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        String.format("%02d:%02d", local.hour, local.minute)
    } catch (_: Exception) {
        iso.take(16)
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
                        "tap to expand \u2193",
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
