package com.slock.app.ui.profile

import com.slock.app.data.model.Member
import com.slock.app.data.model.User
import org.junit.Assert.*
import org.junit.Test

class ProfileDisplayDataTest {

    @Test
    fun `own profile with user and member data`() {
        val user = User(id = "u1", email = "test@example.com", name = "Alice", avatar = "https://img/a.png")
        val member = Member(id = "m1", userId = "u1", role = "admin", user = user, name = "Alice")
        val state = ProfileUiState(user = user, member = member, isOwnProfile = true, isOnline = true)

        val display = resolveProfileDisplayData(state)

        assertEquals("Alice", display.name)
        assertEquals("test@example.com", display.email)
        assertEquals("admin", display.role)
        assertEquals("https://img/a.png", display.avatar)
        assertEquals("A", display.initial)
        assertTrue(display.isOnline)
        assertTrue(display.isOwnProfile)
    }

    @Test
    fun `other user profile read only`() {
        val user = User(id = "u2", email = "bob@example.com", name = "Bob")
        val member = Member(id = "m2", userId = "u2", role = "member", user = user, name = "Bob")
        val state = ProfileUiState(user = user, member = member, isOwnProfile = false, isOnline = false)

        val display = resolveProfileDisplayData(state)

        assertEquals("Bob", display.name)
        assertEquals("bob@example.com", display.email)
        assertEquals("member", display.role)
        assertFalse(display.isOnline)
        assertFalse(display.isOwnProfile)
    }

    @Test
    fun `display name priority - member displayName first`() {
        val user = User(id = "u1", name = "UserName")
        val member = Member(id = "m1", userId = "u1", role = "owner", user = user, displayName = "DisplayName", name = "MemberName")
        val state = ProfileUiState(user = user, member = member, isOwnProfile = true)

        val display = resolveProfileDisplayData(state)

        assertEquals("DisplayName", display.name)
    }

    @Test
    fun `display name falls back to member name`() {
        val user = User(id = "u1", name = "UserName")
        val member = Member(id = "m1", userId = "u1", role = "owner", user = user, name = "MemberName")
        val state = ProfileUiState(user = user, member = member, isOwnProfile = true)

        val display = resolveProfileDisplayData(state)

        assertEquals("MemberName", display.name)
    }

    @Test
    fun `display name falls back to user name when no member`() {
        val user = User(id = "u1", name = "UserName")
        val state = ProfileUiState(user = user, member = null, isOwnProfile = true)

        val display = resolveProfileDisplayData(state)

        assertEquals("UserName", display.name)
    }

    @Test
    fun `initial is question mark when name is empty`() {
        val state = ProfileUiState(user = User(id = "u1"), member = null, isOwnProfile = true)

        val display = resolveProfileDisplayData(state)

        assertEquals("?", display.initial)
    }

    @Test
    fun `initial is uppercase first char of name`() {
        val user = User(id = "u1", name = "charlie")
        val state = ProfileUiState(user = user, isOwnProfile = true)

        val display = resolveProfileDisplayData(state)

        assertEquals("C", display.initial)
    }

    @Test
    fun `role empty when no member`() {
        val user = User(id = "u1", name = "Test")
        val state = ProfileUiState(user = user, member = null, isOwnProfile = true)

        val display = resolveProfileDisplayData(state)

        assertEquals("", display.role)
    }

    @Test
    fun `email empty when user has no email`() {
        val user = User(id = "u1", name = "Test")
        val state = ProfileUiState(user = user, isOwnProfile = false)

        val display = resolveProfileDisplayData(state)

        assertEquals("", display.email)
    }

    @Test
    fun `default state produces empty display data`() {
        val state = ProfileUiState()

        val display = resolveProfileDisplayData(state)

        assertEquals("", display.name)
        assertEquals("", display.email)
        assertEquals("", display.role)
        assertNull(display.avatar)
        assertEquals("?", display.initial)
        assertFalse(display.isOnline)
        assertTrue(display.isOwnProfile)
    }

    @Test
    fun `initial state defaults`() {
        val state = ProfileUiState()

        assertNull(state.user)
        assertNull(state.member)
        assertTrue(state.isOwnProfile)
        assertFalse(state.isOnline)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isEditing)
        assertEquals("", state.editName)
        assertFalse(state.isSaving)
        assertNull(state.saveError)
    }

    @Test
    fun `editing state tracks name changes`() {
        val state = ProfileUiState(
            isEditing = true,
            editName = "New Name"
        )

        assertTrue(state.isEditing)
        assertEquals("New Name", state.editName)
    }

    @Test
    fun `owner role resolved correctly`() {
        val member = Member(id = "m1", userId = "u1", role = "owner")
        val state = ProfileUiState(member = member)

        val display = resolveProfileDisplayData(state)

        assertEquals("owner", display.role)
    }
}
