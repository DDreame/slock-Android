package com.slock.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Channel
import com.slock.app.data.model.Message
import com.slock.app.data.model.Agent
import com.slock.app.data.model.Server
import com.slock.app.ui.channel.ChannelUiState
import com.slock.app.ui.member.MemberItem
import com.slock.app.ui.server.ServerUiState
import com.slock.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverState: ServerUiState,
    channelState: ChannelUiState,
    selectedServer: Server?,
    searchState: SearchUiState = SearchUiState(),
    onSearchQueryChange: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onServerSelect: (Server) -> Unit,
    onChannelClick: (channelId: String, channelName: String) -> Unit,
    onDmClick: (channelId: String, channelName: String) -> Unit,
    onCreateChannel: (name: String, type: String) -> Unit,
    onCreateServer: (name: String, slug: String) -> Unit,
    onEditChannel: (channelId: String, newName: String) -> Unit = { _, _ -> },
    onDeleteChannel: (channelId: String) -> Unit = {},
    onLeaveChannel: (channelId: String) -> Unit = {},
    onSearchMessageClick: (Message) -> Unit = {},
    onSearchAgentClick: (Agent) -> Unit = {},
    onOpenSettings: () -> Unit,
    isConnected: Boolean = true,
    isReconnecting: Boolean = false,
    onTabSelected: (Int) -> Unit = {},
    members: List<MemberItem> = emptyList(),
    onNewDmMemberSelected: (MemberItem) -> Unit = {},
    threadsContent: @Composable () -> Unit = {},
    membersContent: @Composable () -> Unit = {},
    tasksContent: @Composable () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showNewDmDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<Pair<String, String>?>(null) }
    var deletingChannelId by remember { mutableStateOf<String?>(null) }
    var leavingChannelId by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ServerDrawer(
                servers = serverState.servers,
                selectedServer = selectedServer,
                onServerSelect = { server ->
                    onServerSelect(server)
                    scope.launch { drawerState.close() }
                },
                onCreateServer = onCreateServer
            )
        }
    ) {
        Scaffold(
            topBar = {
                NeoTopBar(
                    serverName = selectedServer?.name ?: "Select Server",
                    serverInitial = selectedServer?.name?.take(1)?.uppercase() ?: "?",
                    onServerSelectorClick = { scope.launch { drawerState.open() } },
                    onSettingsClick = onOpenSettings
                )
            },
            bottomBar = {
                NeoBottomNav(
                    selectedTab = selectedTab,
                    onTabSelect = { tab ->
                        selectedTab = tab
                        onTabSelected(tab)
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Cream)
                    .padding(padding)
            ) {
                NetworkStatusBanner(
                    isConnected = isConnected,
                    isReconnecting = isReconnecting
                )
                Box(modifier = Modifier.weight(1f)) {
                    // All tabs stay composed to preserve scroll/selection state.
                    // Visible tab gets zIndex(1f) so it draws on top and receives touches first.
                    val tabs = listOf<@Composable () -> Unit>(
                        {
                            ChannelsTabContent(
                                channelState = channelState,
                                searchState = searchState,
                                onSearchQueryChange = onSearchQueryChange,
                                onChannelClick = onChannelClick,
                                onDmClick = onDmClick,
                                onShowCreateChannel = { showCreateChannelDialog = true },
                                onShowNewDm = { showNewDmDialog = true },
                                onSearchMessageClick = onSearchMessageClick,
                                onSearchAgentClick = onSearchAgentClick,
                                onEditChannel = { id, name -> editingChannel = id to name },
                                onDeleteChannel = { deletingChannelId = it },
                                onLeaveChannel = { leavingChannelId = it }
                            )
                        },
                        { threadsContent() },
                        { membersContent() },
                        { tasksContent() }
                    )
                    tabs.forEachIndexed { index, tab ->
                        val isVisible = index == selectedTab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(if (isVisible) 1f else 0f)
                                .graphicsLayer(alpha = if (isVisible) 1f else 0f)
                        ) {
                            tab()
                        }
                    }
                }
            }
        }
    }

    if (showCreateChannelDialog) {
        CreateChannelNeoDialog(
            onDismiss = { showCreateChannelDialog = false },
            onCreate = { name ->
                onCreateChannel(name, "text")
                showCreateChannelDialog = false
            }
        )
    }

    if (showNewDmDialog) {
        NewDmDialog(
            members = members,
            onDismiss = { showNewDmDialog = false },
            onMemberSelected = { member ->
                showNewDmDialog = false
                onNewDmMemberSelected(member)
            }
        )
    }

    editingChannel?.let { (channelId, currentName) ->
        EditChannelNeoDialog(
            currentName = currentName,
            onDismiss = { editingChannel = null },
            onSave = { newName ->
                onEditChannel(channelId, newName)
                editingChannel = null
            }
        )
    }

    deletingChannelId?.let { channelId ->
        ConfirmActionNeoDialog(
            title = "Delete Channel",
            message = "Are you sure you want to delete this channel? This cannot be undone.",
            confirmText = "DELETE",
            confirmColor = Pink,
            onDismiss = { deletingChannelId = null },
            onConfirm = {
                onDeleteChannel(channelId)
                deletingChannelId = null
            }
        )
    }

    leavingChannelId?.let { channelId ->
        ConfirmActionNeoDialog(
            title = "Leave Channel",
            message = "Are you sure you want to leave this channel?",
            confirmText = "LEAVE",
            confirmColor = Orange,
            onDismiss = { leavingChannelId = null },
            onConfirm = {
                onLeaveChannel(channelId)
                leavingChannelId = null
            }
        )
    }
}

// Channels Tab Content (Tab 0)
@Composable
private fun ChannelsTabContent(
    channelState: ChannelUiState,
    searchState: SearchUiState,
    onSearchQueryChange: (String) -> Unit,
    onChannelClick: (channelId: String, channelName: String) -> Unit,
    onDmClick: (channelId: String, channelName: String) -> Unit,
    onShowCreateChannel: () -> Unit,
    onShowNewDm: () -> Unit,
    onSearchMessageClick: (Message) -> Unit = {},
    onSearchAgentClick: (Agent) -> Unit = {},
    onEditChannel: (channelId: String, currentName: String) -> Unit = { _, _ -> },
    onDeleteChannel: (channelId: String) -> Unit = {},
    onLeaveChannel: (channelId: String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            NeoTextField(
                value = searchState.query,
                onValueChange = onSearchQueryChange,
                placeholder = "Search channels, messages, agents...",
                leadingIcon = { Text(text = "\uD83D\uDD0D", fontSize = 16.sp) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        val isSearchActive = searchState.query.isNotBlank()

        when {
            channelState.isLoading && !isSearchActive -> {
                NeoSkeletonChannelList()
            }
            isSearchActive -> {
                // Search Results
                SearchResultsContent(
                    searchState = searchState,
                    onChannelClick = onChannelClick,
                    onMessageClick = onSearchMessageClick,
                    onAgentClick = onSearchAgentClick
                )
            }
            else -> {
                // Normal channel list
                ChannelListContent(
                    channelState = channelState,
                    onChannelClick = onChannelClick,
                    onDmClick = onDmClick,
                    onShowCreateChannel = onShowCreateChannel,
                    onShowNewDm = onShowNewDm,
                    onEditChannel = onEditChannel,
                    onDeleteChannel = onDeleteChannel,
                    onLeaveChannel = onLeaveChannel
                )
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    searchState: SearchUiState,
    onChannelClick: (channelId: String, channelName: String) -> Unit,
    onMessageClick: (Message) -> Unit,
    onAgentClick: (Agent) -> Unit
) {
    val hasChannels = searchState.channels.isNotEmpty()
    val hasMessages = searchState.messages.isNotEmpty()
    val hasAgents = searchState.agents.isNotEmpty()
    val hasNoResults = !hasChannels && !hasMessages && !hasAgents && searchState.hasSearched && !searchState.isSearching

    if (searchState.isSearching) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Black, strokeWidth = 2.dp)
        }
        return
    }

    if (hasNoResults) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "\uD83D\uDD0D", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No results found",
                    style = MaterialTheme.typography.titleSmall,
                    color = Black
                )
                Text(
                    text = "Try different keywords",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Channels section
        if (hasChannels) {
            item {
                SearchSectionHeader(title = "CHANNELS", count = searchState.channels.size)
            }
            items(searchState.channels) { channel ->
                SearchChannelItem(
                    channel = channel,
                    onClick = { onChannelClick(channel.id.orEmpty(), channel.name.orEmpty()) }
                )
            }
        }

        // Agents section
        if (hasAgents) {
            item {
                SearchSectionHeader(title = "AGENTS", count = searchState.agents.size)
            }
            items(searchState.agents) { agent ->
                SearchAgentItem(
                    agent = agent,
                    onClick = { onAgentClick(agent) }
                )
            }
        }

        // Messages section
        if (hasMessages) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MESSAGES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Black.copy(alpha = 0.6f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${searchState.messages.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        if (searchState.isRemoteSearching) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Black,
                                strokeWidth = 1.5.dp
                            )
                        }
                    }
                }
            }
            items(searchState.messages) { message ->
                SearchMessageItem(
                    message = message,
                    onClick = { onMessageClick(message) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SearchSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Black.copy(alpha = 0.6f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun SearchChannelItem(channel: Channel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp)
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Lavender)
                .border(2.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "#", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Black)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "# ${channel.name.orEmpty()}",
            style = MaterialTheme.typography.titleSmall,
            color = Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchAgentItem(agent: Agent, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp)
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Orange)
                .border(2.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = agent.name.orEmpty().take(1).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = agent.name.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(Orange)
                        .border(1.dp, Black, RectangleShape)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "AGENT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }
            }
            if (agent.description?.isNotBlank() == true) {
                Text(
                    text = agent.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        // Status dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (agent.status == "active") Lime else Color(0xFFCCCCCC))
                .border(1.5.dp, Black, RectangleShape)
        )
    }
}

@Composable
private fun SearchMessageItem(message: Message, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp)
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Cyan)
                .border(2.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.senderName.orEmpty().take(1).uppercase().ifEmpty { "?" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.senderName.orEmpty().ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                val displayTime = formatPreviewTime(message.createdAt.orEmpty())
                if (displayTime.isNotEmpty()) {
                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
            Text(
                text = message.content.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// Normal channel list (when not searching)
@Composable
private fun ChannelListContent(
    channelState: ChannelUiState,
    onChannelClick: (channelId: String, channelName: String) -> Unit,
    onDmClick: (channelId: String, channelName: String) -> Unit,
    onShowCreateChannel: () -> Unit,
    onShowNewDm: () -> Unit,
    onEditChannel: (channelId: String, currentName: String) -> Unit = { _, _ -> },
    onDeleteChannel: (channelId: String) -> Unit = {},
    onLeaveChannel: (channelId: String) -> Unit = {}
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SectionHeader(title = "CHANNELS", onAdd = onShowCreateChannel)
        }

        items(channelState.channels) { channel ->
            val preview = channelState.channelPreviews[channel.id.orEmpty()]
            ChannelItem(
                channel = channel,
                onClick = { onChannelClick(channel.id.orEmpty(), channel.name.orEmpty()) },
                lastMessageSender = preview?.senderName.orEmpty(),
                lastMessageContent = preview?.content.orEmpty(),
                lastMessageTime = preview?.createdAt.orEmpty(),
                onEdit = { onEditChannel(channel.id.orEmpty(), channel.name.orEmpty()) },
                onDelete = { onDeleteChannel(channel.id.orEmpty()) },
                onLeave = { onLeaveChannel(channel.id.orEmpty()) }
            )
        }

        if (channelState.channels.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No channels yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            SectionHeader(title = "DIRECT MESSAGES", onAdd = onShowNewDm)
        }

        if (channelState.dms.isNotEmpty()) {
            items(channelState.dms) { dm ->
                val contactId = dm.members?.firstOrNull { it.agentId != null }?.agentId
                    ?: dm.members?.firstOrNull { it.userId != null }?.userId
                val isOnline = contactId != null && contactId in channelState.onlineIds
                val isAgent = dm.members?.any { it.agentId != null } ?: true
                DMItem(
                    name = dm.name.orEmpty(),
                    isAgent = isAgent,
                    isOnline = isOnline,
                    onClick = { onDmClick(dm.id.orEmpty(), dm.name.orEmpty()) }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No direct messages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

// Yellow Top Bar
@Composable
private fun NeoTopBar(
    serverName: String,
    serverInitial: String,
    onServerSelectorClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Yellow,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Server selector
            Row(
                modifier = Modifier
                    .clickable(onClick = onServerSelectorClick)
                    .border(2.dp, Black, RectangleShape)
                    .background(White)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Yellow)
                        .border(1.5.dp, Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = serverInitial,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Black
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = serverName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "\u25BE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoIconButton(icon = "\u2699", onClick = onSettingsClick)
            }
        }
    }
    // Bottom border
    Divider(thickness = 3.dp, color = Black)
}

@Composable
private fun NeoIconButton(icon: String, onClick: () -> Unit) {
    NeoPressableBox(onClick = onClick) {
        Text(text = icon, fontSize = 18.sp)
    }
}

// Section Header
@Composable
private fun SectionHeader(title: String, onAdd: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Black.copy(alpha = 0.6f)
        )
        if (onAdd != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Yellow)
                    .border(2.dp, Black, RectangleShape)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Black)
            }
        }
    }
}

// Channel Item
@Composable
private fun ChannelItem(
    channel: Channel,
    onClick: () -> Unit,
    unreadCount: Int = 0,
    lastMessageSender: String = "",
    lastMessageContent: String = "",
    lastMessageTime: String = "",
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onLeave: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .padding(bottom = 8.dp)
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Lavender)
                .border(2.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Channel info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "# ${channel.name.orEmpty()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Black,
                    fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (lastMessageTime.isNotEmpty()) {
                    val displayTime = formatPreviewTime(lastMessageTime)
                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
            if (lastMessageContent.isNotEmpty()) {
                Text(
                    text = if (lastMessageSender.isNotEmpty()) "$lastMessageSender: $lastMessageContent" else lastMessageContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else if (channel.type.orEmpty().isNotBlank()) {
                Text(
                    text = channel.type.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Unread badge with scale bounce
        if (unreadCount > 0) {
            val scale = remember { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(unreadCount) {
                scale.snapTo(0f)
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.keyframes {
                        durationMillis = 300
                        0f at 0
                        1.3f at 150
                        1f at 300
                    }
                )
            }
            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                    .size(24.dp)
                    .background(Pink)
                    .border(2.dp, Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
        }

        // Overflow menu
        Box {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u22EE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = { showMenu = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("Leave") },
                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = { showMenu = false; onLeave() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFD32F2F)) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp)) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

// Format ISO timestamp to short display time (e.g. "14:30" or "Apr 16")
private fun formatPreviewTime(isoTime: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoTime) ?: return ""
        val now = java.util.Calendar.getInstance()
        val msgCal = java.util.Calendar.getInstance().apply { time = date }
        if (now.get(java.util.Calendar.DATE) == msgCal.get(java.util.Calendar.DATE) &&
            now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR)) {
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
        } else {
            java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        ""
    }
}

// DM Item skeleton
@Composable
private fun DMItem(
    name: String,
    isAgent: Boolean = false,
    isOnline: Boolean = false,
    lastMessage: String = "",
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with status dot
        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isAgent) Orange else Cyan)
                    .border(2.dp, Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Black
                )
            }
            // Online/Agent status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.BottomEnd)
                    .background(if (isOnline) Lime else Color(0xFFCCCCCC))
                    .border(1.5.dp, Black, RectangleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isAgent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Orange)
                            .border(1.dp, Black, RectangleShape)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "AGENT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    }
                }
            }
            if (lastMessage.isNotBlank()) {
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Bottom Navigation
@Composable
private fun NeoBottomNav(selectedTab: Int, onTabSelect: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = White,
        shadowElevation = 0.dp
    ) {
        Column {
            Divider(thickness = 3.dp, color = Black)
            Row(modifier = Modifier.fillMaxWidth()) {
                NeoNavItem(
                    icon = "\uD83D\uDCAC",
                    label = "CHANNELS",
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    modifier = Modifier.weight(1f)
                )
                NeoNavItem(
                    icon = "\uD83E\uDDF5",
                    label = "THREADS",
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    modifier = Modifier.weight(1f)
                )
                NeoNavItem(
                    icon = "\uD83D\uDC65",
                    label = "MEMBERS",
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelect(2) },
                    modifier = Modifier.weight(1f)
                )
                NeoNavItem(
                    icon = "\uD83D\uDCDD",
                    label = "TASKS",
                    isSelected = selectedTab == 3,
                    onClick = { onTabSelect(3) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NeoNavItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(if (isSelected) Yellow else White)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (isSelected) Black else Black.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// Server Drawer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerDrawer(
    servers: List<Server>,
    selectedServer: Server?,
    onServerSelect: (Server) -> Unit,
    onCreateServer: (name: String, slug: String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = White,
        drawerShape = RectangleShape,
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            Divider(thickness = 3.dp, color = Black, modifier = Modifier.padding(end = 16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "YOUR SERVERS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Black.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            servers.forEach { server ->
                val isActive = server.id == selectedServer?.id
                ServerDrawerItem(
                    server = server,
                    isActive = isActive,
                    onClick = { onServerSelect(server) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Create server button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Black, RectangleShape)
                    .background(Cream)
                    .clickable { showCreateDialog = true }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Create New Server",
                    style = MaterialTheme.typography.titleSmall,
                    color = Black
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateServerNeoDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, slug ->
                onCreateServer(name, slug)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ServerDrawerItem(server: Server, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadowSmall()
            .background(if (isActive) Yellow else White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isActive) White else Lavender)
                .border(2.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = server.name.orEmpty().take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Black
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = server.name.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = Black
            )
            Text(
                text = "@${server.slug}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

// Dialogs
@Composable
private fun CreateChannelNeoDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "Create Channel",
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeoLabel("CHANNEL NAME")
                NeoTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. general"
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeoButton(
                    text = "CREATE",
                    onClick = { if (name.isNotBlank()) onCreate(name) },
                    enabled = name.isNotBlank()
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeoButtonSecondary(
                    text = "Cancel",
                    onClick = onDismiss,
                    containerColor = Cream
                )
            }
        }
    }
}

@Composable
private fun CreateServerNeoDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, slug: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }

    LaunchedEffect(name) {
        slug = name.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "Create Server",
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeoLabel("SERVER NAME")
                NeoTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "My Server"
                )

                Spacer(modifier = Modifier.height(14.dp))

                NeoLabel("URL SLUG")
                NeoTextField(
                    value = slug,
                    onValueChange = { slug = it },
                    placeholder = "my-server"
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeoButton(
                    text = "CREATE",
                    onClick = { if (name.isNotBlank() && slug.isNotBlank()) onCreate(name, slug) },
                    enabled = name.isNotBlank() && slug.isNotBlank()
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeoButtonSecondary(
                    text = "Cancel",
                    onClick = onDismiss,
                    containerColor = Cream
                )
            }
        }
    }
}

@Composable
private fun EditChannelNeoDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "Edit Channel",
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeoLabel("CHANNEL NAME")
                NeoTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. general"
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeoButton(
                    text = "SAVE",
                    onClick = { if (name.isNotBlank() && name != currentName) onSave(name) },
                    enabled = name.isNotBlank() && name != currentName
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeoButtonSecondary(
                    text = "Cancel",
                    onClick = onDismiss,
                    containerColor = Cream
                )
            }
        }
    }
}

@Composable
private fun ConfirmActionNeoDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                NeoButton(
                    text = confirmText,
                    onClick = onConfirm,
                    containerColor = confirmColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeoButtonSecondary(
                    text = "Cancel",
                    onClick = onDismiss,
                    containerColor = Cream
                )
            }
        }
    }
}

@Composable
private fun NewDmDialog(
    members: List<MemberItem>,
    onDismiss: () -> Unit,
    onMemberSelected: (MemberItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredMembers = remember(members, searchQuery) {
        if (searchQuery.isBlank()) members
        else members.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "New Message",
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeoTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search members..."
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(filteredMembers) { member ->
                        NewDmMemberRow(
                            member = member,
                            onClick = { onMemberSelected(member) }
                        )
                    }
                    if (filteredMembers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No members found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                NeoButtonSecondary(
                    text = "Cancel",
                    onClick = onDismiss,
                    containerColor = Cream
                )
            }
        }
    }
}

@Composable
private fun NewDmMemberRow(member: MemberItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (member.isAgent) Orange else Cyan)
                    .border(2.dp, Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Black
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.BottomEnd)
                    .background(if (member.isOnline) Lime else Color(0xFFCCCCCC))
                    .border(1.5.dp, Black, RectangleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (member.isAgent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Orange)
                            .border(1.dp, Black, RectangleShape)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "AGENT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    }
                }
            }
            if (member.subtitle.isNotBlank()) {
                Text(
                    text = member.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
