package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NotificationCategory
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.EmptyLoreState
import com.example.ui.components.LoreCard
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.LocalLoreTheme

@Composable
fun NotificationsScreen(
    viewModel: SocialLoreViewModel,
    onNavigateTarget: (String) -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current
    var selectedCat by remember { mutableStateOf<NotificationCategory?>(null) }

    val unreadCount = remember(notifications) { notifications.count { !it.read } }
    val filtered = remember(notifications, selectedCat) {
        if (selectedCat == null) notifications
        else notifications.filter { NotificationCategory.fromRaw(it.category) == selectedCat }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "notif_header") {
                LoreCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notifications", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                if (unreadCount > 0) "$unreadCount unread alert(s)" else "All caught up",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.textSecondary
                            )
                        }
                        if (unreadCount > 0) {
                            OutlinedButton(onClick = { viewModel.markAllNotificationsRead() }) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Mark All Read")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCat == null,
                                onClick = { selectedCat = null },
                                label = { Text("All (${notifications.size})") }
                            )
                        }
                        items(NotificationCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCat == cat,
                                onClick = { selectedCat = if (selectedCat == cat) null else cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyLoreState(
                        title = "Quiet in the Archive",
                        subtitle = "Comments, reactions, friend requests, messages, and admin notices appear here in real time."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    LoreCard(
                        onClick = {
                            if (!item.read) viewModel.markNotificationRead(item.id)
                            onNavigateTarget(item.targetRoute)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (!item.read) theme.accent.copy(alpha = 0.25f) else theme.cardVariantColor
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (!item.read) theme.accent else theme.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (!item.read) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!item.read) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(theme.accent)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(item.body, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${NotificationCategory.fromRaw(item.category).label} • ${formatTimestamp(item.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
