package com.slock.app.data.local

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveServerHolder @Inject constructor(
    private val secureTokenStorage: SecureTokenStorage
) {
    @Volatile
    var serverId: String? = secureTokenStorage.serverId
        set(value) {
            field = value
            secureTokenStorage.serverId = value
        }
}
