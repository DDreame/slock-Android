package com.slock.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object Home : Screen("home")
    data object AgentDetail : Screen("agent/{agentId}") {
        fun createRoute(agentId: String) = "agent/$agentId"
    }
    data object Profile : Screen("profile")
    data object UserProfile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    data object Settings : Screen("settings")
}
