package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FeedPost
import com.example.data.model.MemberProfile
import com.example.data.model.PostType
import com.example.data.model.ProfileLayoutStyle
import com.example.data.model.UiState
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.ColorSwatchField
import com.example.ui.components.EmptyLoreState
import com.example.ui.components.FontPickerDialog
import com.example.ui.components.LoreAvatar
import com.example.ui.components.LoreCard
import com.example.ui.components.LoreColorPickerDialog
import com.example.ui.components.LoreImage
import com.example.ui.components.LoreThemedBackground
import com.example.ui.theme.LocalLoreTheme
import com.example.ui.theme.SocialLoreTheme
import com.example.ui.theme.ThemeLibrary
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.toHexString
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

private enum class ProfileColorTarget(val label: String) {
    BACKGROUND("Background"),
    CARD("Card Background"),
    GRADIENT_START("Gradient Start"),
    GRADIENT_END("Gradient End"),
    ACCENT("Accent"),
    TEXT("Text"),
    BORDER("Border")
}

@Composable
fun ProfileScreen(
    viewModel: SocialLoreViewModel,
    targetUserId: String,
    onOpenProfile: (String) -> Unit,
    onOpenCharacter: (String) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenMessageThread: (String) -> Unit,
    onNavigateSection: (String) -> Unit
) {
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val approvedProfiles by viewModel.approvedProfiles.collectAsStateWithLifecycle()
    val friendships by viewModel.friendships.collectAsStateWithLifecycle()
    val albumsState by viewModel.albumsState.collectAsStateWithLifecycle()
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()

    val isMe = targetUserId == viewModel.currentUserId
    val profile = if (isMe) {
        myProfile ?: approvedProfiles.firstOrNull { it.userId == targetUserId }
    } else {
        approvedProfiles.firstOrNull { it.userId == targetUserId }
    }

    if (profile == null) {
        EmptyLoreState(
            title = "Profile Not Found",
            subtitle = "This member profile is not available yet."
        )
        return
    }

    // Build independent personal profile theme (independent of overall app theme!)
    val baseTheme = ThemeLibrary.findTheme(profile.themeId, customThemes)
    val profileTheme = remember(profile, baseTheme) {
        baseTheme.copy(
            background = parseHexColor(profile.bgColorHex, baseTheme.background),
            gradientStart = parseHexColor(profile.gradientStartHex, baseTheme.gradientStart),
            gradientEnd = parseHexColor(profile.gradientEndHex, baseTheme.gradientEnd),
            cardColor = parseHexColor(profile.cardColorHex, baseTheme.cardColor),
            textPrimary = parseHexColor(profile.textColorHex, baseTheme.textPrimary),
            textSecondary = parseHexColor(profile.textColorHex, baseTheme.textPrimary).copy(alpha = 0.74f),
            accent = parseHexColor(profile.accentColorHex, baseTheme.accent),
            borderColor = parseHexColor(profile.borderColorHex, baseTheme.borderColor),
            borderWidthDp = profile.borderWidthDp?.toFloat() ?: baseTheme.borderWidthDp,
            cardOpacity = profile.cardOpacity?.toFloat() ?: baseTheme.cardOpacity,
            bgImageUrl = profile.wallpaperUrl ?: baseTheme.bgImageUrl
        )
    }

    var showCustomizeDialog by remember { mutableStateOf(false) }
    var personalPostText by remember { mutableStateOf("") }
    var editingPost by remember { mutableStateOf<FeedPost?>(null) }
    var commentingPost by remember { mutableStateOf<FeedPost?>(null) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val friendUids = friendships.flatMap { it.participantUids }.filter { it != profile.userId }.distinct()
    val profileFriends = approvedProfiles.filter { it.userId in friendUids }
    val memberAlbums = (albumsState as? UiState.Success)?.data?.filter { it.ownerUid == profile.userId }.orEmpty()
    val memberPosts = (feedState as? UiState.Success)?.data?.filter { it.authorId == profile.userId }.orEmpty()
    val layoutStyle = ProfileLayoutStyle.fromRaw(profile.layoutStyle)

    SocialLoreTheme(
        themeSpec = profileTheme,
        headingFontName = profile.headingFont,
        bodyFontName = profile.bodyFont,
        fontScale = (profile.fontScale ?: 1.0).toFloat()
    ) {
        LoreThemedBackground(theme = profileTheme, wallpaperUrl = profileTheme.bgImageUrl) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Banner + Avatar + Identity Header
                    item(key = "header") {
                        LoreCard(theme = profileTheme, contentPadding = PaddingValues(0.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(155.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(profileTheme.accent.copy(alpha = 0.5f), profileTheme.gradientEnd)
                                        )
                                    )
                            ) {
                                if (!profile.bannerUrl.isNullOrBlank()) {
                                    LoreImage(
                                        imageUrl = profile.bannerUrl,
                                        contentDescription = "Profile banner",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = profileTheme.cardColor.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = layoutStyle.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        LoreAvatar(
                                            imageUrl = profile.avatarUrl,
                                            name = profile.displayName,
                                            size = 68.dp
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                profile.displayName,
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                            if (!profile.tagline.isNullOrBlank()) {
                                                Text(
                                                    profile.tagline,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = profileTheme.accent
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "Code: ${profile.friendCode}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = profileTheme.textSecondary
                                                )
                                                IconButton(
                                                    onClick = {
                                                        clipboard.setText(AnnotatedString(profile.friendCode))
                                                        viewModel.showMessage("Friend code copied")
                                                    },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ContentCopy,
                                                        contentDescription = "Copy friend code",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (isMe) {
                                        Button(
                                            onClick = { showCustomizeDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = profileTheme.accent),
                                            modifier = Modifier.testTag("customize_profile_button")
                                        ) {
                                            Icon(Icons.Default.Palette, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Customize")
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.openDirectChat(profile, onOpenMessageThread)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = profileTheme.accent)
                                        ) {
                                            Text("Message")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Profile Music Widget (MySpace Classic!)
                    if (!profile.profileMusicTitle.isNullOrBlank()) {
                        item(key = "music") {
                            LoreCard(theme = profileTheme) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(profileTheme.accent.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = profileTheme.accent
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("Profile Song", style = MaterialTheme.typography.labelSmall, color = profileTheme.textSecondary)
                                            Text(profile.profileMusicTitle, style = MaterialTheme.typography.titleMedium)
                                            if (!profile.profileMusicArtist.isNullOrBlank()) {
                                                Text(profile.profileMusicArtist, style = MaterialTheme.typography.bodySmall, color = profileTheme.textSecondary)
                                            }
                                        }
                                    }
                                    if (!profile.profileMusicUrl.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(profile.profileMusicUrl))
                                                    )
                                                }.onFailure {
                                                    viewModel.showMessage("Invalid song link")
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Play")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // About Me
                    item(key = "about") {
                        LoreCard(theme = profileTheme) {
                            Text("About Me", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = profile.bio?.takeIf { it.isNotBlank() }
                                    ?: if (isMe) "Tell your family and friends a bit about yourself using Customize." else "No bio added yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (profile.bio.isNullOrBlank()) profileTheme.textSecondary else profileTheme.textPrimary
                            )
                        }
                    }

                    // Friends Showcase
                    item(key = "friends_showcase") {
                        LoreCard(theme = profileTheme) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Top Friends (${profileFriends.size})", style = MaterialTheme.typography.titleLarge)
                                TextButton(onClick = onOpenFriends) { Text("Manage") }
                            }
                            if (profileFriends.isEmpty()) {
                                Text(
                                    "No friends connected yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = profileTheme.textSecondary
                                )
                            } else {
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(profileFriends, key = { it.userId }) { friend ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(74.dp)
                                                .clickable { onOpenProfile(friend.userId) }
                                        ) {
                                            LoreAvatar(friend.avatarUrl, friend.displayName, size = 54.dp)
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                friend.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Photo Albums Showcase
                    item(key = "albums_showcase") {
                        LoreCard(theme = profileTheme) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Photo Albums (${memberAlbums.size})", style = MaterialTheme.typography.titleLarge)
                                TextButton(onClick = onOpenAlbums) { Text("Open Albums") }
                            }
                            if (memberAlbums.isEmpty()) {
                                Text("No photo albums created yet.", style = MaterialTheme.typography.bodySmall, color = profileTheme.textSecondary)
                            } else {
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(memberAlbums, key = { it.id }) { album ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = profileTheme.cardVariantColor,
                                            modifier = Modifier
                                                .width(130.dp)
                                                .clickable(onClick = onOpenAlbums)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                if (!album.coverImageUrl.isNullOrBlank()) {
                                                    LoreImage(
                                                        imageUrl = album.coverImageUrl,
                                                        contentDescription = album.title,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(74.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(74.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(profileTheme.accent.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = profileTheme.accent)
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(album.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${album.photoCount} photos", style = MaterialTheme.typography.labelSmall, color = profileTheme.textSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Personal Post Composer (visible on own profile; publishes to both profile and main feed)
                    if (isMe) {
                        item(key = "personal_composer") {
                            LoreCard(theme = profileTheme) {
                                Text("Post to Your Wall & Main Feed", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = personalPostText,
                                        onValueChange = { personalPostText = it },
                                        placeholder = { Text("Share an update...") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (personalPostText.isNotBlank()) {
                                                viewModel.createPost(personalPostText, null) {
                                                    personalPostText = ""
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post", tint = profileTheme.accent)
                                    }
                                }
                            }
                        }
                    }

                    // Member's Posts
                    item(key = "posts_header") {
                        Text(
                            "${profile.displayName}'s Activity (${memberPosts.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = profileTheme.textPrimary
                        )
                    }

                    if (memberPosts.isEmpty()) {
                        item {
                            LoreCard(theme = profileTheme) {
                                Text("No posts shared yet.", style = MaterialTheme.typography.bodyMedium, color = profileTheme.textSecondary)
                            }
                        }
                    } else {
                        items(memberPosts, key = { it.id }) { post ->
                            FeedPostCard(
                                post = post,
                                currentUserId = viewModel.currentUserId,
                                onAuthorClick = { onOpenProfile(post.authorId) },
                                onReact = { viewModel.toggleReaction(post, it) },
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

    if (showCustomizeDialog) {
        ProfileCustomizerDialog(
            profile = profile,
            viewModel = viewModel,
            onDismiss = { showCustomizeDialog = false }
        )
    }

    editingPost?.let { post ->
        EditPostDialog(
            post = post,
            viewModel = viewModel,
            onDismiss = { editingPost = null }
        )
    }

    commentingPost?.let { post ->
        CommentsDialog(
            post = post,
            viewModel = viewModel,
            onDismiss = { commentingPost = null }
        )
    }
}

@Composable
private fun ProfileCustomizerDialog(
    profile: MemberProfile,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current

    var displayName by remember { mutableStateOf(profile.displayName) }
    var tagline by remember { mutableStateOf(profile.tagline ?: "") }
    var bio by remember { mutableStateOf(profile.bio ?: "") }
    var avatarUrl by remember { mutableStateOf(profile.avatarUrl) }
    var bannerUrl by remember { mutableStateOf(profile.bannerUrl) }
    var wallpaperUrl by remember { mutableStateOf(profile.wallpaperUrl) }
    var musicTitle by remember { mutableStateOf(profile.profileMusicTitle ?: "") }
    var musicArtist by remember { mutableStateOf(profile.profileMusicArtist ?: "") }
    var musicUrl by remember { mutableStateOf(profile.profileMusicUrl ?: "") }
    var layoutStyle by remember { mutableStateOf(ProfileLayoutStyle.fromRaw(profile.layoutStyle)) }
    var selectedThemeId by remember { mutableStateOf(profile.themeId) }
    val initialBaseTheme = remember(profile.themeId, customThemes) {
        ThemeLibrary.findTheme(profile.themeId, customThemes)
    }
    var bgHex by remember { mutableStateOf(profile.bgColorHex ?: initialBaseTheme.background.toHexString()) }
    var gradStartHex by remember { mutableStateOf(profile.gradientStartHex ?: initialBaseTheme.gradientStart.toHexString()) }
    var gradEndHex by remember { mutableStateOf(profile.gradientEndHex ?: initialBaseTheme.gradientEnd.toHexString()) }
    var cardHex by remember { mutableStateOf(profile.cardColorHex ?: initialBaseTheme.cardColor.toHexString()) }
    var textHex by remember { mutableStateOf(profile.textColorHex ?: initialBaseTheme.textPrimary.toHexString()) }
    var accentHex by remember { mutableStateOf(profile.accentColorHex ?: initialBaseTheme.accent.toHexString()) }
    var borderHex by remember { mutableStateOf(profile.borderColorHex ?: initialBaseTheme.borderColor.toHexString()) }
    var borderWidth by remember { mutableFloatStateOf((profile.borderWidthDp ?: 1.5).toFloat()) }
    var cardOpacity by remember { mutableFloatStateOf((profile.cardOpacity ?: 0.92).toFloat()) }
    var headingFont by remember { mutableStateOf(profile.headingFont ?: "Playfair Display") }
    var bodyFont by remember { mutableStateOf(profile.bodyFont ?: "Plus Jakarta Sans") }
    var fontPickerTarget by remember { mutableStateOf<String?>(null) }
    var activeColorTarget by remember { mutableStateOf<ProfileColorTarget?>(null) }

    val selectedBaseTheme = remember(selectedThemeId, customThemes) {
        ThemeLibrary.findTheme(selectedThemeId, customThemes)
    }
    val previewProfileTheme = remember(
        selectedBaseTheme,
        bgHex,
        gradStartHex,
        gradEndHex,
        cardHex,
        textHex,
        accentHex,
        borderHex,
        borderWidth,
        cardOpacity,
        wallpaperUrl
    ) {
        val resolvedText = parseHexColor(textHex, selectedBaseTheme.textPrimary)
        selectedBaseTheme.copy(
            background = parseHexColor(bgHex, selectedBaseTheme.background),
            gradientStart = parseHexColor(gradStartHex, selectedBaseTheme.gradientStart),
            gradientEnd = parseHexColor(gradEndHex, selectedBaseTheme.gradientEnd),
            cardColor = parseHexColor(cardHex, selectedBaseTheme.cardColor),
            textPrimary = resolvedText,
            textSecondary = resolvedText.copy(alpha = 0.74f),
            accent = parseHexColor(accentHex, selectedBaseTheme.accent),
            borderColor = parseHexColor(borderHex, selectedBaseTheme.borderColor),
            borderWidthDp = borderWidth,
            cardOpacity = cardOpacity,
            bgImageUrl = wallpaperUrl ?: selectedBaseTheme.bgImageUrl
        )
    }

    var pendingImageTarget by remember { mutableStateOf("avatar") }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImageUtils.encodeUriToDataUrl(context, uri)
                    .onSuccess { dataUrl ->
                        when (pendingImageTarget) {
                            "avatar" -> avatarUrl = dataUrl
                            "banner" -> bannerUrl = dataUrl
                            "wallpaper" -> wallpaperUrl = dataUrl
                        }
                    }
                    .onFailure { viewModel.showMessage(it.localizedMessage ?: "Failed to load image") }
            }
        }
    }

    val allThemes = remember(customThemes) {
        ThemeLibrary.premadeThemes + customThemes.map {
            ThemeLibrary.findTheme("custom_${it.id}", customThemes)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(640.dp),
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
                    Text("Customize Personal Space", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                // Live Personal Space Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    previewProfileTheme.gradientStart,
                                    previewProfileTheme.background,
                                    previewProfileTheme.gradientEnd
                                )
                            )
                        )
                ) {
                    if (!wallpaperUrl.isNullOrBlank()) {
                        LoreImage(
                            imageUrl = wallpaperUrl,
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(previewProfileTheme.background.copy(alpha = 0.65f))
                        )
                    }
                    Box(modifier = Modifier.padding(10.dp)) {
                        LoreCard(
                            theme = previewProfileTheme,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    displayName.ifBlank { "My Space" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = previewProfileTheme.accent,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = previewProfileTheme.accent.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, previewProfileTheme.accent)
                                ) {
                                    Text(
                                        layoutStyle.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = previewProfileTheme.accent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                tagline.ifBlank { "Live profile style preview" },
                                style = MaterialTheme.typography.bodySmall,
                                color = previewProfileTheme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it },
                        label = { Text("Mood / Tagline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("About Me") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Profile Media (Avatar, Banner, Wallpaper)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                pendingImageTarget = "avatar"
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Avatar")
                        }
                        OutlinedButton(
                            onClick = {
                                pendingImageTarget = "banner"
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Banner")
                        }
                        OutlinedButton(
                            onClick = {
                                pendingImageTarget = "wallpaper"
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Wallpaper")
                        }
                    }

                    Text("Profile Music (MySpace Style)", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = musicTitle,
                        onValueChange = { musicTitle = it },
                        label = { Text("Song Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = musicArtist,
                        onValueChange = { musicArtist = it },
                        label = { Text("Artist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = musicUrl,
                        onValueChange = { musicUrl = it },
                        label = { Text("Song Link (Spotify / YouTube / URL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Layout Style", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ProfileLayoutStyle.entries) { style ->
                            FilterChip(
                                selected = layoutStyle == style,
                                onClick = { layoutStyle = style },
                                label = { Text(style.displayName) }
                            )
                        }
                    }

                    Text("Profile Theme (Independent of App Theme)", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allThemes, key = { it.id }) { spec ->
                            val selected = selectedThemeId == spec.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = spec.cardColor,
                                border = BorderStroke(
                                    if (selected) 2.5.dp else 1.dp,
                                    if (selected) spec.accent else spec.borderColor
                                ),
                                modifier = Modifier
                                    .width(120.dp)
                                    .clickable {
                                        selectedThemeId = spec.id
                                        bgHex = spec.background.toHexString()
                                        gradStartHex = spec.gradientStart.toHexString()
                                        gradEndHex = spec.gradientEnd.toHexString()
                                        cardHex = spec.cardColor.toHexString()
                                        textHex = spec.textPrimary.toHexString()
                                        accentHex = spec.accent.toHexString()
                                        borderHex = spec.borderColor.toHexString()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Brush.horizontalGradient(listOf(spec.gradientStart, spec.accent)))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(spec.name, style = MaterialTheme.typography.labelSmall, color = spec.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    Text("Profile Colors (Tap any swatch to customize)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ColorSwatchField(
                            label = "Background",
                            colorHex = bgHex,
                            fallbackColor = selectedBaseTheme.background,
                            onClick = { activeColorTarget = ProfileColorTarget.BACKGROUND },
                            modifier = Modifier.weight(1f),
                            testTag = "profile_color_bg"
                        )
                        ColorSwatchField(
                            label = "Card",
                            colorHex = cardHex,
                            fallbackColor = selectedBaseTheme.cardColor,
                            onClick = { activeColorTarget = ProfileColorTarget.CARD },
                            modifier = Modifier.weight(1f),
                            testTag = "profile_color_card"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ColorSwatchField(
                            label = "Gradient Start",
                            colorHex = gradStartHex,
                            fallbackColor = selectedBaseTheme.gradientStart,
                            onClick = { activeColorTarget = ProfileColorTarget.GRADIENT_START },
                            modifier = Modifier.weight(1f),
                            testTag = "profile_color_grad_start"
                        )
                        ColorSwatchField(
                            label = "Gradient End",
                            colorHex = gradEndHex,
                            fallbackColor = selectedBaseTheme.gradientEnd,
                            onClick = { activeColorTarget = ProfileColorTarget.GRADIENT_END },
                            modifier = Modifier.weight(1f),
                            testTag = "profile_color_grad_end"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ColorSwatchField(
                            label = "Accent",
                            colorHex = accentHex,
                            fallbackColor = selectedBaseTheme.accent,
                            onClick = { activeColorTarget = ProfileColorTarget.ACCENT },
                            modifier = Modifier.weight(1f),
                            testTag = "profile_color_accent"
                        )
                        ColorSwatchField(
                            label = "Text",
                            colorHex = textHex,
                            fallbackColor = selectedBaseTheme.textPrimary,
                            onClick = { activeColorTarget = ProfileColorTarget.TEXT },
                            modifier = Modifier.weight(1f),
                            testTag = "profile_color_text"
                        )
                    }
                    ColorSwatchField(
                        label = "Border",
                        colorHex = borderHex,
                        fallbackColor = selectedBaseTheme.borderColor,
                        onClick = { activeColorTarget = ProfileColorTarget.BORDER },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "profile_color_border"
                    )

                    Text("Border Width: ${String.format("%.1f", borderWidth)}dp", style = MaterialTheme.typography.labelMedium)
                    Slider(value = borderWidth, onValueChange = { borderWidth = it }, valueRange = 0f..6f)

                    Text("Card Opacity: ${(cardOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(value = cardOpacity, onValueChange = { cardOpacity = it }, valueRange = 0.35f..1f)

                    Text("Profile Typography", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { fontPickerTarget = "heading" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Heading: $headingFont", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { fontPickerTarget = "body" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Body: $bodyFont", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (displayName.isNotBlank()) {
                                viewModel.updateProfile(
                                    displayName = displayName,
                                    tagline = tagline,
                                    bio = bio,
                                    avatarUrl = avatarUrl,
                                    bannerUrl = bannerUrl,
                                    wallpaperUrl = wallpaperUrl,
                                    profileMusicTitle = musicTitle,
                                    profileMusicArtist = musicArtist,
                                    profileMusicUrl = musicUrl,
                                    layoutStyle = layoutStyle,
                                    themeId = selectedThemeId,
                                    bgColorHex = bgHex.takeIf { it.isNotBlank() },
                                    gradientStartHex = gradStartHex.takeIf { it.isNotBlank() },
                                    gradientEndHex = gradEndHex.takeIf { it.isNotBlank() },
                                    cardColorHex = cardHex.takeIf { it.isNotBlank() },
                                    textColorHex = textHex.takeIf { it.isNotBlank() },
                                    accentColorHex = accentHex.takeIf { it.isNotBlank() },
                                    borderColorHex = borderHex.takeIf { it.isNotBlank() },
                                    borderWidthDp = borderWidth.toDouble(),
                                    cardOpacity = cardOpacity.toDouble(),
                                    headingFont = headingFont,
                                    bodyFont = bodyFont,
                                    fontScale = null
                                ) {
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.testTag("save_profile_customization_button")
                    ) { Text("Save Profile") }
                }
            }
        }
    }

    activeColorTarget?.let { target ->
        val (currentHex, fallback) = when (target) {
            ProfileColorTarget.BACKGROUND -> bgHex to selectedBaseTheme.background
            ProfileColorTarget.CARD -> cardHex to selectedBaseTheme.cardColor
            ProfileColorTarget.GRADIENT_START -> gradStartHex to selectedBaseTheme.gradientStart
            ProfileColorTarget.GRADIENT_END -> gradEndHex to selectedBaseTheme.gradientEnd
            ProfileColorTarget.ACCENT -> accentHex to selectedBaseTheme.accent
            ProfileColorTarget.TEXT -> textHex to selectedBaseTheme.textPrimary
            ProfileColorTarget.BORDER -> borderHex to selectedBaseTheme.borderColor
        }
        val updateColor: (String) -> Unit = { newHex ->
            when (target) {
                ProfileColorTarget.BACKGROUND -> bgHex = newHex
                ProfileColorTarget.CARD -> cardHex = newHex
                ProfileColorTarget.GRADIENT_START -> gradStartHex = newHex
                ProfileColorTarget.GRADIENT_END -> gradEndHex = newHex
                ProfileColorTarget.ACCENT -> accentHex = newHex
                ProfileColorTarget.TEXT -> textHex = newHex
                ProfileColorTarget.BORDER -> borderHex = newHex
            }
        }
        LoreColorPickerDialog(
            title = "Pick Profile ${target.label} Color",
            initialColorHex = currentHex,
            fallbackColor = fallback,
            onLivePreview = updateColor,
            onApply = { appliedHex ->
                updateColor(appliedHex)
                activeColorTarget = null
            },
            onCancel = {
                activeColorTarget = null
            }
        )
    }

    fontPickerTarget?.let { target ->
        FontPickerDialog(
            title = if (target == "heading") "Profile Heading Font" else "Profile Body Font",
            selectedFont = if (target == "heading") headingFont else bodyFont,
            onSelect = { chosen ->
                if (target == "heading") headingFont = chosen else bodyFont = chosen
                fontPickerTarget = null
            },
            onDismiss = { fontPickerTarget = null }
        )
    }
}
