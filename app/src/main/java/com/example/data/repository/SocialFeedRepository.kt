package com.example.data.repository

import com.example.data.model.FeedPost
import com.example.data.model.NotificationCategory
import com.example.data.model.PostComment
import com.example.data.model.PostType
import com.example.data.model.ReactionType
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

class SocialFeedRepository(
    private val db: FirebaseFirestore,
    private val accountRepository: AccountRepository,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing feed.")

    fun observeFeed(): Flow<List<FeedPost>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("posts")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    // Strict reverse chronological order by when added, newest first (no algorithmic sorting)
                    snap.toObjects(FeedPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    fun observeComments(postId: String): Flow<List<PostComment>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    suspend fun createPost(
        authorName: String,
        authorAvatarUrl: String?,
        postType: PostType,
        text: String,
        imageUrl: String? = null,
        linkedEntityId: String? = null,
        linkedEntityTitle: String? = null,
        memberUids: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val allMembers = (memberUids + uid).distinct().take(60)
            val ref = db.collection("posts").document()
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to authorName.take(80),
                "authorAvatarUrl" to authorAvatarUrl,
                "postType" to postType.name,
                "text" to text.trim().take(5000),
                "imageUrl" to imageUrl,
                "linkedEntityId" to linkedEntityId,
                "linkedEntityTitle" to linkedEntityTitle?.take(200),
                "userReactions" to emptyMap<String, String>(),
                "commentCount" to 0L,
                "memberUids" to allMembers,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts")
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, text: String, imageUrl: String?): Result<Unit> {
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "text" to text.trim().take(5000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (imageUrl != null) {
                updates["imageUrl"] = imageUrl
            }
            db.collection("posts").document(postId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(
        post: FeedPost,
        reaction: ReactionType,
        reactorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val updatedReactions = post.userReactions.toMutableMap()
            val added = if (updatedReactions[uid] == reaction.name) {
                updatedReactions.remove(uid)
                false
            } else {
                updatedReactions[uid] = reaction.name
                true
            }
            db.collection("posts").document(post.id).update(
                mapOf(
                    "userReactions" to updatedReactions,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (added && post.authorId != uid) {
                accountRepository.sendNotification(
                    recipientUid = post.authorId,
                    senderName = reactorName,
                    category = NotificationCategory.SOCIAL,
                    title = "New Reaction on Your Post",
                    body = "$reactorName reacted ${reaction.symbol} to your post.",
                    targetRoute = "feed"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}")
            Result.failure(e)
        }
    }

    suspend fun addComment(
        post: FeedPost,
        authorName: String,
        authorAvatarUrl: String?,
        text: String,
        parentCommentId: String? = null,
        replyTargetUid: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val commentRef = db.collection("posts").document(post.id).collection("comments").document()
            val allMembers = (post.memberUids + uid).distinct().take(60)
            val payload = mapOf(
                "postId" to post.id,
                "authorId" to uid,
                "authorName" to authorName.take(80),
                "authorAvatarUrl" to authorAvatarUrl,
                "text" to text.trim().take(2000),
                "parentCommentId" to parentCommentId,
                "memberUids" to allMembers,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(commentRef, payload)
            batch.update(
                db.collection("posts").document(post.id),
                mapOf(
                    "commentCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val notifyUid = replyTargetUid ?: post.authorId
            if (notifyUid != uid) {
                accountRepository.sendNotification(
                    recipientUid = notifyUid,
                    senderName = authorName,
                    category = NotificationCategory.SOCIAL,
                    title = if (parentCommentId != null) "New Reply" else "New Comment",
                    body = "$authorName: ${text.trim().take(100)}",
                    targetRoute = "feed"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments")
            Result.failure(e)
        }
    }
}
