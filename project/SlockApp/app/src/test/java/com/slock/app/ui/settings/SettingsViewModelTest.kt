package com.slock.app.ui.settings

import com.slock.app.data.local.NotificationPreference
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.local.SettingsPreferencesStore
import com.slock.app.data.model.User
import com.slock.app.data.repository.AuthRepository
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mock()
    private val secureTokenStorage: SecureTokenStorage = mock()
    private val settingsPreferencesStore: SettingsPreferencesStore = mock()

    @Test
    fun init_loadsCachedIdentityAndRefreshesAccountState() = runTest {
        val preferenceFlow = MutableStateFlow(NotificationPreference.MUTE)
        whenever(secureTokenStorage.userName).thenReturn("Cached J2")
        whenever(secureTokenStorage.userId).thenReturn("cached-id")
        whenever(settingsPreferencesStore.notificationPreferenceFlow).thenReturn(preferenceFlow)
        whenever(authRepository.getMe()).thenReturn(
            Result.success(
                User(
                    id = "user-1",
                    email = "j2@slock.dev",
                    name = "J2"
                )
            )
        )

        val viewModel = SettingsViewModel(authRepository, secureTokenStorage, settingsPreferencesStore)
        advanceUntilIdle()

        assertEquals(NotificationPreference.MUTE, viewModel.state.value.notificationPreference)
        assertEquals("J2", viewModel.state.value.userName)
        assertEquals("j2@slock.dev", viewModel.state.value.userEmail)
        assertEquals("user-1", viewModel.state.value.userId)
        assertFalse(viewModel.state.value.isRefreshingAccount)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun refreshAccount_failureExposesMessageInUiState() = runTest {
        whenever(secureTokenStorage.userName).thenReturn("Cached J2")
        whenever(secureTokenStorage.userId).thenReturn("cached-id")
        whenever(settingsPreferencesStore.notificationPreferenceFlow)
            .thenReturn(MutableStateFlow(NotificationPreference.ALL_MESSAGES))
        whenever(authRepository.getMe()).thenReturn(Result.failure(IllegalStateException("network down")))

        val viewModel = SettingsViewModel(authRepository, secureTokenStorage, settingsPreferencesStore)
        advanceUntilIdle()

        assertEquals("network down", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isRefreshingAccount)
        assertEquals("Cached J2", viewModel.state.value.userName)
        assertEquals("cached-id", viewModel.state.value.userId)
    }

    @Test
    fun updateNotificationPreference_persistsSelection() = runTest {
        whenever(secureTokenStorage.userName).thenReturn(null)
        whenever(secureTokenStorage.userId).thenReturn(null)
        whenever(settingsPreferencesStore.notificationPreferenceFlow)
            .thenReturn(MutableStateFlow(NotificationPreference.ALL_MESSAGES))
        whenever(authRepository.getMe()).thenReturn(Result.failure(IllegalStateException("skip")))

        val viewModel = SettingsViewModel(authRepository, secureTokenStorage, settingsPreferencesStore)
        advanceUntilIdle()

        viewModel.updateNotificationPreference(NotificationPreference.MENTIONS_ONLY)
        advanceUntilIdle()

        verify(settingsPreferencesStore).setNotificationPreference(NotificationPreference.MENTIONS_ONLY)
    }
}
