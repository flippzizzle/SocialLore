package com.example.data.repository

import com.example.data.model.AttributionType
import com.example.data.model.CharacterRelationship
import com.example.data.model.DatePrecision
import com.example.data.model.LoreCharacter
import com.example.data.model.LoreEntry
import com.example.data.model.LyricEntry
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import com.example.data.model.PostType
import com.example.data.model.QuoteEntry
import com.example.data.model.RelationshipCategory
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LoreRepository(
    private val db: FirebaseFirestore,
    private val feedRepository: SocialFeedRepository,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Lore Book.")

    fun observeCharacters(): Flow<List<LoreCharacter>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("characters")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LoreCharacter::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
        )
    }

    fun observeLoreEntries(): Flow<List<LoreEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("lore_entries")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LoreEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<LoreEntry> { it.sortOrderEpoch }.thenBy { it.createdAt?.seconds ?: 0L })
                }
        )
    }

    fun observeRelationships(): Flow<List<CharacterRelationship>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("relationships")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CharacterRelationship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
        )
    }

    fun observeQuotes(): Flow<List<QuoteEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("quotes")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuoteEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    fun observeLyrics(): Flow<List<LyricEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("lyrics")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LyricEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    fun observeAlbums(): Flow<List<PhotoAlbum>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("albums")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PhotoAlbum::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
        )
    }

    fun observePhotos(): Flow<List<PhotoItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("photos")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PhotoItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    // --- Characters & Lore ---

    suspend fun saveCharacter(
        existing: LoreCharacter?,
        name: String,
        nicknames: List<String>,
        aliases: List<String>,
        portraitUrl: String?,
        biography: String,
        customDetails: Map<String, String>,
        creatorName: String,
        creatorAvatar: String?,
        memberUids: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val allMembers = (memberUids + uid).distinct().take(60)
            if (existing == null) {
                val ref = db.collection("characters").document()
                val payload = mapOf(
                    "name" to name.trim().take(100),
                    "nicknames" to nicknames.take(15),
                    "aliases" to aliases.take(15),
                    "portraitUrl" to portraitUrl,
                    "biography" to biography.trim().take(10000),
                    "customDetails" to customDetails,
                    "createdByUid" to uid,
                    "createdByName" to creatorName.take(80),
                    "memberUids" to allMembers,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.set(payload).await()

                feedRepository.createPost(
                    authorName = creatorName,
                    authorAvatarUrl = creatorAvatar,
                    postType = PostType.CHARACTER_ADDED,
                    text = "$creatorName added ${name.trim()} to the Lore Book.",
                    imageUrl = portraitUrl,
                    linkedEntityId = ref.id,
                    linkedEntityTitle = name.trim(),
                    memberUids = allMembers
                )
                Result.success(ref.id)
            } else {
                val updates = mapOf(
                    "name" to name.trim().take(100),
                    "nicknames" to nicknames.take(15),
                    "aliases" to aliases.take(15),
                    "portraitUrl" to portraitUrl,
                    "biography" to biography.trim().take(10000),
                    "customDetails" to customDetails,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                db.collection("characters").document(existing.id).update(updates).await()
                Result.success(existing.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "characters")
            Result.failure(e)
        }
    }

    suspend fun deleteCharacter(characterId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("characters").document(characterId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "characters/$characterId")
            Result.failure(e)
        }
    }

    suspend fun addLoreEntry(
        character: LoreCharacter,
        title: String,
        story: String,
        datePrecision: DatePrecision,
        historicalDateLabel: String,
        sortOrderEpoch: Long,
        contributorName: String,
        contributorAvatar: String?,
        memberUids: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val allMembers = (memberUids + uid).distinct().take(60)
            val ref = db.collection("lore_entries").document()
            val dateLabel = historicalDateLabel.trim().ifBlank {
                if (datePrecision == DatePrecision.UNKNOWN) "Unknown Date" else "Undated"
            }.take(100)
            val payload = mapOf(
                "characterId" to character.id,
                "characterName" to character.name.take(100),
                "title" to title.trim().take(160),
                "story" to story.trim().take(10000),
                "datePrecision" to datePrecision.name,
                "historicalDateLabel" to dateLabel,
                "sortOrderEpoch" to sortOrderEpoch,
                "contributorUid" to uid,
                "contributorName" to contributorName.take(80),
                "memberUids" to allMembers,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()

            feedRepository.createPost(
                authorName = contributorName,
                authorAvatarUrl = contributorAvatar,
                postType = PostType.LORE_ADDED,
                text = "$contributorName updated the Lore of ${character.name}: \"${title.trim()}\"",
                linkedEntityId = character.id,
                linkedEntityTitle = character.name,
                memberUids = allMembers
            )
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "lore_entries")
            Result.failure(e)
        }
    }

    suspend fun deleteLoreEntry(entryId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("lore_entries").document(entryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "lore_entries/$entryId")
            Result.failure(e)
        }
    }

    // --- Relationships ---

    suspend fun addRelationship(
        fromChar: LoreCharacter,
        toChar: LoreCharacter,
        label: String,
        category: RelationshipCategory,
        notes: String?,
        memberUids: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            require(fromChar.id != toChar.id) { "Select two different characters" }
            val allMembers = (memberUids + uid).distinct().take(60)
            val ref = db.collection("relationships").document()
            val payload = mapOf(
                "fromCharacterId" to fromChar.id,
                "fromCharacterName" to fromChar.name.take(100),
                "toCharacterId" to toChar.id,
                "toCharacterName" to toChar.name.take(100),
                "label" to label.trim().take(80),
                "category" to category.name,
                "notes" to notes?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                "createdByUid" to uid,
                "memberUids" to allMembers,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "relationships")
            Result.failure(e)
        }
    }

    suspend fun deleteRelationship(relationshipId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("relationships").document(relationshipId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "relationships/$relationshipId")
            Result.failure(e)
        }
    }

    // --- Quotes ---

    suspend fun saveQuote(
        existing: QuoteEntry?,
        text: String,
        attributedToName: String,
        attributedType: AttributionType,
        attributedId: String?,
        dateLabel: String?,
        context: String?,
        category: String,
        addedByName: String,
        addedByAvatar: String?,
        memberUids: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val allMembers = (memberUids + uid).distinct().take(60)
            if (existing == null) {
                val ref = db.collection("quotes").document()
                val payload = mapOf(
                    "text" to text.trim().take(2000),
                    "attributedToName" to attributedToName.trim().take(100),
                    "attributedType" to attributedType.name,
                    "attributedId" to attributedId,
                    "dateLabel" to dateLabel?.trim()?.takeIf { it.isNotBlank() }?.take(80),
                    "context" to context?.trim()?.takeIf { it.isNotBlank() }?.take(1000),
                    "category" to category.trim().ifBlank { "Memorable" }.take(60),
                    "favoriteByUids" to emptyList<String>(),
                    "addedByUid" to uid,
                    "addedByName" to addedByName.take(80),
                    "memberUids" to allMembers,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.set(payload).await()

                feedRepository.createPost(
                    authorName = addedByName,
                    authorAvatarUrl = addedByAvatar,
                    postType = PostType.QUOTE_ADDED,
                    text = "$addedByName archived a quote in the Quote Book:\n\"${text.trim().take(240)}\" — ${attributedToName.trim()}",
                    linkedEntityId = ref.id,
                    linkedEntityTitle = attributedToName.trim(),
                    memberUids = allMembers
                )
                Result.success(ref.id)
            } else {
                val updates = mapOf(
                    "text" to text.trim().take(2000),
                    "attributedToName" to attributedToName.trim().take(100),
                    "attributedType" to attributedType.name,
                    "attributedId" to attributedId,
                    "dateLabel" to dateLabel?.trim()?.takeIf { it.isNotBlank() }?.take(80),
                    "context" to context?.trim()?.takeIf { it.isNotBlank() }?.take(1000),
                    "category" to category.trim().ifBlank { "Memorable" }.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                db.collection("quotes").document(existing.id).update(updates).await()
                Result.success(existing.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "quotes")
            Result.failure(e)
        }
    }

    suspend fun toggleQuoteFavorite(quote: QuoteEntry): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val next = if (uid in quote.favoriteByUids) {
                quote.favoriteByUids - uid
            } else {
                (quote.favoriteByUids + uid).distinct()
            }
            db.collection("quotes").document(quote.id).update(
                mapOf(
                    "favoriteByUids" to next,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "quotes/${quote.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteQuote(quoteId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("quotes").document(quoteId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "quotes/$quoteId")
            Result.failure(e)
        }
    }

    // --- Song Lyrics ---

    suspend fun addLyric(
        excerpt: String,
        songTitle: String,
        artist: String,
        commentary: String?,
        songLink: String?,
        category: String,
        addedByName: String,
        addedByAvatar: String?,
        memberUids: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val allMembers = (memberUids + uid).distinct().take(60)
            val ref = db.collection("lyrics").document()
            val payload = mapOf(
                "excerpt" to excerpt.trim().take(3000),
                "songTitle" to songTitle.trim().take(160),
                "artist" to artist.trim().take(160),
                "commentary" to commentary?.trim()?.takeIf { it.isNotBlank() }?.take(1500),
                "songLink" to songLink?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                "category" to category.trim().ifBlank { "Nostalgic" }.take(60),
                "favoriteByUids" to emptyList<String>(),
                "addedByUid" to uid,
                "addedByName" to addedByName.take(80),
                "memberUids" to allMembers,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()

            feedRepository.createPost(
                authorName = addedByName,
                authorAvatarUrl = addedByAvatar,
                postType = PostType.LYRIC_ADDED,
                text = "$addedByName added lyrics from \"${songTitle.trim()}\" by ${artist.trim()}:\n\"${excerpt.trim().take(200)}\"",
                linkedEntityId = ref.id,
                linkedEntityTitle = songTitle.trim(),
                memberUids = allMembers
            )
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "lyrics")
            Result.failure(e)
        }
    }

    suspend fun toggleLyricFavorite(lyric: LyricEntry): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val next = if (uid in lyric.favoriteByUids) {
                lyric.favoriteByUids - uid
            } else {
                (lyric.favoriteByUids + uid).distinct()
            }
            db.collection("lyrics").document(lyric.id).update(
                mapOf(
                    "favoriteByUids" to next,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "lyrics/${lyric.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteLyric(lyricId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("lyrics").document(lyricId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "lyrics/$lyricId")
            Result.failure(e)
        }
    }

    // --- Photo Albums & Photos ---

    suspend fun saveAlbum(
        existing: PhotoAlbum?,
        title: String,
        description: String?,
        coverImageUrl: String?,
        ownerName: String,
        ownerAvatar: String?,
        memberUids: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val allMembers = (memberUids + uid).distinct().take(60)
            if (existing == null) {
                val ref = db.collection("albums").document()
                val payload = mapOf(
                    "title" to title.trim().take(120),
                    "description" to description?.trim()?.takeIf { it.isNotBlank() }?.take(1000),
                    "coverImageUrl" to coverImageUrl,
                    "ownerUid" to uid,
                    "ownerName" to ownerName.take(80),
                    "photoCount" to 0L,
                    "memberUids" to allMembers,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.set(payload).await()

                feedRepository.createPost(
                    authorName = ownerName,
                    authorAvatarUrl = ownerAvatar,
                    postType = PostType.ALBUM_ADDED,
                    text = "$ownerName created a new Photo Album: \"${title.trim()}\"",
                    imageUrl = coverImageUrl,
                    linkedEntityId = ref.id,
                    linkedEntityTitle = title.trim(),
                    memberUids = allMembers
                )
                Result.success(ref.id)
            } else {
                val updates = mapOf(
                    "title" to title.trim().take(120),
                    "description" to description?.trim()?.takeIf { it.isNotBlank() }?.take(1000),
                    "coverImageUrl" to coverImageUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                db.collection("albums").document(existing.id).update(updates).await()
                Result.success(existing.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "albums")
            Result.failure(e)
        }
    }

    suspend fun uploadPhotoToAlbum(
        album: PhotoAlbum,
        imageUrl: String,
        caption: String?,
        dateTakenLabel: String?,
        taggedCharacters: List<LoreCharacter>,
        uploaderName: String,
        uploaderAvatar: String?,
        memberUids: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val allMembers = (memberUids + uid).distinct().take(60)
            val photoRef = db.collection("photos").document()
            val payload = mapOf(
                "albumId" to album.id,
                "albumTitle" to album.title.take(120),
                "imageUrl" to imageUrl,
                "caption" to caption?.trim()?.takeIf { it.isNotBlank() }?.take(1000),
                "dateTakenLabel" to dateTakenLabel?.trim()?.takeIf { it.isNotBlank() }?.take(80),
                "taggedCharacterIds" to taggedCharacters.map { it.id }.take(20),
                "taggedCharacterNames" to taggedCharacters.map { it.name }.take(20),
                "uploadedByUid" to uid,
                "uploadedByName" to uploaderName.take(80),
                "memberUids" to allMembers,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(photoRef, payload)
            val albumUpdates = mutableMapOf<String, Any>(
                "photoCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (album.coverImageUrl.isNullOrBlank()) {
                albumUpdates["coverImageUrl"] = imageUrl
            }
            batch.update(db.collection("albums").document(album.id), albumUpdates)
            batch.commit().await()

            val tagSuffix = if (taggedCharacters.isNotEmpty()) {
                " featuring ${taggedCharacters.joinToString(", ") { it.name }}"
            } else ""
            feedRepository.createPost(
                authorName = uploaderName,
                authorAvatarUrl = uploaderAvatar,
                postType = PostType.PHOTO_ADDED,
                text = "$uploaderName shared a photo in \"${album.title}\"$tagSuffix${if (!caption.isNullOrBlank()) ": \"${caption.trim()}\"" else ""}",
                imageUrl = imageUrl,
                linkedEntityId = album.id,
                linkedEntityTitle = album.title,
                memberUids = allMembers
            )
            Result.success(photoRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "photos")
            Result.failure(e)
        }
    }

    suspend fun deletePhoto(photo: PhotoItem): Result<Unit> {
        return try {
            requireUserId()
            db.collection("photos").document(photo.id).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "photos/${photo.id}")
            Result.failure(e)
        }
    }
}
