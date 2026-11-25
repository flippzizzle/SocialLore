package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FeedPost
import com.example.data.model.PostComment
import com.example.data.model.PostType
import com.example.data.model.ReactionType
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

enum class FeedFilter(val label: String) {
    ALL("All Lore"),
    POSTS("Posts & Photos"),
    CHARACTERS("Characters"),
    LORE("Lore Updates"),
    QUOTES("Quotes"),
    LYRICS("Lyrics"),
    ALBUMS("Albums")
}

@Composable
fun FeedScreen(
    viewModel: SocialLoreViewModel,
    onOpenProfile: (String) -> Unit,
    onOpenCharacter: (String) -> Unit,
    onNavigateSection: (String) -> Unit
) {
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf(FeedFilter.ALL) }
    var composerText by remember { mutableStateOf("") }
    var composerImageUrl by remember { mutableStateOf<String?>(null) }
    var editingPost by remember { mutableStateOf<FeedPost?>(null) }
    var commentingPost by remember { mutableStateOf<FeedPost?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImageUtils.encodeUriToDataUrl(context, uri)
                    .onSuccess { composerImageUrl = it }
                    .onFailure { viewModel.showMessage(it.localizedMessage ?: "Image load failed") }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Post Composer Card
            item(key = "composer") {
                LoreCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LoreAvatar(
                            imageUrl = myProfile?.avatarUrl,
                            name = myProfile?.displayName ?: "Member",
                            size = 42.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Share with the Circle",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Newest entries appear first — strictly chronological",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.textSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = composerText,
                        onValueChange = { composerText = it },
                        placeholder = { Text("Write a story, memory, or update...") },
                        minLines = 2,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feed_composer_input")
                    )
                    if (composerImageUrl != null) {
                        Spacer(Modifier.height(10.dp))
                        Box {
                            LoreImage(
                                imageUrl = composerImageUrl,
                                contentDescription = "Selected post photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { composerImageUrl = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove image")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("feed_add_photo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Attach Photo")
                        }
                        Button(
                            onClick = {
                                val clean = composerText.trim()
                                if (clean.isNotEmpty() || composerImageUrl != null) {
                                    viewModel.createPost(
                                        text = clean.ifBlank { "Shared a photo" },
                                        imageUrl = composerImageUrl
                                    ) {
                                        composerText = ""
                                        composerImageUrl = null
                                    }
                                }
                            },
                            enabled = composerText.isNotBlank() || composerImageUrl != null,
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                            modifier = Modifier.testTag("feed_publish_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Publish")
                        }
                    }
                }
            }

            // Content Type Filter Chips
            item(key = "filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FeedFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }

            // Feed Items
            when (val state = feedState) {
                is UiState.Loading -> {
                    item { LoadingLoreState("Loading Social Lore feed...") }
                }
                is UiState.Error -> {
                    item { ErrorLoreState(state.message) }
                }
                is UiState.Success -> {
                    val filtered = state.data.filter { post ->
                        val type = PostType.fromRaw(post.postType)
                        when (selectedFilter) {
                            FeedFilter.ALL -> true
                            FeedFilter.POSTS -> type == PostType.TEXT || type == PostType.PHOTO
                            FeedFilter.CHARACTERS -> type == PostType.CHARACTER_ADDED
                            FeedFilter.LORE -> type == PostType.LORE_ADDED
                            FeedFilter.QUOTES -> type == PostType.QUOTE_ADDED
                            FeedFilter.LYRICS -> type == PostType.LYRIC_ADDED
                            FeedFilter.ALBUMS -> type == PostType.ALBUM_ADDED || type == PostType.PHOTO_ADDED
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            EmptyLoreState(
                                title = "The Chronicle Awaits",
                                subtitle = "No entries match this filter yet. Post a story above, add a character to the Lore Book, or archive a quote!"
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { post ->
                            FeedPostCard(
                                post = post,
                                currentUserId = viewModel.currentUserId,
                                onAuthorClick = { onOpenProfile(post.authorId) },
                                onReact = { reaction -> viewModel.toggleReaction(post, reaction) },
                                onOpenComments = { commentingPost = post },
                                onEdit = { editingPost = post },
                                onDelete = { viewModel.deletePost(post.id) },
                                onOpenLinked = {
                                    val type = PostType.fromRaw(post.postType)
                                    val eid = post.linkedEntityId
                                    when (type) {
                                        PostType.CHARACTER_ADDED, PostType.LORE_ADDED ->
                                            if (!eid.isNullOrBlank()) onOpenCharacter(eid) else onNavigateSection("lore_book")
                                        PostType.QUOTE_ADDED -> onNavigateSection("quotes")
                                        PostType.LYRIC_ADDED -> onNavigateSection("lyrics")
                                        PostType.ALBUM_ADDED, PostType.PHOTO_ADDED -> onNavigateSection("albums")
                                        else -> {}
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Own Post Dialog
    editingPost?.let { post ->
        EditPostDialog(
            post = post,
            viewModel = viewModel,
            onDismiss = { editingPost = null }
        )
    }

    // Comments & Replies Dialog
    commentingPost?.let { post ->
        CommentsDialog(
            post = post,
            viewModel = viewModel,
            onDismiss = { commentingPost = null }
        )
    }
}

@Composable
fun EditPostDialog(
    post: FeedPost,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    var editedText by remember(post.id) { mutableStateOf(post.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Post") },
        text = {
            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editedText.isNotBlank()) {
                        viewModel.updatePost(post.id, editedText, post.imageUrl) {
                            onDismiss()
                        }
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FeedPostCard(
    post: FeedPost,
    currentUserId: String,
    onAuthorClick: () -> Unit,
    onReact: (ReactionType) -> Unit,
    onOpenComments: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenLinked: () -> Unit
) {
    val theme = LocalLoreTheme.current
    val postType = PostType.fromRaw(post.postType)
    val isMine = post.authorId == currentUserId
    var menuExpanded by remember { mutableStateOf(false) }

    LoreCard(modifier = Modifier.testTag("feed_post_${post.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAuthorClick)
            ) {
                LoreAvatar(imageUrl = post.authorAvatarUrl, name = post.authorName, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatTimestamp(post.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.textSecondary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (postType != PostType.TEXT && postType != PostType.PHOTO) {
                    AssistChip(
                        onClick = onOpenLinked,
                        label = { Text(postType.label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                if (isMine) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Post options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (postType == PostType.TEXT || postType == PostType.PHOTO) {
                                DropdownMenuItem(
                                    text = { Text("Edit Post") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete Post") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = post.text,
            style = MaterialTheme.typography.bodyLarge,
            color = theme.textPrimary
        )

        if (!post.imageUrl.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            LoreImage(
                imageUrl = post.imageUrl,
                contentDescription = "Post image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }

        if (postType != PostType.TEXT && postType != PostType.PHOTO && !post.linkedEntityTitle.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenLinked) {
                Text("View ${post.linkedEntityTitle} →", color = theme.accent)
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = theme.borderColor.copy(alpha = 0.3f))
        Spacer(Modifier.height(8.dp))

        // Standard Reactions Row + Comment Button
        val myReaction = post.userReactions[currentUserId]
        val counts = remember(post.userReactions) {
            ReactionType.entries.associateWith { rt ->
                post.userReactions.values.count { it == rt.name }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ReactionType.entries) { rt ->
                val count = counts[rt] ?: 0
                val selected = myReaction == rt.name
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) theme.accent.copy(alpha = 0.25f) else theme.cardVariantColor,
                    border = BorderStroke(
                        1.dp,
                        if (selected) theme.accent else theme.borderColor.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onReact(rt) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rt.symbol)
                        if (count > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) theme.accent else theme.textSecondary
                            )
                        }
                    }
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = theme.cardVariantColor,
                    border = BorderStroke(1.dp, theme.borderColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenComments)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comments",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${post.commentCount}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsDialog(
    post: FeedPost,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalLoreTheme.current
    val comments by viewModel.observeComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<PostComment?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(540.dp),
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
                    Text("Comments & Replies", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close comments")
                    }
                }
                HorizontalDivider(color = theme.borderColor.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                val topLevel = comments.filter { it.parentCommentId == null }
                val repliesByParent = comments.filter { it.parentCommentId != null }.groupBy { it.parentCommentId }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (topLevel.isEmpty()) {
                        item {
                            Text(
                                "No comments yet. Start the conversation!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.textSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        items(topLevel, key = { it.id }) { comment ->
                            CommentItemView(
                                comment = comment,
                                isReply = false,
                                onReply = { replyingTo = comment }
                            )
                            repliesByParent[comment.id].orEmpty().forEach { reply ->
                                Spacer(Modifier.height(6.dp))
                                CommentItemView(
                                    comment = reply,
                                    isReply = true,
                                    onReply = { replyingTo = comment }
                                )
                            }
                        }
                    }
                }

                if (replyingTo != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            "Replying to ${replyingTo?.authorName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.accent
                        )
                        IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(if (replyingTo != null) "Write a reply..." else "Write a comment...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(
                                    post = post,
                                    text = commentText,
                                    parentCommentId = replyingTo?.id,
                                    replyTargetUid = replyingTo?.authorId
                                ) {
                                    commentText = ""
                                    replyingTo = null
                                }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment", tint = theme.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItemView(
    comment: PostComment,
    isReply: Boolean,
    onReply: () -> Unit
) {
    val theme = LocalLoreTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 28.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        LoreAvatar(imageUrl = comment.authorAvatarUrl, name = comment.authorName, size = 30.dp)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = theme.cardVariantColor,
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(comment.authorName, style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatTimestamp(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.textSecondary
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = onReply,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reply", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
