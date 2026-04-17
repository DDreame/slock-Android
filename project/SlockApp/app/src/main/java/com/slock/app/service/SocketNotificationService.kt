package com.slock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.slock.app.MainActivity
import com.slock.app.R
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.NotificationPreference
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.local.SettingsPreferencesStore
import com.slock.app.data.local.dao.ChannelDao
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

internal object NotificationDecision {
    fun shouldNotify(
        preference: NotificationPreference,
        isDm: Boolean,
        isMentioned: Boolean
    ): Boolean {
        return when (preference) {
            NotificationPreference.ALL_MESSAGES -> true
            NotificationPreference.MENTIONS_ONLY -> isDm || isMentioned
            NotificationPreference.MUTE -> false
        }
    }
}

internal suspend fun rejoinCachedChannelsForServer(
    serverId: String?,
    loadChannelIds: suspend (String) -> List<String>,
    joinChannel: (String) -> Unit
): List<String> {
    if (serverId.isNullOrBlank()) return emptyList()
    val channelIds = loadChannelIds(serverId)
    channelIds.forEach(joinChannel)
    return channelIds
}

@AndroidEntryPoint
class SocketNotificationService : Service() {

    companion object {
        private const val TAG = "SocketNotifService"
        const val CHANNEL_ID_SERVICE = "slock_service"
        const val CHANNEL_ID_MENTIONS = "slock_mentions"
        const val CHANNEL_ID_DM = "slock_dm"
        private const val SERVICE_NOTIFICATION_ID = 1
        private var notificationCounter = 100

        fun start(context: Context) {
            val intent = Intent(context, SocketNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SocketNotificationService::class.java))
        }
    }

    @Inject lateinit var socketManager: SocketIOManager
    @Inject lateinit var secureTokenStorage: SecureTokenStorage
    @Inject lateinit var settingsPreferencesStore: SettingsPreferencesStore
    @Inject lateinit var activeServerHolder: ActiveServerHolder
    @Inject lateinit var lifecycleTracker: AppLifecycleTracker
    @Inject lateinit var channelDao: ChannelDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var eventsJob: Job? = null
    private var connectionJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        observeConnectionState()
        ensureSocketConnected()
        observeSocketEvents()
        return START_STICKY
    }

    private fun ensureSocketConnected() {
        if (!socketManager.isConnected()) {
            val serverId = activeServerHolder.serverId
            socketManager.connect(serverId)
        }
    }

    private fun observeConnectionState() {
        connectionJob?.cancel()
        connectionJob = serviceScope.launch {
            socketManager.connectionState.collect { state ->
                if (state == SocketIOManager.ConnectionState.CONNECTED) {
                    joinCachedChannels(activeServerHolder.serverId)
                }
            }
        }
    }

    private fun joinCachedChannels(serverId: String?) {
        if (serverId.isNullOrBlank()) return
        serviceScope.launch(Dispatchers.IO) {
            try {
                val channelIds = rejoinCachedChannelsForServer(
                    serverId = serverId,
                    loadChannelIds = channelDao::getChannelIdsByServer,
                    joinChannel = socketManager::joinChannel
                )
                Log.d(TAG, "Joining ${channelIds.size} cached channels after connect")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to join cached channels", e)
            }
        }
    }

    private fun observeSocketEvents() {
        eventsJob?.cancel()
        eventsJob = serviceScope.launch {
            socketManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.MessageNew -> {
                        launch(Dispatchers.IO) { handleNewMessage(event.data) }
                    }
                    is SocketIOManager.SocketEvent.DMNew -> {
                        socketManager.joinChannel(event.data.id)
                        try {
                            val serverId = activeServerHolder.serverId ?: return@collect
                            channelDao.insertChannel(
                                com.slock.app.data.local.entity.ChannelEntity(
                                    id = event.data.id,
                                    serverId = serverId,
                                    name = event.data.name,
                                    type = event.data.type
                                )
                            )
                        } catch (_: Exception) { }
                    }
                    else -> { /* only care about messages for notifications */ }
                }
            }
        }
    }

    private suspend fun handleNewMessage(data: SocketIOManager.MessageNewData) {
        Log.d(TAG, "handleNewMessage: channelId=${data.channelId}, senderId=${data.senderId}, senderName=${data.senderName}, foreground=${lifecycleTracker.isAppInForeground}")

        // Don't notify for own messages (skip filter if userId not cached)
        val currentUserId = secureTokenStorage.userId
        if (currentUserId != null && data.senderId == currentUserId) return

        // Determine notification style: DM vs @mention vs regular channel message
        val isDm = isDmChannel(data.channelId)
        val isMentioned = if (currentUserId != null) {
            val currentUserName = secureTokenStorage.userName ?: ""
            isMentionedInContent(data.content, currentUserId, currentUserName)
        } else false

        val notificationPreference = settingsPreferencesStore.getNotificationPreference()
        val shouldNotify = NotificationDecision.shouldNotify(
            preference = notificationPreference,
            isDm = isDm,
            isMentioned = isMentioned
        )

        if (!shouldNotify) return

        showMessageNotification(data, isDm, isMentioned)
    }

    private suspend fun isDmChannel(channelId: String): Boolean {
        return try {
            channelDao.getChannelById(channelId)?.type == "dm"
        } catch (_: Exception) {
            false
        }
    }

    private fun isMentionedInContent(content: String, userId: String, userName: String): Boolean {
        val lowerContent = content.lowercase()
        return lowerContent.contains("@$userId".lowercase()) ||
                (userName.isNotBlank() && lowerContent.contains("@${userName}".lowercase())) ||
                lowerContent.contains("@everyone") ||
                lowerContent.contains("@here")
    }

    private fun showMessageNotification(data: SocketIOManager.MessageNewData, isDm: Boolean, isMentioned: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = when {
            isDm -> CHANNEL_ID_DM
            isMentioned -> CHANNEL_ID_MENTIONS
            else -> CHANNEL_ID_MENTIONS  // Use mentions channel for all messages
        }
        val title = when {
            isDm -> data.senderName.ifBlank { "Direct Message" }
            isMentioned -> "${data.senderName.ifBlank { "Someone" }} mentioned you"
            else -> data.senderName.ifBlank { "New message" }
        }

        // Truncate content preview to ~80 chars
        val contentPreview = if (data.content.length > 80) {
            data.content.take(77) + "..."
        } else {
            data.content
        }

        // Deep link intent to open the specific channel
        val deepLinkIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "channel")
            putExtra("channelId", data.channelId)
            putExtra("channelName", resolveNotificationChannelName(isDm, data.senderName))
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            data.id.hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentPreview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentPreview))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(++notificationCounter, notification)
    }

    private fun buildServiceNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Slock running")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground service channel (silent, minimal)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Background Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps Slock connected for notifications"
                setShowBadge(false)
            }

            // Mentions channel (high importance — banner + sound)
            val mentionsChannel = NotificationChannel(
                CHANNEL_ID_MENTIONS,
                "Mentions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when someone mentions you"
            }

            // DM channel (high importance)
            val dmChannel = NotificationChannel(
                CHANNEL_ID_DM,
                "Direct Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for direct messages"
            }

            notificationManager.createNotificationChannels(
                listOf(serviceChannel, mentionsChannel, dmChannel)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        eventsJob?.cancel()
        connectionJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}

internal fun resolveNotificationChannelName(isDm: Boolean, senderName: String): String {
    return if (isDm) senderName else ""
}
