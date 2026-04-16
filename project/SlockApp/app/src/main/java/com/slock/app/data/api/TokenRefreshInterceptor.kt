package com.slock.app.data.api

import com.slock.app.data.local.SecureTokenStorage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import com.google.gson.Gson
import com.slock.app.data.model.AuthResponse
import javax.inject.Inject

class TokenRefreshInterceptor @Inject constructor(
    private val secureTokenStorage: SecureTokenStorage
) : Interceptor {

    companion object {
        private const val BASE_URL = "https://api.slock.ai/api/"
    }

    private val client = OkHttpClient()
    private val gson = Gson()
    private val refreshMutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        // Skip refresh endpoint to avoid infinite loop
        if (path.contains("/auth/refresh")) {
            return chain.proceed(chain.request())
        }

        val originalResponse = chain.proceed(chain.request())

        if (originalResponse.code == 401) {
            originalResponse.close()

            val refreshed = tryRefreshToken()
            if (refreshed) {
                val newToken = runBlocking {
                    secureTokenStorage.accessToken
                }
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(newRequest)
            } else {
                // Refresh failed — token expired, trigger logout
                secureTokenStorage.notifyAuthExpired()
            }
        }

        return originalResponse
    }

    private fun tryRefreshToken(): Boolean {
        return runBlocking {
            refreshMutex.withLock {
                try {
                    val refreshToken = secureTokenStorage.refreshToken
                    if (refreshToken == null) return@withLock false

                    val jsonBody = """{"refreshToken":"$refreshToken"}"""
                    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url("${BASE_URL}auth/refresh")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val authResponse = gson.fromJson(body, AuthResponse::class.java)
                            secureTokenStorage.saveTokens(authResponse.accessToken.orEmpty(), authResponse.refreshToken.orEmpty())
                            return@withLock true
                        }
                    }
                    false
                } catch (e: Exception) {
                    false
                }
            }
        }
    }
}
