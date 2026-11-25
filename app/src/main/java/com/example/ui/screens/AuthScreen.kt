package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.auth.GoogleSignInButton
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.signOutUser
import com.example.data.model.AppLockType
import com.example.data.model.MemberStatus
import com.example.data.model.UserAccount
import com.example.ui.components.LoreCard
import com.example.ui.components.LoreThemedBackground
import com.example.ui.theme.LocalLoreTheme
import com.example.util.SecurityUtils

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var authError by remember { mutableStateOf<String?>(null) }
    val theme = LocalLoreTheme.current

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    LoreThemedBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_lore_crest_1795574174731),
                    contentDescription = "Social Lore Archive Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(22.dp))
                LoreCard {
                    Text(
                        text = "Social Lore",
                        style = MaterialTheme.typography.displaySmall,
                        color = theme.accent
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Private Circle • Living Family & Friends Chronicle",
                        style = MaterialTheme.typography.titleSmall,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Share stories, customize your personal MySpace-inspired space, chronicle real-life characters and timelines, and archive quotes, lyrics, and albums with your circle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.textPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = theme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "New accounts require administrator approval before community lore is unlocked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.textSecondary
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    GoogleSignInButton(
                        onAuthSuccess = {
                            authError = null
                            onAuthSuccess()
                        },
                        onAuthError = { err -> authError = err }
                    )
                    if (authError != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = authError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApprovalGateScreen(
    account: UserAccount?,
    initError: String?,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    val theme = LocalLoreTheme.current
    val status = MemberStatus.fromRaw(account?.status)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LoreThemedBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LoreCard(modifier = Modifier.widthIn(max = 480.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (status == MemberStatus.PENDING) Icons.Default.HourglassTop else Icons.Default.Lock,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when {
                            initError != null -> "Account Setup Issue"
                            status == MemberStatus.PENDING -> "Awaiting Administrator Approval"
                            status == MemberStatus.SUSPENDED -> "Membership Suspended"
                            status == MemberStatus.BLOCKED -> "Account Blocked"
                            status == MemberStatus.REMOVED -> "Membership Removed"
                            else -> "Checking Membership..."
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = when {
                            initError != null -> initError
                            status == MemberStatus.PENDING ->
                                "Welcome, ${account?.displayName ?: "friend"}! Your Google account (${account?.email ?: ""}) has been registered and an approval request was sent to the administrator. Community lore, profiles, and messages stay hidden until approved."
                            status == MemberStatus.SUSPENDED ->
                                "Your Social Lore membership is currently suspended by the administrator."
                            status == MemberStatus.BLOCKED ->
                                "Access to this community is blocked for this account."
                            status == MemberStatus.REMOVED ->
                                "This account was removed from the Social Lore community."
                            else -> "Verifying your membership status..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("refresh_approval_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Refresh")
                        }
                        Button(
                            onClick = {
                                signOutUser(credentialManager, scope, onSignOut)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pending_sign_out_button")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    account: UserAccount,
    onUnlocked: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val theme = LocalLoreTheme.current
    val lockType = AppLockType.fromRaw(account.appLockType)
    var input by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun triggerBiometric() {
        val activity = context as? FragmentActivity ?: return
        val manager = BiometricManager.from(context)
        val canAuth = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            errorMsg = "Biometrics unavailable on this device. Use PIN/Password or update Security settings."
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    errorMsg = errString.toString()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Social Lore")
            .setSubtitle("Verify your identity to view community lore")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(lockType) {
        if (lockType == AppLockType.BIOMETRIC) {
            triggerBiometric()
        }
    }

    LoreThemedBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LoreCard(modifier = Modifier.widthIn(max = 420.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (lockType == AppLockType.BIOMETRIC) Icons.Default.Fingerprint else Icons.Default.Lock,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Social Lore Locked", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Locked for ${account.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(16.dp))

                    if (lockType == AppLockType.BIOMETRIC) {
                        Button(
                            onClick = { triggerBiometric() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("biometric_unlock_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Unlock with Biometrics")
                        }
                    } else {
                        OutlinedTextField(
                            value = input,
                            onValueChange = {
                                input = it
                                errorMsg = null
                            },
                            label = { Text(if (lockType == AppLockType.PIN) "Enter PIN" else "Enter Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (lockType == AppLockType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_lock_input")
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val hash = account.appLockHash.orEmpty()
                                val salt = account.appLockSalt.orEmpty()
                                if (SecurityUtils.verifySecret(input, hash, salt)) {
                                    onUnlocked()
                                } else {
                                    errorMsg = "Incorrect ${if (lockType == AppLockType.PIN) "PIN" else "password"}"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unlock_submit_button")
                        ) {
                            Text("Unlock")
                        }
                    }

                    if (errorMsg != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            errorMsg ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { signOutUser(credentialManager, scope, onSignOut) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Switch Google Account")
                    }
                }
            }
        }
    }
}
