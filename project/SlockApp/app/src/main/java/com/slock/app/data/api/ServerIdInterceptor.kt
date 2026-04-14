package com.slock.app.data.api

import android.util.Log
import com.slock.app.data.local.ActiveServerHolder
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that automatically adds X-Server-Id header
 * to server-scoped API requests. Reads the current server ID
 * from ActiveServerHolder.
 *
 * Skips auth and server-level endpoints.
 */
class ServerIdInterceptor @Inject constructor(
    private val activeServerHolder: ActiveServerHolder
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip auth and server-level endpoints that don't need X-Server-Id
        if (path.contains("/auth/") || path.contains("/servers")) {
            return chain.proceed(originalRequest)
        }

        val serverId = activeServerHolder.serverId
        if (serverId == null) {
            Log.w("ServerIdInterceptor", "serverId is NULL for path=$path — request will lack X-Server-Id header")
        }
        val newRequest = if (serverId != null) {
            originalRequest.newBuilder()
                .header("X-Server-Id", serverId)
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
