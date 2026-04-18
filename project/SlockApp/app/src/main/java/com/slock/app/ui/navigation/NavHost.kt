package com.slock.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
import androidx.activity.compose.BackHandler
import com.google.gson.Gson
import com.slock.app.data.model.Message
import com.slock.app.data.model.Server
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.service.SocketNotificationService
import com.slock.app.ui.auth.LoginScreen
import com.slock.app.ui.auth.RegisterScreen
import com.slock.app.ui.auth.ForgotPasswordScreen
import com.slock.app.ui.auth.AuthViewModel
import com.slock.app.ui.billing.BillingPlansScreen
import com.slock.app.ui.billing.BillingPlansViewModel
import com.slock.app.ui.home.HomeScreen
import com.slock.app.ui.server.ServerViewModel
import com.slock.app.ui.channel.ChannelViewModel
import com.slock.app.ui.channel.SavedChannelsScreen
import com.slock.app.ui.channel.SavedChannelsViewModel
import com.slock.app.ui.message.MessageListScreen
import com.slock.app.ui.message.MessageViewModel
import com.slock.app.ui.agent.AgentListScreen
import com.slock.app.ui.agent.AgentViewModel
import com.slock.app.ui.agent.AgentDetailScreen
import com.slock.app.ui.agent.AgentDetailViewModel
import com.slock.app.ui.member.MembersListScreen
import com.slock.app.ui.member.MembersViewModel
import com.slock.app.ui.machine.MachineListScreen
import com.slock.app.ui.machine.MachineViewModel
import com.slock.app.ui.task.TaskListScreen
import com.slock.app.ui.task.TaskViewModel
import com.slock.app.ui.task.ServerTasksScreen
import com.slock.app.ui.task.ServerTasksViewModel
import com.slock.app.ui.thread.ThreadReplyScreen
import com.slock.app.ui.thread.ThreadReplyViewModel
import com.slock.app.ui.thread.ThreadListScreen
import com.slock.app.ui.thread.ThreadListViewModel
import com.slock.app.ui.home.SearchViewModel
import com.slock.app.ui.settings.SettingsScreen
import com.slock.app.ui.settings.SettingsViewModel
import com.slock.app.ui.profile.ProfileScreen
import com.slock.app.ui.profile.ProfileViewModel
import com.slock.app.util.LogCollector

object Routes {
    private const val CONTEXT_ARG = "contextLabel"
    private const val SERVER_ID_ARG = "serverId"

    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val MESSAGES = "channel/{channelId}/messages?name={channelName}&context={$CONTEXT_ARG}"
    const val AGENT_LIST = "server/{serverId}/agents"
    const val THREAD_LIST = "server/{serverId}/threads"
    const val THREAD_REPLY = "thread/{threadChannelId}/reply/{parentMessageJson}?channelName={threadChannelName}&context={$CONTEXT_ARG}"
    const val TASK_LIST = "server/{serverId}/tasks"
    const val AGENT_DETAIL = "agent/{agentId}?context={$CONTEXT_ARG}&serverId={$SERVER_ID_ARG}"
    const val MACHINE_LIST = "server/{serverId}/machines"
    const val SETTINGS = "settings"
    const val BILLING_PLANS = "billing_plans"
    const val SAVED_CHANNELS = "saved_channels"
    const val PROFILE = "profile"
    const val USER_PROFILE = "profile/{userId}?context={$CONTEXT_ARG}"
    const val RELEASE_NOTES = "release_notes"

    private fun encodeQueryValue(value: String): String = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun encodePathSegment(value: String): String = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun buildContextLabel(vararg parts: String?): String? {
        val cleanedParts = parts.mapNotNull { part ->
            part?.trim()?.takeIf { it.isNotEmpty() }
        }
        return cleanedParts.joinToString(" · ").ifBlank { null }
    }

    fun messagesRoute(channelId: String, channelName: String, contextLabel: String? = null): String {
        val encodedChannelId = encodePathSegment(channelId)
        val encodedName = encodeQueryValue(channelName)
        val encodedContext = encodeQueryValue(contextLabel.orEmpty())
        return "channel/$encodedChannelId/messages?name=$encodedName&context=$encodedContext"
    }

    fun agentDetailRoute(agentId: String, contextLabel: String? = null, serverId: String? = null): String {
        val encodedContext = encodeQueryValue(contextLabel.orEmpty())
        val encodedServerId = encodeQueryValue(serverId.orEmpty())
        return "agent/$agentId?context=$encodedContext&serverId=$encodedServerId"
    }

    fun userProfileRoute(userId: String, contextLabel: String? = null): String {
        val encodedContext = encodeQueryValue(contextLabel.orEmpty())
        return "profile/$userId?context=$encodedContext"
    }

    fun threadReplyRoute(
        threadChannelId: String,
        parentMessageJson: String,
        channelName: String,
        contextLabel: String? = null
    ): String {
        val encodedChannelName = encodeQueryValue(channelName)
        val encodedContext = encodeQueryValue(contextLabel.orEmpty())
        return "thread/$threadChannelId/reply/$parentMessageJson?channelName=$encodedChannelName&context=$encodedContext"
    }

    fun agentListRoute(serverId: String) = "server/$serverId/agents"
    fun machineListRoute(serverId: String) = "server/$serverId/machines"
    fun dmMessagesRoute(channelId: String, channelName: String, contextLabel: String? = null): String =
        messagesRoute(channelId = channelId, channelName = channelName, contextLabel = contextLabel)
}

@Composable
fun SlockNavHost(
    navController: NavHostController = rememberNavController(),
    deepLinkChannelId: String? = null,
    deepLinkChannelName: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isSplashDone by remember { mutableStateOf(false) }

    // Observe auth expiry globally — redirect to login when token refresh fails
    val topAuthViewModel: AuthViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        topAuthViewModel.authExpired.collect {
            SocketNotificationService.stop(context)
            android.widget.Toast.makeText(context, "登录已过期，请重新登录", android.widget.Toast.LENGTH_LONG).show()
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Handle deep link from notification (warm start only — cold start is handled by splash)
    LaunchedEffect(deepLinkChannelId, isSplashDone) {
        if (shouldHandleWarmStartDeepLink(isSplashDone, deepLinkChannelId)) {
            val action = resolveWarmStartDeepLinkNav(deepLinkChannelId.orEmpty(), deepLinkChannelName)
            navController.navigate(action.route) {
                popUpTo(action.popUpToRoute) { inclusive = action.inclusive }
                launchSingleTop = action.singleTop
            }
            onDeepLinkConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // Splash — check token and redirect
        composable(Routes.SPLASH) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state.isCheckingSession, state.isLoggedIn) {
                if (!state.isCheckingSession) {
                    val result = resolveSplashNavigation(state.isLoggedIn, deepLinkChannelId, deepLinkChannelName)
                    navController.navigate(result.target) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                    if (result.deepLinkRoute != null) {
                        navController.navigate(result.deepLinkRoute) {
                            launchSingleTop = true
                        }
                        onDeepLinkConsumed()
                    }
                    isSplashDone = true
                }
            }
        }

        // Login Screen
        composable(Routes.LOGIN) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            LoginScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onLogin = viewModel::login,
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onLoginSuccess = {
                    SocketNotificationService.start(context)
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Register Screen
        composable(Routes.REGISTER) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            RegisterScreen(
                state = state,
                onNameChange = viewModel::onNameChange,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onRegister = viewModel::register,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    SocketNotificationService.start(context)
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Forgot Password Screen
        composable(Routes.FORGOT_PASSWORD) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            ForgotPasswordScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onSendReset = viewModel::forgotPassword,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Home Screen (merged Server + Channel list with embedded tabs)
        composable(Routes.HOME) {
            val serverViewModel: ServerViewModel = hiltViewModel()
            val channelViewModel: ChannelViewModel = hiltViewModel()
            val agentViewModel: AgentViewModel = hiltViewModel()
            val membersViewModel: MembersViewModel = hiltViewModel()
            val threadListViewModel: ThreadListViewModel = hiltViewModel()
            val serverTasksViewModel: ServerTasksViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val searchViewModel: SearchViewModel = hiltViewModel()

            val serverState by serverViewModel.state.collectAsState()
            val channelState by channelViewModel.state.collectAsState()
            val agentState by agentViewModel.state.collectAsState()
            val membersState by membersViewModel.state.collectAsState()
            val threadListState by threadListViewModel.state.collectAsState()
            val tasksState by serverTasksViewModel.state.collectAsState()
            val searchState by searchViewModel.state.collectAsState()
            val connectionState by serverViewModel.connectionState.collectAsState(
                initial = com.slock.app.data.socket.SocketIOManager.ConnectionState.CONNECTING
            )
            val context = androidx.compose.ui.platform.LocalContext.current

            BackHandler {
                (context as? android.app.Activity)?.finish()
            }

            LaunchedEffect(channelState.actionFeedbackMessage) {
                channelState.actionFeedbackMessage?.let { msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    channelViewModel.consumeActionFeedback()
                }
            }

            var selectedServer by remember { mutableStateOf<Server?>(null) }

            // Restore selected server from ViewModel (survives navigation)
            LaunchedEffect(serverState.servers) {
                if (serverState.servers.isNotEmpty()) {
                    val savedId = serverViewModel.selectedServerId
                    val toSelect = if (savedId != null) {
                        serverState.servers.find { it.id == savedId }
                    } else null
                    val server = toSelect ?: serverState.servers.first()
                    if (selectedServer?.id != server.id) {
                        selectedServer = server
                        serverViewModel.selectServer(server.id.orEmpty())
                        channelViewModel.loadChannels(server.id.orEmpty())
                        channelViewModel.loadDMs()
                        agentViewModel.loadAgents(server.id.orEmpty())
                        membersViewModel.loadMembers(server.id.orEmpty())
                        threadListViewModel.loadThreads(server.id.orEmpty())
                        serverTasksViewModel.loadAllTasks(server.id.orEmpty())
                    }
                }
            }

            HomeScreen(
                serverState = serverState,
                channelState = channelState,
                selectedServer = selectedServer,
                agentCount = agentState.agents.size,
                isAgentListLoading = agentState.isLoading,
                searchState = searchState,
                onSearchQueryChange = searchViewModel::onQueryChange,
                onClearSearch = searchViewModel::clearSearch,
                isConnected = connectionState == com.slock.app.data.socket.SocketIOManager.ConnectionState.CONNECTED,
                isReconnecting = connectionState == com.slock.app.data.socket.SocketIOManager.ConnectionState.CONNECTING,
                onServerSelect = { server ->
                    selectedServer = server
                    serverViewModel.selectServer(server.id.orEmpty())
                    channelViewModel.loadChannels(server.id.orEmpty())
                    channelViewModel.loadDMs()
                    agentViewModel.loadAgents(server.id.orEmpty())
                    membersViewModel.loadMembers(server.id.orEmpty())
                    threadListViewModel.loadThreads(server.id.orEmpty())
                    serverTasksViewModel.loadAllTasks(server.id.orEmpty())
                },
                onChannelClick = { channelId, channelName ->
                    navController.navigate(Routes.messagesRoute(channelId, channelName))
                },
                onDmClick = { channelId, channelName ->
                    navController.navigate(Routes.messagesRoute(channelId, channelName))
                },
                onCreateChannel = channelViewModel::createChannel,
                onCreateServer = serverViewModel::createServer,
                onOpenAgents = {
                    selectedServer?.id?.let { serverId ->
                        navController.navigate(Routes.agentListRoute(serverId))
                    }
                },
                onEditChannel = channelViewModel::updateChannel,
                onDeleteChannel = channelViewModel::deleteChannel,
                onLeaveChannel = channelViewModel::leaveChannel,
                onSearchMessageClick = { message ->
                    val channelId = message.channelId.orEmpty()
                    if (channelId.isNotEmpty()) {
                        val channelName = (channelState.channels + channelState.dms)
                            .firstOrNull { it.id == channelId }
                            ?.name
                            .orEmpty()
                            .ifBlank { channelId }
                        navController.navigate(
                            Routes.messagesRoute(
                                channelId = channelId,
                                channelName = channelName,
                                contextLabel = Routes.buildContextLabel(selectedServer?.name, "Search Results")
                            )
                        )
                    }
                },
                onSearchAgentClick = { agent ->
                    val agentId = agent.id.orEmpty()
                    if (agentId.isNotEmpty()) {
                        navController.navigate(
                            Routes.agentDetailRoute(
                                agentId = agentId,
                                contextLabel = Routes.buildContextLabel(selectedServer?.name, "Search Results"),
                                serverId = selectedServer?.id
                            )
                        )
                    }
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onTabSelected = { tab ->
                    when (tab) {
                        2 -> membersViewModel.retryIfEmpty()
                        3 -> serverTasksViewModel.retryIfEmpty()
                    }
                },
                members = membersState.members,
                onNewDmMemberSelected = { member ->
                    val agentId = if (member.isAgent) member.id else null
                    val userId = if (!member.isAgent) member.userId else null
                    channelViewModel.createDM(
                        agentId = agentId,
                        userId = userId,
                        onSuccess = { dmChannel ->
                            navController.navigate(
                                Routes.dmMessagesRoute(dmChannel.id.orEmpty(), member.name)
                            )
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, "Failed to create DM: $error", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                threadsContent = {
                    ThreadListScreen(
                        state = threadListState,
                        onThreadClick = { threadChannelId, parentMessage, channelName ->
                            val parentJson = Uri.encode(Gson().toJson(parentMessage))
                            navController.navigate(
                                Routes.threadReplyRoute(
                                    threadChannelId = threadChannelId,
                                    parentMessageJson = parentJson,
                                    channelName = channelName,
                                    contextLabel = Routes.buildContextLabel(selectedServer?.name, "Threads")
                                )
                            )
                        },
                        onNavigateBack = { },
                        onRetry = { selectedServer?.id?.let { threadListViewModel.loadThreads(it) } },
                        onTabSelected = { threadListViewModel.selectTab(it) },
                        onMarkDone = { threadListViewModel.markThreadDone(it) },
                        onUndoDone = { threadListViewModel.undoThreadDone(it) },
                        onUnfollow = { threadListViewModel.unfollowThread(it) },
                        showHeader = false
                    )
                },
                membersContent = {
                    MembersListScreen(
                        state = membersState,
                        onMemberClick = { member ->
                            val contextLabel = Routes.buildContextLabel(selectedServer?.name, "Members")
                            if (member.isAgent && member.id.isNotBlank()) {
                                navController.navigate(
                                    Routes.agentDetailRoute(
                                        agentId = member.id,
                                        contextLabel = contextLabel,
                                        serverId = selectedServer?.id
                                    )
                                )
                            } else if (!member.isAgent && !member.userId.isNullOrBlank()) {
                                navController.navigate(Routes.userProfileRoute(member.userId, contextLabel))
                            }
                        },
                        onNavigateBack = { },
                        onRetry = { selectedServer?.id?.let { membersViewModel.loadMembers(it) } },
                        showHeader = false
                    )
                },
                tasksContent = {
                    ServerTasksScreen(
                        state = tasksState,
                        onToggleGroup = serverTasksViewModel::toggleGroup,
                        onUpdateStatus = serverTasksViewModel::updateTaskStatus,
                        onDeleteTask = serverTasksViewModel::deleteTask,
                        onNavigateBack = { },
                        onRetry = { selectedServer?.id?.let { serverTasksViewModel.loadAllTasks(it) } },
                        showHeader = false
                    )
                }
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            SettingsScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onNotificationPreferenceChange = viewModel::updateNotificationPreference,
                onRefreshAccount = { viewModel.refreshAccount() },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenBillingPlans = { navController.navigate(Routes.BILLING_PLANS) },
                onOpenSavedChannels = { navController.navigate(Routes.SAVED_CHANNELS) },
                onOpenReleaseNotes = { navController.navigate(Routes.RELEASE_NOTES) },
                onSendFeedback = { LogCollector.shareReport(context) },
                onLogout = {
                    authViewModel.logout {
                        SocketNotificationService.stop(context)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.BILLING_PLANS) {
            val viewModel: BillingPlansViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            BillingPlansScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onRetry = viewModel::loadBillingPlan
            )
        }

        // Own Profile Screen
        composable(Routes.PROFILE) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            ProfileScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onStartEditing = viewModel::startEditing,
                onCancelEditing = viewModel::cancelEditing,
                onEditNameChange = viewModel::updateEditName,
                onSaveName = viewModel::saveName,
                onLogout = {
                    authViewModel.logout {
                        SocketNotificationService.stop(context)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onRetry = viewModel::retry
            )
        }

        // Other User Profile Screen
        composable(
            Routes.USER_PROFILE,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("contextLabel") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val viewModel: ProfileViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val contextLabel = backStackEntry.arguments?.getString("contextLabel").orEmpty()

            ProfileScreen(
                state = state,
                contextLabel = contextLabel,
                onNavigateBack = { navController.popBackStack() },
                onRetry = viewModel::retry
            )
        }

        // Message List Screen
        composable(
            Routes.MESSAGES,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType },
                navArgument("channelName") { type = NavType.StringType; defaultValue = "" },
                navArgument("contextLabel") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
            val contextLabel = backStackEntry.arguments?.getString("contextLabel").orEmpty()
            val viewModel: MessageViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val channelVM: ChannelViewModel = run {
                val homeEntry = try { navController.getBackStackEntry(Routes.HOME) } catch (_: Exception) { null }
                if (homeEntry != null) hiltViewModel(homeEntry) else hiltViewModel()
            }
            val channelAgents by channelVM.channelAgents.collectAsState()
            val channelState by channelVM.state.collectAsState()

            LaunchedEffect(channelId) {
                viewModel.loadMessages(channelId)
                channelVM.loadChannelAgents(channelId)
                channelVM.clearUnreadCount(channelId)
            }

            DisposableEffect(channelId) {
                onDispose { channelVM.clearCurrentChannel() }
            }

            MessageListScreen(
                channelName = channelName.ifBlank { channelId },
                contextLabel = contextLabel,
                state = state,
                onSendMessage = viewModel::sendMessage,
                onLoadMore = viewModel::loadMoreMessages,
                onRetryLoad = viewModel::retryLoadMessages,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToThread = { threadChannelId, parentMessage ->
                    val parentJson = Uri.encode(Gson().toJson(parentMessage))
                    navController.navigate(
                        Routes.threadReplyRoute(
                            threadChannelId = threadChannelId,
                            parentMessageJson = parentJson,
                            channelName = channelName.ifBlank { channelId },
                            contextLabel = Routes.buildContextLabel("# ${channelName.ifBlank { channelId }}", "Messages")
                        )
                    )
                },
                onReplyTo = viewModel::setReplyTo,
                onClearReply = viewModel::clearReplyTo,
                onAddAttachment = viewModel::addAttachment,
                onRemoveAttachment = viewModel::removeAttachment,
                onImageClick = viewModel::showImagePreview,
                onDismissPreview = viewModel::dismissImagePreview,
                onToggleSearch = viewModel::toggleSearch,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onNextSearchResult = viewModel::nextSearchResult,
                onPreviousSearchResult = viewModel::previousSearchResult,
                onToggleReaction = viewModel::toggleReaction,
                onToggleSavedChannel = viewModel::toggleSavedChannel,
                onSavedChannelFeedbackShown = viewModel::consumeSavedChannelFeedback,
                onSendErrorShown = viewModel::dismissSendError,
                channelAgents = channelAgents,
                channelName_raw = channelName.ifBlank { channelId },
                onStopAllAgents = { onSuccess, onError -> channelVM.stopAllChannelAgents(onSuccess, onError) },
                onResumeAllAgents = { prompt, onSuccess, onError -> channelVM.resumeAllChannelAgents(prompt, onSuccess, onError) },
                onShowConvertToTask = viewModel::showConvertToTask,
                onDismissConvertToTask = viewModel::dismissConvertToTask,
                onConvertTaskStatusChange = viewModel::updateConvertToTaskStatus,
                onSubmitConvertToTask = viewModel::submitConvertToTask,
                onConvertTaskFeedbackShown = viewModel::consumeConvertTaskFeedback,
                onMarkAsRead = { channelVM.markAsRead(channelId) },
                onMarkAsUnread = { channelVM.markAsUnread(channelId) },
                markReadFeedbackMessage = channelState.actionFeedbackMessage,
                onMarkReadFeedbackShown = channelVM::consumeActionFeedback
            )
        }

        composable(Routes.SAVED_CHANNELS) {
            val viewModel: SavedChannelsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.loadSavedChannels()
            }

            SavedChannelsScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onOpenChannel = { channel ->
                    val channelId = channel.id.orEmpty()
                    if (channelId.isNotEmpty()) {
                        navController.navigate(
                            Routes.messagesRoute(
                                channelId = channelId,
                                channelName = channel.name.orEmpty().ifBlank { channelId },
                                contextLabel = Routes.buildContextLabel("Saved Channels")
                            )
                        )
                    }
                },
                onRemoveSavedChannel = viewModel::removeSavedChannel,
                onRetry = viewModel::loadSavedChannels,
                onFeedbackShown = viewModel::consumeFeedback
            )
        }

        composable(Routes.RELEASE_NOTES) {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.slock.app.ui.release.ReleaseNotesViewModel>()
            val state by viewModel.state.collectAsState()

            com.slock.app.ui.release.ReleaseNotesScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Agent List Screen
        composable(Routes.AGENT_LIST) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
            val viewModel: AgentViewModel = hiltViewModel()
            val channelVM: ChannelViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            LaunchedEffect(serverId) {
                viewModel.loadAgents(serverId)
                channelVM.ensureDMsLoaded()
            }

            AgentListScreen(
                state = state,
                onCreateAgent = { name, desc, prompt, model, runtime, reasoningEffort, envVars ->
                    viewModel.createAgent(name, desc, prompt, model, runtime, reasoningEffort, envVars)
                },
                onStartAgent = viewModel::startAgent,
                onStopAgent = viewModel::stopAgent,
                onResetAgent = viewModel::resetAgent,
                onDeleteAgent = viewModel::deleteAgent,
                onUpdateAgent = { agentId, name, desc, prompt, runtime, reasoningEffort, envVars ->
                    viewModel.updateAgent(agentId, name, desc, prompt, runtime, reasoningEffort, envVars)
                },
                onDmAgent = { agentId ->
                    channelVM.createDM(
                        agentId = agentId,
                        onSuccess = { dmChannel ->
                            val agentName = state.agents.find { it.id == agentId }?.name ?: "DM"
                            navController.navigate(Routes.dmMessagesRoute(dmChannel.id.orEmpty(), agentName))
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, "Failed to create DM: $error", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onAgentClick = { agentId ->
                    navController.navigate(
                        Routes.agentDetailRoute(
                            agentId = agentId,
                            serverId = serverId
                        )
                    )
                },
                onConsumeCreateFeedback = viewModel::consumeCreateFeedback,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMachines = { navController.navigate(Routes.machineListRoute(serverId)) },
                onRetry = { viewModel.loadAgents(serverId) }
            )
        }

        // Thread List Screen
        composable(Routes.THREAD_LIST) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
            val viewModel: ThreadListViewModel = hiltViewModel()
            val serverVM: ServerViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val serverState by serverVM.state.collectAsState()

            LaunchedEffect(serverId) {
                viewModel.loadThreads(serverId)
            }

            ThreadListScreen(
                state = state,
                onThreadClick = { threadChannelId, parentMessage, channelName ->
                    val parentJson = Uri.encode(Gson().toJson(parentMessage))
                    val serverName = resolveThreadListServerName(
                        serverId,
                        serverState.servers.find { it.id == serverId }?.name
                    )
                    navController.navigate(
                        Routes.threadReplyRoute(
                            threadChannelId = threadChannelId,
                            parentMessageJson = parentJson,
                            channelName = channelName,
                            contextLabel = Routes.buildContextLabel(serverName, "Threads")
                        )
                    )
                },
                onNavigateBack = { navController.popBackStack() },
                onRetry = { viewModel.loadThreads(serverId) },
                onTabSelected = { viewModel.selectTab(it) },
                onMarkDone = { viewModel.markThreadDone(it) },
                onUndoDone = { viewModel.undoThreadDone(it) },
                onUnfollow = { viewModel.unfollowThread(it) }
            )
        }

        // Task List Screen (server-wide: aggregates tasks from all channels)
        composable(Routes.TASK_LIST) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
            val viewModel: ServerTasksViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(serverId) {
                viewModel.loadAllTasks(serverId)
            }

            ServerTasksScreen(
                state = state,
                onToggleGroup = viewModel::toggleGroup,
                onUpdateStatus = viewModel::updateTaskStatus,
                onDeleteTask = viewModel::deleteTask,
                onNavigateBack = { navController.popBackStack() },
                onRetry = { viewModel.loadAllTasks(serverId) }
            )
        }

        // Thread Reply Screen
        composable(
            Routes.THREAD_REPLY,
            arguments = listOf(
                navArgument("threadChannelId") { type = NavType.StringType },
                navArgument("parentMessageJson") { type = NavType.StringType },
                navArgument("threadChannelName") { type = NavType.StringType; defaultValue = "" },
                navArgument("contextLabel") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val threadChannelId = backStackEntry.arguments?.getString("threadChannelId") ?: return@composable
            val parentMessageJson = backStackEntry.arguments?.getString("parentMessageJson") ?: return@composable
            val threadChannelName = backStackEntry.arguments?.getString("threadChannelName") ?: ""
            val contextLabel = backStackEntry.arguments?.getString("contextLabel").orEmpty()
            val parentMessage = Gson().fromJson(Uri.decode(parentMessageJson), Message::class.java)
            val viewModel: ThreadReplyViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(threadChannelId) {
                viewModel.loadThread(parentMessage, threadChannelId)
            }

            ThreadReplyScreen(
                channelName = threadChannelName.ifBlank { threadChannelId },
                contextLabel = contextLabel,
                state = state,
                onSendReply = viewModel::sendReply,
                onLoadMore = viewModel::loadMoreReplies,
                onNavigateBack = { navController.popBackStack() },
                onFollowThread = viewModel::followThread,
                onUnfollowThread = viewModel::unfollowThread,
                onMarkDone = {
                    viewModel.markThreadDone()
                    navController.popBackStack()
                }
            )
        }

        // Agent Detail Screen
        composable(
            Routes.AGENT_DETAIL,
            arguments = listOf(
                navArgument("agentId") { type = NavType.StringType },
                navArgument("contextLabel") { type = NavType.StringType; defaultValue = "" },
                navArgument("serverId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val viewModel: AgentDetailViewModel = hiltViewModel()
            val channelVM: ChannelViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            val contextLabel = backStackEntry.arguments?.getString("contextLabel").orEmpty()

            LaunchedEffect(Unit) {
                channelVM.ensureDMsLoaded()
            }

            AgentDetailScreen(
                state = state,
                contextLabel = contextLabel,
                onNavigateBack = { navController.popBackStack() },
                onStartAgent = viewModel::startAgent,
                onStopAgent = viewModel::stopAgent,
                onDmClick = {
                    val agentId = state.agent?.id ?: return@AgentDetailScreen
                    channelVM.createDM(
                        agentId = agentId,
                        onSuccess = { dmChannel ->
                            val agentName = state.agent?.name ?: "DM"
                            navController.navigate(Routes.dmMessagesRoute(dmChannel.id.orEmpty(), agentName, contextLabel))
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, "Failed to create DM: $error", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onMachineClick = {
                    val serverId = viewModel.serverId.orEmpty()
                    resolveAgentToMachineNav(serverId)?.let { action ->
                        navController.navigate(action.route) {
                            popUpTo(action.popUpToRoute) { inclusive = action.inclusive }
                        }
                    } ?: android.widget.Toast.makeText(
                        context,
                        "Server context unavailable",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onSelectTab = viewModel::selectTab,
                onRetry = viewModel::retry,
                onResetAgent = viewModel::resetAgent,
                onConsumeResetFeedback = viewModel::consumeResetFeedback,
                onUpdateAgent = viewModel::updateAgent,
                onConsumeUpdateFeedback = viewModel::consumeUpdateFeedback
            )
        }

        // Machine List Screen
        composable(
            Routes.MACHINE_LIST,
            arguments = listOf(
                navArgument("serverId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
            val viewModel: MachineViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(serverId) {
                viewModel.loadMachines(serverId)
            }

            MachineListScreen(
                state = state,
                onDeleteMachine = viewModel::deleteMachine,
                onAgentClick = { agentId ->
                    resolveMachineToAgentNav(agentId, serverId)?.let { action ->
                        navController.navigate(action.route) {
                            popUpTo(action.popUpToRoute) { inclusive = action.inclusive }
                        }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onRetry = { viewModel.loadMachines(serverId) }
            )
        }
    }
}
