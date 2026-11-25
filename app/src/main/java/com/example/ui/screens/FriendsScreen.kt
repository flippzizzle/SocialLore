package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Friendship
import com.example.data.model.MemberProfile
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.EmptyLoreState
import com.example.ui.components.LoreAvatar
import com.example.ui.components.LoreCard
import com.example.ui.theme.LocalLoreTheme

@Composable
fun FriendsScreen(
    viewModel: SocialLoreViewModel,
    onOpenProfile: (String) -> Unit,
    onOpenMessageThread: (String) -> Unit
) {
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val approvedProfiles by viewModel.approvedProfiles.collectAsStateWithLifecycle()
    val friendRequests by viewModel.friendRequests.collectAsStateWithLifecycle()
    val friendships by viewModel.friendships.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current
    val clipboard = LocalClipboardManager.current

    var friendCodeInput by remember { mutableStateOf("") }
    var confirmRemoveFriendship by remember { mutableStateOf<Pair<Friendship, String>?>(null) }

    val incomingRequests = remember(friendRequests, viewModel.currentUserId) {
        friendRequests.filter { it.toUid == viewModel.currentUserId && it.status == "PENDING" }
    }
    val outgoingRequests = remember(friendRequests, viewModel.currentUserId) {
        friendRequests.filter { it.fromUid == viewModel.currentUserId && it.status == "PENDING" }
    }

    val friendUidToFriendship = remember(friendships, viewModel.currentUserId) {
        buildMap {
            friendships.forEach { fs ->
                fs.participantUids.firstOrNull { it != viewModel.currentUserId }?.let { otherUid ->
                    put(otherUid, fs)
                }
            }
        }
    }

    val friendProfiles = remember(approvedProfiles, friendUidToFriendship) {
        approvedProfiles.filter { it.userId in friendUidToFriendship.keys }
    }

    val pendingOtherUids = remember(incomingRequests, outgoingRequests) {
        (incomingRequests.map { it.fromUid } + outgoingRequests.map { it.toUid }).toSet()
    }

    val discoverableMembers = remember(approvedProfiles, friendUidToFriendship, pendingOtherUids, viewModel.currentUserId) {
        approvedProfiles.filter {
            it.userId != viewModel.currentUserId &&
                it.userId !in friendUidToFriendship.keys &&
                it.userId !in pendingOtherUids
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Own Friend Code & Add by Code Card
            item(key = "friend_code_card") {
                LoreCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Your Friend Code", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    val myCode = myProfile?.friendCode ?: "—"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = theme.cardVariantColor,
                        border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = myCode,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = theme.accent
                            )
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(myCode))
                                    viewModel.showMessage("Friend code copied: $myCode")
                                },
                                modifier = Modifier.testTag("copy_friend_code_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Share your code with approved Social Lore members. Friend codes never bypass administrator membership approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = friendCodeInput,
                            onValueChange = { friendCodeInput = it.uppercase() },
                            label = { Text("Enter Friend Code") },
                            placeholder = { Text("LORE-XXXXXX") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("friend_code_input")
                        )
                        Button(
                            onClick = {
                                viewModel.sendFriendRequestByCode(friendCodeInput) {
                                    friendCodeInput = ""
                                }
                            },
                            enabled = friendCodeInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                            modifier = Modifier.testTag("send_friend_code_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add")
                        }
                    }
                }
            }

            // Incoming Requests
            if (incomingRequests.isNotEmpty()) {
                item(key = "incoming_header") {
                    Text(
                        "Incoming Friend Requests (${incomingRequests.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                items(incomingRequests, key = { it.id }) { req ->
                    LoreCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenProfile(req.fromUid) }
                            ) {
                                LoreAvatar(req.fromAvatar, req.fromName, size = 44.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(req.fromName, style = MaterialTheme.typography.titleMedium)
                                    Text("Wants to connect", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { viewModel.respondToFriendRequest(req, accept = true) }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Accept request", tint = theme.accent)
                                }
                                IconButton(
                                    onClick = { viewModel.respondToFriendRequest(req, accept = false) }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Decline request", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Outgoing Requests
            if (outgoingRequests.isNotEmpty()) {
                item(key = "outgoing_header") {
                    Text(
                        "Sent Requests (${outgoingRequests.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                items(outgoingRequests, key = { it.id }) { req ->
                    LoreCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(req.toName, style = MaterialTheme.typography.titleMedium)
                                Text("Pending acceptance", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                            }
                            TextButton(onClick = { viewModel.cancelFriendRequest(req.id) }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Current Friends List
            item(key = "friends_header") {
                Text("Friends (${friendProfiles.size})", style = MaterialTheme.typography.titleLarge)
            }
            if (friendProfiles.isEmpty()) {
                item {
                    EmptyLoreState(
                        title = "No Friends Connected Yet",
                        subtitle = "Enter a friend code above or send a request to another approved circle member below."
                    )
                }
            } else {
                items(friendProfiles, key = { it.userId }) { friend ->
                    val fs = friendUidToFriendship[friend.userId]
                    LoreCard(onClick = { onOpenProfile(friend.userId) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                LoreAvatar(friend.avatarUrl, friend.displayName, size = 48.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
                                    if (!friend.tagline.isNullOrBlank()) {
                                        Text(
                                            friend.tagline,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = theme.textSecondary
                                        )
                                    }
                                    Text(
                                        friend.friendCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.accent
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        viewModel.openDirectChat(friend, onOpenMessageThread)
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message ${friend.displayName}", tint = theme.accent)
                                }
                                if (fs != null) {
                                    IconButton(
                                        onClick = { confirmRemoveFriendship = fs to friend.displayName }
                                    ) {
                                        Icon(Icons.Default.PersonRemove, contentDescription = "Remove ${friend.displayName}")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Discover Approved Circle Members
            if (discoverableMembers.isNotEmpty()) {
                item(key = "circle_directory_header") {
                    Text(
                        "Approved Circle Members (${discoverableMembers.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                items(discoverableMembers, key = { "dir_${it.userId}" }) { member ->
                    MemberDirectoryRow(
                        member = member,
                        onOpenProfile = { onOpenProfile(member.userId) },
                        onSendRequest = { viewModel.sendFriendRequest(member) }
                    )
                }
            }
        }
    }

    confirmRemoveFriendship?.let { (friendship, name) ->
        AlertDialog(
            onDismissRequest = { confirmRemoveFriendship = null },
            title = { Text("Remove Friend") },
            text = { Text("Remove $name from your friends list? You can reconnect anytime with their friend code.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFriend(friendship.id)
                        confirmRemoveFriendship = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveFriendship = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MemberDirectoryRow(
    member: MemberProfile,
    onOpenProfile: () -> Unit,
    onSendRequest: () -> Unit
) {
    val theme = LocalLoreTheme.current
    LoreCard(onClick = onOpenProfile) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                LoreAvatar(member.avatarUrl, member.displayName, size = 44.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(member.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        member.tagline?.takeIf { it.isNotBlank() } ?: member.friendCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondary
                    )
                }
            }
            OutlinedButton(onClick = onSendRequest) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Connect")
            }
        }
    }
}
