package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttributionType
import com.example.data.model.LoreCharacter
import com.example.data.model.LyricEntry
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import com.example.data.model.QuoteEntry
import com.example.data.model.UiState
import com.example.data.model.UserRole
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.EmptyLoreState
import com.example.ui.components.ErrorLoreState
import com.example.ui.components.LoadingLoreState
import com.example.ui.components.LoreCard
import com.example.ui.components.LoreImage
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.LocalLoreTheme
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun QuoteBookScreen(
    viewModel: SocialLoreViewModel,
    onOpenCharacter: (String) -> Unit
) {
    val quotesState by viewModel.quotesState.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val approvedProfiles by viewModel.approvedProfiles.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current

    val isAdmin = (accountState as? UiState.Success)?.data?.role == UserRole.ADMIN.name
    val characters = (charactersState as? UiState.Success)?.data.orEmpty()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var editingQuote by remember { mutableStateOf<QuoteEntry?>(null) }
    var showQuoteDialog by remember { mutableStateOf(false) }
    var spotlightQuote by remember { mutableStateOf<QuoteEntry?>(null) }

    val categories = listOf("All", "Funny", "Heartfelt", "Wisdom", "Iconic", "Chaos", "Inside Story")

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "quote_header") {
                    LoreCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quote Book", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "Unforgettable lines attributed to characters and circle members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.textSecondary
                                )
                            }
                            val allQuotes = (quotesState as? UiState.Success)?.data.orEmpty()
                            if (allQuotes.isNotEmpty()) {
                                OutlinedButton(onClick = { spotlightQuote = allQuotes.random() }) {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Random")
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search quotes or speakers...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = favoritesOnly,
                                    onClick = { favoritesOnly = !favoritesOnly },
                                    label = { Text("Favorites") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                }

                when (val state = quotesState) {
                    is UiState.Loading -> item { LoadingLoreState("Opening Quote Book...") }
                    is UiState.Error -> item { ErrorLoreState(state.message) }
                    is UiState.Success -> {
                        val q = searchQuery.trim().lowercase()
                        val filtered = state.data.filter { entry ->
                            val matchQ = q.isBlank() ||
                                entry.text.lowercase().contains(q) ||
                                entry.attributedToName.lowercase().contains(q) ||
                                (entry.context?.lowercase()?.contains(q) == true)
                            val matchCat = selectedCategory == "All" || entry.category.equals(selectedCategory, ignoreCase = true)
                            val matchFav = !favoritesOnly || viewModel.currentUserId in entry.favoriteByUids
                            matchQ && matchCat && matchFav
                        }

                        if (filtered.isEmpty()) {
                            item {
                                EmptyLoreState(
                                    title = "No Quotes Yet",
                                    subtitle = "Capture hilarious one-liners, heartfelt toasts, and legendary family remarks.",
                                    actionLabel = "Add Quote",
                                    onAction = {
                                        editingQuote = null
                                        showQuoteDialog = true
                                    }
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { quote ->
                                val isFav = viewModel.currentUserId in quote.favoriteByUids
                                val canModify = quote.addedByUid == viewModel.currentUserId || isAdmin
                                LoreCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = theme.accent)
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    "“${quote.text}”",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontStyle = FontStyle.Italic
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                val speakerClick = quote.attributedType == AttributionType.CHARACTER.name &&
                                                    !quote.attributedId.isNullOrBlank()
                                                Text(
                                                    "— ${quote.attributedToName}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = theme.accent,
                                                    modifier = if (speakerClick) {
                                                        Modifier.clickable { onOpenCharacter(quote.attributedId!!) }
                                                    } else Modifier
                                                )
                                                val meta = listOfNotNull(
                                                    quote.context?.takeIf { it.isNotBlank() },
                                                    quote.dateLabel?.takeIf { it.isNotBlank() },
                                                    quote.category
                                                ).joinToString(" • ")
                                                if (meta.isNotBlank()) {
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(meta, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Added by ${quote.addedByName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = theme.textSecondary
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            IconButton(onClick = { viewModel.toggleQuoteFavorite(quote) }) {
                                                Icon(
                                                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "Favorite quote",
                                                    tint = if (isFav) theme.accent else theme.textSecondary
                                                )
                                            }
                                            if (canModify) {
                                                IconButton(
                                                    onClick = {
                                                        editingQuote = quote
                                                        showQuoteDialog = true
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit quote", modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(onClick = { viewModel.deleteQuote(quote.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete quote", modifier = Modifier.size(18.dp))
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
        }

        FloatingActionButton(
            onClick = {
                editingQuote = null
                showQuoteDialog = true
            },
            containerColor = theme.accent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_quote_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add quote")
        }
    }

    if (showQuoteDialog) {
        QuoteEditorDialog(
            existing = editingQuote,
            characters = characters,
            members = approvedProfiles,
            viewModel = viewModel,
            onDismiss = { showQuoteDialog = false }
        )
    }

    spotlightQuote?.let { q ->
        AlertDialog(
            onDismissRequest = { spotlightQuote = null },
            title = { Text("Lore Quote Spotlight") },
            text = {
                Column {
                    Text("“${q.text}”", style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
                    Spacer(Modifier.height(8.dp))
                    Text("— ${q.attributedToName}", color = theme.accent, fontWeight = FontWeight.Bold)
                    q.context?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { spotlightQuote = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun QuoteEditorDialog(
    existing: QuoteEntry?,
    characters: List<LoreCharacter>,
    members: List<com.example.data.model.MemberProfile>,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalLoreTheme.current
    var text by remember { mutableStateOf(existing?.text ?: "") }
    var attrType by remember {
        mutableStateOf(AttributionType.fromRaw(existing?.attributedType))
    }
    var attributedName by remember { mutableStateOf(existing?.attributedToName ?: "") }
    var attributedId by remember { mutableStateOf(existing?.attributedId) }
    var dateLabel by remember { mutableStateOf(existing?.dateLabel ?: "") }
    var context by remember { mutableStateOf(existing?.context ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Funny") }
    val categories = listOf("Funny", "Heartfelt", "Wisdom", "Iconic", "Chaos", "Inside Story")

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
                    Text(if (existing == null) "Add Quote" else "Edit Quote", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Quote Text *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Attributed To", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AttributionType.entries.forEach { t ->
                            FilterChip(
                                selected = attrType == t,
                                onClick = { attrType = t },
                                label = { Text(t.label) }
                            )
                        }
                    }

                    if (attrType == AttributionType.CHARACTER && characters.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(characters, key = { it.id }) { ch ->
                                FilterChip(
                                    selected = attributedId == ch.id,
                                    onClick = {
                                        attributedId = ch.id
                                        attributedName = ch.name
                                    },
                                    label = { Text(ch.name) }
                                )
                            }
                        }
                    } else if (attrType == AttributionType.MEMBER && members.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(members, key = { it.userId }) { m ->
                                FilterChip(
                                    selected = attributedId == m.userId,
                                    onClick = {
                                        attributedId = m.userId
                                        attributedName = m.displayName
                                    },
                                    label = { Text(m.displayName) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = attributedName,
                        onValueChange = {
                            attributedName = it
                            attributedId = null
                        },
                        label = { Text("Speaker Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Category", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = context,
                        onValueChange = { context = it },
                        label = { Text("Context / Backstory (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateLabel,
                        onValueChange = { dateLabel = it },
                        label = { Text("Date / Era (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank() && attributedName.isNotBlank()) {
                                viewModel.saveQuote(
                                    existing = existing,
                                    text = text,
                                    attributedToName = attributedName,
                                    attributedType = attrType,
                                    attributedId = attributedId,
                                    dateLabel = dateLabel.takeIf { it.isNotBlank() },
                                    context = context.takeIf { it.isNotBlank() },
                                    category = category
                                ) { onDismiss() }
                            }
                        },
                        enabled = text.isNotBlank() && attributedName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                    ) { Text("Save Quote") }
                }
            }
        }
    }
}

@Composable
fun LyricsLibraryScreen(
    viewModel: SocialLoreViewModel
) {
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current
    val context = LocalContext.current
    val isAdmin = (accountState as? UiState.Success)?.data?.role == UserRole.ADMIN.name

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var spotlightLyric by remember { mutableStateOf<LyricEntry?>(null) }

    val categories = listOf("All", "Nostalgic", "Road Trip", "Heartfelt", "Anthem", "Late Night", "Bittersweet")

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "lyrics_header") {
                    LoreCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Song Lyrics Library", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "Meaningful song lyric excerpts and community liner notes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.textSecondary
                                )
                            }
                            val all = (lyricsState as? UiState.Success)?.data.orEmpty()
                            if (all.isNotEmpty()) {
                                OutlinedButton(onClick = { spotlightLyric = all.random() }) {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Random")
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search lyrics, songs, or artists...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = favoritesOnly,
                                    onClick = { favoritesOnly = !favoritesOnly },
                                    label = { Text("Favorites") }
                                )
                            }
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                }

                when (val state = lyricsState) {
                    is UiState.Loading -> item { LoadingLoreState("Loading Song Lyrics...") }
                    is UiState.Error -> item { ErrorLoreState(state.message) }
                    is UiState.Success -> {
                        val q = searchQuery.trim().lowercase()
                        val filtered = state.data.filter { ly ->
                            val matchQ = q.isBlank() ||
                                ly.excerpt.lowercase().contains(q) ||
                                ly.songTitle.lowercase().contains(q) ||
                                ly.artist.lowercase().contains(q)
                            val matchCat = selectedCategory == "All" || ly.category.equals(selectedCategory, ignoreCase = true)
                            val matchFav = !favoritesOnly || viewModel.currentUserId in ly.favoriteByUids
                            matchQ && matchCat && matchFav
                        }

                        if (filtered.isEmpty()) {
                            item {
                                EmptyLoreState(
                                    title = "No Song Lyrics Saved Yet",
                                    subtitle = "Add lyric excerpts from songs that define your family road trips, playlists, or memories.",
                                    actionLabel = "Add Lyric Excerpt",
                                    onAction = { showAddDialog = true }
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { lyric ->
                                val isFav = viewModel.currentUserId in lyric.favoriteByUids
                                val canDelete = lyric.addedByUid == viewModel.currentUserId || isAdmin
                                LoreCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = theme.accent)
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    "“${lyric.excerpt}”",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontStyle = FontStyle.Italic
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    "${lyric.songTitle} — ${lyric.artist}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = theme.accent
                                                )
                                                if (!lyric.commentary.isNullOrBlank()) {
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(lyric.commentary, style = MaterialTheme.typography.bodySmall)
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "${lyric.category} • Added by ${lyric.addedByName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = theme.textSecondary
                                                )
                                                if (!lyric.songLink.isNullOrBlank()) {
                                                    Spacer(Modifier.height(6.dp))
                                                    OutlinedButton(
                                                        onClick = {
                                                            runCatching {
                                                                context.startActivity(
                                                                    Intent(Intent.ACTION_VIEW, Uri.parse(lyric.songLink))
                                                                )
                                                            }.onFailure { viewModel.showMessage("Could not open link") }
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Listen")
                                                    }
                                                }
                                            }
                                        }
                                        Column {
                                            IconButton(onClick = { viewModel.toggleLyricFavorite(lyric) }) {
                                                Icon(
                                                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "Favorite lyric",
                                                    tint = if (isFav) theme.accent else theme.textSecondary
                                                )
                                            }
                                            if (canDelete) {
                                                IconButton(onClick = { viewModel.deleteLyric(lyric.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete lyric", modifier = Modifier.size(18.dp))
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
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = theme.accent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_lyric_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add lyric")
        }
    }

    if (showAddDialog) {
        var excerpt by remember { mutableStateOf("") }
        var songTitle by remember { mutableStateOf("") }
        var artist by remember { mutableStateOf("") }
        var commentary by remember { mutableStateOf("") }
        var songLink by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Nostalgic") }

        Dialog(
            onDismissRequest = { showAddDialog = false },
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
                        Text("Add Song Lyric Excerpt", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { showAddDialog = false }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = excerpt,
                            onValueChange = { excerpt = it },
                            label = { Text("Lyric Excerpt *") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = songTitle,
                            onValueChange = { songTitle = it },
                            label = { Text("Song Title *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = artist,
                            onValueChange = { artist = it },
                            label = { Text("Artist *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories.drop(1)) { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = commentary,
                            onValueChange = { commentary = it },
                            label = { Text("Why this lyric matters (optional)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = songLink,
                            onValueChange = { songLink = it },
                            label = { Text("Song URL (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (excerpt.isNotBlank() && songTitle.isNotBlank() && artist.isNotBlank()) {
                                    viewModel.addLyric(
                                        excerpt = excerpt,
                                        songTitle = songTitle,
                                        artist = artist,
                                        commentary = commentary.takeIf { it.isNotBlank() },
                                        songLink = songLink.takeIf { it.isNotBlank() },
                                        category = category
                                    ) { showAddDialog = false }
                                }
                            },
                            enabled = excerpt.isNotBlank() && songTitle.isNotBlank() && artist.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                        ) { Text("Save Lyrics") }
                    }
                }
            }
        }
    }

    spotlightLyric?.let { ly ->
        AlertDialog(
            onDismissRequest = { spotlightLyric = null },
            title = { Text("Lyric Spotlight") },
            text = {
                Column {
                    Text("“${ly.excerpt}”", style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
                    Spacer(Modifier.height(8.dp))
                    Text("${ly.songTitle} — ${ly.artist}", color = theme.accent, fontWeight = FontWeight.Bold)
                    ly.commentary?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { spotlightLyric = null }) { Text("Close") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoAlbumsScreen(
    viewModel: SocialLoreViewModel,
    onOpenCharacter: (String) -> Unit
) {
    val albumsState by viewModel.albumsState.collectAsStateWithLifecycle()
    val photosState by viewModel.photosState.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current
    val isAdmin = (accountState as? UiState.Success)?.data?.role == UserRole.ADMIN.name

    val characters = (charactersState as? UiState.Success)?.data.orEmpty()
    val allPhotos = (photosState as? UiState.Success)?.data.orEmpty()

    var selectedAlbum by remember { mutableStateOf<PhotoAlbum?>(null) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var editingAlbum by remember { mutableStateOf<PhotoAlbum?>(null) }
    var showUploadPhotoDialog by remember { mutableStateOf(false) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItem?>(null) }

    if (selectedAlbum != null) {
        BackHandler { selectedAlbum = null }
    }

    val currentAlbum = selectedAlbum?.let { sel ->
        (albumsState as? UiState.Success)?.data?.firstOrNull { it.id == sel.id } ?: sel
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (currentAlbum == null) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "albums_header") {
                        LoreCard {
                            Text("Photo Albums", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Curated family & friends photo collections with tagged Lore Book characters",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.textSecondary
                            )
                        }
                    }

                    when (val state = albumsState) {
                        is UiState.Loading -> item { LoadingLoreState("Opening Photo Albums...") }
                        is UiState.Error -> item { ErrorLoreState(state.message) }
                        is UiState.Success -> {
                            if (state.data.isEmpty()) {
                                item {
                                    EmptyLoreState(
                                        title = "No Photo Albums Yet",
                                        subtitle = "Create a shared album for vacations, holidays, throwbacks, or everyday memories.",
                                        actionLabel = "Create Album",
                                        onAction = {
                                            editingAlbum = null
                                            showCreateAlbumDialog = true
                                        }
                                    )
                                }
                            } else {
                                items(state.data, key = { it.id }) { album ->
                                    LoreCard(onClick = { selectedAlbum = album }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!album.coverImageUrl.isNullOrBlank()) {
                                                LoreImage(
                                                    imageUrl = album.coverImageUrl,
                                                    contentDescription = album.title,
                                                    modifier = Modifier
                                                        .size(84.dp)
                                                        .clip(RoundedCornerShape(14.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(84.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(theme.cardVariantColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = theme.accent, modifier = Modifier.size(32.dp))
                                                }
                                            }
                                            Spacer(Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(album.title, style = MaterialTheme.typography.titleLarge)
                                                if (!album.description.isNullOrBlank()) {
                                                    Text(
                                                        album.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = theme.textSecondary,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "${album.photoCount} photos • Curated by ${album.ownerName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = theme.accent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        editingAlbum = null
                        showCreateAlbumDialog = true
                    },
                    containerColor = theme.accent,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .testTag("create_album_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create album")
                }
            } else {
                val albumPhotos = remember(allPhotos, currentAlbum.id) {
                    allPhotos.filter { it.albumId == currentAlbum.id }
                }
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "album_top") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { selectedAlbum = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("All Albums")
                            }
                            if (currentAlbum.ownerUid == viewModel.currentUserId || isAdmin) {
                                IconButton(
                                    onClick = {
                                        editingAlbum = currentAlbum
                                        showCreateAlbumDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit album")
                                }
                            }
                        }
                    }

                    item(key = "album_info") {
                        LoreCard {
                            Text(currentAlbum.title, style = MaterialTheme.typography.headlineSmall)
                            if (!currentAlbum.description.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(currentAlbum.description, style = MaterialTheme.typography.bodyMedium, color = theme.textSecondary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showUploadPhotoDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                                modifier = Modifier.testTag("upload_photo_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Upload Photo to Album")
                            }
                        }
                    }

                    if (albumPhotos.isEmpty()) {
                        item {
                            EmptyLoreState(
                                title = "Album Is Empty",
                                subtitle = "Upload the first photograph and tag characters from the Lore Book!",
                                actionLabel = "Upload Photo",
                                onAction = { showUploadPhotoDialog = true }
                            )
                        }
                    } else {
                        items(albumPhotos, key = { it.id }) { photo ->
                            LoreCard(onClick = { lightboxPhoto = photo }) {
                                LoreImage(
                                    imageUrl = photo.imageUrl,
                                    contentDescription = photo.caption ?: "Album photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                if (!photo.caption.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(photo.caption, style = MaterialTheme.typography.bodyLarge)
                                }
                                if (photo.taggedCharacterNames.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        photo.taggedCharacterIds.zip(photo.taggedCharacterNames).forEach { (cid, cname) ->
                                            AssistChip(
                                                onClick = { onOpenCharacter(cid) },
                                                label = { Text(cname) }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        listOfNotNull(
                                            photo.dateTakenLabel?.takeIf { it.isNotBlank() },
                                            "By ${photo.uploadedByName}"
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.textSecondary
                                    )
                                    if (photo.uploadedByUid == viewModel.currentUserId || isAdmin) {
                                        IconButton(onClick = { viewModel.deletePhoto(photo) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete photo", modifier = Modifier.size(18.dp))
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

    if (showCreateAlbumDialog) {
        AlbumEditorDialog(
            existing = editingAlbum,
            viewModel = viewModel,
            onDismiss = { showCreateAlbumDialog = false }
        )
    }

    if (showUploadPhotoDialog && currentAlbum != null) {
        UploadPhotoDialog(
            album = currentAlbum,
            characters = characters,
            viewModel = viewModel,
            onDismiss = { showUploadPhotoDialog = false }
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
private fun AlbumEditorDialog(
    existing: PhotoAlbum?,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme = LocalLoreTheme.current

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var coverUrl by remember { mutableStateOf(existing?.coverImageUrl) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                ImageUtils.encodeUriToDataUrl(context, uri)
                    .onSuccess { coverUrl = it }
                    .onFailure { viewModel.showMessage(it.localizedMessage ?: "Failed to load cover") }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create Photo Album" else "Edit Album") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Album Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!coverUrl.isNullOrBlank()) {
                    LoreImage(
                        imageUrl = coverUrl,
                        contentDescription = "Cover preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (coverUrl == null) "Select Cover Photo" else "Change Cover Photo")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.saveAlbum(
                            existing = existing,
                            title = title,
                            description = description.takeIf { it.isNotBlank() },
                            coverImageUrl = coverUrl
                        ) { onDismiss() }
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UploadPhotoDialog(
    album: PhotoAlbum,
    characters: List<LoreCharacter>,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme = LocalLoreTheme.current

    var imageUrl by remember { mutableStateOf<String?>(null) }
    var caption by remember { mutableStateOf("") }
    var dateTaken by remember { mutableStateOf("") }
    val selectedChars = remember { mutableStateListOf<LoreCharacter>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                ImageUtils.encodeUriToDataUrl(context, uri)
                    .onSuccess { imageUrl = it }
                    .onFailure { viewModel.showMessage(it.localizedMessage ?: "Failed to load image") }
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
                    Text("Upload to ${album.title}", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (imageUrl != null) {
                        LoreImage(
                            imageUrl = imageUrl,
                            contentDescription = "Selected photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (imageUrl == null) "Choose Photo *" else "Change Photo")
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Caption / Story") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateTaken,
                        onValueChange = { dateTaken = it },
                        label = { Text("Date Taken (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (characters.isNotEmpty()) {
                        Text("Tag Lore Book Characters", style = MaterialTheme.typography.titleSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(characters, key = { it.id }) { ch ->
                                val isTagged = selectedChars.any { it.id == ch.id }
                                FilterChip(
                                    selected = isTagged,
                                    onClick = {
                                        if (isTagged) selectedChars.removeAll { it.id == ch.id }
                                        else selectedChars.add(ch)
                                    },
                                    label = { Text(ch.name) }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            imageUrl?.let { url ->
                                viewModel.uploadPhoto(
                                    album = album,
                                    imageUrl = url,
                                    caption = caption.takeIf { it.isNotBlank() },
                                    dateTakenLabel = dateTaken.takeIf { it.isNotBlank() },
                                    taggedCharacters = selectedChars.toList()
                                ) { onDismiss() }
                            }
                        },
                        enabled = imageUrl != null,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                    ) { Text("Upload") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoLightboxDialog(
    photo: PhotoItem,
    onOpenCharacter: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalLoreTheme.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
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
                    Text(photo.albumTitle, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close lightbox") }
                }
                LoreImage(
                    imageUrl = photo.imageUrl,
                    contentDescription = photo.caption ?: "Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Fit
                )
                if (!photo.caption.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(photo.caption, style = MaterialTheme.typography.bodyLarge)
                }
                if (photo.taggedCharacterNames.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        photo.taggedCharacterIds.zip(photo.taggedCharacterNames).forEach { (cid, cname) ->
                            AssistChip(
                                onClick = { onOpenCharacter(cid) },
                                label = { Text(cname) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Uploaded by ${photo.uploadedByName} • ${photo.dateTakenLabel ?: formatTimestamp(photo.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.textSecondary
                )
            }
        }
    }
}
