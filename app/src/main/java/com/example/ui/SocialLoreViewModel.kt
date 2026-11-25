package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminAlert
import com.example.data.model.AppLockType
import com.example.data.model.AttributionType
import com.example.data.model.BugReport
import com.example.data.model.CharacterRelationship
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.CustomThemeEntity
import com.example.data.model.DatePrecision
import com.example.data.model.FeedPost
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.LoreCharacter
import com.example.data.model.LoreEntry
import com.example.data.model.LyricEntry
import com.example.data.model.MemberProfile
import com.example.data.model.MemberStatus
import com.example.data.model.NotificationCategory
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import com.example.data.model.PostComment
import com.example.data.model.PostType
import com.example.data.model.ProfileLayoutStyle
import com.example.data.model.QuoteEntry
import com.example.data.model.ReactionType
import com.example.data.model.RelationshipCategory
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.UserNotification
import com.example.data.model.UserRole
import com.example.data.model.WhitelistedEmail
import com.example.data.repository.AccountRepository
import com.example.data.repository.LoreRepository
import com.example.data.repository.MessagingRepository
import com.example.data.repository.SocialFeedRepository
import com.example.util.extractFirestoreRootError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SocialLoreViewModel(
    private val accountRepo: AccountRepository,
    private val feedRepo: SocialFeedRepository,
    private val loreRepo: LoreRepository,
    private val msgRepo: MessagingRepository,
    val currentUserId: String
) : ViewModel() {

    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _appUnlocked = MutableStateFlow(false)
    val appUnlocked: StateFlow<Boolean> = _appUnlocked.asStateFlow()

    init {
        retryInitialization()
    }

    fun retryInitialization() {
        viewModelScope.launch {
            _initError.value = null
            accountRepo.ensureAccountInitialized().onFailure { err ->
                _initError.value = err.localizedMessage ?: "Failed to initialize account"
            }
        }
    }

    fun markAppUnlocked() {
        _appUnlocked.value = true
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    private fun <T> Flow<T>.toUiStateFlow(tag: String, fallbackMsg: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { err ->
                val root = extractFirestoreRootError(err)
                Log.w(tag, fallbackMsg, root)
                emit(UiState.Error(root.message ?: fallbackMsg))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val accountState: StateFlow<UiState<UserAccount?>> =
        accountRepo.observeMyAccount()
            .toUiStateFlow("AccountVM", "Failed to load account")

    val myProfile: StateFlow<MemberProfile?> =
        accountRepo.observeMyProfile()
            .catch { err ->
                val root = extractFirestoreRootError(err)
                Log.w("ProfileVM", "Error observing own profile", root)
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val customThemes: StateFlow<List<CustomThemeEntity>> =
        accountRepo.observeCustomThemes()
            .catch { err ->
                extractFirestoreRootError(err)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<UserNotification>> =
        accountRepo.observeNotifications()
            .catch { err ->
                extractFirestoreRootError(err)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val isApprovedFlow: Flow<Boolean> =
        accountState.map { state ->
            (state as? UiState.Success)?.data?.status == MemberStatus.APPROVED.name
        }.distinctUntilChanged()

    private val isAdminFlow: Flow<Boolean> =
        accountState.map { state ->
            val acc = (state as? UiState.Success)?.data
            acc?.status == MemberStatus.APPROVED.name && acc.role == UserRole.ADMIN.name
        }.distinctUntilChanged()

    private fun <T> approvedStream(provider: () -> Flow<List<T>>, tag: String, errMsg: String): StateFlow<UiState<List<T>>> =
        isApprovedFlow.flatMapLatest { approved ->
            if (!approved) flowOf(emptyList()) else provider()
        }.toUiStateFlow(tag, errMsg)

    val approvedProfiles: StateFlow<List<MemberProfile>> =
        isApprovedFlow.flatMapLatest { approved ->
            if (!approved) flowOf(emptyList()) else accountRepo.observeApprovedProfiles()
        }.catch { err ->
            extractFirestoreRootError(err)
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val feedState: StateFlow<UiState<List<FeedPost>>> =
        approvedStream({ feedRepo.observeFeed() }, "FeedVM", "Failed to load Social Lore feed")

    val charactersState: StateFlow<UiState<List<LoreCharacter>>> =
        approvedStream({ loreRepo.observeCharacters() }, "LoreVM", "Failed to load Lore Book")

    val loreEntriesState: StateFlow<UiState<List<LoreEntry>>> =
        approvedStream({ loreRepo.observeLoreEntries() }, "LoreVM", "Failed to load lore entries")

    val relationshipsState: StateFlow<UiState<List<CharacterRelationship>>> =
        approvedStream({ loreRepo.observeRelationships() }, "MapVM", "Failed to load relationships")

    val quotesState: StateFlow<UiState<List<QuoteEntry>>> =
        approvedStream({ loreRepo.observeQuotes() }, "QuoteVM", "Failed to load Quote Book")

    val lyricsState: StateFlow<UiState<List<LyricEntry>>> =
        approvedStream({ loreRepo.observeLyrics() }, "LyricVM", "Failed to load Song Lyrics")

    val albumsState: StateFlow<UiState<List<PhotoAlbum>>> =
        approvedStream({ loreRepo.observeAlbums() }, "AlbumVM", "Failed to load Photo Albums")

    val photosState: StateFlow<UiState<List<PhotoItem>>> =
        approvedStream({ loreRepo.observePhotos() }, "PhotoVM", "Failed to load Photos")

    val friendRequests: StateFlow<List<FriendRequest>> =
        isApprovedFlow.flatMapLatest { approved ->
            if (!approved) flowOf(emptyList()) else accountRepo.observeFriendRequests()
        }.catch { err ->
            extractFirestoreRootError(err)
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val friendships: StateFlow<List<Friendship>> =
        isApprovedFlow.flatMapLatest { approved ->
            if (!approved) flowOf(emptyList()) else accountRepo.observeFriendships()
        }.catch { err ->
            extractFirestoreRootError(err)
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        approvedStream({ msgRepo.observeConversations() }, "MsgVM", "Failed to load messages")

    // Admin streams (active only when ADMIN)
    val adminAllUsers: StateFlow<List<UserAccount>> =
        isAdminFlow.flatMapLatest { admin ->
            if (!admin) flowOf(emptyList()) else accountRepo.observeAllUsersForAdmin()
        }.catch { err ->
            extractFirestoreRootError(err)
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminWhitelist: StateFlow<List<WhitelistedEmail>> =
        isAdminFlow.flatMapLatest { admin ->
            if (!admin) flowOf(emptyList()) else accountRepo.observeWhitelistedEmails()
        }.catch { err ->
            extractFirestoreRootError(err)
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminBugReports: StateFlow<List<BugReport>> =
        isAdminFlow.flatMapLatest { admin ->
            if (!admin) flowOf(emptyList()) else accountRepo.observeBugReports()
        }.catch { err ->
            extractFirestoreRootError(err)
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminAlerts: StateFlow<List<AdminAlert>> =
        isAdminFlow.flatMapLatest { admin ->
            if (!admin) flowOf(emptyList()) else accountRepo.observeAdminAlerts()
        }.catch { err ->
            extractFirestoreRootError(err)
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun observeComments(postId: String): Flow<List<PostComment>> =
        feedRepo.observeComments(postId).catch { err ->
            extractFirestoreRootError(err)
            emit(emptyList())
        }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        msgRepo.observeMessages(conversationId).catch { err ->
            extractFirestoreRootError(err)
            emit(emptyList())
        }

    private fun currentMemberUids(): List<String> =
        (approvedProfiles.value.map { it.userId } + currentUserId).distinct()

    private fun currentDisplayName(): String =
        myProfile.value?.displayName?.takeIf { it.isNotBlank() }
            ?: (accountState.value as? UiState.Success)?.data?.displayName
            ?: "Member"

    private fun currentAvatarUrl(): String? = myProfile.value?.avatarUrl

    // --- Feed Actions ---

    fun createPost(text: String, imageUrl: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val type = if (imageUrl != null) PostType.PHOTO else PostType.TEXT
            val res = feedRepo.createPost(
                authorName = currentDisplayName(),
                authorAvatarUrl = currentAvatarUrl(),
                postType = type,
                text = text,
                imageUrl = imageUrl,
                memberUids = currentMemberUids()
            )
            res.onSuccess {
                _snackbarMessage.value = "Posted to Social Lore feed"
                onDone()
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to create post"
            }
        }
    }

    fun updatePost(postId: String, text: String, imageUrl: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            feedRepo.updatePost(postId, text, imageUrl)
                .onSuccess {
                    _snackbarMessage.value = "Post updated"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update post" }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            feedRepo.deletePost(postId)
                .onSuccess { _snackbarMessage.value = "Post deleted" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete post" }
        }
    }

    fun toggleReaction(post: FeedPost, reaction: ReactionType) {
        viewModelScope.launch {
            feedRepo.toggleReaction(post, reaction, currentDisplayName())
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to react" }
        }
    }

    fun addComment(
        post: FeedPost,
        text: String,
        parentCommentId: String? = null,
        replyTargetUid: String? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            feedRepo.addComment(
                post = post,
                authorName = currentDisplayName(),
                authorAvatarUrl = currentAvatarUrl(),
                text = text,
                parentCommentId = parentCommentId,
                replyTargetUid = replyTargetUid
            ).onSuccess { onDone() }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to comment" }
        }
    }

    // --- Lore Book Actions ---

    fun saveCharacter(
        existing: LoreCharacter?,
        name: String,
        nicknames: List<String>,
        aliases: List<String>,
        portraitUrl: String?,
        biography: String,
        customDetails: Map<String, String>,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            loreRepo.saveCharacter(
                existing = existing,
                name = name,
                nicknames = nicknames,
                aliases = aliases,
                portraitUrl = portraitUrl,
                biography = biography,
                customDetails = customDetails,
                creatorName = currentDisplayName(),
                creatorAvatar = currentAvatarUrl(),
                memberUids = currentMemberUids()
            ).onSuccess { id ->
                _snackbarMessage.value = if (existing == null) "$name added to Lore Book" else "$name updated"
                onSuccess(id)
            }.onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save character" }
        }
    }

    fun deleteCharacter(characterId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            loreRepo.deleteCharacter(characterId)
                .onSuccess {
                    _snackbarMessage.value = "Character removed"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete character" }
        }
    }

    fun addLoreEntry(
        character: LoreCharacter,
        title: String,
        story: String,
        datePrecision: DatePrecision,
        historicalDateLabel: String,
        sortOrderEpoch: Long,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            loreRepo.addLoreEntry(
                character = character,
                title = title,
                story = story,
                datePrecision = datePrecision,
                historicalDateLabel = historicalDateLabel,
                sortOrderEpoch = sortOrderEpoch,
                contributorName = currentDisplayName(),
                contributorAvatar = currentAvatarUrl(),
                memberUids = currentMemberUids()
            ).onSuccess {
                _snackbarMessage.value = "Lore of ${character.name} updated"
                onDone()
            }.onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to add lore" }
        }
    }

    fun deleteLoreEntry(entryId: String) {
        viewModelScope.launch {
            loreRepo.deleteLoreEntry(entryId)
                .onSuccess { _snackbarMessage.value = "Lore entry removed" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete lore" }
        }
    }

    fun addRelationship(
        fromChar: LoreCharacter,
        toChar: LoreCharacter,
        label: String,
        category: RelationshipCategory,
        notes: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            loreRepo.addRelationship(fromChar, toChar, label, category, notes, currentMemberUids())
                .onSuccess {
                    _snackbarMessage.value = "Relationship added to map"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to add relationship" }
        }
    }

    fun deleteRelationship(relId: String) {
        viewModelScope.launch {
            loreRepo.deleteRelationship(relId)
                .onSuccess { _snackbarMessage.value = "Relationship removed" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete relationship" }
        }
    }

    // --- Quotes & Lyrics & Albums ---

    fun saveQuote(
        existing: QuoteEntry?,
        text: String,
        attributedToName: String,
        attributedType: AttributionType,
        attributedId: String?,
        dateLabel: String?,
        context: String?,
        category: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            loreRepo.saveQuote(
                existing = existing,
                text = text,
                attributedToName = attributedToName,
                attributedType = attributedType,
                attributedId = attributedId,
                dateLabel = dateLabel,
                context = context,
                category = category,
                addedByName = currentDisplayName(),
                addedByAvatar = currentAvatarUrl(),
                memberUids = currentMemberUids()
            ).onSuccess {
                _snackbarMessage.value = "Quote saved to Quote Book"
                onDone()
            }.onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save quote" }
        }
    }

    fun toggleQuoteFavorite(quote: QuoteEntry) {
        viewModelScope.launch { loreRepo.toggleQuoteFavorite(quote) }
    }

    fun deleteQuote(quoteId: String) {
        viewModelScope.launch {
            loreRepo.deleteQuote(quoteId).onSuccess { _snackbarMessage.value = "Quote removed" }
        }
    }

    fun addLyric(
        excerpt: String,
        songTitle: String,
        artist: String,
        commentary: String?,
        songLink: String?,
        category: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            loreRepo.addLyric(
                excerpt = excerpt,
                songTitle = songTitle,
                artist = artist,
                commentary = commentary,
                songLink = songLink,
                category = category,
                addedByName = currentDisplayName(),
                addedByAvatar = currentAvatarUrl(),
                memberUids = currentMemberUids()
            ).onSuccess {
                _snackbarMessage.value = "Lyrics added to Library"
                onDone()
            }.onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to add lyrics" }
        }
    }

    fun toggleLyricFavorite(lyric: LyricEntry) {
        viewModelScope.launch { loreRepo.toggleLyricFavorite(lyric) }
    }

    fun deleteLyric(lyricId: String) {
        viewModelScope.launch {
            loreRepo.deleteLyric(lyricId).onSuccess { _snackbarMessage.value = "Lyric removed" }
        }
    }

    fun saveAlbum(
        existing: PhotoAlbum?,
        title: String,
        description: String?,
        coverImageUrl: String?,
        onDone: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            loreRepo.saveAlbum(
                existing = existing,
                title = title,
                description = description,
                coverImageUrl = coverImageUrl,
                ownerName = currentDisplayName(),
                ownerAvatar = currentAvatarUrl(),
                memberUids = currentMemberUids()
            ).onSuccess { id ->
                _snackbarMessage.value = if (existing == null) "Album created" else "Album updated"
                onDone(id)
            }.onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save album" }
        }
    }

    fun uploadPhoto(
        album: PhotoAlbum,
        imageUrl: String,
        caption: String?,
        dateTakenLabel: String?,
        taggedCharacters: List<LoreCharacter>,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            loreRepo.uploadPhotoToAlbum(
                album = album,
                imageUrl = imageUrl,
                caption = caption,
                dateTakenLabel = dateTakenLabel,
                taggedCharacters = taggedCharacters,
                uploaderName = currentDisplayName(),
                uploaderAvatar = currentAvatarUrl(),
                memberUids = currentMemberUids()
            ).onSuccess {
                _snackbarMessage.value = "Photo shared to ${album.title}"
                onDone()
            }.onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to upload photo" }
        }
    }

    fun deletePhoto(photo: PhotoItem) {
        viewModelScope.launch {
            loreRepo.deletePhoto(photo).onSuccess { _snackbarMessage.value = "Photo removed" }
        }
    }

    // --- Profile, Friends, Messaging, Notifications ---

    fun updateProfile(
        displayName: String,
        tagline: String?,
        bio: String?,
        avatarUrl: String?,
        bannerUrl: String?,
        wallpaperUrl: String?,
        profileMusicTitle: String?,
        profileMusicArtist: String?,
        profileMusicUrl: String?,
        layoutStyle: ProfileLayoutStyle,
        themeId: String,
        bgColorHex: String?,
        gradientStartHex: String?,
        gradientEndHex: String?,
        cardColorHex: String?,
        textColorHex: String?,
        accentColorHex: String?,
        borderColorHex: String?,
        borderWidthDp: Double?,
        cardOpacity: Double?,
        headingFont: String?,
        bodyFont: String?,
        fontScale: Double?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            accountRepo.updateMemberProfile(
                displayName, tagline, bio, avatarUrl, bannerUrl, wallpaperUrl,
                profileMusicTitle, profileMusicArtist, profileMusicUrl,
                layoutStyle, themeId, bgColorHex, gradientStartHex, gradientEndHex,
                cardColorHex, textColorHex, accentColorHex, borderColorHex,
                borderWidthDp, cardOpacity, headingFont, bodyFont, fontScale
            ).onSuccess {
                _snackbarMessage.value = "Profile customization synced"
                onDone()
            }.onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update profile" }
        }
    }

    fun sendFriendRequestByCode(codeRaw: String, onDone: () -> Unit = {}) {
        val code = codeRaw.trim().uppercase()
        val me = myProfile.value ?: return
        val target = approvedProfiles.value.firstOrNull {
            it.friendCode.equals(code, ignoreCase = true)
        }
        if (target == null) {
            _snackbarMessage.value = "No approved member found with friend code $code"
            return
        }
        if (target.userId == currentUserId) {
            _snackbarMessage.value = "That is your own friend code"
            return
        }
        viewModelScope.launch {
            accountRepo.sendFriendRequest(me, target)
                .onSuccess {
                    _snackbarMessage.value = "Friend request sent to ${target.displayName}"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to send friend request" }
        }
    }

    fun sendFriendRequest(target: MemberProfile) {
        val me = myProfile.value ?: return
        viewModelScope.launch {
            accountRepo.sendFriendRequest(me, target)
                .onSuccess { _snackbarMessage.value = "Friend request sent to ${target.displayName}" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to send request" }
        }
    }

    fun respondToFriendRequest(request: FriendRequest, accept: Boolean) {
        viewModelScope.launch {
            accountRepo.respondToFriendRequest(request, accept)
                .onSuccess {
                    _snackbarMessage.value = if (accept) "Friend added!" else "Request declined"
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Action failed" }
        }
    }

    fun cancelFriendRequest(requestId: String) {
        viewModelScope.launch {
            accountRepo.cancelFriendRequest(requestId)
                .onSuccess { _snackbarMessage.value = "Friend request cancelled" }
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            accountRepo.removeFriend(friendshipId)
                .onSuccess { _snackbarMessage.value = "Friend removed" }
        }
    }

    fun openDirectChat(other: MemberProfile, onOpened: (String) -> Unit) {
        val me = myProfile.value ?: return
        viewModelScope.launch {
            msgRepo.getOrCreateDirectConversation(me, other)
                .onSuccess(onOpened)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Unable to open chat" }
        }
    }

    fun createGroupChat(title: String, members: List<MemberProfile>, onCreated: (String) -> Unit) {
        val me = myProfile.value ?: return
        viewModelScope.launch {
            msgRepo.createGroupConversation(title, me, members)
                .onSuccess(onCreated)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Unable to create group chat" }
        }
    }

    fun sendMessage(conversation: Conversation, text: String, imageUrl: String?, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            msgRepo.sendMessage(conversation, currentDisplayName(), currentAvatarUrl(), text, imageUrl)
                .onSuccess { onSent() }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to send message" }
        }
    }

    fun markConversationRead(conversation: Conversation) {
        viewModelScope.launch { msgRepo.markConversationRead(conversation) }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { accountRepo.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            accountRepo.markAllNotificationsRead(notifications.value)
                .onSuccess { _snackbarMessage.value = "All notifications marked as read" }
        }
    }

    // --- Settings, Themes, Security, Bugs, Admin ---

    fun updateAppThemeAndTypography(
        themeId: String,
        headingFont: String,
        bodyFont: String,
        fontScale: Double,
        headingWeight: Int,
        bodyWeight: Int
    ) {
        viewModelScope.launch {
            accountRepo.updateAppThemeAndTypography(
                themeId, headingFont, bodyFont, fontScale, headingWeight, bodyWeight
            ).onSuccess { _snackbarMessage.value = "Theme & typography updated" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update theme" }
        }
    }

    fun saveCustomTheme(theme: CustomThemeEntity, applyImmediately: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            accountRepo.saveCustomTheme(theme).onSuccess { id ->
                if (applyImmediately) {
                    val acc = (accountState.value as? UiState.Success)?.data
                    accountRepo.updateAppThemeAndTypography(
                        themeId = "custom_$id",
                        headingFont = theme.headingFont,
                        bodyFont = theme.bodyFont,
                        fontScale = acc?.fontScale ?: 1.0,
                        headingWeight = acc?.headingWeight ?: 700,
                        bodyWeight = acc?.bodyWeight ?: 400
                    )
                }
                _snackbarMessage.value = "Custom theme saved"
                onDone()
            }.onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save theme" }
        }
    }

    fun deleteCustomTheme(id: String) {
        viewModelScope.launch {
            accountRepo.deleteCustomTheme(id).onSuccess { _snackbarMessage.value = "Custom theme deleted" }
        }
    }

    fun updateNotificationPrefs(messages: Boolean, social: Boolean, lore: Boolean, admin: Boolean) {
        viewModelScope.launch {
            accountRepo.updateNotificationPreferences(messages, social, lore, admin)
                .onSuccess { _snackbarMessage.value = "Notification preferences saved" }
        }
    }

    fun updateAppLock(lockType: AppLockType, secret: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            accountRepo.updateAppLock(lockType, secret)
                .onSuccess {
                    _snackbarMessage.value = "App security lock updated"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update app lock" }
        }
    }

    fun submitBugReport(
        title: String,
        description: String,
        steps: String?,
        screen: String?,
        appVersion: String,
        screenshotUrl: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = accountRepo.submitBugReport(
                reporterName = currentDisplayName(),
                title = title,
                description = description,
                stepsToReproduce = steps,
                affectedScreen = screen,
                appVersion = appVersion,
                screenshotUrl = screenshotUrl
            )
            onResult(res)
        }
    }

    fun setMemberStatus(target: UserAccount, status: MemberStatus) {
        viewModelScope.launch {
            accountRepo.setMemberStatus(target, status, currentDisplayName())
                .onSuccess { _snackbarMessage.value = "${target.displayName} set to ${status.name}" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Admin update failed" }
        }
    }

    fun addWhitelistedEmail(email: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            accountRepo.addWhitelistedEmail(email)
                .onSuccess {
                    _snackbarMessage.value = "$email added to whitelist"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to whitelist email" }
        }
    }

    fun removeWhitelistedEmail(email: String) {
        viewModelScope.launch {
            accountRepo.removeWhitelistedEmail(email)
                .onSuccess { _snackbarMessage.value = "$email removed from whitelist" }
        }
    }

    fun resolveBugReport(reportId: String) {
        viewModelScope.launch {
            accountRepo.resolveBugReport(reportId)
                .onSuccess { _snackbarMessage.value = "Bug report marked resolved" }
        }
    }

    fun markAdminAlertRead(alertId: String) {
        viewModelScope.launch { accountRepo.markAdminAlertRead(alertId) }
    }

    fun syncCommunityAccess() {
        viewModelScope.launch {
            accountRepo.syncAllApprovedMemberships()
                .onSuccess { _snackbarMessage.value = "Community access synced across all archives" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Sync failed" }
        }
    }

    fun restoreBackupJson(jsonText: String) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonText)
                val memberUids = currentMemberUids()
                val myName = currentDisplayName()
                val myAvatar = currentAvatarUrl()
                var importedCount = 0

                if (root.has("characters")) {
                    val arr = root.getJSONArray("characters")
                    val existingNames = (charactersState.value as? UiState.Success)?.data?.map { it.name.lowercase() }?.toSet() ?: emptySet()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val name = obj.optString("name").trim()
                        if (name.isNotBlank() && name.lowercase() !in existingNames) {
                            val nicknames = mutableListOf<String>()
                            obj.optJSONArray("nicknames")?.let { na -> for (j in 0 until na.length()) nicknames.add(na.optString(j)) }
                            val aliases = mutableListOf<String>()
                            obj.optJSONArray("aliases")?.let { aa -> for (j in 0 until aa.length()) aliases.add(aa.optString(j)) }
                            val details = mutableMapOf<String, String>()
                            obj.optJSONObject("customDetails")?.let { cd ->
                                cd.keys().forEach { k -> details[k] = cd.optString(k) }
                            }
                            loreRepo.saveCharacter(
                                existing = null,
                                name = name,
                                nicknames = nicknames,
                                aliases = aliases,
                                portraitUrl = obj.optString("portraitUrl").takeIf { it.isNotBlank() },
                                biography = obj.optString("biography"),
                                customDetails = details,
                                creatorName = myName,
                                creatorAvatar = myAvatar,
                                memberUids = memberUids
                            )
                            importedCount++
                        }
                    }
                }

                if (root.has("quotes")) {
                    val arr = root.getJSONArray("quotes")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val text = obj.optString("text").trim()
                        if (text.isNotBlank()) {
                            loreRepo.saveQuote(
                                existing = null,
                                text = text,
                                attributedToName = obj.optString("attributedToName", "Unknown"),
                                attributedType = AttributionType.fromRaw(obj.optString("attributedType")),
                                attributedId = null,
                                dateLabel = obj.optString("dateLabel").takeIf { it.isNotBlank() },
                                context = obj.optString("context").takeIf { it.isNotBlank() },
                                category = obj.optString("category", "Memorable"),
                                addedByName = myName,
                                addedByAvatar = myAvatar,
                                memberUids = memberUids
                            )
                            importedCount++
                        }
                    }
                }

                if (root.has("lyrics")) {
                    val arr = root.getJSONArray("lyrics")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val excerpt = obj.optString("excerpt").trim()
                        if (excerpt.isNotBlank()) {
                            loreRepo.addLyric(
                                excerpt = excerpt,
                                songTitle = obj.optString("songTitle", "Untitled"),
                                artist = obj.optString("artist", "Unknown"),
                                commentary = obj.optString("commentary").takeIf { it.isNotBlank() },
                                songLink = obj.optString("songLink").takeIf { it.isNotBlank() },
                                category = obj.optString("category", "Nostalgic"),
                                addedByName = myName,
                                addedByAvatar = myAvatar,
                                memberUids = memberUids
                            )
                            importedCount++
                        }
                    }
                }

                _snackbarMessage.value = "Restored $importedCount items from backup"
            } catch (e: Exception) {
                _snackbarMessage.value = "Invalid backup file: ${e.localizedMessage}"
            }
        }
    }
}
