package com.slock.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.BuildConfig
import com.slock.app.data.local.NotificationPreference
import com.slock.app.ui.theme.Black
import com.slock.app.ui.theme.Cream
import com.slock.app.ui.theme.Cyan
import com.slock.app.ui.theme.Lavender
import com.slock.app.ui.theme.Lime
import com.slock.app.ui.theme.NeoButton
import com.slock.app.ui.theme.NeoButtonSecondary
import com.slock.app.ui.theme.NeoCard
import com.slock.app.ui.theme.NeoPressableBox
import com.slock.app.ui.theme.Orange
import com.slock.app.ui.theme.Pink
import com.slock.app.ui.theme.SpaceGrotesk
import com.slock.app.ui.theme.SpaceMono
import com.slock.app.ui.theme.White
import com.slock.app.ui.theme.Yellow

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onNavigateBack: () -> Unit,
    onNotificationPreferenceChange: (NotificationPreference) -> Unit,
    onRefreshAccount: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onSendFeedback: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        SettingsHeader(onBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SETTINGS",
                        fontFamily = SpaceMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Black.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Control notifications, account details, theme, and app info.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Black
                    )
                }
            }

            SettingsSection(title = "Notifications") {
                NotificationPreference.entries.forEachIndexed { index, preference ->
                    NotificationOptionCard(
                        preference = preference,
                        isSelected = state.notificationPreference == preference,
                        onClick = { onNotificationPreferenceChange(preference) }
                    )
                    if (index != NotificationPreference.entries.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            SettingsSection(title = "Account") {
                SettingsInfoCard(
                    accentColor = Cyan,
                    title = state.userName.ifBlank { "Signed-in user" },
                    subtitle = state.userEmail.ifBlank { "Email not available" }
                ) {
                    InfoRow(label = "USER ID", value = state.userId.ifBlank { "Not loaded yet" })
                    Spacer(modifier = Modifier.height(14.dp))
                    NeoButtonSecondary(
                        text = if (state.isRefreshingAccount) "REFRESHING..." else "REFRESH ACCOUNT",
                        onClick = onRefreshAccount,
                        containerColor = Cyan,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NeoButtonSecondary(
                        text = "VIEW PROFILE",
                        onClick = onOpenProfile,
                        containerColor = Yellow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SettingsSection(title = "Theme") {
                SettingsInfoCard(
                    accentColor = Lavender,
                    title = "Neo Brutalism",
                    subtitle = "Current app theme"
                )
            }

            SettingsSection(title = "About") {
                SettingsInfoCard(
                    accentColor = Lime,
                    title = "Slock Android",
                    subtitle = "Mobile client for collaborative human + agent workflows"
                ) {
                    InfoRow(label = "VERSION", value = BuildConfig.VERSION_NAME)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(label = "PACKAGE", value = BuildConfig.APPLICATION_ID)
                }
            }

            if (state.error != null) {
                NeoCard(containerColor = Pink, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = state.error,
                        modifier = Modifier.padding(16.dp),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }
            }

            NeoButtonSecondary(
                text = "SEND FEEDBACK",
                onClick = onSendFeedback,
                containerColor = Cyan,
                modifier = Modifier.fillMaxWidth()
            )

            NeoButton(
                text = "LOG OUT",
                onClick = onLogout,
                containerColor = Pink,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Orange)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoPressableBox(onClick = onBack, size = 36.dp, backgroundColor = White) {
                Text(text = "←", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Settings",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Black
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Black)
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            fontFamily = SpaceMono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Black.copy(alpha = 0.65f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun NotificationOptionCard(
    preference: NotificationPreference,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        when (preference) {
            NotificationPreference.MENTIONS_ONLY -> Lavender
            NotificationPreference.ALL_MESSAGES -> Cyan
            NotificationPreference.MUTE -> Pink
        }
    } else {
        White
    }

    NeoCard(modifier = Modifier.fillMaxWidth(), containerColor = backgroundColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(2.dp, Black, RectangleShape)
                    .background(if (isSelected) Black else Color.Transparent)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preference.title,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preference.description,
                    fontFamily = SpaceGrotesk,
                    fontSize = 12.sp,
                    color = Black.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsInfoCard(
    accentColor: Color,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit = {}
) {
    NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 8.dp)
                    .background(accentColor)
                    .border(2.dp, Black, RectangleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontFamily = SpaceGrotesk,
                fontSize = 13.sp,
                color = Black.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontFamily = SpaceMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Black.copy(alpha = 0.55f)
        )
        Text(
            text = value,
            fontFamily = SpaceGrotesk,
            fontSize = 13.sp,
            color = Black,
            lineHeight = 18.sp
        )
    }
}
