package com.slock.app.ui.navigation

import androidx.compose.runtime.Composable
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
import com.google.gson.Gson
import com.slock.app.data.model.Message
import com.slock.app.data.model.Server
import com.slock.app.service.SocketNotificationService
import com.slock.app.ui.auth.LoginScreen
import com.slock.app.ui.auth.RegisterScreen
import com.slock.app.ui.auth.ForgotPasswordScreen
import com.slock.app.ui.auth.AuthViewModel
import com.slock.app.ui.home.HomeScreen
import com.slock.app.ui.server.ServerViewModel
import com.slock.app.ui.channel.ChannelViewModel
import com.slock.app.ui.message.MessageListScreen
import com.slock.app.ui.message.MessageViewModel
import com.slock.app.ui.agent.AgentListScreen
import com.slock.app.ui.agent.AgentViewModel
import com.slock.app.ui.task.TaskListScreen
import com.slock.app.ui.task.TaskViewModel
import com.slock.app.ui.task.ServerTasksScreen
import com.slock.app.ui.task.ServerTasksViewModel
import com.slock.app.ui.thread.ThreadReplyScreen
import com.slock.app.ui.thread.ThreadReplyViewModel
import com.slock.app.ui.thread.ThreadListScreen
import com.slock.app.ui.thread.ThreadListViewModel

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val MESSAGES = "channel/{channelId}/messages?name={channelName}"
    const val AGENT_LIST = "server/{serverId}/agents"
    const val THREAD_LIST = "server/{serverId}/threads"
    const val THREAD_REPLY = "thread/{threadChannelId}/reply/{parentMessageJson}?channelName={threadChannelName}"
    const val TASK_LIST = "server/{serverId}/tasks"
}

@Composable
fun SlockNavHost(
    navController: NavHostController = rememberNavController(),
    deepLinkChannelId: String? = null,
    deepLinkChannelName: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    // Handle deep link from notification
    LaunchedEffect(deepLinkChannelId) {
        if (!deepLinkChannelId.isNullOrBlank()) {
            val encodedName = Uri.encode(deepLinkChannelName ?: "")
            navController.navigate("channel/$deepLinkChannelId/messages?name=$encodedName") {
                launchSingleTop = true
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
                    val target = if (state.isLoggedIn) Routes.HOME else Routes.LOGIN
                    navController.navigate(target) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
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
                },
                onJoinWithInvite = { link ->
                    // TODO: Implement invite link joining via API
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
            val threadListViewModel: ThreadListViewModel = hiltViewModel()
            val serverTasksViewModel: ServerTasksViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()

            val serverState by serverViewModel.state.collectAsState()
            val channelState by channelViewModel.state.collectAsState()
            val agentState by agentViewModel.state.collectAsState()
            val threadListState by threadListViewModel.state.collectAsState()
            val tasksState by serverTasksViewModel.state.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

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
                        serverViewModel.selectServer(server.id)
                        channelViewModel.loadChannels(server.id)
                        channelViewModel.loadDMs()
                        agentViewModel.loadAgents(server.id)
                        threadListViewModel.loadThreads(server.id)
                        serverTasksViewModel.loadAllTasks(server.id)
                    }
                }
            }

            HomeScreen(
                serverState = serverState,
                channelState = channelState,
                selectedServer = selectedServer,
                onServerSelect = { server ->
                    selectedServer = server
                    serverViewModel.selectServer(server.id)
                    channelViewModel.loadChannels(server.id)
                    channelViewModel.loadDMs()
                    agentViewModel.loadAgents(server.id)
                    threadListViewModel.loadThreads(server.id)
                    serverTasksViewModel.loadAllTasks(server.id)
                },
                onChannelClick = { channelId, channelName ->
                    val encodedName = Uri.encode(channelName)
                    navController.navigate("channel/$channelId/messages?name=$encodedName")
                },
                onDmClick = { channelId, channelName ->
                    val encodedName = Uri.encode(channelName)
                    navController.navigate("channel/$channelId/messages?name=$encodedName")
                },
                onCreateChannel = channelViewModel::createChannel,
                onCreateServer = serverViewModel::createServer,
                onLogout = {
                    authViewModel.logout {
                        SocketNotificationService.stop(context)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onTabSelected = { tab ->
                    when (tab) {
                        2 -> agentViewModel.retryIfEmpty()
                        3 -> serverTasksViewModel.retryIfEmpty()
                    }
                },
                threadsContent = {
                    ThreadListScreen(
                        state = threadListState,
                        onThreadClick = { threadChannelId, parentMessage, channelName ->
                            val parentJson = Uri.encode(Gson().toJson(parentMessage))
                            val encodedChName = Uri.encode(channelName)
                            navController.navigate("thread/$threadChannelId/reply/$parentJson?channelName=$encodedChName")
                        },
                        onNavigateBack = { },
                        onRetry = { selectedServer?.id?.let { threadListViewModel.loadThreads(it) } },
                        showHeader = false
                    )
                },
                agentsContent = {
                    AgentListScreen(
                        state = agentState,
                        onCreateAgent = { name, desc, prompt, model -> agentViewModel.createAgent(name, desc, prompt, model) },
                        onStartAgent = agentViewModel::startAgent,
                        onStopAgent = agentViewModel::stopAgent,
                        onResetAgent = agentViewModel::resetAgent,
                        onDeleteAgent = agentViewModel::deleteAgent,
                        onUpdateAgent = { agentId, name, desc, prompt -> agentViewModel.updateAgent(agentId, name, desc, prompt) },
                        onDmAgent = { agentId ->
                            channelViewModel.createDM(
                                agentId = agentId,
                                onSuccess = { dmChannel ->
                                    val agentName = agentState.agents.find { it.id == agentId }?.name ?: "DM"
                                    val encodedName = Uri.encode(agentName)
                                    navController.navigate("channel/${dmChannel.id}/messages?name=$encodedName")
                                },
                                onError = { error ->
                                    android.widget.Toast.makeText(context, "Failed to create DM: $error", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        onAgentClick = { },
                        onNavigateBack = { },
                        onRetry = { selectedServer?.id?.let { agentViewModel.loadAgents(it) } },
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

        // Message List Screen
        composable(
            Routes.MESSAGES,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType },
                navArgument("channelName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
            val viewModel: MessageViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(channelId) {
                viewModel.loadMessages(channelId)
            }

            MessageListScreen(
                channelName = channelName.ifBlank { channelId },
                state = state,
                onSendMessage = viewModel::sendMessage,
                onLoadMore = viewModel::loadMoreMessages,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToThread = { threadChannelId, parentMessage ->
                    val parentJson = Uri.encode(Gson().toJson(parentMessage))
                    val encodedChName = Uri.encode(channelName.ifBlank { channelId })
                    navController.navigate("thread/$threadChannelId/reply/$parentJson?channelName=$encodedChName")
                }
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
            }

            AgentListScreen(
                state = state,
                onCreateAgent = { name, desc, prompt, model -> viewModel.createAgent(name, desc, prompt, model) },
                onStartAgent = viewModel::startAgent,
                onStopAgent = viewModel::stopAgent,
                onResetAgent = viewModel::resetAgent,
                onDeleteAgent = viewModel::deleteAgent,
                onUpdateAgent = { agentId, name, desc, prompt -> viewModel.updateAgent(agentId, name, desc, prompt) },
                onDmAgent = { agentId ->
                    channelVM.createDM(
                        agentId = agentId,
                        onSuccess = { dmChannel ->
                            val agentName = state.agents.find { it.id == agentId }?.name ?: "DM"
                            val encodedName = Uri.encode(agentName)
                            navController.navigate("channel/${dmChannel.id}/messages?name=$encodedName")
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, "Failed to create DM: $error", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onAgentClick = { },
                onNavigateBack = { navController.popBackStack() },
                onRetry = { viewModel.loadAgents(serverId) }
            )
        }

        // Thread List Screen
        composable(Routes.THREAD_LIST) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
            val viewModel: ThreadListViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(serverId) {
                viewModel.loadThreads(serverId)
            }

            ThreadListScreen(
                state = state,
                onThreadClick = { threadChannelId, parentMessage, channelName ->
                    val parentJson = Uri.encode(Gson().toJson(parentMessage))
                    val encodedChName = Uri.encode(channelName)
                    navController.navigate("thread/$threadChannelId/reply/$parentJson?channelName=$encodedChName")
                },
                onNavigateBack = { navController.popBackStack() },
                onRetry = { viewModel.loadThreads(serverId) }
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
                navArgument("threadChannelName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val threadChannelId = backStackEntry.arguments?.getString("threadChannelId") ?: return@composable
            val parentMessageJson = backStackEntry.arguments?.getString("parentMessageJson") ?: return@composable
            val threadChannelName = backStackEntry.arguments?.getString("threadChannelName") ?: ""
            val parentMessage = Gson().fromJson(Uri.decode(parentMessageJson), Message::class.java)
            val viewModel: ThreadReplyViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(threadChannelId) {
                viewModel.loadThread(parentMessage, threadChannelId)
            }

            ThreadReplyScreen(
                channelName = threadChannelName.ifBlank { threadChannelId },
                state = state,
                onSendReply = viewModel::sendReply,
                onLoadMore = viewModel::loadMoreReplies,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
