package com.slock.app.ui.profile

import androidx.lifecycle.SavedStateHandle
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.AuthResponse
import com.slock.app.data.model.Member
import com.slock.app.data.model.Server
import com.slock.app.data.model.User
import com.slock.app.data.repository.AuthRepository
import com.slock.app.data.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var presenceTracker: PresenceTracker
    private lateinit var activeServerHolder: ActiveServerHolder
    private lateinit var secureTokenStorage: SecureTokenStorage

    private val testUser = User(id = "u1", email = "alice@test.com", name = "Alice", avatar = "https://img/a.png")
    private val testMember = Member(id = "m1", userId = "u1", role = "admin", user = testUser, name = "Alice", displayName = "Alice")
    private val otherUser = User(id = "u2", email = "bob@test.com", name = "Bob")
    private val otherMember = Member(id = "m2", userId = "u2", role = "member", user = otherUser, name = "Bob")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        presenceTracker = PresenceTracker()
        activeServerHolder = ActiveServerHolder()
        activeServerHolder.serverId = "server1"
        secureTokenStorage = mock()
        whenever(secureTokenStorage.userId).thenReturn("u1")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        authRepository: AuthRepository = FakeAuthRepository(),
        serverRepository: ServerRepository = FakeServerRepository()
    ): ProfileViewModel {
        return ProfileViewModel(
            savedStateHandle = savedStateHandle,
            authRepository = authRepository,
            serverRepository = serverRepository,
            activeServerHolder = activeServerHolder,
            presenceTracker = presenceTracker,
            secureTokenStorage = secureTokenStorage
        )
    }

    @Test
    fun `own profile loads user from authRepository and member role`() = runTest {
        val authRepo = FakeAuthRepository(getMeResult = Result.success(testUser))
        val serverRepo = FakeServerRepository(membersResult = Result.success(listOf(testMember, otherMember)))

        val vm = createViewModel(authRepository = authRepo, serverRepository = serverRepo)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.isOwnProfile)
        assertEquals("Alice", state.user?.name)
        assertEquals("admin", state.member?.role)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Active now", state.lastActiveText)
    }

    @Test
    fun `other profile loads from server members`() = runTest {
        presenceTracker.setOnline("u2")
        val serverRepo = FakeServerRepository(membersResult = Result.success(listOf(testMember, otherMember)))
        val handle = SavedStateHandle(mapOf("userId" to "u2"))

        val vm = createViewModel(savedStateHandle = handle, serverRepository = serverRepo)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isOwnProfile)
        assertEquals("Bob", state.user?.name)
        assertEquals("member", state.member?.role)
        assertTrue(state.isOnline)
        assertEquals("Active now", state.lastActiveText)
    }

    @Test
    fun `other profile offline shows Offline`() = runTest {
        val serverRepo = FakeServerRepository(membersResult = Result.success(listOf(testMember, otherMember)))
        val handle = SavedStateHandle(mapOf("userId" to "u2"))

        val vm = createViewModel(savedStateHandle = handle, serverRepository = serverRepo)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isOnline)
        assertEquals("Offline", state.lastActiveText)
    }

    @Test
    fun `other profile user not found sets error`() = runTest {
        val serverRepo = FakeServerRepository(membersResult = Result.success(listOf(testMember)))
        val handle = SavedStateHandle(mapOf("userId" to "unknown"))

        val vm = createViewModel(savedStateHandle = handle, serverRepository = serverRepo)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("User not found", state.error)
    }

    @Test
    fun `saveName updates user and syncs member`() = runTest {
        val updatedUser = User(id = "u1", email = "alice@test.com", name = "NewAlice")
        val authRepo = FakeAuthRepository(
            getMeResult = Result.success(testUser),
            updateMeResult = Result.success(updatedUser)
        )
        val serverRepo = FakeServerRepository(membersResult = Result.success(listOf(testMember)))

        val vm = createViewModel(authRepository = authRepo, serverRepository = serverRepo)
        advanceUntilIdle()

        vm.startEditing()
        vm.updateEditName("NewAlice")
        vm.saveName()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("NewAlice", state.user?.name)
        assertEquals("NewAlice", state.member?.name)
        assertEquals("NewAlice", state.member?.displayName)
        assertFalse(state.isEditing)
        assertFalse(state.isSaving)

        val display = resolveProfileDisplayData(state)
        assertEquals("NewAlice", display.name)
    }

    @Test
    fun `saveName with blank name sets error`() = runTest {
        val authRepo = FakeAuthRepository(getMeResult = Result.success(testUser))
        val serverRepo = FakeServerRepository(membersResult = Result.success(listOf(testMember)))

        val vm = createViewModel(authRepository = authRepo, serverRepository = serverRepo)
        advanceUntilIdle()

        vm.startEditing()
        vm.updateEditName("   ")
        vm.saveName()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Name cannot be empty", state.saveError)
        assertTrue(state.isEditing)
    }

    @Test
    fun `saveName failure preserves editing state`() = runTest {
        val authRepo = FakeAuthRepository(
            getMeResult = Result.success(testUser),
            updateMeResult = Result.failure(RuntimeException("Network error"))
        )
        val serverRepo = FakeServerRepository(membersResult = Result.success(listOf(testMember)))

        val vm = createViewModel(authRepository = authRepo, serverRepository = serverRepo)
        advanceUntilIdle()

        vm.startEditing()
        vm.updateEditName("NewName")
        vm.saveName()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Network error", state.saveError)
        assertFalse(state.isSaving)
    }

    @Test
    fun `own profile getMe failure sets error`() = runTest {
        val authRepo = FakeAuthRepository(getMeResult = Result.failure(RuntimeException("Auth failed")))

        val vm = createViewModel(authRepository = authRepo)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Auth failed", state.error)
        assertFalse(state.isLoading)
    }
}

private class FakeAuthRepository(
    private val getMeResult: Result<User> = Result.success(User(id = "u1", name = "Test")),
    private val updateMeResult: Result<User> = Result.success(User(id = "u1", name = "Updated"))
) : AuthRepository {
    override suspend fun login(email: String, password: String) = Result.failure<AuthResponse>(NotImplementedError())
    override suspend fun register(email: String, password: String, name: String) = Result.failure<AuthResponse>(NotImplementedError())
    override suspend fun refreshToken() = Result.failure<AuthResponse>(NotImplementedError())
    override suspend fun getMe() = getMeResult
    override suspend fun updateMe(name: String?) = updateMeResult
    override suspend fun logout() = Result.success(Unit)
    override suspend fun forgotPassword(email: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun resetPassword(token: String, password: String) = Result.failure<Unit>(NotImplementedError())
    override fun isLoggedIn(): Flow<Boolean> = flowOf(true)
}

private class FakeServerRepository(
    private val membersResult: Result<List<Member>> = Result.success(emptyList())
) : ServerRepository {
    override suspend fun getServers() = Result.failure<List<Server>>(NotImplementedError())
    override suspend fun refreshServers() = Result.failure<List<Server>>(NotImplementedError())
    override suspend fun createServer(name: String, slug: String) = Result.failure<Server>(NotImplementedError())
    override suspend fun deleteServer(serverId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun getServerMembers(serverId: String) = membersResult
    override suspend fun updateMemberRole(serverId: String, memberId: String, role: String) = Result.failure<Unit>(NotImplementedError())
}
