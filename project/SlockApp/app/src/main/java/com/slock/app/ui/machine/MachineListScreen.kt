package com.slock.app.ui.machine

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Machine
import com.slock.app.data.model.MachineAgent
import com.slock.app.ui.theme.*

@Composable
fun MachineListScreen(
    state: MachineUiState,
    onDeleteMachine: (machineId: String) -> Unit = {},
    onAgentClick: (agentId: String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    showHeader: Boolean = true
) {
    var confirmDeleteMachine by remember { mutableStateOf<Machine?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Header
        if (showHeader) {
            MachineHeader(onBack = onNavigateBack)
        }

        // Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> {
                    NeoSkeletonCardList()
                }
                state.error != null && state.machines.isEmpty() -> {
                    NeoErrorState(
                        message = "Machine 加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = onRetry,
                        logContext = state.error
                    )
                }
                state.machines.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "\uD83D\uDCBB", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No machines yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Black
                        )
                        Text(
                            "Connect a machine to run agents",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                else -> {
                    val connected = state.machines.filter { it.status == "connected" }
                    val offline = state.machines.filter { it.status != "connected" }

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (connected.isNotEmpty()) {
                            item {
                                SectionLabel("Connected \u00B7 ${connected.size}")
                            }
                            items(connected) { machine ->
                                MachineCard(
                                    machine = machine,
                                    onAgentClick = onAgentClick,
                                    onDelete = { confirmDeleteMachine = machine }
                                )
                            }
                        }
                        if (offline.isNotEmpty()) {
                            item {
                                SectionLabel("Offline \u00B7 ${offline.size}")
                            }
                            items(offline) { machine ->
                                MachineCard(
                                    machine = machine,
                                    isOffline = true,
                                    onAgentClick = onAgentClick,
                                    onDelete = { confirmDeleteMachine = machine }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation
    confirmDeleteMachine?.let { machine ->
        NeoConfirmDialog(
            title = "Remove Machine",
            message = "确定要移除 ${machine.name.orEmpty()} 吗？移除后需要重新连接。",
            confirmText = "REMOVE",
            confirmColor = Pink,
            onConfirm = {
                onDeleteMachine(machine.id.orEmpty())
                confirmDeleteMachine = null
                Toast.makeText(context, "${machine.name.orEmpty()} 已移除", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { confirmDeleteMachine = null }
        )
    }
}

// ── Header ──
@Composable
private fun MachineHeader(onBack: () -> Unit) {
    Surface(color = Yellow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoPressableBox(onClick = onBack) {
                Text(text = "\u2190", fontSize = 18.sp, color = Black)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Machines",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Black
            )
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

// ── Section label ──
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

// ── Machine Card ──
@Composable
private fun MachineCard(
    machine: Machine,
    isOffline: Boolean = false,
    onAgentClick: (agentId: String) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val stripColor = if (isOffline) Color.Black.copy(alpha = 0.2f) else Lime
    val contentAlpha = if (isOffline) 0.5f else 1f

    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
        // Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(4.dp, 4.dp)
                .background(Color.Black, RectangleShape)
                .height(IntrinsicSize.Min)
        ) {
            // Invisible content placeholder for sizing
            Column(Modifier.padding(14.dp).alpha(0f)) {
                MachineCardContent(machine, isOffline, onAgentClick, onDelete)
            }
        }
        // Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RectangleShape)
                .background(Color.White, RectangleShape)
        ) {
            // Top strip
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(stripColor)
            )
            Column(
                modifier = Modifier
                    .alpha(contentAlpha)
                    .padding(14.dp)
            ) {
                MachineCardContent(machine, isOffline, onAgentClick, onDelete)
            }
        }
    }
}

@Composable
private fun MachineCardContent(
    machine: Machine,
    isOffline: Boolean,
    onAgentClick: (agentId: String) -> Unit,
    onDelete: () -> Unit
) {
    // Machine header row
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(2.dp, Color.Black, RectangleShape)
                .background(if (isOffline) Color(0xFFCCCCCC) else Yellow, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\uD83D\uDCBB", fontSize = 20.sp)
        }
        // Name + status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = machine.name.orEmpty().ifEmpty { "Unknown Machine" },
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .border(1.dp, Color.Black, RectangleShape)
                        .background(if (isOffline) Color(0xFFCCCCCC) else Lime)
                )
                Text(
                    text = if (isOffline) "Offline" else "Connected",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isOffline) Color.Black.copy(alpha = 0.4f) else Lime
                )
                // Uptime / last seen
                val timeInfo = if (isOffline) {
                    machine.lastSeen?.let { "Last seen $it" }
                } else {
                    machine.uptime?.let { "$it uptime" }
                }
                if (timeInfo != null) {
                    Text(
                        text = timeInfo,
                        fontFamily = SpaceGrotesk,
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }

    // Meta tags
    val metaTags = machine.meta
    if (!metaTags.isNullOrEmpty()) {
        Spacer(Modifier.height(10.dp))
        MetaTagsRow(tags = metaTags)
    }

    // Running Agents
    if (!isOffline) {
        Spacer(Modifier.height(10.dp))
        Divider(color = Color.Black.copy(alpha = 0.1f), thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "RUNNING AGENTS",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = Color.Black.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(6.dp))
        val agents = machine.runningAgents
        if (agents.isNullOrEmpty()) {
            Text(
                text = "No agents running",
                fontFamily = SpaceGrotesk,
                fontSize = 11.sp,
                color = Color.Black.copy(alpha = 0.3f)
            )
        } else {
            RunningAgentsRow(agents = agents, onAgentClick = onAgentClick)
        }
    }
}

// ── Meta tags row ──
@Composable
private fun MetaTagsRow(tags: List<String>) {
    // Using a FlowRow-like layout with wrapping
    var currentRow = mutableListOf<String>()
    val rows = mutableListOf<List<String>>()

    // Simple wrap — just lay them out in a Column of Rows
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            tags.forEach { tag ->
                MetaTag(tag)
            }
        }
    }
}

@Composable
private fun MetaTag(text: String) {
    Text(
        text = text,
        fontFamily = SpaceMono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .border(1.dp, Color.Black, RectangleShape)
            .background(Cream)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ── Running agents chips ──
@Composable
private fun RunningAgentsRow(
    agents: List<MachineAgent>,
    onAgentClick: (agentId: String) -> Unit
) {
    // Wrap-like horizontal layout
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        agents.forEach { agent ->
            AgentChip(
                name = agent.name.orEmpty(),
                isActive = agent.status == "active",
                onClick = { agent.id?.let { onAgentClick(it) } }
            )
        }
    }
}

@Composable
private fun AgentChip(
    name: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .border(1.dp, Color.Black, RectangleShape)
            .background(Orange)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .border(1.dp, Color.Black, RectangleShape)
                .background(if (isActive) Lime else Color(0xFFCCCCCC))
        )
        Text(
            text = name,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
