package com.slock.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.AppDatabase
import com.slock.app.data.local.dao.ChannelDao
import com.slock.app.data.local.dao.MessageDao
import com.slock.app.data.local.dao.ServerDao
import com.slock.app.data.local.entity.ChannelEntity
import com.slock.app.data.local.entity.MessageEntity
import com.slock.app.data.local.entity.ServerEntity
import com.slock.app.data.model.Message
import com.slock.app.data.model.MessagesResponse
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DmForeignKeyRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var serverDao: ServerDao
    private lateinit var channelDao: ChannelDao
    private lateinit var messageDao: MessageDao
    private lateinit var apiService: ApiService
    private lateinit var activeServerHolder: ActiveServerHolder
    private lateinit var repo: MessageRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        serverDao = db.serverDao()
        channelDao = db.channelDao()
        messageDao = db.messageDao()
        apiService = mock()
        activeServerHolder = mock()
        repo = MessageRepositoryImpl(apiService, activeServerHolder, messageDao, channelDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getMessages with missing channel row does not throw FK constraint`() = runTest {
        serverDao.insertServer(ServerEntity(id = "srv-1", name = "Test Server"))

        whenever(apiService.getMessages(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Response.success(MessagesResponse(
                messages = listOf(Message(id = "m1", channelId = "dm-ch-1", content = "hello", seq = 1))
            )))

        val result = repo.getMessages("srv-1", "dm-ch-1", 50)

        assertTrue("getMessages must succeed when channel row was missing", result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)

        val hydratedChannel = channelDao.getChannelById("dm-ch-1")
        assertNotNull("ensureChannelExists must have inserted the missing channel row", hydratedChannel)
        assertEquals("srv-1", hydratedChannel!!.serverId)
    }

    @Test
    fun `refreshMessages with missing channel row does not throw FK constraint`() = runTest {
        serverDao.insertServer(ServerEntity(id = "srv-1", name = "Test Server"))

        whenever(apiService.getMessages(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Response.success(MessagesResponse(
                messages = listOf(Message(id = "m2", channelId = "dm-ch-2", content = "hi", seq = 5))
            )))

        val result = repo.refreshMessages("srv-1", "dm-ch-2", 50)

        assertTrue("refreshMessages must succeed when channel row was missing", result.isSuccess)

        val hydratedChannel = channelDao.getChannelById("dm-ch-2")
        assertNotNull("ensureChannelExists must have inserted the missing channel row", hydratedChannel)

        val stored = messageDao.getMessagesByChannel("dm-ch-2", 50).firstOrNull()
        assertNotNull("Message must be stored in Room after hydration", stored)
        assertTrue("Stored messages must not be empty", stored!!.isNotEmpty())
    }

    @Test
    fun `getMessages with existing cached channel does not duplicate or overwrite`() = runTest {
        serverDao.insertServer(ServerEntity(id = "srv-1", name = "Test Server"))
        channelDao.insertChannel(ChannelEntity(id = "ch-existing", serverId = "srv-1", name = "General", type = "text"))

        whenever(apiService.getMessages(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Response.success(MessagesResponse(
                messages = listOf(Message(id = "m3", channelId = "ch-existing", content = "msg", seq = 10))
            )))

        val result = repo.getMessages("srv-1", "ch-existing", 50)

        assertTrue("getMessages must succeed for already-cached channel", result.isSuccess)

        val channel = channelDao.getChannelById("ch-existing")
        assertNotNull(channel)
        assertEquals("General", channel!!.name)
        assertEquals("text", channel.type)
    }

    @Test
    fun `existing cached DM messages load from cache without API call`() = runTest {
        serverDao.insertServer(ServerEntity(id = "srv-1", name = "Test Server"))
        channelDao.insertChannel(ChannelEntity(id = "dm-cached", serverId = "srv-1", name = "DM"))
        messageDao.insertMessage(MessageEntity(id = "cached-m1", channelId = "dm-cached", content = "cached msg", seq = 1))

        val result = repo.getMessages("srv-1", "dm-cached", 50)

        assertTrue("Cached DM messages must load successfully", result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
        assertEquals("cached msg", result.getOrNull()!!.first().content)
    }
}
