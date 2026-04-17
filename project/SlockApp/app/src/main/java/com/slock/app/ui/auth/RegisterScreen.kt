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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.slock.app.ui.theme.*

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegister: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onRegisterSuccess()
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
        // Logo area (lime colored for register)
        RegisterLogoArea()

        Spacer(modifier = Modifier.height(28.dp))

        // Register Card
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Display Name
                NeoLabel("DISPLAY NAME")
                NeoTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    placeholder = "Your name",
                    focusHighlight = Lime
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email
                NeoLabel("EMAIL")
                NeoTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    placeholder = "you@example.com",
                    keyboardType = KeyboardType.Email,
                    focusHighlight = Lime
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password
                NeoLabel("PASSWORD")
                NeoTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    placeholder = "Min 8 characters",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePassword = { passwordVisible = !passwordVisible },
                    focusHighlight = Lime
                )
                Text(
                    text = "At least 8 characters, include letters & numbers",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Password
                NeoLabel("CONFIRM PASSWORD")
                NeoTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Re-enter password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePassword = { passwordVisible = !passwordVisible },
                    focusHighlight = Lime
                )

                // Error
                state.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
                }

                // Password mismatch
                if (confirmPassword.isNotEmpty() && state.password.isNotEmpty() && confirmPassword != state.password) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Passwords don't match", color = Error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Create Account button
                NeoButton(
                    text = if (state.isLoading) "CREATING..." else "CREATE ACCOUNT",
                    onClick = { onRegister() },
                    enabled = !state.isLoading
                            && state.name.isNotBlank()
                            && state.email.isNotBlank()
                            && state.password.length >= 8
                            && state.password == confirmPassword
                )
            }
        }

        // Footer
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            TextButton(
                onClick = onNavigateToLogin,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Sign In",
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

@Composable
private fun RegisterLogoArea() {
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
                .size(64.dp)
                .neoShadow()
                .background(Lime)
                .border(3.dp, Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineSmall,
            color = Black
        )
    }
}
