package com.slock.app.data.api

import com.slock.app.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth/me")
    suspend fun getMe(): Response<User>

    @PATCH("auth/me")
    suspend fun updateMe(@Body request: UpdateUserRequest): Response<User>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Unit>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>

    // Servers
    @GET("servers")
    suspend fun getServers(): Response<List<Server>>

    @POST("servers")
    suspend fun createServer(@Body request: CreateServerRequest): Response<Server>

    @DELETE("servers/{serverId}")
    suspend fun deleteServer(@Path("serverId") serverId: String): Response<Unit>

    @GET("servers/{serverId}/members")
    suspend fun getServerMembers(@Path("serverId") serverId: String): Response<List<Member>>

    @PATCH("servers/{serverId}/members/{memberId}")
    suspend fun updateMemberRole(
        @Path("serverId") serverId: String,
        @Path("memberId") memberId: String,
        @Body request: UpdateMemberRoleRequest
    ): Response<Unit>

    // Channels (X-Server-Id header added automatically by ServerIdInterceptor)
    @GET("channels")
    suspend fun getChannels(): Response<List<Channel>>

    @POST("channels")
    suspend fun createChannel(@Body request: CreateChannelRequest): Response<Channel>

    @PATCH("channels/{channelId}")
    suspend fun updateChannel(
        @Path("channelId") channelId: String,
        @Body request: UpdateChannelRequest
    ): Response<Channel>

    @DELETE("channels/{channelId}")
    suspend fun deleteChannel(@Path("channelId") channelId: String): Response<Unit>

    @POST("channels/{channelId}/join")
    suspend fun joinChannel(@Path("channelId") channelId: String): Response<Unit>

    @POST("channels/{channelId}/leave")
    suspend fun leaveChannel(@Path("channelId") channelId: String): Response<Unit>

    @POST("channels/{channelId}/read")
    suspend fun markChannelRead(@Path("channelId") channelId: String, @Body request: MarkReadRequest): Response<Unit>

    @GET("channels/dm")
    suspend fun getDMs(): Response<List<Channel>>

    @POST("channels/dm")
    suspend fun createDM(@Body request: CreateDMRequest): Response<Channel>

    @GET("channels/{channelId}/members")
    suspend fun getChannelMembers(@Path("channelId") channelId: String): Response<List<ChannelMember>>

    @GET("channels/unread")
    suspend fun getUnreadChannels(): Response<List<Channel>>

    @GET("channels/saved")
    suspend fun getSavedChannels(): Response<List<Channel>>

    @POST("channels/saved")
    suspend fun saveChannel(@Body request: SaveChannelRequest): Response<Unit>

    @POST("channels/saved/check")
    suspend fun checkSavedChannel(@Body request: SaveChannelRequest): Response<SavedChannelCheckResponse>

    @DELETE("channels/saved/{channelId}")
    suspend fun removeSavedChannel(@Path("channelId") channelId: String): Response<Unit>

    // Messages (X-Server-Id header added automatically by ServerIdInterceptor)
    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<Message>

    @GET("messages/channel/{channelId}")
    suspend fun getMessages(
        @Path("channelId") channelId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null,
        @Query("after") after: String? = null
    ): Response<MessagesResponse>

    // Some endpoints may return plain array instead of { messages: [...] }
    @GET("messages/channel/{channelId}")
    suspend fun getMessagesRaw(
        @Path("channelId") channelId: String,
        @Query("limit") limit: Int = 50
    ): Response<List<Message>>

    @GET("messages/search")
    suspend fun searchMessages(
        @Query("query") query: String,
        @Query("serverId") searchServerId: String? = null,
        @Query("channelId") channelId: String? = null
    ): Response<SearchMessagesResponse>

    // Agents (X-Server-Id header added automatically by ServerIdInterceptor)
    @GET("agents")
    suspend fun getAgents(): Response<List<Agent>>

    @POST("agents")
    suspend fun createAgent(@Body request: CreateAgentRequest): Response<Agent>

    @PATCH("agents/{agentId}")
    suspend fun updateAgent(
        @Path("agentId") agentId: String,
        @Body request: UpdateAgentRequest
    ): Response<Agent>

    @DELETE("agents/{agentId}")
    suspend fun deleteAgent(@Path("agentId") agentId: String): Response<Unit>

    @POST("agents/{agentId}/start")
    suspend fun startAgent(@Path("agentId") agentId: String): Response<Unit>

    @POST("agents/{agentId}/stop")
    suspend fun stopAgent(@Path("agentId") agentId: String): Response<Unit>

    @POST("agents/{agentId}/reset")
    suspend fun resetAgent(@Path("agentId") agentId: String, @Body request: ResetAgentRequest): Response<Unit>

    @GET("agents/{agentId}/activity-log")
    suspend fun getAgentActivityLog(
        @Path("agentId") agentId: String,
        @Query("limit") limit: Int = 50
    ): Response<List<ActivityLogEntry>>

    // Threads (X-Server-Id header added automatically by ServerIdInterceptor)
    @GET("channels/threads/followed")
    suspend fun getFollowedThreads(): Response<FollowedThreadsResponse>

    @GET("threads/channel/{channelId}")
    suspend fun getThreadMessages(
        @Path("channelId") channelId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<MessagesResponse>

    @GET("threads/{threadId}/messages")
    suspend fun getThreadReplies(
        @Path("threadId") threadId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<MessagesResponse>

    // Tasks (X-Server-Id header added automatically by ServerIdInterceptor)
    @GET("tasks/server")
    suspend fun getServerTasks(): Response<TasksResponse>

    @GET("tasks/channel/{channelId}")
    suspend fun getTasks(@Path("channelId") channelId: String): Response<TasksResponse>

    @POST("tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): Response<Task>

    @PATCH("tasks/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: String,
        @Body request: UpdateTaskStatusRequest
    ): Response<Task>

    @DELETE("tasks/{taskId}")
    suspend fun deleteTask(@Path("taskId") taskId: String): Response<Unit>

    // Machines (X-Server-Id header added automatically by ServerIdInterceptor)
    @GET("machines")
    suspend fun getMachines(): Response<List<Machine>>

    @DELETE("machines/{machineId}")
    suspend fun deleteMachine(@Path("machineId") machineId: String): Response<Unit>

    // File Upload
    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>
}
