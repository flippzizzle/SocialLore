package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttributionType
import com.example.data.model.DatePrecision
import com.example.data.model.LoreCharacter
import com.example.data.model.PhotoItem
import com.example.data.model.RelationshipCategory
import com.example.data.model.UiState
import com.example.data.model.UserRole
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.EmptyLoreState
import com.example.ui.components.LoreAvatar
import com.example.ui.components.LoreCard
import com.example.ui.components.LoreImage
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.LocalLoreTheme
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterDetailScreen(
    viewModel: SocialLoreViewModel,
    characterId: String,
    onBack: () -> Unit,
    onOpenCharacter: (String) -> Unit,
    onOpenRelationshipMap: () -> Unit
) {
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val loreEntriesState by viewModel.loreEntriesState.collectAsStateWithLifecycle()
    val quotesState by viewModel.quotesState.collectAsStateWithLifecycle()
    val relationshipsState by viewModel.relationshipsState.collectAsStateWithLifecycle()
    val photosState by viewModel.photosState.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current

    val allCharacters = (charactersState as? UiState.Success)?.data.orEmpty()
    val character = allCharacters.firstOrNull { it.id == characterId }
    val isAdmin = (accountState as? UiState.Success)?.data?.role == UserRole.ADMIN.name

    if (character == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Back to Lore Book")
            }
            EmptyLoreState(
                title = "Character Not Found",
                subtitle = "This character page may have been removed or is still loading."
            )
        }
        return
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddLoreDialog by remember { mutableStateOf(false) }
    var showAddRelDialog by remember { mutableStateOf(false) }
    var showAddQuoteDialog by remember { mutableStateOf(false) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItem?>(null) }

    val characterLore = remember(loreEntriesState, character.id) {
        (loreEntriesState as? UiState.Success)?.data
            ?.filter { it.characterId == character.id }
            ?.sortedWith(
                compareBy<com.example.data.model.LoreEntry> {
                    it.datePrecision == DatePrecision.UNKNOWN.name
                }.thenBy { it.sortOrderEpoch }
                    .thenBy { it.createdAt?.seconds ?: 0L }
            ).orEmpty()
    }

    val associatedQuotes = remember(quotesState, character.id, character.name) {
        (quotesState as? UiState.Success)?.data?.filter { q ->
            q.attributedId == character.id || q.attributedToName.equals(character.name, ignoreCase = true)
        }.orEmpty()
    }

    val charRelationships = remember(relationshipsState, character.id) {
        (relationshipsState as? UiState.Success)?.data?.filter { r ->
            r.fromCharacterId == character.id || r.toCharacterId == character.id
        }.orEmpty()
    }

    val taggedPhotos = remember(photosState, character.id) {
        (photosState as? UiState.Success)?.data?.filter { p ->
            character.id in p.taggedCharacterIds
        }.orEmpty()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "top_nav") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Lore Book")
                    }
                    Row {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit character")
                        }
                        if (character.createdByUid == viewModel.currentUserId || isAdmin) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete character", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Character Hero Card
            item(key = "char_header") {
                LoreCard {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                        if (!character.portraitUrl.isNullOrBlank()) {
                            LoreImage(
                                imageUrl = character.portraitUrl,
                                contentDescription = "${character.name} portrait",
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            LoreAvatar(null, character.name, size = 96.dp)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(character.name, style = MaterialTheme.typography.headlineMedium)
                            if (character.nicknames.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Nicknames: ${character.nicknames.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.accent
                                )
                            }
                            if (character.aliases.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Aliases: ${character.aliases.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.textSecondary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Created by ${character.createdByName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.textSecondary
                            )
                        }
                    }

                    if (character.biography.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text("Biography", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(character.biography, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (character.customDetails.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = theme.borderColor.copy(alpha = 0.3f))
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            character.customDetails.forEach { (k, v) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = theme.cardVariantColor
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(k, style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                                        Text(v, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    // Prominent required CTA button
                    Button(
                        onClick = { showAddLoreDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("update_lore_button")
                    ) {
                        Icon(Icons.Default.HistoryEdu, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Update Lore of ${character.name}")
                    }
                }
            }

            // Chronological Lore Timeline
            item(key = "timeline_header") {
                Text(
                    "Lore Timeline (${characterLore.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            if (characterLore.isEmpty()) {
                item {
                    LoreCard {
                        Text(
                            "No lore entries yet. Tap \"Update Lore of ${character.name}\" above to record the first story!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.textSecondary
                        )
                    }
                }
            } else {
                items(characterLore, key = { it.id }) { entry ->
                    val precision = DatePrecision.fromRaw(entry.datePrecision)
                    LoreCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(theme.accent)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    entry.historicalDateLabel.ifBlank { precision.label },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = theme.accent
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = theme.cardVariantColor
                                ) {
                                    Text(
                                        precision.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.textSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (entry.contributorUid == viewModel.currentUserId || isAdmin) {
                                IconButton(
                                    onClick = { viewModel.deleteLoreEntry(entry.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete lore entry", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(entry.story, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Recorded by ${entry.contributorName} • ${formatTimestamp(entry.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.textSecondary
                        )
                    }
                }
            }

            // Relationships Section
            item(key = "rel_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Relationships (${charRelationships.size})", style = MaterialTheme.typography.titleLarge)
                    Row {
                        TextButton(onClick = onOpenRelationshipMap) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("View Map")
                        }
                        if (allCharacters.size > 1) {
                            TextButton(onClick = { showAddRelDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }
                }
            }
            if (charRelationships.isEmpty()) {
                item {
                    LoreCard {
                        Text("No mapped relationships yet.", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                    }
                }
            } else {
                items(charRelationships, key = { it.id }) { rel ->
                    val isSource = rel.fromCharacterId == character.id
                    val otherId = if (isSource) rel.toCharacterId else rel.fromCharacterId
                    val otherName = if (isSource) rel.toCharacterName else rel.fromCharacterName
                    val cat = RelationshipCategory.fromRaw(rel.category)
                    LoreCard(onClick = { onOpenCharacter(otherId) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${rel.label} • $otherName",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    cat.label + (rel.notes?.let { " — $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.textSecondary
                                )
                            }
                            if (rel.createdByUid == viewModel.currentUserId || isAdmin) {
                                IconButton(onClick = { viewModel.deleteRelationship(rel.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete relationship", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Associated Quotes Section
            item(key = "quotes_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Associated Quotes (${associatedQuotes.size})", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { showAddQuoteDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Quote")
                    }
                }
            }
            if (associatedQuotes.isEmpty()) {
                item {
                    LoreCard {
                        Text("No quotes attributed to ${character.name} yet.", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                    }
                }
            } else {
                items(associatedQuotes, key = { it.id }) { q ->
                    LoreCard {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = theme.accent)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("“${q.text}”", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    listOfNotNull(q.context, q.dateLabel, q.category).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Tagged Photos
            if (taggedPhotos.isNotEmpty()) {
                item(key = "tagged_photos") {
                    LoreCard {
                        Text("Tagged Photographs (${taggedPhotos.size})", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(taggedPhotos, key = { it.id }) { photo ->
                                LoreImage(
                                    imageUrl = photo.imageUrl,
                                    contentDescription = photo.caption ?: "Tagged photo",
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { lightboxPhoto = photo },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        CharacterEditorDialog(
            existing = character,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false },
            onSaved = { showEditDialog = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${character.name}?") },
            text = { Text("This deletes the character profile from the Lore Book.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCharacter(character.id) {
                            showDeleteConfirm = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showAddLoreDialog) {
        AddLoreEntryDialog(
            character = character,
            viewModel = viewModel,
            onDismiss = { showAddLoreDialog = false }
        )
    }

    if (showAddRelDialog) {
        AddRelationshipDialog(
            initialFrom = character,
            allCharacters = allCharacters,
            viewModel = viewModel,
            onDismiss = { showAddRelDialog = false }
        )
    }

    if (showAddQuoteDialog) {
        AddQuickCharacterQuoteDialog(
            character = character,
            viewModel = viewModel,
            onDismiss = { showAddQuoteDialog = false }
        )
    }

    lightboxPhoto?.let { photo ->
        PhotoLightboxDialog(
            photo = photo,
            onOpenCharacter = { cid ->
                lightboxPhoto = null
                onOpenCharacter(cid)
            },
            onDismiss = { lightboxPhoto = null }
        )
    }
}

@Composable
private fun AddLoreEntryDialog(
    character: LoreCharacter,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalLoreTheme.current
    var title by remember { mutableStateOf("") }
    var story by remember { mutableStateOf("") }
    var precision by remember { mutableStateOf(DatePrecision.EXACT) }
    var dateLabel by remember { mutableStateOf("") }
    var approxYear by remember { mutableStateOf("") }

    fun computeSortEpoch(): Long {
        if (precision == DatePrecision.UNKNOWN) return Long.MAX_VALUE / 2
        val clean = dateLabel.trim()
        val exactFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        runCatching { exactFmt.parse(clean)?.time }
            .getOrNull()
            ?.let { return it }
        val yearMatch = Regex("""\b(19\d{2}|20\d{2})\b""").find(approxYear.ifBlank { clean })?.value?.toIntOrNull()
        if (yearMatch != null) {
            return (yearMatch - 1970).toLong() * 31_536_000_000L
        }
        return System.currentTimeMillis()
    }

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
                    Text("Update Lore of ${character.name}", style = MaterialTheme.typography.titleLarge)
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
                        label = { Text("Chapter / Event Title *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lore_title_input")
                    )

                    Text("Historical Date Precision", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DatePrecision.entries) { dp ->
                            FilterChip(
                                selected = precision == dp,
                                onClick = { precision = dp },
                                label = { Text(dp.label) }
                            )
                        }
                    }

                    if (precision != DatePrecision.UNKNOWN) {
                        OutlinedTextField(
                            value = dateLabel,
                            onValueChange = { dateLabel = it },
                            label = {
                                Text(if (precision == DatePrecision.EXACT) "Date (YYYY-MM-DD or readable)" else "Approximate Era / Label")
                            },
                            placeholder = {
                                Text(if (precision == DatePrecision.EXACT) "2018-07-14" else "Summer 2012")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = approxYear,
                            onValueChange = { approxYear = it.filter { ch -> ch.isDigit() }.take(4) },
                            label = { Text("Timeline Sort Year (optional, e.g. 2012)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = story,
                        onValueChange = { story = it },
                        label = { Text("Story / Memory / Historical Lore *") },
                        minLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lore_story_input")
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && story.isNotBlank()) {
                                val resolvedLabel = when (precision) {
                                    DatePrecision.UNKNOWN -> "Unknown Date"
                                    else -> dateLabel.ifBlank { approxYear.ifBlank { "Undated" } }
                                }
                                viewModel.addLoreEntry(
                                    character = character,
                                    title = title,
                                    story = story,
                                    datePrecision = precision,
                                    historicalDateLabel = resolvedLabel,
                                    sortOrderEpoch = computeSortEpoch()
                                ) {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = title.isNotBlank() && story.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        modifier = Modifier.testTag("submit_lore_button")
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Publish Lore")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddQuickCharacterQuoteDialog(
    character: LoreCharacter,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    var quoteText by remember { mutableStateOf("") }
    var contextText by remember { mutableStateOf("") }
    var dateLabel by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Funny") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quote by ${character.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quoteText,
                    onValueChange = { quoteText = it },
                    label = { Text("Quote *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contextText,
                    onValueChange = { contextText = it },
                    label = { Text("Context / Story") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { dateLabel = it },
                    label = { Text("When was it said?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (quoteText.isNotBlank()) {
                        viewModel.saveQuote(
                            existing = null,
                            text = quoteText,
                            attributedToName = character.name,
                            attributedType = AttributionType.CHARACTER,
                            attributedId = character.id,
                            dateLabel = dateLabel.takeIf { it.isNotBlank() },
                            context = contextText.takeIf { it.isNotBlank() },
                            category = category
                        ) { onDismiss() }
                    }
                },
                enabled = quoteText.isNotBlank()
            ) { Text("Save Quote") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
