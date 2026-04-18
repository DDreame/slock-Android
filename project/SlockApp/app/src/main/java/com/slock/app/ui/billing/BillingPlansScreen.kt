package com.slock.app.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.ui.theme.Black
import com.slock.app.ui.theme.Cream
import com.slock.app.ui.theme.Cyan
import com.slock.app.ui.theme.NeoButtonSecondary
import com.slock.app.ui.theme.NeoCard
import com.slock.app.ui.theme.NeoPressableBox
import com.slock.app.ui.theme.Orange
import com.slock.app.ui.theme.Pink
import com.slock.app.ui.theme.SpaceGrotesk
import com.slock.app.ui.theme.SpaceMono
import com.slock.app.ui.theme.TextMuted
import com.slock.app.ui.theme.White
import com.slock.app.ui.theme.Yellow
import com.slock.app.ui.theme.neoShadowSmall

@Composable
fun BillingPlansScreen(
    state: BillingPlansUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        BillingPlansHeader(onBack = onNavigateBack)

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
                        text = "CURRENT PLAN",
                        fontFamily = SpaceMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Black.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PlanBadge(planName = state.planSummary.planName)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = state.planSummary.statusLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.planSummary.renewalLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            BillingInfoCard(
                title = "Upgrade Prompt",
                accentColor = Orange,
                body = "Billing changes and upgrades are still managed on web. Android now shows your current plan and status so this page is no longer a dead end."
            )

            BillingInfoCard(
                title = "What You Can Do Here",
                accentColor = Cyan,
                body = state.notice
            )

            if (state.error != null) {
                BillingInfoCard(
                    title = "Live Sync Status",
                    accentColor = Pink,
                    body = state.error
                )
            }

            NeoButtonSecondary(
                text = if (state.isLoading) "LOADING PLAN..." else "REFRESH PLAN",
                onClick = onRetry,
                containerColor = Yellow,
                modifier = Modifier.fillMaxWidth()
            )

            NeoButtonSecondary(
                text = "BACK TO SETTINGS",
                onClick = onNavigateBack,
                containerColor = White,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BillingPlansHeader(onBack: () -> Unit) {
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
            Column {
                Text(
                    text = "Billing / Plans",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Black
                )
                Text(
                    text = "Read-only subscription overview",
                    fontFamily = SpaceMono,
                    fontSize = 11.sp,
                    color = Black.copy(alpha = 0.65f)
                )
            }
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
private fun PlanBadge(planName: String) {
    Box(
        modifier = Modifier
            .neoShadowSmall()
            .background(Yellow, RectangleShape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = planName.uppercase(),
            fontFamily = SpaceGrotesk,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Black
        )
    }
}

@Composable
private fun BillingInfoCard(
    title: String,
    accentColor: androidx.compose.ui.graphics.Color,
    body: String
) {
    NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .background(accentColor)
                    .size(width = 12.dp, height = 120.dp)
            )
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = title.uppercase(),
                    fontFamily = SpaceMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Black.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Black
                )
            }
        }
    }
}
