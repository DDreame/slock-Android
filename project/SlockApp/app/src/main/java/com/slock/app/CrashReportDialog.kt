package com.slock.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.slock.app.ui.theme.*

@Composable
fun CrashReportDialog(
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeoCard(containerColor = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "\uD83D\uDEA8",
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "App Crashed",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The app crashed during the last session. Would you like to send a crash report to help us fix the issue?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                NeoButton(
                    text = "SEND CRASH REPORT",
                    onClick = onSend,
                    containerColor = Pink,
                    contentColor = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeoButtonSecondary(
                    text = "Dismiss",
                    onClick = onDismiss,
                    containerColor = Cream
                )
            }
        }
    }
}
