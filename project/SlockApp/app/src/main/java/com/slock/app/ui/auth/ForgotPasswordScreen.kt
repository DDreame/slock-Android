package com.slock.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSendReset: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var resetSent by remember { mutableStateOf(false) }
    
    LaunchedEffect(state.resetEmailSent) {
        if (state.resetEmailSent) resetSent = true
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (resetSent) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✓", fontSize = 64.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Check your email", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Password reset instructions sent to ${state.email}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Sign In") }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Forgot your password?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Enter your email for reset instructions", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    state.error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onSendReset,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !state.isLoading && state.email.isNotBlank()
                    ) {
                        if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Send Reset Instructions")
                    }
                }
            }
        }
    }
}
