package com.slock.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object Home : Screen("home")
    data object Servers : Screen("servers")
    data object ServerDetail : Screen("server/{serverId}") {
        fun createRoute(serverId: String) = "server/$serverId"
    }
    data object Channel : Screen("channel/{channelId}") {
        fun createRoute(channelId: String) = "channel/$channelId"
    }
    data object Messages : Screen("messages/{channelId}") {
        fun createRoute(channelId: String) = "messages/$channelId"
    }
    data object Agents : Screen("agents")
    data object AgentDetail : Screen("agent/{agentId}") {
        fun createRoute(agentId: String) = "agent/$agentId"
    }
    data object Profile : Screen("profile")
    data object UserProfile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Default.Home, "Home"),
    BottomNavItem(Screen.Servers, Icons.Default.Forum, "Servers"),
    BottomNavItem(Screen.Agents, Icons.Default.Chat, "Agents"),
    BottomNavItem(Screen.Profile, Icons.Default.Person, "Profile")
)
