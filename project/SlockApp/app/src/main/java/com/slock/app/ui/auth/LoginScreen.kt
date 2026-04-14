package com.slock.app.ui.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.slock.app.ui.theme.*

@Composable
fun LoginScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    onJoinWithInvite: (String) -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteLink by remember { mutableStateOf("") }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
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
        // Logo area
        LogoArea()

        Spacer(modifier = Modifier.height(32.dp))

        // Login Card
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email field
                NeoLabel("EMAIL")
                NeoTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    placeholder = "you@example.com",
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password field
                NeoLabel("PASSWORD")
                NeoTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    placeholder = "Enter password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePassword = { passwordVisible = !passwordVisible }
                )

                // Forgot password link
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Forgot password?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                        color = Black
                    )
                }

                // Error message
                state.error?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sign In button
                NeoButton(
                    text = if (state.isLoading) "SIGNING IN..." else "SIGN IN",
                    onClick = onLogin,
                    enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Divider
                NeoDivider("or")

                Spacer(modifier = Modifier.height(24.dp))

                // Invite link button
                NeoButtonSecondary(
                    text = "Join via Invite Link",
                    onClick = { showInviteDialog = true }
                )
            }
        }

        // Footer
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            TextButton(
                onClick = onNavigateToRegister,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = Black
                )
            }
        }
    }

    // Invite Link Dialog
    if (showInviteDialog) {
        InviteLinkDialog(
            inviteLink = inviteLink,
            onLinkChange = { inviteLink = it },
            onJoin = {
                if (inviteLink.isNotBlank()) onJoinWithInvite(inviteLink)
                showInviteDialog = false
            },
            onDismiss = { showInviteDialog = false }
        )
    }
}

@Composable
private fun LogoArea() {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .offset(y = offsetY.dp)
                .size(72.dp)
                .neoShadow()
                .background(Yellow)
                .border(3.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Slock",
            style = MaterialTheme.typography.headlineMedium,
            color = Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Where humans and AI agents collaborate",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun InviteLinkDialog(
    inviteLink: String,
    onLinkChange: (String) -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "Paste Invite Link",
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeoTextField(
                    value = inviteLink,
                    onValueChange = onLinkChange,
                    placeholder = "https://slock.ai/invite/..."
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeoButton(
                    text = "JOIN SERVER",
                    onClick = onJoin
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeoButtonSecondary(
                    text = "Cancel",
                    onClick = onDismiss,
                    containerColor = Cream
                )
            }
        }
    }
}
