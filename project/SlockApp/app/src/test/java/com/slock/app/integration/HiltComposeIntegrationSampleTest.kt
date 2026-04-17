package com.slock.app.integration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.slock.app.data.api.ApiService
import com.slock.app.data.local.AppDatabase
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.local.dao.AgentDao
import com.slock.app.data.local.dao.ChannelDao
import com.slock.app.data.local.dao.MessageDao
import com.slock.app.data.local.dao.ServerDao
import com.slock.app.data.local.dao.TaskDao
import com.slock.app.data.local.dao.UserDao
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.repository.AuthRepository
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MachineRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.ServerRepository
import com.slock.app.data.repository.TaskRepository
import com.slock.app.data.repository.ThreadRepository
import com.slock.app.di.AppModule
import com.slock.app.di.DatabaseModule
import com.slock.app.di.DefaultDispatcher
import com.slock.app.di.IoDispatcher
import com.slock.app.di.MainDispatcher
import com.slock.app.di.NetworkModule
import com.slock.app.di.RepositoryModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import javax.inject.Singleton

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(RobolectricTestRunner::class)
@UninstallModules(
    AppModule::class,
    NetworkModule::class,
    DatabaseModule::class,
    RepositoryModule::class
)
class HiltComposeIntegrationSampleTest {

    @Module
    @InstallIn(SingletonComponent::class)
    object FakeModule {
        private val testDispatcher = StandardTestDispatcher()

        @Provides @Singleton fun provideDataStore(): DataStore<Preferences> = mock()
        @Provides @IoDispatcher fun provideIoDispatcher(): CoroutineDispatcher = testDispatcher
        @Provides @MainDispatcher fun provideMainDispatcher(): CoroutineDispatcher = testDispatcher
        @Provides @DefaultDispatcher fun provideDefaultDispatcher(): CoroutineDispatcher = testDispatcher

        @Provides @Singleton fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor()
        @Provides @Singleton fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
        @Provides @Singleton fun provideRetrofit(): Retrofit = Retrofit.Builder().baseUrl("http://localhost/").build()
        @Provides @Singleton fun provideApiService(): ApiService = mock()

        @Provides @Singleton fun provideAppDatabase(): AppDatabase = mock()
        @Provides @Singleton fun provideServerDao(): ServerDao = mock()
        @Provides @Singleton fun provideChannelDao(): ChannelDao = mock()
        @Provides @Singleton fun provideMessageDao(): MessageDao = mock()
        @Provides @Singleton fun provideUserDao(): UserDao = mock()
        @Provides @Singleton fun provideAgentDao(): AgentDao = mock()
        @Provides @Singleton fun provideTaskDao(): TaskDao = mock()

        @Provides @Singleton fun provideAuthRepository(): AuthRepository = mock()
        @Provides @Singleton fun provideServerRepository(): ServerRepository = mock()
        @Provides @Singleton fun provideChannelRepository(): ChannelRepository = mock()
        @Provides @Singleton fun provideMessageRepository(): MessageRepository = mock()
        @Provides @Singleton fun provideAgentRepository(): AgentRepository = mock()
        @Provides @Singleton fun provideTaskRepository(): TaskRepository = mock()
        @Provides @Singleton fun provideThreadRepository(): ThreadRepository = mock()
        @Provides @Singleton fun provideMachineRepository(): MachineRepository = mock()
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestHiltActivity>()

    @BindValue @JvmField
    val secureTokenStorage: SecureTokenStorage = mock()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `hilt launches real AndroidEntryPoint activity via Robolectric`() {
        composeTestRule.onNodeWithText("Hilt Activity Injected").assertIsDisplayed()
    }

    @Test
    fun `hilt injects ActiveServerHolder into test activity`() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            assertNotNull(
                "ActiveServerHolder must be injected via Hilt",
                activity.activeServerHolder
            )
        }
    }
}
