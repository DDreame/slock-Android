package com.slock.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.slock.app.ui.auth.LoginScreen
import com.slock.app.ui.auth.RegisterScreen
import com.slock.app.ui.auth.ForgotPasswordScreen
import com.slock.app.ui.auth.AuthViewModel
import com.slock.app.ui.server.ServerListScreen
import com.slock.app.ui.server.ServerViewModel
import com.slock.app.ui.channel.ChannelListScreen
import com.slock.app.ui.channel.ChannelViewModel
import com.slock.app.ui.message.MessageListScreen
import com.slock.app.ui.message.MessageViewModel
import com.slock.app.ui.agent.AgentListScreen
import com.slock.app.ui.agent.AgentViewModel
import com.slock.app.ui.task.TaskListScreen
import com.slock.app.ui.task.TaskViewModel
import com.slock.app.ui.thread.ThreadReplyScreen
import com.slock.app.ui.thread.ThreadReplyViewModel

/**
 * Navigation routes
 */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val SERVER_LIST = "servers"
    const val CHANNEL_LIST = "server/{serverId}/channels"
    const val MESSAGES = "channel/{channelId}/messages"
    const val AGENT_LIST = "agents"
    const val THREAD_REPLY = "thread/{threadChannelId}/reply/{parentMessageJson}"
    const val TASK_LIST = "channel/{channelId}/tasks"
}

/**
 * Main Navigation Host
 */
@Composable
fun SlockNavHost(
    navController: NavHostController = rememberNavController()
) {
    val startDestination = Routes.LOGIN
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable(Routes.LOGIN) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            
            LoginScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onLogin = viewModel::login,
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onLoginSuccess = {
                    navController.navigate(Routes.SERVER_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        // Register Screen
        composable(Routes.REGISTER) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            
            RegisterScreen(
                state = state,
                onNameChange = viewModel::onNameChange,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onRegister = viewModel::register,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Routes.SERVER_LIST) {
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
        
        // Server List Screen
        composable(Routes.SERVER_LIST) {
            val viewModel: ServerViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            
            ServerListScreen(
                state = state,
                onCreateServer = viewModel::createServer,
                onDeleteServer = viewModel::deleteServer,
                onServerClick = { serverId ->
                    navController.navigate("server/$serverId/channels")
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToAgents = {
                    navController.navigate(Routes.AGENT_LIST)
                }
            )
        }
        
        // Channel List Screen
        composable(Routes.CHANNEL_LIST) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
            val viewModel: ChannelViewModel = hiltViewModel()
            val agentViewModel: AgentViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            // Load channels when entering
            androidx.compose.runtime.LaunchedEffect(serverId) {
                viewModel.loadChannels(serverId)
            }

            ChannelListScreen(
                serverId = serverId,
                state = state,
                onCreateChannel = viewModel::createChannel,
                onCreateAgent = agentViewModel::createAgent,
                onChannelClick = { channelId ->
                    navController.navigate("channel/$channelId/messages")
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAgents = {
                    navController.navigate(Routes.AGENT_LIST)
                }
            )
        }
        
        // Message List Screen
        composable(Routes.MESSAGES) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
            val viewModel: MessageViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            
            // Load messages when entering
            androidx.compose.runtime.LaunchedEffect(channelId) {
                viewModel.loadMessages(channelId)
            }
            
            MessageListScreen(
                channelId = channelId,
                state = state,
                onSendMessage = viewModel::sendMessage,
                onLoadMore = viewModel::loadMoreMessages,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToThread = { threadChannelId, parentMessage ->
                    val parentJson = Uri.encode(com.google.gson.Gson().toJson(parentMessage))
                    navController.navigate("thread/$threadChannelId/reply/$parentJson")
                }
            )
        }
        
        // Agent List Screen
        composable(Routes.AGENT_LIST) {
            val viewModel: AgentViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            AgentListScreen(
                state = state,
                onCreateAgent = viewModel::createAgent,
                onStartAgent = viewModel::startAgent,
                onStopAgent = viewModel::stopAgent,
                onResetAgent = viewModel::resetAgent,
                onDeleteAgent = viewModel::deleteAgent,
                onAgentClick = { /* Navigate to agent detail */ },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Task List Screen
        composable(Routes.TASK_LIST) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
            val viewModel: TaskViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            androidx.compose.runtime.LaunchedEffect(channelId) {
                viewModel.loadTasks(channelId)
            }

            TaskListScreen(
                channelId = channelId,
                state = state,
                onCreateTask = viewModel::createTask,
                onUpdateStatus = viewModel::updateTaskStatus,
                onDeleteTask = viewModel::deleteTask,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Thread Reply Screen
        composable(
            Routes.THREAD_REPLY,
            arguments = listOf(
                navArgument("threadChannelId") { type = NavType.StringType },
                navArgument("parentMessageJson") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val threadChannelId = backStackEntry.arguments?.getString("threadChannelId") ?: return@composable
            val parentMessageJson = backStackEntry.arguments?.getString("parentMessageJson") ?: return@composable
            val parentMessage = Gson().fromJson(Uri.decode(parentMessageJson), Message::class.java)
            val viewModel: ThreadReplyViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            androidx.compose.runtime.LaunchedEffect(threadChannelId) {
                viewModel.loadThread(parentMessage, threadChannelId)
            }

            ThreadReplyScreen(
                state = state,
                onSendReply = viewModel::sendReply,
                onLoadMore = viewModel::loadMoreReplies,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
