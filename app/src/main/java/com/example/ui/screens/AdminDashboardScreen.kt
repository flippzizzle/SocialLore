package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MemberStatus
import com.example.data.model.UiState
import com.example.data.model.UserRole
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.EmptyLoreState
import com.example.ui.components.LoreCard
import com.example.ui.components.LoreImage
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.LocalLoreTheme
import com.example.util.BackupSection
import com.example.util.BackupUtils
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    viewModel: SocialLoreViewModel
) {
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val allUsers by viewModel.adminAllUsers.collectAsStateWithLifecycle()
    val whitelist by viewModel.adminWhitelist.collectAsStateWithLifecycle()
    val bugReports by viewModel.adminBugReports.collectAsStateWithLifecycle()
    val alerts by viewModel.adminAlerts.collectAsStateWithLifecycle()

    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val loreEntriesState by viewModel.loreEntriesState.collectAsStateWithLifecycle()
    val relationshipsState by viewModel.relationshipsState.collectAsStateWithLifecycle()
    val quotesState by viewModel.quotesState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val albumsState by viewModel.albumsState.collectAsStateWithLifecycle()
    val photosState by viewModel.photosState.collectAsStateWithLifecycle()

    val theme = LocalLoreTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isAdmin = (accountState as? UiState.Success)?.data?.role == UserRole.ADMIN.name

    if (!isAdmin) {
        EmptyLoreState(
            title = "Administrator Access Required",
            subtitle = "This governance console is restricted to the Social Lore administrator."
        )
        return
    }

    var whitelistEmailInput by remember { mutableStateOf("") }

    val pendingUsers = remember(allUsers) { allUsers.filter { it.status == MemberStatus.PENDING.name } }
    val otherUsers = remember(allUsers) { allUsers.filter { it.status != MemberStatus.PENDING.name } }
    val unreadAlerts = remember(alerts) { alerts.filter { !it.read } }

    val communityBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = BackupUtils.exportJson(
                        section = BackupSection.FULL_COMMUNITY,
                        myProfile = myProfile,
                        posts = (feedState as? UiState.Success)?.data.orEmpty(),
                        characters = (charactersState as? UiState.Success)?.data.orEmpty(),
                        loreEntries = (loreEntriesState as? UiState.Success)?.data.orEmpty(),
                        relationships = (relationshipsState as? UiState.Success)?.data.orEmpty(),
                        quotes = (quotesState as? UiState.Success)?.data.orEmpty(),
                        lyrics = (lyricsState as? UiState.Success)?.data.orEmpty(),
                        albums = (albumsState as? UiState.Success)?.data.orEmpty(),
                        photos = (photosState as? UiState.Success)?.data.orEmpty()
                    )
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.onSuccess {
                    viewModel.showMessage("Full Community Backup exported (private messages excluded)")
                }.onFailure {
                    viewModel.showMessage("Backup failed: ${it.localizedMessage}")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Admin Header + Quick Backup / Access Sync
            item(key = "admin_overview") {
                LoreCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Administrator Dashboard", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "${pendingUsers.size} pending approval(s) • ${unreadAlerts.size} unread alert(s) • ${bugReports.count { it.status == "OPEN" }} open bug(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.accent
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { communityBackupLauncher.launch("social_lore_community_backup.json") },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Community Backup")
                        }
                        OutlinedButton(
                            onClick = { viewModel.syncCommunityAccess() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sync Access")
                        }
                    }
                }
            }

            // Admin Alerts Inbox
            if (unreadAlerts.isNotEmpty()) {
                item(key = "alerts_header") {
                    Text("Admin Alerts (${unreadAlerts.size})", style = MaterialTheme.typography.titleLarge)
                }
                items(unreadAlerts, key = { "alert_${it.id}" }) { alert ->
                    LoreCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(alert.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text(alert.body, style = MaterialTheme.typography.bodySmall)
                                Text(formatTimestamp(alert.createdAt), style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                            }
                            TextButton(onClick = { viewModel.markAdminAlertRead(alert.id) }) { Text("Dismiss") }
                        }
                    }
                }
            }

            // Pending Approval Queue
            item(key = "pending_header") {
                Text("Pending Approvals (${pendingUsers.size})", style = MaterialTheme.typography.titleLarge)
            }
            if (pendingUsers.isEmpty()) {
                item {
                    LoreCard {
                        Text("No pending membership requests right now.", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                    }
                }
            } else {
                items(pendingUsers, key = { "pend_${it.userId}" }) { user ->
                    LoreCard {
                        Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.setMemberStatus(user, MemberStatus.APPROVED) },
                                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                                modifier = Modifier.testTag("approve_user_${user.userId}")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Approve")
                            }
                            OutlinedButton(
                                onClick = { viewModel.setMemberStatus(user, MemberStatus.BLOCKED) }
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Block")
                            }
                        }
                    }
                }
            }

            // Email Whitelist
            item(key = "whitelist") {
                LoreCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Whitelisted Emails (${whitelist.size})", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Verified Google accounts on this whitelist are automatically approved when they sign in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = whitelistEmailInput,
                            onValueChange = { whitelistEmailInput = it },
                            label = { Text("Email to whitelist") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                viewModel.addWhitelistedEmail(whitelistEmailInput) { whitelistEmailInput = "" }
                            },
                            enabled = whitelistEmailInput.contains("@"),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                        ) { Text("Add") }
                    }
                    whitelist.forEach { w ->
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(w.email, style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { viewModel.removeWhitelistedEmail(w.email) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove whitelist email", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Member Governance Directory
            item(key = "members_header") {
                Text("Community Members (${otherUsers.size})", style = MaterialTheme.typography.titleLarge)
            }
            items(otherUsers, key = { "mem_${it.userId}" }) { member ->
                LoreCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${member.displayName} (${member.role})", style = MaterialTheme.typography.titleMedium)
                            Text(member.email, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = theme.cardVariantColor
                        ) {
                            Text(
                                member.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (member.userId != viewModel.currentUserId) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (member.status != MemberStatus.APPROVED.name) {
                                OutlinedButton(onClick = { viewModel.setMemberStatus(member, MemberStatus.APPROVED) }) {
                                    Text("Approve")
                                }
                            }
                            if (member.status == MemberStatus.APPROVED.name) {
                                OutlinedButton(onClick = { viewModel.setMemberStatus(member, MemberStatus.SUSPENDED) }) {
                                    Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Suspend")
                                }
                            }
                            if (member.status != MemberStatus.REMOVED.name) {
                                TextButton(onClick = { viewModel.setMemberStatus(member, MemberStatus.REMOVED) }) {
                                    Text("Remove")
                                }
                            }
                            if (member.status != MemberStatus.BLOCKED.name) {
                                TextButton(onClick = { viewModel.setMemberStatus(member, MemberStatus.BLOCKED) }) {
                                    Text("Block", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Bug Reports Queue
            item(key = "bugs_header") {
                Text("Bug Reports (${bugReports.size})", style = MaterialTheme.typography.titleLarge)
            }
            if (bugReports.isEmpty()) {
                item {
                    LoreCard {
                        Text("No bug reports filed yet.", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                    }
                }
            } else {
                items(bugReports, key = { "bug_${it.id}" }) { bug ->
                    LoreCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bug.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Surface(shape = RoundedCornerShape(8.dp), color = theme.cardVariantColor) {
                                Text(bug.status, style = MaterialTheme.typography.labelSmall, color = theme.accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(bug.description, style = MaterialTheme.typography.bodyMedium)
                        if (!bug.stepsToReproduce.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Steps: ${bug.stepsToReproduce}", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                        }
                        Text(
                            "By ${bug.reporterName} • ${bug.appVersion} • ${bug.deviceInfo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.textSecondary
                        )
                        if (!bug.screenshotUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            LoreImage(
                                imageUrl = bug.screenshotUrl,
                                contentDescription = "Bug screenshot",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (bug.status == "OPEN") {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.resolveBugReport(bug.id) }) {
                                Text("Mark Resolved")
                            }
                        }
                    }
                }
            }
        }
    }
}
