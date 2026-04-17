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
