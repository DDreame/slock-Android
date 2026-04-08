package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthResponse>
    suspend fun register(email: String, password: String, name: String): Result<AuthResponse>
    suspend fun refreshToken(): Result<AuthResponse>
    suspend fun logout(): Result<Unit>
    suspend fun getMe(): Result<User>
    suspend fun updateMe(name: String?): Result<User>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun resetPassword(token: String, password: String): Result<Unit>
    fun isLoggedIn(): Flow<Boolean>
}

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val secureTokenStorage: SecureTokenStorage
) : AuthRepository {

    private suspend fun saveTokens(accessToken: String, refreshToken: String) {
        secureTokenStorage.saveTokens(accessToken, refreshToken)
    }

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                saveTokens(authResponse.accessToken, authResponse.refreshToken)
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String, name: String): Result<AuthResponse> {
        return try {
            val response = apiService.register(RegisterRequest(email, password, name))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                saveTokens(authResponse.accessToken, authResponse.refreshToken)
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Register failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<AuthResponse> {
        return try {
            val refreshToken = secureTokenStorage.refreshToken
            if (refreshToken == null) {
                return Result.failure(Exception("No refresh token"))
            }
            val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Refresh failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val response = apiService.logout()
            if (response.isSuccessful) {
                secureTokenStorage.clearTokens()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Logout failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMe(): Result<User> {
        return try {
            val response = apiService.getMe()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get user failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMe(name: String?): Result<User> {
        return try {
            val response = apiService.updateMe(UpdateUserRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Update user failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Forgot password failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(token: String, password: String): Result<Unit> {
        return try {
            val response = apiService.resetPassword(ResetPasswordRequest(token, password))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Reset password failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isLoggedIn(): Flow<Boolean> = flow {
        emit(secureTokenStorage.hasTokens())
    }
}
