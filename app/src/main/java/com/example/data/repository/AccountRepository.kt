package com.example.data.repository

import android.os.Build
import com.example.data.model.AdminAlert
import com.example.data.model.AppLockType
import com.example.data.model.BugReport
import com.example.data.model.CustomThemeEntity
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.MemberProfile
import com.example.data.model.MemberStatus
import com.example.data.model.NotificationCategory
import com.example.data.model.ProfileLayoutStyle
import com.example.data.model.UserAccount
import com.example.data.model.UserNotification
import com.example.data.model.UserRole
import com.example.data.model.WhitelistedEmail
import com.example.util.OperationType
import com.example.util.SecurityUtils
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AccountRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    companion object {
        const val ADMIN_EMAIL = "flippzizzle@gmail.com"
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    suspend fun ensureAccountInitialized(): Result<UserAccount> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        val email = (user.email ?: "").trim().lowercase()
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").ifBlank { "Member" }
        val isEmailAdmin = email == ADMIN_EMAIL && user.isEmailVerified

        return try {
            val userRef = db.collection("users").document(uid)
            val existingSnap = userRef.get().await()
            if (existingSnap.exists()) {
                val account = existingSnap.toObject(
                    UserAccount::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserAccount(userId = uid, email = email, displayName = displayName)

                // Ensure admin docs exist if owner signs in
                if (isEmailAdmin && account.status != MemberStatus.APPROVED.name) {
                    userRef.update(
                        mapOf(
                            "status" to MemberStatus.APPROVED.name,
                            "role" to UserRole.ADMIN.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    ensureAdminAndApprovedDocs(uid, email, displayName)
                } else if (isEmailAdmin) {
                    ensureAdminAndApprovedDocs(uid, email, displayName)
                }
                return Result.success(account)
            }

            // Check whitelist if not owner admin
            var isWhitelisted = false
            if (!isEmailAdmin && email.isNotBlank() && user.isEmailVerified) {
                isWhitelisted = try {
                    db.collection("whitelisted_emails").document(email).get().await().exists()
                } catch (_: Exception) {
                    false
                }
            }

            val initialStatus = if (isEmailAdmin || isWhitelisted) {
                MemberStatus.APPROVED.name
            } else {
                MemberStatus.PENDING.name
            }
            val initialRole = if (isEmailAdmin) UserRole.ADMIN.name else UserRole.MEMBER.name
            val friendCode = SecurityUtils.generateFriendCode(uid)

            val userPayload = mapOf(
                "userId" to uid,
                "email" to (user.email ?: email),
                "displayName" to displayName,
                "status" to initialStatus,
                "role" to initialRole,
                "friendCode" to friendCode,
                "themeId" to "midnight_dark",
                "headingFont" to "Playfair Display",
                "bodyFont" to "Plus Jakarta Sans",
                "fontScale" to 1.0,
                "headingWeight" to 700,
                "bodyWeight" to 400,
                "appLockType" to AppLockType.NONE.name,
                "notifMessages" to true,
                "notifSocial" to true,
                "notifLore" to true,
                "notifAdmin" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userRef.set(userPayload).await()

            if (isEmailAdmin || isWhitelisted) {
                ensureAdminAndApprovedDocs(uid, email, displayName, createAdminDoc = isEmailAdmin)
            }

            val profilePayload = mapOf(
                "userId" to uid,
                "displayName" to displayName,
                "friendCode" to friendCode,
                "layoutStyle" to ProfileLayoutStyle.CLASSIC_MYSPACE.name,
                "themeId" to "midnight_dark",
                "status" to initialStatus,
                "memberUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("member_profiles").document(uid).set(profilePayload).await()

            if (initialStatus == MemberStatus.PENDING.name) {
                notifyAdminOfPendingApproval(uid, displayName, user.email ?: email)
            }

            val created = userRef.get().await().toObject(
                UserAccount::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserAccount(
                userId = uid,
                email = email,
                displayName = displayName,
                status = initialStatus,
                role = initialRole,
                friendCode = friendCode
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    private suspend fun ensureAdminAndApprovedDocs(
        uid: String,
        email: String,
        displayName: String,
        createAdminDoc: Boolean = true
    ) {
        if (createAdminDoc) {
            val adminRef = db.collection("admins").document(uid)
            if (!adminRef.get().await().exists()) {
                adminRef.set(
                    mapOf(
                        "userId" to uid,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        val approvedRef = db.collection("approved_members").document(uid)
        if (!approvedRef.get().await().exists()) {
            approvedRef.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "approvedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private suspend fun notifyAdminOfPendingApproval(uid: String, displayName: String, email: String) {
        try {
            db.collection("admin_alerts").document().set(
                mapOf(
                    "alertType" to "APPROVAL_REQUIRED",
                    "senderUid" to uid,
                    "senderName" to displayName,
                    "senderEmail" to email,
                    "title" to "Membership Approval Required: $displayName",
                    "body" to "$displayName ($email) registered for Social Lore and is awaiting administrator approval.",
                    "referenceId" to uid,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("mail").document().set(
                mapOf(
                    "to" to listOf(ADMIN_EMAIL),
                    "senderUid" to uid,
                    "message" to mapOf(
                        "subject" to "[Social Lore] New Membership Approval Needed: $displayName",
                        "text" to "Hello,\n\nA new member has signed in to Social Lore and requires your approval:\n\nName: $displayName\nEmail: $email\nUser ID: $uid\n\nOpen the Social Lore Admin Dashboard to approve, whitelist, or block this request."
                    ),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "admin_alerts")
        }
    }

    fun observeMyAccount(): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).snapshots().map { snap ->
                if (!snap.exists()) null
                else snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )
    }

    fun observeApprovedProfiles(): Flow<List<MemberProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("member_profiles")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MemberProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == MemberStatus.APPROVED.name }
                        .sortedBy { it.displayName.lowercase() }
                }
        )
    }

    fun observeMyProfile(): Flow<MemberProfile?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("member_profiles").document(uid).snapshots().map { snap ->
                if (!snap.exists()) null
                else snap.toObject(MemberProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )
    }

    fun observeFriendRequests(): Flow<List<FriendRequest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("friend_requests")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    fun observeFriendships(): Flow<List<Friendship>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("friendships")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
        )
    }

    fun observeNotifications(): Flow<List<UserNotification>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
        )
    }

    fun observeCustomThemes(): Flow<List<CustomThemeEntity>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("custom_themes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CustomThemeEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
        )
    }

    fun observeAllUsersForAdmin(): Flow<List<UserAccount>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").snapshots().map { snap ->
                snap.toObjects(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<UserAccount> { it.status != MemberStatus.PENDING.name }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
            }
        )
    }

    fun observeWhitelistedEmails(): Flow<List<WhitelistedEmail>> = flow {
        requireUserId()
        emitAll(
            db.collection("whitelisted_emails").snapshots().map { snap ->
                snap.toObjects(WhitelistedEmail::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.email }
            }
        )
    }

    fun observeBugReports(): Flow<List<BugReport>> = flow {
        requireUserId()
        emitAll(
            db.collection("bug_reports").snapshots().map { snap ->
                snap.toObjects(BugReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        )
    }

    fun observeAdminAlerts(): Flow<List<AdminAlert>> = flow {
        requireUserId()
        emitAll(
            db.collection("admin_alerts").snapshots().map { snap ->
                snap.toObjects(AdminAlert::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        )
    }

    suspend fun updateAppThemeAndTypography(
        themeId: String,
        headingFont: String,
        bodyFont: String,
        fontScale: Double,
        headingWeight: Int,
        bodyWeight: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "themeId" to themeId,
                    "headingFont" to headingFont,
                    "bodyFont" to bodyFont,
                    "fontScale" to fontScale.coerceIn(0.75, 1.6),
                    "headingWeight" to headingWeight.coerceIn(100, 900),
                    "bodyWeight" to bodyWeight.coerceIn(100, 900),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPreferences(
        messages: Boolean,
        social: Boolean,
        lore: Boolean,
        admin: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "notifMessages" to messages,
                    "notifSocial" to social,
                    "notifLore" to lore,
                    "notifAdmin" to admin,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateAppLock(
        lockType: AppLockType,
        rawSecret: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val payload = mutableMapOf<String, Any>(
                "appLockType" to lockType.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (lockType == AppLockType.PIN || lockType == AppLockType.PASSWORD) {
                require(!rawSecret.isNullOrBlank() && rawSecret.length >= 4) {
                    "PIN or password must be at least 4 characters"
                }
                val salt = SecurityUtils.generateSalt()
                val hash = SecurityUtils.hashSecret(rawSecret, salt)
                payload["appLockSalt"] = salt
                payload["appLockHash"] = hash
            } else {
                payload["appLockSalt"] = ""
                payload["appLockHash"] = ""
            }
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateMemberProfile(
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
        fontScale: Double?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val profileUpdates = mapOf(
                "displayName" to displayName.trim().take(80),
                "tagline" to tagline?.trim()?.take(160),
                "bio" to bio?.trim()?.take(2500),
                "avatarUrl" to avatarUrl,
                "bannerUrl" to bannerUrl,
                "wallpaperUrl" to wallpaperUrl,
                "profileMusicTitle" to profileMusicTitle?.trim()?.take(120),
                "profileMusicArtist" to profileMusicArtist?.trim()?.take(120),
                "profileMusicUrl" to profileMusicUrl?.trim()?.take(500),
                "layoutStyle" to layoutStyle.name,
                "themeId" to themeId,
                "bgColorHex" to bgColorHex,
                "gradientStartHex" to gradientStartHex,
                "gradientEndHex" to gradientEndHex,
                "cardColorHex" to cardColorHex,
                "textColorHex" to textColorHex,
                "accentColorHex" to accentColorHex,
                "borderColorHex" to borderColorHex,
                "borderWidthDp" to borderWidthDp,
                "cardOpacity" to cardOpacity,
                "headingFont" to headingFont,
                "bodyFont" to bodyFont,
                "fontScale" to fontScale,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.collection("member_profiles").document(uid).update(profileUpdates).await()
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to displayName.trim().take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "member_profiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun saveCustomTheme(theme: CustomThemeEntity): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val ref = if (theme.id.isBlank()) {
                db.collection("users").document(uid).collection("custom_themes").document()
            } else {
                db.collection("users").document(uid).collection("custom_themes").document(theme.id)
            }
            val payload = mapOf(
                "userId" to uid,
                "name" to theme.name.trim().ifBlank { "My Custom Theme" }.take(60),
                "darkTheme" to theme.darkTheme,
                "bgColorHex" to theme.bgColorHex,
                "gradientStartHex" to theme.gradientStartHex,
                "gradientEndHex" to theme.gradientEndHex,
                "cardColorHex" to theme.cardColorHex,
                "textColorHex" to theme.textColorHex,
                "accentColorHex" to theme.accentColorHex,
                "borderColorHex" to theme.borderColorHex,
                "borderWidthDp" to theme.borderWidthDp.coerceIn(0.0, 12.0),
                "cardOpacity" to theme.cardOpacity.coerceIn(0.25, 1.0),
                "bgImageUrl" to theme.bgImageUrl,
                "headingFont" to theme.headingFont,
                "bodyFont" to theme.bodyFont,
                "createdAt" to (theme.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/custom_themes")
            Result.failure(e)
        }
    }

    suspend fun deleteCustomTheme(themeId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).collection("custom_themes")
                .document(themeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/custom_themes/$themeId")
            Result.failure(e)
        }
    }

    // --- Friends ---

    suspend fun sendFriendRequest(
        fromProfile: MemberProfile,
        targetProfile: MemberProfile
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            require(targetProfile.userId != uid) { "You cannot send a friend request to yourself." }
            val requestId = "${uid}_${targetProfile.userId}"
            val payload = mapOf(
                "fromUid" to uid,
                "toUid" to targetProfile.userId,
                "fromName" to fromProfile.displayName,
                "toName" to targetProfile.displayName,
                "fromAvatar" to fromProfile.avatarUrl,
                "status" to "PENDING",
                "participantUids" to listOf(uid, targetProfile.userId),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("friend_requests").document(requestId).set(payload).await()
            sendNotification(
                recipientUid = targetProfile.userId,
                senderName = fromProfile.displayName,
                category = NotificationCategory.FRIENDS,
                title = "New Friend Request",
                body = "${fromProfile.displayName} wants to connect with you on Social Lore.",
                targetRoute = "friends"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "friend_requests")
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(
        request: FriendRequest,
        accept: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val newStatus = if (accept) "ACCEPTED" else "DECLINED"
            db.collection("friend_requests").document(request.id).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (accept) {
                val sorted = listOf(request.fromUid, request.toUid).sorted()
                val friendshipId = "${sorted[0]}_${sorted[1]}"
                db.collection("friendships").document(friendshipId).set(
                    mapOf(
                        "participantUids" to sorted,
                        "participantNames" to mapOf(
                            request.fromUid to request.fromName,
                            request.toUid to request.toName
                        ),
                        "createdByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                sendNotification(
                    recipientUid = request.fromUid,
                    senderName = request.toName,
                    category = NotificationCategory.FRIENDS,
                    title = "Friend Request Accepted",
                    body = "${request.toName} accepted your friend request.",
                    targetRoute = "friends"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "friend_requests/${request.id}")
            Result.failure(e)
        }
    }

    suspend fun cancelFriendRequest(requestId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("friend_requests").document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "friend_requests/$requestId")
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendshipId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("friendships").document(friendshipId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "friendships/$friendshipId")
            Result.failure(e)
        }
    }

    // --- Notifications ---

    suspend fun sendNotification(
        recipientUid: String,
        senderName: String,
        category: NotificationCategory,
        title: String,
        body: String,
        targetRoute: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        if (recipientUid == uid && category != NotificationCategory.ADMIN) return
        try {
            db.collection("users").document(recipientUid)
                .collection("notifications").document().set(
                    mapOf(
                        "recipientUid" to recipientUid,
                        "senderUid" to uid,
                        "senderName" to senderName.take(80),
                        "category" to category.name,
                        "title" to title.take(160),
                        "body" to body.take(500),
                        "targetRoute" to targetRoute.take(160),
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$recipientUid/notifications")
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).collection("notifications")
                .document(notificationId).update(
                    mapOf(
                        "read" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications/$notificationId")
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(notifications: List<UserNotification>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val unread = notifications.filter { !it.read }
            if (unread.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            unread.forEach { n ->
                val ref = db.collection("users").document(uid).collection("notifications").document(n.id)
                batch.update(ref, mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications")
            Result.failure(e)
        }
    }

    // --- Bug Reporting ---

    suspend fun submitBugReport(
        reporterName: String,
        title: String,
        description: String,
        stepsToReproduce: String?,
        affectedScreen: String?,
        appVersion: String,
        screenshotUrl: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
            val reportRef = db.collection("bug_reports").document()
            val payload = mapOf(
                "reporterUid" to uid,
                "reporterName" to reporterName.take(80),
                "title" to title.trim().take(160),
                "description" to description.trim().take(5000),
                "stepsToReproduce" to stepsToReproduce?.trim()?.takeIf { it.isNotBlank() }?.take(3000),
                "affectedScreen" to affectedScreen?.trim()?.takeIf { it.isNotBlank() }?.take(120),
                "appVersion" to appVersion.take(60),
                "deviceInfo" to deviceInfo.take(200),
                "screenshotUrl" to screenshotUrl,
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            reportRef.set(payload).await()

            db.collection("admin_alerts").document().set(
                mapOf(
                    "alertType" to "BUG_REPORT",
                    "senderUid" to uid,
                    "senderName" to reporterName.take(80),
                    "title" to "Bug Report: ${title.trim().take(120)}",
                    "body" to description.trim().take(2000),
                    "referenceId" to reportRef.id,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("mail").document().set(
                mapOf(
                    "to" to listOf(ADMIN_EMAIL),
                    "senderUid" to uid,
                    "message" to mapOf(
                        "subject" to "[Social Lore Bug Report] ${title.trim().take(120)}",
                        "text" to buildString {
                            appendLine("Reporter: $reporterName (UID: $uid)")
                            appendLine("App Version: $appVersion")
                            appendLine("Device: $deviceInfo")
                            if (!affectedScreen.isNullOrBlank()) appendLine("Screen: $affectedScreen")
                            appendLine()
                            appendLine("Description:")
                            appendLine(description.trim())
                            if (!stepsToReproduce.isNullOrBlank()) {
                                appendLine()
                                appendLine("Steps to Reproduce:")
                                appendLine(stepsToReproduce.trim())
                            }
                            if (!screenshotUrl.isNullOrBlank()) {
                                appendLine()
                                appendLine("[Screenshot attached in Admin Dashboard Bug Reports]")
                            }
                        }
                    ),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "bug_reports")
            Result.failure(e)
        }
    }

    // --- Admin Actions ---

    suspend fun setMemberStatus(
        targetUser: UserAccount,
        newStatus: MemberStatus,
        adminName: String
    ): Result<Unit> {
        val adminUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(targetUser.userId).update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("member_profiles").document(targetUser.userId).update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (newStatus == MemberStatus.APPROVED) {
                db.collection("approved_members").document(targetUser.userId).set(
                    mapOf(
                        "userId" to targetUser.userId,
                        "displayName" to targetUser.displayName,
                        "approvedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                syncMemberUidAcrossCommunity(targetUser.userId, add = true)
                sendNotification(
                    recipientUid = targetUser.userId,
                    senderName = adminName,
                    category = NotificationCategory.ADMIN,
                    title = "Membership Approved!",
                    body = "Welcome to Social Lore! Your account has been approved by the administrator.",
                    targetRoute = "feed"
                )
            } else {
                db.collection("approved_members").document(targetUser.userId).delete().await()
                syncMemberUidAcrossCommunity(targetUser.userId, add = false)
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${targetUser.userId}")
            Result.failure(e)
        }
    }

    suspend fun addWhitelistedEmail(emailRaw: String): Result<Unit> {
        val adminUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val clean = emailRaw.trim().lowercase()
        return try {
            require(clean.contains("@") && clean.length in 3..200) { "Enter a valid email address" }
            db.collection("whitelisted_emails").document(clean).set(
                mapOf(
                    "email" to clean,
                    "addedByUid" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "whitelisted_emails/$clean")
            Result.failure(e)
        }
    }

    suspend fun removeWhitelistedEmail(email: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("whitelisted_emails").document(email.trim().lowercase()).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "whitelisted_emails/$email")
            Result.failure(e)
        }
    }

    suspend fun resolveBugReport(reportId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("bug_reports").document(reportId).update(
                mapOf(
                    "status" to "RESOLVED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "bug_reports/$reportId")
            Result.failure(e)
        }
    }

    suspend fun markAdminAlertRead(alertId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("admin_alerts").document(alertId).update(mapOf("read" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "admin_alerts/$alertId")
            Result.failure(e)
        }
    }

    suspend fun syncAllApprovedMemberships(): Result<Unit> {
        return try {
            requireUserId()
            val approvedUids = db.collection("approved_members").get().await().documents.map { it.id }
            if (approvedUids.isEmpty()) return Result.success(Unit)
            val collections = listOf(
                "member_profiles", "posts", "characters", "lore_entries",
                "relationships", "quotes", "lyrics", "albums", "photos"
            )
            for (col in collections) {
                val docs = db.collection(col).get().await().documents
                docs.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { doc ->
                        batch.update(
                            doc.reference,
                            mapOf(
                                "memberUids" to approvedUids,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "community_sync")
            Result.failure(e)
        }
    }

    private suspend fun syncMemberUidAcrossCommunity(targetUid: String, add: Boolean) {
        val op = if (add) FieldValue.arrayUnion(targetUid) else FieldValue.arrayRemove(targetUid)
        val collections = listOf(
            "member_profiles", "posts", "characters", "lore_entries",
            "relationships", "quotes", "lyrics", "albums", "photos"
        )
        for (col in collections) {
            val docs = db.collection(col).get().await().documents
            docs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "memberUids" to op,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
        }
    }
}
