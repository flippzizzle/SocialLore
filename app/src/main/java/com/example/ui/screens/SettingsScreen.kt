package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.auth.signOutUser
import com.example.data.model.AppLockType
import com.example.data.model.CustomThemeEntity
import com.example.data.model.UiState
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.FontPickerDialog
import com.example.ui.components.LoreCard
import com.example.ui.components.LoreImage
import com.example.ui.theme.LocalLoreTheme
import com.example.ui.theme.ThemeLibrary
import com.example.util.BackupSection
import com.example.util.BackupUtils
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SocialLoreViewModel,
    onSignedOut: () -> Unit
) {
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val loreEntriesState by viewModel.loreEntriesState.collectAsStateWithLifecycle()
    val relationshipsState by viewModel.relationshipsState.collectAsStateWithLifecycle()
    val quotesState by viewModel.quotesState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val albumsState by viewModel.albumsState.collectAsStateWithLifecycle()
    val photosState by viewModel.photosState.collectAsStateWithLifecycle()

    val account = (accountState as? UiState.Success)?.data ?: return
    val theme = LocalLoreTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var themeCategory by remember { mutableStateOf("All") }
    var editingCustomTheme by remember { mutableStateOf<CustomThemeEntity?>(null) }
    var showCustomThemeDialog by remember { mutableStateOf(false) }
    var fontPickerTarget by remember { mutableStateOf<String?>(null) }
    var showLockSecretDialog by remember { mutableStateOf<AppLockType?>(null) }
    var showBugReportDialog by remember { mutableStateOf(false) }
    var selectedBackupSection by remember { mutableStateOf(BackupSection.FULL_COMMUNITY) }

    var fontScale by remember(account.fontScale) { mutableFloatStateOf(account.fontScale.toFloat()) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = BackupUtils.exportJson(
                        section = selectedBackupSection,
                        myProfile = myProfile,
                        posts = (feedState as? UiState.Success)?.data.orEmpty(),
                        characters = (charactersState as? UiState.Success)?.data.orEmpty(),
                        loreEntries = (loreEntriesState as? UiState.Success)?.data.orEmpty(),
                        relationships = (relationshipsState as? UiState.Success)?.data.orEmpty(),
                        quotes = (quotesState as? UiState.Success)?.data.orEmpty(),
                        lyrics = (lyricsState as? UiState.Success)?.data.orEmpty(),
                        albums = (albumsState as? UiState.Success)?.data.orEmpty(),
                        photos = (photosState as? UiState.Success)?.data.orEmpty()
                    )
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.onSuccess {
                    viewModel.showMessage("Exported ${selectedBackupSection.label}")
                }.onFailure {
                    viewModel.showMessage("Export failed: ${it.localizedMessage}")
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Empty file")
                }.onSuccess { jsonText ->
                    viewModel.restoreBackupJson(jsonText)
                }.onFailure {
                    viewModel.showMessage("Failed to read backup: ${it.localizedMessage}")
                }
            }
        }
    }

    val themeCategories = remember {
        listOf("All") + ThemeLibrary.premadeThemes.map { it.category }.distinct()
    }
    val filteredPremade = remember(themeCategory) {
        if (themeCategory == "All") ThemeLibrary.premadeThemes
        else ThemeLibrary.premadeThemes.filter { it.category == themeCategory }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Account & App Lock Security
            item(key = "security") {
                LoreCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Account & Security Lock", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${account.displayName} • ${account.email}", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                    Spacer(Modifier.height(10.dp))
                    Text("App Lock Protection", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    val currentLock = AppLockType.fromRaw(account.appLockType)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppLockType.entries) { lt ->
                            FilterChip(
                                selected = currentLock == lt,
                                onClick = {
                                    when (lt) {
                                        AppLockType.NONE -> viewModel.updateAppLock(AppLockType.NONE, null)
                                        AppLockType.BIOMETRIC -> {
                                            val canBio = BiometricManager.from(context).canAuthenticate(
                                                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                                            )
                                            if (canBio == BiometricManager.BIOMETRIC_SUCCESS) {
                                                viewModel.updateAppLock(AppLockType.BIOMETRIC, null)
                                            } else {
                                                viewModel.showMessage("Biometric authentication is not configured on this device.")
                                            }
                                        }
                                        AppLockType.PIN, AppLockType.PASSWORD -> showLockSecretDialog = lt
                                    }
                                },
                                label = { Text(lt.name) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { signOutUser(credentialManager, scope, onSignedOut) },
                        modifier = Modifier.testTag("settings_sign_out_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }

            // 2. Theme Library (30+ Premade + Custom Themes)
            item(key = "themes") {
                LoreCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = theme.accent)
                            Spacer(Modifier.width(8.dp))
                            Text("Theme Library (${ThemeLibrary.premadeThemes.size}+)", style = MaterialTheme.typography.titleLarge)
                        }
                        Button(
                            onClick = {
                                editingCustomTheme = null
                                showCustomThemeDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                            modifier = Modifier.testTag("create_custom_theme_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Custom")
                        }
                    }

                    if (customThemes.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Your Custom Themes", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(customThemes, key = { it.id }) { ct ->
                                val customId = "custom_${ct.id}"
                                val selected = account.themeId == customId
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = theme.cardVariantColor,
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) theme.accent else theme.borderColor),
                                    modifier = Modifier
                                        .width(165.dp)
                                        .clickable {
                                            viewModel.updateAppThemeAndTypography(
                                                themeId = customId,
                                                headingFont = ct.headingFont,
                                                bodyFont = ct.bodyFont,
                                                fontScale = account.fontScale,
                                                headingWeight = account.headingWeight,
                                                bodyWeight = account.bodyWeight
                                            )
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(ct.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            IconButton(
                                                onClick = {
                                                    editingCustomTheme = ct
                                                    showCustomThemeDialog = true
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit custom theme", modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteCustomTheme(ct.id) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete custom theme", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(themeCategories) { cat ->
                            FilterChip(
                                selected = themeCategory == cat,
                                onClick = { themeCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredPremade, key = { it.id }) { spec ->
                            val selected = account.themeId == spec.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = spec.cardColor,
                                border = BorderStroke(
                                    if (selected) 2.5.dp else 1.dp,
                                    if (selected) spec.accent else spec.borderColor
                                ),
                                modifier = Modifier
                                    .width(136.dp)
                                    .clickable {
                                        viewModel.updateAppThemeAndTypography(
                                            themeId = spec.id,
                                            headingFont = account.headingFont,
                                            bodyFont = account.bodyFont,
                                            fontScale = account.fontScale,
                                            headingWeight = account.headingWeight,
                                            bodyWeight = account.bodyWeight
                                        )
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(22.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Brush.horizontalGradient(listOf(spec.gradientStart, spec.accent)))
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        spec.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = spec.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(spec.category, style = MaterialTheme.typography.labelSmall, color = spec.textSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Typography Studio (Full Google Fonts Catalog)
            item(key = "typography") {
                LoreCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Typography & Google Fonts", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { fontPickerTarget = "heading" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Heading: ${account.headingFont}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { fontPickerTarget = "body" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Body: ${account.bodyFont}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Text Size Scale: ${(fontScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = fontScale,
                        onValueChange = { fontScale = it },
                        onValueChangeFinished = {
                            viewModel.updateAppThemeAndTypography(
                                account.themeId, account.headingFont, account.bodyFont,
                                fontScale.toDouble(), account.headingWeight, account.bodyWeight
                            )
                        },
                        valueRange = 0.8f..1.45f
                    )
                    Text("Heading Weight", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(500 to "Medium", 600 to "SemiBold", 700 to "Bold", 800 to "ExtraBold")) { (w, lbl) ->
                            FilterChip(
                                selected = account.headingWeight == w,
                                onClick = {
                                    viewModel.updateAppThemeAndTypography(
                                        account.themeId, account.headingFont, account.bodyFont,
                                        fontScale.toDouble(), w, account.bodyWeight
                                    )
                                },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Body Weight", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(300 to "Light", 400 to "Regular", 500 to "Medium", 600 to "SemiBold")) { (w, lbl) ->
                            FilterChip(
                                selected = account.bodyWeight == w,
                                onClick = {
                                    viewModel.updateAppThemeAndTypography(
                                        account.themeId, account.headingFont, account.bodyFont,
                                        fontScale.toDouble(), account.headingWeight, w
                                    )
                                },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }
            }

            // 4. Notification Preferences
            item(key = "notif_prefs") {
                LoreCard {
                    Text("Notification Preferences", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    PrefSwitchRow("Direct & Group Messages", account.notifMessages) {
                        viewModel.updateNotificationPrefs(it, account.notifSocial, account.notifLore, account.notifAdmin)
                    }
                    PrefSwitchRow("Comments & Reactions", account.notifSocial) {
                        viewModel.updateNotificationPrefs(account.notifMessages, it, account.notifLore, account.notifAdmin)
                    }
                    PrefSwitchRow("Lore Book & Archive Updates", account.notifLore) {
                        viewModel.updateNotificationPrefs(account.notifMessages, account.notifSocial, it, account.notifAdmin)
                    }
                    PrefSwitchRow("Membership & Admin Notices", account.notifAdmin) {
                        viewModel.updateNotificationPrefs(account.notifMessages, account.notifSocial, account.notifLore, it)
                    }
                }
            }

            // 5. Data Export & Backup Restore
            item(key = "backup_export") {
                LoreCard {
                    Text("Backup & Data Export", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Export personal or community data in JSON format. Private DMs and group chats are always excluded from community backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BackupSection.entries) { sec ->
                            FilterChip(
                                selected = selectedBackupSection == sec,
                                onClick = { selectedBackupSection = sec },
                                label = { Text(sec.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                exportBackupLauncher.launch("social_lore_${selectedBackupSection.name.lowercase()}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                importBackupLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore JSON")
                        }
                    }
                }
            }

            // 6. Bug Reporting
            item(key = "bug_reporting") {
                LoreCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Report a Bug", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Automatically attaches device details & app version (${BuildConfig.VERSION_NAME}) and notifies flippzizzle@gmail.com.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { showBugReportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_bug_report_button")
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Submit Bug Report")
                    }
                }
            }
        }
    }

    if (showCustomThemeDialog) {
        CustomThemeEditorDialog(
            existing = editingCustomTheme,
            viewModel = viewModel,
            onDismiss = { showCustomThemeDialog = false }
        )
    }

    fontPickerTarget?.let { target ->
        FontPickerDialog(
            title = if (target == "heading") "App Heading Font" else "App Body Font",
            selectedFont = if (target == "heading") account.headingFont else account.bodyFont,
            onSelect = { chosen ->
                val newH = if (target == "heading") chosen else account.headingFont
                val newB = if (target == "body") chosen else account.bodyFont
                viewModel.updateAppThemeAndTypography(
                    account.themeId, newH, newB, fontScale.toDouble(), account.headingWeight, account.bodyWeight
                )
                fontPickerTarget = null
            },
            onDismiss = { fontPickerTarget = null }
        )
    }

    showLockSecretDialog?.let { lt ->
        var secret by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLockSecretDialog = null },
            title = { Text("Set ${lt.name} Lock") },
            text = {
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Enter at least 4 characters") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (lt == AppLockType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAppLock(lt, secret) { showLockSecretDialog = null }
                    },
                    enabled = secret.length >= 4
                ) { Text("Save Lock") }
            },
            dismissButton = { TextButton(onClick = { showLockSecretDialog = null }) { Text("Cancel") } }
        )
    }

    if (showBugReportDialog) {
        BugReportDialog(
            viewModel = viewModel,
            onDismiss = { showBugReportDialog = false }
        )
    }
}

@Composable
private fun PrefSwitchRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun BugReportDialog(
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalLoreTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var affectedScreen by remember { mutableStateOf("") }
    var screenshotUrl by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                ImageUtils.encodeUriToDataUrl(context, uri)
                    .onSuccess { screenshotUrl = it }
                    .onFailure { viewModel.showMessage(it.localizedMessage ?: "Failed to attach screenshot") }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(580.dp),
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
                    Text("Report a Bug", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Bug Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = affectedScreen,
                        onValueChange = { affectedScreen = it },
                        label = { Text("Affected Screen (e.g. Lore Book, Messages)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("What happened? *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = steps,
                        onValueChange = { steps = it },
                        label = { Text("Steps to Reproduce (optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (screenshotUrl != null) {
                        LoreImage(
                            imageUrl = screenshotUrl,
                            contentDescription = "Bug screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (screenshotUrl == null) "Attach Screenshot (optional)" else "Change Screenshot")
                    }
                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            submitting = true
                            errorMsg = null
                            viewModel.submitBugReport(
                                title = title,
                                description = description,
                                steps = steps,
                                screen = affectedScreen,
                                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                screenshotUrl = screenshotUrl
                            ) { res ->
                                submitting = false
                                res.onSuccess {
                                    viewModel.showMessage("Bug report saved to Admin Dashboard & queued alert for flippzizzle@gmail.com")
                                    onDismiss()
                                }.onFailure { err ->
                                    errorMsg = err.localizedMessage ?: "Submission failed. Please retry."
                                }
                            }
                        },
                        enabled = !submitting && title.isNotBlank() && description.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                    ) {
                        Text(if (submitting) "Sending..." else "Submit Report")
                    }
                }
            }
        }
    }
}
