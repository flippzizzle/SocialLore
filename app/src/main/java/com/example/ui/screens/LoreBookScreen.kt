package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LoreCharacter
import com.example.data.model.UiState
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.EmptyLoreState
import com.example.ui.components.ErrorLoreState
import com.example.ui.components.LoadingLoreState
import com.example.ui.components.LoreAvatar
import com.example.ui.components.LoreCard
import com.example.ui.components.LoreImage
import com.example.ui.theme.LocalLoreTheme
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoreBookScreen(
    viewModel: SocialLoreViewModel,
    onOpenCharacter: (String) -> Unit,
    onOpenRelationshipMap: () -> Unit
) {
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val loreEntriesState by viewModel.loreEntriesState.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val loreCountByChar = remember(loreEntriesState) {
        (loreEntriesState as? UiState.Success)?.data
            ?.groupingBy { it.characterId }
            ?.eachCount()
            ?: emptyMap()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "lore_book_header") {
                    LoreCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("The Lore Book", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "Collaborative character archive for real people in our stories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.textSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenRelationshipMap,
                                modifier = Modifier.testTag("open_relationship_map_button")
                            ) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Map")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search characters, nicknames, aliases...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lore_book_search_input")
                        )
                    }
                }

                when (val state = charactersState) {
                    is UiState.Loading -> {
                        item { LoadingLoreState("Opening the Lore Book...") }
                    }
                    is UiState.Error -> {
                        item { ErrorLoreState(state.message) }
                    }
                    is UiState.Success -> {
                        val q = searchQuery.trim().lowercase()
                        val filtered = if (q.isBlank()) {
                            state.data
                        } else {
                            state.data.filter { ch ->
                                ch.name.lowercase().contains(q) ||
                                    ch.nicknames.any { it.lowercase().contains(q) } ||
                                    ch.aliases.any { it.lowercase().contains(q) } ||
                                    ch.biography.lowercase().contains(q)
                            }
                        }

                        if (filtered.isEmpty()) {
                            item {
                                EmptyLoreState(
                                    title = if (q.isBlank()) "No Characters Archived Yet" else "No Matching Characters",
                                    subtitle = if (q.isBlank()) {
                                        "Create the first character page for a friend, family member, or memorable persona in your circle."
                                    } else {
                                        "Try another name, nickname, or alias."
                                    },
                                    actionLabel = if (q.isBlank()) "Create Character" else null,
                                    onAction = if (q.isBlank()) ({ showCreateDialog = true }) else null
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { character ->
                                val loreCount = loreCountByChar[character.id] ?: 0
                                LoreCard(
                                    onClick = { onOpenCharacter(character.id) },
                                    modifier = Modifier.testTag("character_card_${character.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (!character.portraitUrl.isNullOrBlank()) {
                                            LoreImage(
                                                imageUrl = character.portraitUrl,
                                                contentDescription = "${character.name} portrait",
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(RoundedCornerShape(14.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            LoreAvatar(null, character.name, size = 72.dp)
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    character.name,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = theme.accent.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        "$loreCount lore",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = theme.accent,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            val tags = character.nicknames + character.aliases
                                            if (tags.isNotEmpty()) {
                                                Spacer(Modifier.height(4.dp))
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    tags.take(4).forEach { alias ->
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = theme.cardVariantColor
                                                        ) {
                                                            Text(
                                                                alias,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = theme.textSecondary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (character.biography.isNotBlank()) {
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    character.biography,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = theme.textSecondary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = theme.accent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_character_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create character")
        }
    }

    if (showCreateDialog) {
        CharacterEditorDialog(
            existing = null,
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false },
            onSaved = { createdId ->
                showCreateDialog = false
                onOpenCharacter(createdId)
            }
        )
    }
}

@Composable
fun CharacterEditorDialog(
    existing: LoreCharacter?,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme = LocalLoreTheme.current

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var nicknamesRaw by remember { mutableStateOf(existing?.nicknames?.joinToString(", ") ?: "") }
    var aliasesRaw by remember { mutableStateOf(existing?.aliases?.joinToString(", ") ?: "") }
    var biography by remember { mutableStateOf(existing?.biography ?: "") }
    var portraitUrl by remember { mutableStateOf(existing?.portraitUrl) }

    val detailPairs = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            existing?.customDetails?.forEach { (k, v) -> add(k to v) }
        }
    }
    var newDetailKey by remember { mutableStateOf("") }
    var newDetailValue by remember { mutableStateOf("") }

    val portraitPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImageUtils.encodeUriToDataUrl(context, uri)
                    .onSuccess { portraitUrl = it }
                    .onFailure { viewModel.showMessage(it.localizedMessage ?: "Failed to load portrait") }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(620.dp),
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
                    Text(
                        if (existing == null) "New Lore Character" else "Edit ${existing.name}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!portraitUrl.isNullOrBlank()) {
                            LoreImage(
                                imageUrl = portraitUrl,
                                contentDescription = "Character portrait",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(theme.cardVariantColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = theme.accent)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                portraitPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        ) {
                            Text(if (portraitUrl == null) "Add Portrait" else "Change Portrait")
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Character Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("character_name_input")
                    )
                    OutlinedTextField(
                        value = nicknamesRaw,
                        onValueChange = { nicknamesRaw = it },
                        label = { Text("Nicknames (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = aliasesRaw,
                        onValueChange = { aliasesRaw = it },
                        label = { Text("Aliases / Titles (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = biography,
                        onValueChange = { biography = it },
                        label = { Text("Biography & Origin Story") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Custom Details (e.g., Signature Catchphrase, Hometown)", style = MaterialTheme.typography.titleSmall)
                    detailPairs.forEachIndexed { index, (k, v) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$k: $v", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { detailPairs.removeAt(index) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove detail", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newDetailKey,
                            onValueChange = { newDetailKey = it },
                            label = { Text("Label") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newDetailValue,
                            onValueChange = { newDetailValue = it },
                            label = { Text("Value") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (newDetailKey.isNotBlank() && newDetailValue.isNotBlank()) {
                                    detailPairs.add(newDetailKey.trim() to newDetailValue.trim())
                                    newDetailKey = ""
                                    newDetailValue = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add custom detail", tint = theme.accent)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val nicknames = nicknamesRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                val aliases = aliasesRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                val detailsMap = detailPairs.associate { it.first to it.second }
                                viewModel.saveCharacter(
                                    existing = existing,
                                    name = name,
                                    nicknames = nicknames,
                                    aliases = aliases,
                                    portraitUrl = portraitUrl,
                                    biography = biography,
                                    customDetails = detailsMap,
                                    onSuccess = onSaved
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        modifier = Modifier.testTag("save_character_button")
                    ) { Text("Save Character") }
                }
            }
        }
    }
}
