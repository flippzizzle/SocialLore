package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MemberStatus {
    PENDING, APPROVED, SUSPENDED, BLOCKED, REMOVED;

    companion object {
        fun fromRaw(raw: String?): MemberStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class UserRole {
    MEMBER, ADMIN;

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: MEMBER
    }
}

enum class AppLockType {
    NONE, PIN, PASSWORD, BIOMETRIC;

    companion object {
        fun fromRaw(raw: String?): AppLockType =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class ProfileLayoutStyle(val displayName: String) {
    CLASSIC_MYSPACE("Classic MySpace"),
    HI5_GRID("Hi5 Showcase Grid"),
    CHRONICLE_CARDS("Chronicle Cards");

    companion object {
        fun fromRaw(raw: String?): ProfileLayoutStyle =
            entries.firstOrNull { it.name == raw } ?: CLASSIC_MYSPACE
    }
}

enum class PostType(val label: String) {
    TEXT("Post"),
    PHOTO("Photo"),
    CHARACTER_ADDED("New Character"),
    LORE_ADDED("Lore Update"),
    QUOTE_ADDED("Quote Added"),
    LYRIC_ADDED("Lyric Added"),
    ALBUM_ADDED("New Album"),
    PHOTO_ADDED("Photo Shared");

    companion object {
        fun fromRaw(raw: String?): PostType =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

enum class ReactionType(val label: String, val symbol: String) {
    LIKE("Like", "👍"),
    LOVE("Love", "❤️"),
    LAUGH("Laugh", "😂"),
    WOW("Wow", "😮"),
    SAD("Sad", "😢");

    companion object {
        fun fromRaw(raw: String?): ReactionType? =
            entries.firstOrNull { it.name == raw }
    }
}

enum class DatePrecision(val label: String) {
    EXACT("Exact Date"),
    APPROXIMATE("Approximate"),
    UNKNOWN("Unknown Date");

    companion object {
        fun fromRaw(raw: String?): DatePrecision =
            entries.firstOrNull { it.name == raw } ?: APPROXIMATE
    }
}

enum class RelationshipCategory(val label: String) {
    FAMILY("Family"),
    FRIEND("Friend"),
    COWORKER("Coworker"),
    NEIGHBOR("Neighbor"),
    ACQUAINTANCE("Acquaintance"),
    CUSTOM("Custom");

    companion object {
        fun fromRaw(raw: String?): RelationshipCategory =
            entries.firstOrNull { it.name == raw } ?: FRIEND
    }
}

enum class AttributionType(val label: String) {
    CHARACTER("Lore Character"),
    MEMBER("Community Member");

    companion object {
        fun fromRaw(raw: String?): AttributionType =
            entries.firstOrNull { it.name == raw } ?: CHARACTER
    }
}

enum class NotificationCategory(val label: String) {
    MESSAGES("Messages"),
    SOCIAL("Comments & Reactions"),
    FRIENDS("Friend Requests"),
    LORE("Lore & Archive"),
    ADMIN("Admin Alerts");

    companion object {
        fun fromRaw(raw: String?): NotificationCategory =
            entries.firstOrNull { it.name == raw } ?: SOCIAL
    }
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val status: String = MemberStatus.PENDING.name,
    val role: String = UserRole.MEMBER.name,
    val friendCode: String = "",
    val themeId: String = "midnight_dark",
    val headingFont: String = "Playfair Display",
    val bodyFont: String = "Plus Jakarta Sans",
    val fontScale: Double = 1.0,
    val headingWeight: Int = 700,
    val bodyWeight: Int = 400,
    val appLockType: String = AppLockType.NONE.name,
    val appLockHash: String? = null,
    val appLockSalt: String? = null,
    val notifMessages: Boolean = true,
    val notifSocial: Boolean = true,
    val notifLore: Boolean = true,
    val notifAdmin: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MemberProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val tagline: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val wallpaperUrl: String? = null,
    val profileMusicTitle: String? = null,
    val profileMusicArtist: String? = null,
    val profileMusicUrl: String? = null,
    val friendCode: String = "",
    val layoutStyle: String = ProfileLayoutStyle.CLASSIC_MYSPACE.name,
    val themeId: String = "midnight_dark",
    val bgColorHex: String? = null,
    val gradientStartHex: String? = null,
    val gradientEndHex: String? = null,
    val cardColorHex: String? = null,
    val textColorHex: String? = null,
    val accentColorHex: String? = null,
    val borderColorHex: String? = null,
    val borderWidthDp: Double? = null,
    val cardOpacity: Double? = null,
    val headingFont: String? = null,
    val bodyFont: String? = null,
    val fontScale: Double? = null,
    val status: String = MemberStatus.PENDING.name,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WhitelistedEmail(
    @DocumentId val id: String = "",
    val email: String = "",
    val addedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromName: String = "",
    val toName: String = "",
    val fromAvatar: String? = null,
    val status: String = "PENDING",
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FeedPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val postType: String = PostType.TEXT.name,
    val text: String = "",
    val imageUrl: String? = null,
    val linkedEntityId: String? = null,
    val linkedEntityTitle: String? = null,
    val userReactions: Map<String, String> = emptyMap(),
    val commentCount: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val text: String = "",
    val parentCommentId: String? = null,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LoreCharacter(
    @DocumentId val id: String = "",
    val name: String = "",
    val nicknames: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val portraitUrl: String? = null,
    val biography: String = "",
    val customDetails: Map<String, String> = emptyMap(),
    val createdByUid: String = "",
    val createdByName: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LoreEntry(
    @DocumentId val id: String = "",
    val characterId: String = "",
    val characterName: String = "",
    val title: String = "",
    val story: String = "",
    val datePrecision: String = DatePrecision.APPROXIMATE.name,
    val historicalDateLabel: String = "",
    val sortOrderEpoch: Long = 0L,
    val contributorUid: String = "",
    val contributorName: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CharacterRelationship(
    @DocumentId val id: String = "",
    val fromCharacterId: String = "",
    val fromCharacterName: String = "",
    val toCharacterId: String = "",
    val toCharacterName: String = "",
    val label: String = "",
    val category: String = RelationshipCategory.FRIEND.name,
    val notes: String? = null,
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuoteEntry(
    @DocumentId val id: String = "",
    val text: String = "",
    val attributedToName: String = "",
    val attributedType: String = AttributionType.CHARACTER.name,
    val attributedId: String? = null,
    val dateLabel: String? = null,
    val context: String? = null,
    val category: String = "Funny",
    val favoriteByUids: List<String> = emptyList(),
    val addedByUid: String = "",
    val addedByName: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LyricEntry(
    @DocumentId val id: String = "",
    val excerpt: String = "",
    val songTitle: String = "",
    val artist: String = "",
    val commentary: String? = null,
    val songLink: String? = null,
    val category: String = "Nostalgic",
    val favoriteByUids: List<String> = emptyList(),
    val addedByUid: String = "",
    val addedByName: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PhotoAlbum(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String? = null,
    val coverImageUrl: String? = null,
    val ownerUid: String = "",
    val ownerName: String = "",
    val photoCount: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PhotoItem(
    @DocumentId val id: String = "",
    val albumId: String = "",
    val albumTitle: String = "",
    val imageUrl: String = "",
    val caption: String? = null,
    val dateTakenLabel: String? = null,
    val taggedCharacterIds: List<String> = emptyList(),
    val taggedCharacterNames: List<String> = emptyList(),
    val uploadedByUid: String = "",
    val uploadedByName: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val groupChat: Boolean = false,
    val title: String? = null,
    val participantUids: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessageText: String? = null,
    val lastMessageAt: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String? = null,
    val text: String = "",
    val imageUrl: String? = null,
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val recipientUid: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val category: String = NotificationCategory.SOCIAL.name,
    val title: String = "",
    val body: String = "",
    val targetRoute: String = "feed",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomThemeEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val darkTheme: Boolean = true,
    val bgColorHex: String = "#0E0C1A",
    val gradientStartHex: String = "#17122B",
    val gradientEndHex: String = "#090812",
    val cardColorHex: String = "#1D1836",
    val textColorHex: String = "#F4F1FF",
    val accentColorHex: String = "#A855F7",
    val borderColorHex: String = "#5B3FA8",
    val borderWidthDp: Double = 1.5,
    val cardOpacity: Double = 0.92,
    val bgImageUrl: String? = null,
    val headingFont: String = "Playfair Display",
    val bodyFont: String = "Plus Jakarta Sans",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BugReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val reporterName: String = "",
    val title: String = "",
    val description: String = "",
    val stepsToReproduce: String? = null,
    val affectedScreen: String? = null,
    val appVersion: String = "1.0",
    val deviceInfo: String = "",
    val screenshotUrl: String? = null,
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminAlert(
    @DocumentId val id: String = "",
    val alertType: String = "APPROVAL_REQUIRED",
    val senderUid: String = "",
    val senderName: String = "",
    val senderEmail: String? = null,
    val title: String = "",
    val body: String = "",
    val referenceId: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
