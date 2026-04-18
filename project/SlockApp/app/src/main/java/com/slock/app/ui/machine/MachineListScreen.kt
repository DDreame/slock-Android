package com.slock.app.ui.machine

import android.widget.Toast
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Machine
import com.slock.app.data.model.MachineAgent
import com.slock.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineListScreen(
    state: MachineUiState,
    onDeleteMachine: (machine: Machine) -> Unit = {},
    onAgentClick: (agentId: String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onAddMachine: () -> Unit = {},
    onCreateMachine: (name: String) -> Unit = {},
    onFinishAddMachine: (newName: String) -> Unit = {},
    onCancelAddMachine: () -> Unit = {},
    onDismissDeleteBlocked: () -> Unit = {},
    onConfirmDelete: (machineId: String) -> Unit = {},
    onActionFeedbackShown: () -> Unit = {},
    showHeader: Boolean = true
) {
    var confirmDeleteMachine by remember { mutableStateOf<Machine?>(null) }
    var machineNameInput by remember { mutableStateOf("My Machine") }
    val context = LocalContext.current

    LaunchedEffect(state.actionFeedback) {
        state.actionFeedback?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            onActionFeedbackShown()
        }
    }

    LaunchedEffect(state.addMachineStep) {
        if (state.addMachineStep == AddMachineStep.ChooseType) {
            machineNameInput = "My Machine"
        }
        if (state.addMachineStep == AddMachineStep.Connected) {
            machineNameInput = state.newMachineName ?: state.connectedMachine?.hostname ?: "My Machine"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        if (showHeader) {
            MachineHeader(onBack = onNavigateBack)
        }

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
                state.machines.isEmpty() && state.addMachineStep == null -> {
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
                        Spacer(modifier = Modifier.height(16.dp))
                        AddMachineButton(onClick = onAddMachine)
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
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            AddMachineButton(onClick = onAddMachine)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation
    confirmDeleteMachine?.let { machine ->
        if (!machine.runningAgents.isNullOrEmpty()) {
            DeleteBlockedDialog(
                machine = machine,
                onDismiss = { confirmDeleteMachine = null }
            )
        } else {
            NeoConfirmDialog(
                title = "删除 Machine?",
                message = "确定要删除 ${machine.name.orEmpty()} 吗？\n\n此操作不可撤销。该 Machine 的 API Key 将失效。",
                confirmText = "删除",
                confirmColor = Pink,
                onConfirm = {
                    onConfirmDelete(machine.id.orEmpty())
                    confirmDeleteMachine = null
                },
                onDismiss = { confirmDeleteMachine = null }
            )
        }
    }

    // Delete blocked from VM
    state.deleteBlockedMachine?.let { machine ->
        DeleteBlockedDialog(
            machine = machine,
            onDismiss = onDismissDeleteBlocked
        )
    }

    // Add Machine bottom sheet
    if (state.addMachineStep != null) {
        ModalBottomSheet(
            onDismissRequest = onCancelAddMachine,
            containerColor = Color.White,
            shape = RectangleShape,
            dragHandle = null
        ) {
            AddMachineSheetContent(
                step = state.addMachineStep,
                apiKey = state.newMachineApiKey,
                connectedMachine = state.connectedMachine,
                machineName = machineNameInput,
                onMachineNameChange = { machineNameInput = it },
                onNext = { onCreateMachine(machineNameInput) },
                onFinish = { onFinishAddMachine(machineNameInput) },
                onCancel = onCancelAddMachine
            )
        }
    }
}

// ── Add Machine Button ──
@Composable
private fun AddMachineButton(onClick: () -> Unit) {
    val dashColor = Color.Black.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = dashColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f
                        )
                    )
                )
            }
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+ Add Machine",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.5f)
        )
    }
}

// ── Add Machine Sheet Content ──
@Composable
private fun AddMachineSheetContent(
    step: AddMachineStep,
    apiKey: String?,
    connectedMachine: Machine?,
    machineName: String,
    onMachineNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.padding(20.dp)) {
        // Step indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val steps = AddMachineStep.entries
            steps.forEachIndexed { index, s ->
                val color = when {
                    s.ordinal < step.ordinal -> Lime
                    s == step -> Yellow
                    else -> Color.Black.copy(alpha = 0.1f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(color)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (step) {
            AddMachineStep.ChooseType -> ChooseTypeContent(
                machineName = machineName,
                onMachineNameChange = onMachineNameChange,
                onNext = onNext,
                onCancel = onCancel
            )
            AddMachineStep.Connecting -> ConnectingContent(
                apiKey = apiKey,
                onCancel = onCancel
            )
            AddMachineStep.Connected -> ConnectedContent(
                machine = connectedMachine,
                machineName = machineName,
                onMachineNameChange = onMachineNameChange,
                onFinish = onFinish
            )
        }
    }
}

@Composable
private fun ChooseTypeContent(
    machineName: String,
    onMachineNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        text = "添加 Machine",
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )
    Text(
        text = "选择 Machine 类型",
        fontFamily = SpaceGrotesk,
        fontSize = 13.sp,
        color = Color.Black.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Your Computer option
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, RectangleShape)
            .background(Cream)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(2.dp, Color.Black, RectangleShape)
                    .background(Cream),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDCBB", fontSize = 18.sp)
            }
            Column {
                Text("Your Computer", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("在本地设备上运行 daemon", fontFamily = SpaceGrotesk, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.5f))
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))

    // Cloud Sandbox option (disabled)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black.copy(alpha = 0.3f), RectangleShape)
            .alpha(0.4f)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(2.dp, Color.Black, RectangleShape)
                    .background(Lavender),
                contentAlignment = Alignment.Center
            ) {
                Text("\u2601", fontSize = 18.sp)
            }
            Column {
                Text("Cloud Sandbox", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "COMING SOON",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Orange,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Machine name input
    Text(
        text = "MACHINE 名称",
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = Color.Black.copy(alpha = 0.6f)
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = machineName,
        onValueChange = onMachineNameChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceGrotesk),
        singleLine = true,
        shape = RectangleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Black,
            unfocusedBorderColor = Color.Black
        )
    )
    Spacer(modifier = Modifier.height(16.dp))

    SheetButton(
        text = "下一步",
        onClick = onNext,
        modifier = Modifier.fillMaxWidth(),
        enabled = machineName.isNotBlank()
    )
}

@Composable
private fun ConnectingContent(
    apiKey: String?,
    onCancel: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val command = "npx @slock-ai/daemon@latest \\\n  --server-url https://api.slock.ai \\\n  --api-key ${apiKey.orEmpty()}"

    Text(
        text = "连接 Machine",
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )
    Text(
        text = "在你的设备上运行以下命令来连接 Machine：",
        fontFamily = SpaceGrotesk,
        fontSize = 13.sp,
        color = Color.Black.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Code block
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(12.dp)
    ) {
        Text(
            text = command,
            fontFamily = SpaceMono,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = Lime
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .border(1.dp, Color.White.copy(alpha = 0.3f), RectangleShape)
                .clickable {
                    clipboardManager.setText(AnnotatedString(command))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("COPY", fontFamily = SpaceMono, fontSize = 10.sp, color = Color.White)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    // Waiting indicator
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, RectangleShape)
            .background(Cream)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PulseDot()
        Column {
            Text(
                "等待 Machine 连接...",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                "每 3 秒自动检查连接状态",
                fontFamily = SpaceMono,
                fontSize = 11.sp,
                color = Color.Black.copy(alpha = 0.4f)
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "\uD83D\uDD12 API Key 仅显示一次。请妥善保管。",
        fontFamily = SpaceMono,
        fontSize = 11.sp,
        color = Color.Black.copy(alpha = 0.4f),
        lineHeight = 14.sp
    )
    Spacer(modifier = Modifier.height(12.dp))

    SheetButton(
        text = "取消",
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        isPrimary = false
    )
}

@Composable
private fun ConnectedContent(
    machine: Machine?,
    machineName: String,
    onMachineNameChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    // Success banner
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, RectangleShape)
            .background(Lime)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("\u2713", fontSize = 20.sp)
        Column {
            Text(
                "Machine 已连接!",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            val detail = listOfNotNull(machine?.hostname, machine?.os).joinToString(" \u00B7 ")
            if (detail.isNotEmpty()) {
                Text(detail, fontFamily = SpaceGrotesk, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.6f))
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    // Name input
    Text(
        text = "MACHINE 名称",
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = Color.Black.copy(alpha = 0.6f)
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = machineName,
        onValueChange = onMachineNameChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceGrotesk),
        singleLine = true,
        shape = RectangleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Black,
            unfocusedBorderColor = Color.Black
        )
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Machine info card
    if (machine != null) {
        val infoLines = listOfNotNull(
            machine.hostname?.let { "Hostname: $it" },
            machine.os?.let { "OS: $it" },
            machine.daemonVersion?.let { "Daemon: v$it" },
            machine.runtimes?.takeIf { it.isNotEmpty() }?.let { "Runtimes: ${it.joinToString(", ")}" }
        )
        if (infoLines.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color.Black.copy(alpha = 0.15f), RectangleShape)
                    .background(Cream)
                    .padding(10.dp)
            ) {
                Text(
                    "MACHINE 信息",
                    fontFamily = SpaceMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                infoLines.forEach { line ->
                    Text(line, fontFamily = SpaceMono, fontSize = 11.sp, lineHeight = 16.sp, color = Color.Black.copy(alpha = 0.6f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    SheetButton(
        text = "完成",
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth()
    )
}

// ── Pulse Dot ──
@Composable
private fun PulseDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-alpha"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(alpha)
            .background(Orange, shape = androidx.compose.foundation.shape.CircleShape)
    )
}

// ── Delete Blocked Dialog ──
@Composable
private fun DeleteBlockedDialog(
    machine: Machine,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NeoCard(containerColor = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "\u26A0\uFE0F 无法删除 Machine",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color.Black, RectangleShape)
                        .background(Cream)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${machine.name.orEmpty()} 当前有 ${machine.runningAgents?.size ?: 0} 个 Agent 正在运行。",
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "请先移除所有关联的 Agent，然后再删除此 Machine。",
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        color = Color.Black.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                }

                val agents = machine.runningAgents
                if (!agents.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "关联 AGENTS",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        agents.forEach { agent ->
                            Text(
                                text = agent.name.orEmpty(),
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .border(1.5.dp, Color.Black.copy(alpha = 0.15f), RectangleShape)
                                    .background(Orange)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                SheetButton(
                    text = "知道了",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = false
                )
            }
        }
    }
}

// ── Sheet Button ──
@Composable
private fun SheetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true
) {
    Box(
        modifier = modifier
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.4f)
            .border(2.dp, Color.Black, RectangleShape)
            .background(if (isPrimary) Yellow else Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(2.dp, Color.Black, RectangleShape)
                .background(if (isOffline) Color(0xFFCCCCCC) else Yellow, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\uD83D\uDCBB", fontSize = 20.sp)
        }
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

    val metaTags = machine.meta
    if (!metaTags.isNullOrEmpty()) {
        Spacer(Modifier.height(10.dp))
        MetaTagsRow(tags = metaTags)
    }

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

@Composable
private fun MetaTagsRow(tags: List<String>) {
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

@Composable
private fun RunningAgentsRow(
    agents: List<MachineAgent>,
    onAgentClick: (agentId: String) -> Unit
) {
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
