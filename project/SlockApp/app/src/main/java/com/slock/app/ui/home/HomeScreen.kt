package com.slock.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Channel
import com.slock.app.data.model.Server
import com.slock.app.ui.channel.ChannelUiState
import com.slock.app.ui.server.ServerUiState
import com.slock.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverState: ServerUiState,
    channelState: ChannelUiState,
    selectedServer: Server?,
    onServerSelect: (Server) -> Unit,
    onChannelClick: (channelId: String, channelName: String) -> Unit,
    onDmClick: (channelId: String, channelName: String) -> Unit,
    onCreateChannel: (name: String, type: String) -> Unit,
    onCreateServer: (name: String, slug: String) -> Unit,
    onLogout: () -> Unit,
    isConnected: Boolean = true,
    isReconnecting: Boolean = false,
    onTabSelected: (Int) -> Unit = {},
    threadsContent: @Composable () -> Unit = {},
    membersContent: @Composable () -> Unit = {},
    tasksContent: @Composable () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
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
                    onNotificationClick = { },
                    onSettingsClick = { showSettingsMenu = true }
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
                when (selectedTab) {
                    0 -> ChannelsTabContent(
                        channelState = channelState,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onChannelClick = onChannelClick,
                        onDmClick = onDmClick,
                        onShowCreateChannel = { showCreateChannelDialog = true }
                    )
                    1 -> threadsContent()
                    2 -> membersContent()
                    3 -> tasksContent()
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

    if (showSettingsMenu) {
        SettingsNeoDialog(
            onDismiss = { showSettingsMenu = false },
            onLogout = {
                showSettingsMenu = false
                onLogout()
            }
        )
    }
}

// Channels Tab Content (Tab 0)
@Composable
private fun ChannelsTabContent(
    channelState: ChannelUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onChannelClick: (channelId: String, channelName: String) -> Unit,
    onDmClick: (channelId: String, channelName: String) -> Unit,
    onShowCreateChannel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            NeoTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Search channels, DMs, messages...",
                leadingIcon = { Text(text = "\uD83D\uDD0D", fontSize = 16.sp) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        when {
            channelState.isLoading -> {
                NeoSkeletonChannelList()
            }
            else -> {
                val filteredChannels = channelState.channels.filter {
                    searchQuery.isBlank() || it.name.orEmpty().contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        SectionHeader(title = "CHANNELS", onAdd = onShowCreateChannel)
                    }

                    items(filteredChannels) { channel ->
                        ChannelItem(
                            channel = channel,
                            onClick = { onChannelClick(channel.id.orEmpty(), channel.name.orEmpty()) }
                        )
                    }

                    if (filteredChannels.isEmpty() && searchQuery.isBlank()) {
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
                        SectionHeader(title = "DIRECT MESSAGES", onAdd = { })
                    }

                    if (channelState.dms.isNotEmpty()) {
                        items(channelState.dms) { dm ->
                            DMItem(
                                name = dm.name.orEmpty(),
                                isAgent = true,
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
        }
    }
}

// Yellow Top Bar
@Composable
private fun NeoTopBar(
    serverName: String,
    serverInitial: String,
    onServerSelectorClick: () -> Unit,
    onNotificationClick: () -> Unit,
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
                modifier = Modifier.clickable(onClick = onServerSelectorClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .neoShadowSmall()
                        .background(Yellow)
                        .border(2.dp, Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = serverInitial,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Black
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = serverName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "\u25BC", fontSize = 12.sp, color = Black)
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoIconButton(icon = "\uD83D\uDD14", onClick = onNotificationClick)
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
private fun SectionHeader(title: String, onAdd: () -> Unit) {
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

// Channel Item
@Composable
private fun ChannelItem(channel: Channel, onClick: () -> Unit, unreadCount: Int = 0) {
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
            Text(
                text = "# ${channel.name.orEmpty()}",
                style = MaterialTheme.typography.titleSmall,
                color = if (unreadCount > 0) Black else Black,
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (channel.type.orEmpty().isNotBlank()) {
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
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isAgent) Orange else if (isOnline) Success else TextMuted)
                    .border(2.dp, White, CircleShape)
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
private fun SettingsNeoDialog(
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                NeoButton(
                    text = "LOG OUT",
                    onClick = onLogout,
                    containerColor = Pink,
                    contentColor = Black
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
