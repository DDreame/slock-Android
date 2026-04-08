package com.slock.app.data.api

import com.slock.app.data.local.SecureTokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val secureTokenStorage: SecureTokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for login and register endpoints
        val path = originalRequest.url.encodedPath
        if (path.contains("/auth/login") || path.contains("/auth/register") || path.contains("/auth/refresh")) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking {
            secureTokenStorage.accessToken
        }

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
