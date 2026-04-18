package com.slock.app.ui.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
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

@Composable
fun MembersListScreen(
    state: MembersUiState,
    onMemberClick: (MemberItem) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    showHeader: Boolean = true
) {
    val onlineMembers = state.members.filter { it.isOnline }
    val offlineMembers = state.members.filter { !it.isOnline }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        if (showHeader) {
            MembersHeader(
                memberCount = state.members.size,
                onBack = onNavigateBack
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> {
                    NeoSkeletonCardList()
                }
                state.error != null && state.members.isEmpty() -> {
                    NeoErrorState(
                        message = "Members 加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = onRetry,
                        logContext = state.error
                    )
                }
                state.members.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "\uD83D\uDC65", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No members yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Black
                        )
                        Text(
                            "Members will appear here when they join",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        // Online section
                        if (onlineMembers.isNotEmpty()) {
                            item {
                                SectionLabel(
                                    label = "ONLINE",
                                    count = onlineMembers.size
                                )
                            }
                            items(onlineMembers) { member ->
                                MemberCard(
                                    member = member,
                                    onClick = { onMemberClick(member) }
                                )
                            }
                        }

                        // Offline section
                        if (offlineMembers.isNotEmpty()) {
                            item {
                                SectionLabel(
                                    label = "OFFLINE",
                                    count = offlineMembers.size
                                )
                            }
                            items(offlineMembers) { member ->
                                MemberCard(
                                    member = member,
                                    onClick = { onMemberClick(member) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Cyan header matching mockup
@Composable
private fun MembersHeader(
    memberCount: Int,
    onBack: () -> Unit
) {
    Surface(color = Cyan) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoPressableBox(onClick = onBack) {
                Text(text = "\u2190", fontSize = 18.sp, color = Black)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Members",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Black
            )

            Spacer(modifier = Modifier.weight(1f))

            // Member count badge
            Box(
                modifier = Modifier
                    .background(White)
                    .border(1.5.dp, Black, RectangleShape)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$memberCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceMono,
                    color = Black
                )
            }
        }
    }
    Divider(thickness = 3.dp, color = Black)
}

// Section label with count badge
@Composable
private fun SectionLabel(label: String, count: Int) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Black.copy(alpha = 0.5f)
        )
        Box(
            modifier = Modifier
                .background(White)
                .border(1.dp, Black, RectangleShape)
                .padding(horizontal = 5.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        }
    }
}

// Member card matching mockup
@Composable
private fun MemberCard(
    member: MemberItem,
    onClick: () -> Unit
) {
    val cardAlpha = if (member.isOnline) 1f else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .alpha(cardAlpha)
            .neoShadow(3.dp, 3.dp)
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .clickable(onClick = onClick)
            .padding(12.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar with online dot
        Box {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (member.isAgent) Orange else Cyan)
                    .border(2.dp, Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Black
                )
            }

            // Online status dot (bottom-right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(10.dp)
                    .background(if (member.isOnline) Lime else Color(0xFFCCCCCC))
                    .border(1.5.dp, Black, RectangleShape)
            )
        }

        // Name + subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = member.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = member.subtitle,
                fontFamily = SpaceMono,
                fontSize = 10.sp,
                color = Black.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Role/type badge
        if (member.isAgent) {
            Box(
                modifier = Modifier
                    .background(Orange)
                    .border(1.dp, Black, RectangleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AGENT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Black
                )
            }
        } else {
            val badgeColor = when (member.role.lowercase()) {
                "owner" -> Color(0xFFFFD700)
                "admin" -> Lavender
                else -> Cream
            }
            Box(
                modifier = Modifier
                    .background(badgeColor)
                    .border(1.dp, Black, RectangleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = member.role.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Black
                )
            }
        }
    }
}
