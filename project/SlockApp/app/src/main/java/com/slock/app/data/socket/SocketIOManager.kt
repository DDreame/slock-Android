package com.slock.app.data.socket

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket.IO Manager for real-time communication with Slock.ai
 */
@Singleton
class SocketIOManager @Inject constructor(
    private val secureTokenStorage: com.slock.app.data.local.SecureTokenStorage
) {
    private val tokenProvider: () -> String? = { secureTokenStorage.accessToken }
    companion object {
        private const val TAG = "SocketIOManager"
        private const val BASE_URL = "https://api.slock.ai"
        private const val EIO = "4"
        private const val TRANSPORT = "websocket"
    }

    private var socket: Socket? = null
    private var currentServerId: String? = null
    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1)
    val connectionState: Flow<ConnectionState> = _connectionState.asSharedFlow()

    private val _events = MutableSharedFlow<SocketEvent>(replay = 0)
    val events: Flow<SocketEvent> = _events.asSharedFlow()

    // Event class for unified event handling
    sealed class SocketEvent {
        data class MessageNew(val data: MessageNewData) : SocketEvent()
        data class MessageUpdated(val data: MessageUpdatedData) : SocketEvent()
        data class AgentCreated(val data: AgentCreatedData) : SocketEvent()
        data class AgentDeleted(val agentId: String) : SocketEvent()
        data class AgentActivity(val data: AgentActivityData) : SocketEvent()
        data class ChannelUpdated(val data: ChannelUpdatedData) : SocketEvent()
        data class ChannelMembersUpdated(val data: ChannelMembersUpdatedData) : SocketEvent()
        data class DMNew(val data: DMNewData) : SocketEvent()
        data class ThreadUpdated(val data: ThreadUpdatedData) : SocketEvent()
        data class TaskCreated(val data: TaskCreatedData) : SocketEvent()
        data class TaskUpdated(val data: TaskUpdatedData) : SocketEvent()
        data class TaskDeleted(val taskId: String) : SocketEvent()
        data class MachineStatus(val machineId: String, val status: String) : SocketEvent()
        data class DaemonStatus(val machineId: String, val status: String) : SocketEvent()
        data class RoomsJoined(val rooms: List<String>) : SocketEvent()
        data class ServerPlanUpdated(val plan: String) : SocketEvent()
    }

    // Data classes for events
    data class MessageNewData(
        val id: String,
        val channelId: String,
        val content: String,
        val userId: String,
        val agentId: String? = null,
        val seq: Long,
        val createdAt: String
    )

    data class MessageUpdatedData(
        val id: String,
        val channelId: String,
        val content: String
    )

    data class AgentCreatedData(
        val id: String,
        val name: String,
        val description: String
    )

    data class AgentActivityData(
        val agentId: String,
        val activity: String,
        val message: String? = null
    )

    data class ChannelUpdatedData(
        val id: String,
        val name: String? = null,
        val type: String? = null
    )

    data class ChannelMembersUpdatedData(
        val channelId: String,
        val members: List<String>
    )

    data class DMNewData(
        val id: String,
        val name: String,
        val type: String = "dm"
    )

    data class ThreadUpdatedData(
        val id: String,
        val channelId: String,
        val parentChannelId: String
    )

    data class TaskCreatedData(
        val id: String,
        val channelId: String,
        val title: String,
        val status: String
    )

    data class TaskUpdatedData(
        val id: String,
        val channelId: String,
        val status: String
    )

    enum class ConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    /**
     * Connect to Socket.IO server
     */
    fun connect(serverId: String? = null) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Already connected")
            return
        }

        currentServerId = serverId

        try {
            val options = IO.Options().apply {
                transports = arrayOf(TRANSPORT)
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 10000

                // Pass auth with token and serverId
                val token = tokenProvider()
                if (token != null) {
                    auth = mapOf(
                        "token" to token,
                        "serverId" to (serverId ?: "")
                    )
                }
            }

            socket = IO.socket(BASE_URL, options)
            // Clear any existing listeners before setting up new ones
            socket?.off()
            setupListeners()
            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect", e)
            _connectionState.tryEmit(ConnectionState.ERROR)
        }
    }

    /**
     * Disconnect from Socket.IO server
     */
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.tryEmit(ConnectionState.DISCONNECTED)
    }

    /**
     * Join a channel to receive messages
     */
    fun joinChannel(channelId: String) {
        socket?.emit("join:channel", channelId)
        Log.d(TAG, "Joining channel: $channelId")
    }

    /**
     * Leave a channel
     */
    fun leaveChannel(channelId: String) {
        socket?.emit("leave:channel", channelId)
        Log.d(TAG, "Leaving channel: $channelId")
    }

    /**
     * Resume connection with last sequence number
     */
    fun syncResume(lastSeq: Long = 0) {
        val data = JSONObject().apply {
            put("lastSeq", lastSeq)
        }
        socket?.emit("sync:resume", data)
        Log.d(TAG, "Sync resume with lastSeq: $lastSeq")
    }

    private fun setupListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to Socket.IO")
                _connectionState.tryEmit(ConnectionState.CONNECTED)

                // Resume sync after connection
                syncResume(0)
            }

            on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Disconnected from Socket.IO")
                _connectionState.tryEmit(ConnectionState.DISCONNECTED)
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()?.toString() ?: "Unknown error"
                Log.e(TAG, "Connection error: $error")
                _connectionState.tryEmit(ConnectionState.ERROR)
            }

            on("rooms:joined") { args ->
                val data = args.firstOrNull() as? JSONObject
                val rooms = data?.optJSONArray("rooms")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                _events.tryEmit(SocketEvent.RoomsJoined(rooms))
            }

            on("message:new") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val event = SocketEvent.MessageNew(
                    MessageNewData(
                        id = data.optString("id"),
                        channelId = data.optString("channelId"),
                        content = data.optString("content"),
                        userId = data.optString("userId"),
                        agentId = data.optString("agentId").takeIf { it.isNotEmpty() },
                        seq = data.optLong("seq"),
                        createdAt = data.optString("createdAt")
                    )
                )
                _events.tryEmit(event)
            }

            on("message:updated") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val event = SocketEvent.MessageUpdated(
                    MessageUpdatedData(
                        id = data.optString("id"),
                        channelId = data.optString("channelId"),
                        content = data.optString("content")
                    )
                )
                _events.tryEmit(event)
            }

            on("agent:created") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val event = SocketEvent.AgentCreated(
                    AgentCreatedData(
                        id = data.optString("id"),
                        name = data.optString("name"),
                        description = data.optString("description")
                    )
                )
                _events.tryEmit(event)
            }

            on("agent:deleted") { args ->
                val agentId = args.firstOrNull()?.toString() ?: return@on
                _events.tryEmit(SocketEvent.AgentDeleted(agentId))
            }

            on("agent:activity") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val event = SocketEvent.AgentActivity(
                    AgentActivityData(
                        agentId = data.optString("agentId"),
                        activity = data.optString("activity"),
                        message = data.optString("message").takeIf { it.isNotEmpty() }
                    )
                )
                _events.tryEmit(event)
            }

            on("channel:updated") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val event = SocketEvent.ChannelUpdated(
                    ChannelUpdatedData(
                        id = data.optString("id"),
                        name = data.optString("name").takeIf { it.isNotEmpty() },
                        type = data.optString("type").takeIf { it.isNotEmpty() }
                    )
                )
                _events.tryEmit(event)
            }

            on("channel:members-updated") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val members = data.optJSONArray("members")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                val event = SocketEvent.ChannelMembersUpdated(
                    ChannelMembersUpdatedData(
                        channelId = data.optString("channelId"),
                        members = members
                    )
                )
                _events.tryEmit(event)
            }

            on("dm:new") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val event = SocketEvent.DMNew(
                    DMNewData(
                        id = data.optString("id"),
                        name = data.optString("name"),
                        type = data.optString("type")
                    )
                )
                _events.tryEmit(event)
            }

            on("thread:updated") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val event = SocketEvent.ThreadUpdated(
                    ThreadUpdatedData(
                        id = data.optString("id"),
                        channelId = data.optString("channelId"),
                        parentChannelId = data.optString("parentChannelId")
                    )
                )
                _events.tryEmit(event)
            }

            on("task:created") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val event = SocketEvent.TaskCreated(
                    TaskCreatedData(
                        id = data.optString("id"),
                        channelId = data.optString("channelId"),
                        title = data.optString("title"),
                        status = data.optString("status")
                    )
                )
                _events.tryEmit(event)
            }

            on("task:updated") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val event = SocketEvent.TaskUpdated(
                    TaskUpdatedData(
                        id = data.optString("id"),
                        channelId = data.optString("channelId"),
                        status = data.optString("status")
                    )
                )
                _events.tryEmit(event)
            }

            on("task:deleted") { args ->
                val taskId = args.firstOrNull()?.toString() ?: return@on
                _events.tryEmit(SocketEvent.TaskDeleted(taskId))
            }

            on("machine:status") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                _events.tryEmit(
                    SocketEvent.MachineStatus(
                        machineId = data.optString("machineId"),
                        status = data.optString("status")
                    )
                )
            }

            on("daemon:status") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                _events.tryEmit(
                    SocketEvent.DaemonStatus(
                        machineId = data.optString("machineId"),
                        status = data.optString("status")
                    )
                )
            }

            on("server:plan-updated") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                _events.tryEmit(
                    SocketEvent.ServerPlanUpdated(data.optString("plan"))
                )
            }

            on("heartbeat") {
                // Server heartbeat, no action needed
            }
        }
    }

    fun isConnected(): Boolean = socket?.connected() == true
}
