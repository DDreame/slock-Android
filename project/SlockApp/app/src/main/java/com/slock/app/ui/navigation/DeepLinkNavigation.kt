package com.slock.app.ui.navigation

data class SplashNavResult(
    val target: String,
    val deepLinkRoute: String?
)

fun resolveSplashNavigation(
    isLoggedIn: Boolean,
    deepLinkChannelId: String?,
    deepLinkChannelName: String?
): SplashNavResult {
    val target = if (isLoggedIn) Routes.HOME else Routes.LOGIN
    val deepLinkRoute = if (isLoggedIn && !deepLinkChannelId.isNullOrBlank()) {
        Routes.messagesRoute(
            channelId = deepLinkChannelId,
            channelName = deepLinkChannelName ?: ""
        )
    } else null
    return SplashNavResult(target, deepLinkRoute)
}

fun shouldHandleWarmStartDeepLink(
    isSplashDone: Boolean,
    deepLinkChannelId: String?
): Boolean {
    return isSplashDone && !deepLinkChannelId.isNullOrBlank()
}

data class CrossNavAction(
    val route: String,
    val popUpToRoute: String,
    val inclusive: Boolean
)

fun resolveAgentDetailServerId(routeServerId: String?, activeServerId: String?): String? {
    return routeServerId?.trim()?.takeIf { it.isNotEmpty() }
        ?: activeServerId?.trim()?.takeIf { it.isNotEmpty() }
}

fun resolveAgentToMachineNav(serverId: String): CrossNavAction? {
    if (serverId.isBlank()) return null
    return CrossNavAction(
        route = Routes.machineListRoute(serverId),
        popUpToRoute = Routes.AGENT_DETAIL,
        inclusive = true
    )
}

fun resolveMachineToAgentNav(agentId: String, serverId: String? = null): CrossNavAction? {
    if (agentId.isBlank()) return null
    return CrossNavAction(
        route = Routes.agentDetailRoute(agentId = agentId, serverId = serverId),
        popUpToRoute = Routes.MACHINE_LIST,
        inclusive = true
    )
}

data class WarmDeepLinkNavAction(
    val route: String,
    val popUpToRoute: String,
    val inclusive: Boolean,
    val singleTop: Boolean
)

fun resolveWarmStartDeepLinkNav(channelId: String, channelName: String?): WarmDeepLinkNavAction {
    return WarmDeepLinkNavAction(
        route = Routes.messagesRoute(channelId, channelName ?: ""),
        popUpToRoute = Routes.HOME,
        inclusive = false,
        singleTop = true
    )
}

fun resolveThreadListServerName(serverId: String, serverName: String?): String {
    return serverName?.takeIf { it.isNotBlank() } ?: serverId
}
