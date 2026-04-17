package com.slock.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.ui.theme.*

internal sealed interface ProfileContentState {
    data object Loading : ProfileContentState
    data class Error(val message: String) : ProfileContentState
    data object Content : ProfileContentState
}

internal fun resolveProfileContentState(state: ProfileUiState): ProfileContentState = when {
    state.user != null || state.member != null -> ProfileContentState.Content
    state.error != null -> ProfileContentState.Error(state.error)
    else -> ProfileContentState.Loading
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    contextLabel: String = "",
    onNavigateBack: () -> Unit = {},
    onStartEditing: () -> Unit = {},
    onCancelEditing: () -> Unit = {},
    onEditNameChange: (String) -> Unit = {},
    onSaveName: () -> Unit = {},
    onLogout: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val contentState = resolveProfileContentState(state)
    val headerContextLabel = resolveProfileHeaderContext(state.isOwnProfile, contextLabel)
    val profileKey = state.user?.id ?: state.member?.userId ?: state.isOwnProfile.toString()
    var showLogoutConfirm by remember(profileKey) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Cyan
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeoPressableBox(
                    onClick = onNavigateBack,
                    size = 36.dp,
                    backgroundColor = Color.White
                ) {
                    Text("\u2190", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                if (headerContextLabel != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (state.isOwnProfile) "My Profile" else "User Profile",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            headerContextLabel,
                            fontFamily = SpaceMono,
                            fontSize = 11.sp,
                            color = Color.Black.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        if (state.isOwnProfile) "My Profile" else "User Profile",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
        Divider(thickness = 3.dp, color = Color.Black)

        when (contentState) {
            ProfileContentState.Loading -> {
                NeoSkeletonCardList(count = 3)
            }

            is ProfileContentState.Error -> {
                NeoErrorState(
                    message = contentState.message,
                    onRetry = onRetry
                )
            }

            ProfileContentState.Content -> {
                val displayData = resolveProfileDisplayData(state)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Avatar with shadow
                        Box(modifier = Modifier.size(75.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .offset(3.dp, 3.dp)
                                    .background(Color.Black, RectangleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .border(2.dp, Color.Black, RectangleShape)
                                    .background(Cyan, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    displayData.initial,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk
                                )
                            }
                            // Online status dot
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(14.dp)
                                    .border(2.dp, Color.Black, RectangleShape)
                                    .background(
                                        if (displayData.isOnline) Lime else Color(0xFFCCCCCC),
                                        RectangleShape
                                    )
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                displayData.name.ifEmpty { "Unknown" },
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (displayData.email.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    displayData.email,
                                    fontFamily = SpaceMono,
                                    fontSize = 12.sp,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (displayData.role.isNotEmpty()) {
                                    RoleBadge(displayData.role)
                                }
                                StatusBadge(displayData.isOnline)
                            }
                        }
                    }
                    Divider(thickness = 3.dp, color = Color.Black)

                    // Details Card
                    SectionTitle("Details")
                    ProfileNeoCard(stripColor = Cyan) {
                        InfoRow("Name", displayData.name.ifEmpty { "\u2014" })
                        if (displayData.email.isNotEmpty()) {
                            InfoRow("Email", displayData.email, isMono = true)
                        }
                        InfoRow("Role", displayData.role.replaceFirstChar { it.uppercaseChar() }.ifEmpty { "\u2014" })
                        InfoRow("Status", if (displayData.isOnline) "Online" else "Offline")
                        InfoRow("Last Active", displayData.lastActiveText)
                    }

                    // Edit Name (own profile only)
                    if (state.isOwnProfile) {
                        SectionTitle("Edit Profile")
                        ProfileNeoCard(stripColor = Yellow) {
                            if (state.isEditing) {
                                NeoTextField(
                                    value = state.editName,
                                    onValueChange = onEditNameChange,
                                    placeholder = "Enter new name"
                                )
                                if (state.saveError != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        state.saveError,
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 12.sp,
                                        color = Pink
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(Modifier.weight(1f)) {
                                        NeoButtonSecondary(
                                            text = "CANCEL",
                                            onClick = onCancelEditing
                                        )
                                    }
                                    Box(Modifier.weight(1f)) {
                                        NeoButton(
                                            text = if (state.isSaving) "SAVING..." else "SAVE",
                                            onClick = { if (!state.isSaving) onSaveName() }
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Update your display name",
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 13.sp,
                                        color = Color.Black.copy(alpha = 0.6f)
                                    )
                                    ActionButton("EDIT", Cyan, Modifier, onStartEditing)
                                }
                            }
                        }

                        // Logout
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            NeoButton(
                                text = "LOG OUT",
                                onClick = { showLogoutConfirm = true },
                                containerColor = Pink
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }

                if (showLogoutConfirm) {
                    NeoConfirmDialog(
                        title = "Log Out",
                        message = "Are you sure you want to log out?",
                        confirmText = "LOG OUT",
                        confirmColor = Pink,
                        onConfirm = {
                            showLogoutConfirm = false
                            onLogout()
                        },
                        onDismiss = { showLogoutConfirm = false }
                    )
                }
            }
        }
    }
}

internal fun resolveProfileHeaderContext(isOwnProfile: Boolean, contextLabel: String): String? {
    if (isOwnProfile) return null
    return contextLabel.trim().takeIf { it.isNotEmpty() }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title.uppercase(),
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp)
    )
}

@Composable
private fun RoleBadge(role: String) {
    val bgColor = when (role.lowercase()) {
        "owner" -> Yellow
        "admin" -> Lavender
        else -> Color(0xFFE0E0E0)
    }
    Box(
        modifier = Modifier
            .border(1.dp, Color.Black, RectangleShape)
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            role.uppercase(),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun StatusBadge(isOnline: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .border(1.dp, Color.Black, RectangleShape)
                .background(if (isOnline) Lime else Color(0xFFCCCCCC))
        )
        Text(
            if (isOnline) "Online" else "Offline",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color.Black.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ProfileNeoCard(
    stripColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .offset(4.dp, 4.dp)
                .background(Color.Black, RectangleShape)
                .height(IntrinsicSize.Min)
        ) {
            Column(Modifier.padding(12.dp).alpha(0f), content = content)
        }
        Column(
            modifier = modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RectangleShape)
                .background(Color.White, RectangleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(stripColor)
            )
            Column(Modifier.padding(12.dp, 14.dp), content = content)
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun InfoRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label.uppercase(),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            color = Color.Black.copy(alpha = 0.5f)
        )
        Text(
            value,
            fontFamily = if (isMono) SpaceMono else SpaceGrotesk,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isMono) 11.sp else 13.sp
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(2.dp, 2.dp)
                .background(Color.Black, RectangleShape)
        )
        Box(
            modifier = Modifier
                .border(2.dp, Color.Black, RectangleShape)
                .background(bgColor, RectangleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
