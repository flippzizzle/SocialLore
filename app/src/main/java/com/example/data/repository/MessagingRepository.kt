package com.example.data.repository

import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MemberProfile
import com.example.data.model.NotificationCategory
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

class MessagingRepository(
    private val db: FirebaseFirestore,
    private val accountRepository: AccountRepository,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing messages.")

    fun observeConversations(): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastMessageAt?.seconds ?: it.updatedAt?.seconds ?: 0L }
                }
        )
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    suspend fun getOrCreateDirectConversation(
        myProfile: MemberProfile,
        otherProfile: MemberProfile
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val sorted = listOf(uid, otherProfile.userId).sorted()
            val convId = "dm_${sorted[0]}_${sorted[1]}"
            val ref = db.collection("conversations").document(convId)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "groupChat" to false,
                    "participantUids" to sorted,
                    "participantNames" to mapOf(
                        uid to myProfile.displayName,
                        otherProfile.userId to otherProfile.displayName
                    ),
                    "unreadCounts" to mapOf(uid to 0L, otherProfile.userId to 0L),
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "conversations")
            Result.failure(e)
        }
    }

    suspend fun createGroupConversation(
        title: String,
        myProfile: MemberProfile,
        selectedMembers: List<MemberProfile>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val participants = (selectedMembers + myProfile).distinctBy { it.userId }.take(30)
            require(participants.size >= 2) { "Select at least one other member" }
            val ref = db.collection("conversations").document()
            val payload = mapOf(
                "groupChat" to true,
                "title" to title.trim().ifBlank { "Group Lore Chat" }.take(100),
                "participantUids" to participants.map { it.userId },
                "participantNames" to participants.associate { it.userId to it.displayName },
                "unreadCounts" to participants.associate { it.userId to 0L },
                "createdByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "conversations")
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderName: String,
        senderAvatarUrl: String?,
        text: String,
        imageUrl: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val cleanText = text.trim().take(4000)
            require(cleanText.isNotEmpty() || !imageUrl.isNullOrBlank()) { "Enter a message or select an image" }
            val msgRef = db.collection("conversations").document(conversation.id)
                .collection("messages").document()
            val msgPayload = mapOf(
                "conversationId" to conversation.id,
                "senderId" to uid,
                "senderName" to senderName.take(80),
                "senderAvatarUrl" to senderAvatarUrl,
                "text" to cleanText,
                "imageUrl" to imageUrl,
                "participantUids" to conversation.participantUids,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val nextUnread = conversation.unreadCounts.toMutableMap()
            conversation.participantUids.forEach { pUid ->
                if (pUid == uid) nextUnread[pUid] = 0L
                else nextUnread[pUid] = (nextUnread[pUid] ?: 0L) + 1L
            }

            val summary = if (cleanText.isNotBlank()) cleanText.take(200) else "Sent a photo"
            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(
                db.collection("conversations").document(conversation.id),
                mapOf(
                    "lastMessageText" to summary,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "unreadCounts" to nextUnread,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            conversation.participantUids.filter { it != uid }.forEach { recipientUid ->
                val title = if (conversation.groupChat) {
                    "${conversation.title ?: "Group Chat"} • $senderName"
                } else {
                    "New message from $senderName"
                }
                accountRepository.sendNotification(
                    recipientUid = recipientUid,
                    senderName = senderName,
                    category = NotificationCategory.MESSAGES,
                    title = title,
                    body = summary,
                    targetRoute = "messages/${conversation.id}"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "conversations/${conversation.id}/messages")
            Result.failure(e)
        }
    }

    suspend fun markConversationRead(conversation: Conversation): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            if ((conversation.unreadCounts[uid] ?: 0L) == 0L) return Result.success(Unit)
            val nextUnread = conversation.unreadCounts.toMutableMap()
            nextUnread[uid] = 0L
            db.collection("conversations").document(conversation.id).update(
                mapOf(
                    "unreadCounts" to nextUnread,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "conversations/${conversation.id}")
            Result.failure(e)
        }
    }
}
