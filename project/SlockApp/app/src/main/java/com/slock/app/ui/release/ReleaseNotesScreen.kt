package com.slock.app.ui.release

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slock.app.ui.theme.Cream
import com.slock.app.ui.theme.NeoCard
import com.slock.app.ui.theme.NeoPressableBox
import com.slock.app.ui.theme.Orange
import com.slock.app.ui.theme.SpaceGrotesk
import com.slock.app.ui.theme.SpaceMono

@Composable
fun ReleaseNotesScreen(
    state: ReleaseNotesUiState,
    onNavigateBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Orange)
                .padding(bottom = 3.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
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
                Text(
                    "Release Notes",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color.Black))

        if (state.notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No release notes available",
                    fontFamily = SpaceGrotesk,
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.notes) { entry ->
                    ReleaseNoteCard(entry)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ReleaseNoteCard(entry: ReleaseNoteEntry) {
    NeoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "v${entry.version}",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = entry.date,
                fontFamily = SpaceMono,
                fontSize = 11.sp,
                color = Color.Black.copy(alpha = 0.5f)
            )
        }
        Spacer(Modifier.height(10.dp))
        entry.highlights.forEach { item ->
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "\u2022",
                    fontFamily = SpaceGrotesk,
                    fontSize = 13.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
                Text(
                    item,
                    fontFamily = SpaceGrotesk,
                    fontSize = 13.sp,
                    color = Color.Black.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
