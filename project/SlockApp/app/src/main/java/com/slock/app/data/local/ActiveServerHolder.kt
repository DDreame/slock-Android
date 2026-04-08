package com.slock.app.data.local

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveServerHolder @Inject constructor() {
    @Volatile
    var serverId: String? = null
}
