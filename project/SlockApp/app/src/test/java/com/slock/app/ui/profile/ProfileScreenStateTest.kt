package com.slock.app.ui.profile

import com.slock.app.data.model.Member
import com.slock.app.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileScreenStateTest {

    @Test
    fun `loading state when no user member or error`() {
        val state = ProfileUiState(isLoading = true)

        assertEquals(ProfileContentState.Loading, resolveProfileContentState(state))
    }

    @Test
    fun `error state when no content is available`() {
        val state = ProfileUiState(error = "Failed to load profile")

        assertEquals(
            ProfileContentState.Error("Failed to load profile"),
            resolveProfileContentState(state)
        )
    }

    @Test
    fun `content state wins when user exists even with stale error and loading`() {
        val state = ProfileUiState(
            user = User(id = "u1", name = "Alice"),
            isLoading = true,
            error = "Stale error"
        )

        assertEquals(ProfileContentState.Content, resolveProfileContentState(state))
    }

    @Test
    fun `content state wins when member exists for other profile without user`() {
        val state = ProfileUiState(
            member = Member(id = "m1", userId = "u2", name = "Bob"),
            isOwnProfile = false,
            error = "Stale error"
        )

        assertEquals(ProfileContentState.Content, resolveProfileContentState(state))
    }

    @Test
    fun `profile header context hides context for own profile`() {
        assertEquals(null, resolveProfileHeaderContext(isOwnProfile = true, contextLabel = "Acme Server · Members"))
    }

    @Test
    fun `profile header context uses provided context for other profile`() {
        assertEquals(
            "Acme Server · Members",
            resolveProfileHeaderContext(isOwnProfile = false, contextLabel = "Acme Server · Members")
        )
    }
}
