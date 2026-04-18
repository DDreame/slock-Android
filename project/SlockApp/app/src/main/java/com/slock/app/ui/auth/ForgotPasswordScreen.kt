package com.slock.app.ui.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.ui.theme.Black
import com.slock.app.ui.theme.Cream
import com.slock.app.ui.theme.Error
import com.slock.app.ui.theme.Lime
import com.slock.app.ui.theme.NeoButton
import com.slock.app.ui.theme.NeoButtonSecondary
import com.slock.app.ui.theme.NeoCard
import com.slock.app.ui.theme.NeoLabel
import com.slock.app.ui.theme.NeoTextField
import com.slock.app.ui.theme.Orange
import com.slock.app.ui.theme.TextMuted
import com.slock.app.ui.theme.TextSecondary
import com.slock.app.ui.theme.White
import com.slock.app.ui.theme.Yellow
import com.slock.app.ui.theme.neoShadow
import com.slock.app.ui.theme.neoShadowSmall

@Composable
fun ForgotPasswordScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSendReset: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var resetSent by remember { mutableStateOf(false) }

    LaunchedEffect(state.resetEmailSent) {
        if (state.resetEmailSent) {
            resetSent = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ForgotPasswordHero(resetSent = resetSent)

        Spacer(modifier = Modifier.height(28.dp))

        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                if (resetSent) {
                    ForgotPasswordSuccessCard(
                        email = state.email,
                        onNavigateBack = onNavigateBack
                    )
                } else {
                    ForgotPasswordFormCard(
                        state = state,
                        onEmailChange = onEmailChange,
                        onSendReset = onSendReset
                    )
                }
            }
        }

        if (!resetSent) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Remembered your password? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                TextButton(
                    onClick = onNavigateBack,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Back to Sign In",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        ),
                        color = Black
                    )
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordFormCard(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSendReset: () -> Unit
) {
    Text(
        text = "Reset Password",
        style = MaterialTheme.typography.titleLarge,
        color = Black
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Enter the email tied to your account and we'll send you a reset link.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    NeoLabel("EMAIL")
    NeoTextField(
        value = state.email,
        onValueChange = onEmailChange,
        placeholder = "you@example.com",
        keyboardType = KeyboardType.Email,
        focusHighlight = Orange
    )

    Text(
        text = "We'll only use this to send password reset instructions.",
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        modifier = Modifier.padding(top = 6.dp)
    )

    state.error?.let {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = it,
            color = Error,
            style = MaterialTheme.typography.bodySmall
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    NeoButton(
        text = if (state.isLoading) "SENDING..." else "SEND RESET LINK",
        onClick = onSendReset,
        enabled = !state.isLoading && state.email.isNotBlank(),
        containerColor = Orange
    )
}

@Composable
private fun ForgotPasswordSuccessCard(
    email: String,
    onNavigateBack: () -> Unit
) {
    Text(
        text = "Check your inbox",
        style = MaterialTheme.typography.titleLarge,
        color = Black
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "We sent a password reset link to:",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )

    Spacer(modifier = Modifier.height(16.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadowSmall()
            .background(White)
            .border(2.dp, Black, RectangleShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = email,
            style = MaterialTheme.typography.titleSmall,
            color = Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Open the email and follow the reset steps to get back into Slock.",
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted
    )

    Spacer(modifier = Modifier.height(20.dp))

    NeoButtonSecondary(
        text = "BACK TO SIGN IN",
        onClick = onNavigateBack,
        containerColor = Yellow
    )
}

@Composable
private fun ForgotPasswordHero(resetSent: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "forgot-password-float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "forgot-password-offset"
    )

    val badgeColor = if (resetSent) Lime else Orange
    val badgeText = if (resetSent) "OK" else "?"
    val title = if (resetSent) "Reset Link Sent" else "Forgot Password"
    val subtitle = if (resetSent) {
        "One more step and you're back in."
    } else {
        "Reset access without losing your place."
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .offset(y = offsetY.dp)
                .size(68.dp)
                .neoShadow()
                .background(badgeColor)
                .border(3.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
