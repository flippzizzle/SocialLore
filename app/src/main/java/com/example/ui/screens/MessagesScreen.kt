package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.data.model.MemberProfile
import com.example.data.model.UiState
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.EmptyLoreState
import com.example.ui.components.ErrorLoreState
import com.example.ui.components.LoadingLoreState
import com.example.ui.components.LoreAvatar
import com.example.ui.components.LoreCard
import com.example.ui.components.LoreImage
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.LocalLoreTheme
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    viewModel: SocialLoreViewModel,
    initialConversationId: String? = null,
    onClearInitialConversation: () -> Unit = {}
) {
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val approvedProfiles by viewModel.approvedProfiles.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current

    var activeConvId by remember(initialConversationId) { mutableStateOf(initialConversationId) }
    var showNewDmDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }

    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val activeConv = activeConvId?.let { id -> conversations.firstOrNull { it.id == id } }

    if (activeConv != null) {
        BackHandler {
            activeConvId = null
            onClearInitialConversation()
        }
        ChatThreadScreen(
            viewModel = viewModel,
            conversation = activeConv,
            onBack = {
                activeConvId = null
                onClearInitialConversation()
            }
        )
        return
    }

    val otherMembers = remember(approvedProfiles, viewModel.currentUserId) {
        approvedProfiles.filter { it.userId != viewModel.currentUserId }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "msg_header") {
                LoreCard {
                    Text("Private Messages & Group Chats", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Conversations stay private between participants and never appear in the Social Feed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showNewDmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_dm_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Direct Chat")
                        }
                        OutlinedButton(
                            onClick = { showNewGroupDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_group_chat_button")
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Group Chat")
                        }
                    }
                }
            }

            when (val state = conversationsState) {
                is UiState.Loading -> item { LoadingLoreState("Loading conversations...") }
                is UiState.Error -> item { ErrorLoreState(state.message) }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item {
                            EmptyLoreState(
                                title = "No Conversations Yet",
                                subtitle = "Start a private 1-on-1 message or group chat with your circle above."
                            )
                        }
                    } else {
                        items(state.data, key = { it.id }) { conv ->
                            val unread = (conv.unreadCounts[viewModel.currentUserId] ?: 0L).toInt()
                            val displayTitle = conversationDisplayTitle(conv, viewModel.currentUserId)
                            LoreCard(onClick = { activeConvId = conv.id }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        LoreAvatar(null, displayTitle, size = 46.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    displayTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.SemiBold
                                                )
                                                if (conv.groupChat) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = theme.cardVariantColor
                                                    ) {
                                                        Text(
                                                            "Group • ${conv.participantUids.size}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = theme.accent,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                conv.lastMessageText ?: "Tap to start chatting",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (unread > 0) theme.textPrimary else theme.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            formatTimestamp(conv.lastMessageAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = theme.textSecondary
                                        )
                                        if (unread > 0) {
                                            Spacer(Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(theme.accent)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    unread.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (theme.isDark) Color(0xFF0A0812) else Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewDmDialog) {
        NewDirectChatDialog(
            members = otherMembers,
            onSelectMember = { member ->
                showNewDmDialog = false
                viewModel.openDirectChat(member) { convId -> activeConvId = convId }
            },
            onDismiss = { showNewDmDialog = false }
        )
    }

    if (showNewGroupDialog) {
        NewGroupChatDialog(
            members = otherMembers,
            viewModel = viewModel,
            onCreated = { convId ->
                showNewGroupDialog = false
                activeConvId = convId
            },
            onDismiss = { showNewGroupDialog = false }
        )
    }
}

private fun conversationDisplayTitle(conv: Conversation, currentUid: String): String {
    if (conv.groupChat && !conv.title.isNullOrBlank()) return conv.title
    val otherNames = conv.participantNames.filterKeys { it != currentUid }.values
    return otherNames.joinToString(", ").ifBlank { "Conversation" }
}

@Composable
fun ChatThreadScreen(
    viewModel: SocialLoreViewModel,
    conversation: Conversation,
    onBack: () -> Unit
) {
    val messages by viewModel.observeMessages(conversation.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val theme = LocalLoreTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var draftText by remember { mutableStateOf("") }
    var draftImage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                ImageUtils.encodeUriToDataUrl(context, uri)
                    .onSuccess { draftImage = it }
                    .onFailure { viewModel.showMessage(it.localizedMessage ?: "Failed to attach image") }
            }
        }
    }

    LaunchedEffect(conversation.id, messages.size) {
        viewModel.markConversationRead(conversation)
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            Surface(
                color = theme.cardColor.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, theme.borderColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to conversations")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            conversationDisplayTitle(conversation, viewModel.currentUserId),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${conversation.participantUids.size} participants",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.textSecondary
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == viewModel.currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isMe) theme.accent.copy(alpha = 0.25f) else theme.cardColor,
                            border = BorderStroke(
                                1.dp,
                                if (isMe) theme.accent.copy(alpha = 0.6f) else theme.borderColor.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.widthIn(max = 310.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (!isMe) {
                                    Text(
                                        msg.senderName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                if (!msg.imageUrl.isNullOrBlank()) {
                                    LoreImage(
                                        imageUrl = msg.imageUrl,
                                        contentDescription = "Shared photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                                if (msg.text.isNotBlank()) {
                                    Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatTimestamp(msg.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (draftImage != null) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    LoreImage(
                        imageUrl = draftImage,
                        contentDescription = "Attachment preview",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    IconButton(
                        onClick = { draftImage = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove attachment")
                    }
                }
            }

            Surface(
                color = theme.cardColor,
                border = BorderStroke(1.dp, theme.borderColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach image")
                    }
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        placeholder = { Text("Message...") },
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (draftText.isNotBlank() || draftImage != null) {
                                viewModel.sendMessage(conversation, draftText, draftImage) {
                                    draftText = ""
                                    draftImage = null
                                }
                            }
                        },
                        enabled = draftText.isNotBlank() || draftImage != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = theme.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewDirectChatDialog(
    members: List<MemberProfile>,
    onSelectMember: (MemberProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalLoreTheme.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(460.dp),
            shape = MaterialTheme.shapes.large,
            color = theme.cardColor,
            contentColor = theme.textPrimary,
            border = BorderStroke(1.dp, theme.borderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Start Direct Message", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                if (members.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No other approved members yet.", color = theme.textSecondary)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(members, key = { it.userId }) { member ->
                            LoreCard(onClick = { onSelectMember(member) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LoreAvatar(member.avatarUrl, member.displayName, size = 40.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(member.displayName, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewGroupChatDialog(
    members: List<MemberProfile>,
    viewModel: SocialLoreViewModel,
    onCreated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalLoreTheme.current
    var title by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<MemberProfile>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(520.dp),
            shape = MaterialTheme.shapes.large,
            color = theme.cardColor,
            contentColor = theme.textPrimary,
            border = BorderStroke(1.dp, theme.borderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Group Chat", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Group Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Select Circle Members", style = MaterialTheme.typography.titleSmall)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(members, key = { it.userId }) { m ->
                        val checked = selected.any { it.userId == m.userId }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) selected.add(m) else selected.removeAll { it.userId == m.userId }
                                }
                            )
                            LoreAvatar(m.avatarUrl, m.displayName, size = 34.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(m.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && selected.isNotEmpty()) {
                                viewModel.createGroupChat(title, selected.toList(), onCreated)
                            }
                        },
                        enabled = title.isNotBlank() && selected.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                    ) { Text("Create Group") }
                }
            }
        }
    }
}
