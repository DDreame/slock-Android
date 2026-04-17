package com.slock.app.ui.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.data.model.Channel
import com.slock.app.ui.theme.Black
import com.slock.app.ui.theme.Cream
import com.slock.app.ui.theme.NeoButton
import com.slock.app.ui.theme.NeoButtonSecondary
import com.slock.app.ui.theme.NeoCard
import com.slock.app.ui.theme.NeoPressableBox
import com.slock.app.ui.theme.Orange
import com.slock.app.ui.theme.Pink
import com.slock.app.ui.theme.SpaceGrotesk
import com.slock.app.ui.theme.SpaceMono
import com.slock.app.ui.theme.TextSecondary
import com.slock.app.ui.theme.White
import com.slock.app.ui.theme.Yellow

@Composable
fun SavedChannelsScreen(
    state: SavedChannelsUiState,
    onNavigateBack: () -> Unit,
    onOpenChannel: (Channel) -> Unit,
    onRemoveSavedChannel: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        SavedChannelsHeader(onBack = onNavigateBack)

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Black, strokeWidth = 2.dp)
                }
            }

            state.error != null && state.channels.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Black,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        NeoButtonSecondary(
                            text = "RETRY",
                            onClick = onRetry,
                            containerColor = Yellow
                        )
                    }
                }
            }

            state.channels.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "☆", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No saved channels yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Black
                        )
                        Text(
                            text = "Open a channel and tap the star in the header to save it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.channels, key = { it.id.orEmpty() }) { channel ->
                        SavedChannelCard(
                            channel = channel,
                            isRemoving = channel.id.orEmpty() in state.removingIds,
                            onOpen = { onOpenChannel(channel) },
                            onRemove = { channel.id?.let(onRemoveSavedChannel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedChannelsHeader(onBack: () -> Unit) {
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
                    text = "Saved Channels",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Black
                )
                Text(
                    text = "Bookmarks for the current server",
                    fontFamily = SpaceMono,
                    fontSize = 11.sp,
                    color = Black.copy(alpha = 0.7f)
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
private fun SavedChannelCard(
    channel: Channel,
    isRemoving: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    NeoCard(containerColor = White, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = (channel.name ?: channel.id).orEmpty().ifBlank { "Unnamed channel" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = (channel.type ?: "channel").uppercase(),
                fontFamily = SpaceMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeoButtonSecondary(
                    text = "OPEN",
                    onClick = onOpen,
                    containerColor = Yellow,
                    modifier = Modifier.weight(1f)
                )
                NeoButton(
                    text = if (isRemoving) "REMOVING..." else "REMOVE",
                    onClick = onRemove,
                    enabled = !isRemoving,
                    containerColor = Pink,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
