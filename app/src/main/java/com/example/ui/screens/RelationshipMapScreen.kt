package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CharacterRelationship
import com.example.data.model.LoreCharacter
import com.example.data.model.RelationshipCategory
import com.example.data.model.UiState
import com.example.data.model.UserRole
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.EmptyLoreState
import com.example.ui.components.LoadingLoreState
import com.example.ui.components.LoreCard
import com.example.ui.theme.LocalLoreTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private fun categoryColor(cat: RelationshipCategory): Color = when (cat) {
    RelationshipCategory.FAMILY -> Color(0xFFF59E0B)
    RelationshipCategory.FRIEND -> Color(0xFF38BDF8)
    RelationshipCategory.COWORKER -> Color(0xFF34D399)
    RelationshipCategory.NEIGHBOR -> Color(0xFFA78BFA)
    RelationshipCategory.ACQUAINTANCE -> Color(0xFF94A3B8)
    RelationshipCategory.CUSTOM -> Color(0xFFF472B6)
}

@Composable
fun RelationshipMapScreen(
    viewModel: SocialLoreViewModel,
    onOpenCharacter: (String) -> Unit
) {
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val relationshipsState by viewModel.relationshipsState.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val theme = LocalLoreTheme.current
    val isAdmin = (accountState as? UiState.Success)?.data?.role == UserRole.ADMIN.name

    val characters = (charactersState as? UiState.Success)?.data.orEmpty()
    val relationships = (relationshipsState as? UiState.Success)?.data.orEmpty()

    var selectedCategory by remember { mutableStateOf<RelationshipCategory?>(null) }
    var selectedCharacterId by remember { mutableStateOf<String?>(null) }
    var showAddRelDialog by remember { mutableStateOf(false) }

    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val filteredRelationships = remember(relationships, selectedCategory) {
        if (selectedCategory == null) relationships
        else relationships.filter { RelationshipCategory.fromRaw(it.category) == selectedCategory }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "map_header") {
                LoreCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Relationship Map", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Pinch or drag to explore connections. Multiple relationships between characters appear as curved parallel links.",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.textSecondary
                            )
                        }
                        if (characters.size >= 2) {
                            Button(
                                onClick = { showAddRelDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                                modifier = Modifier.testTag("add_relationship_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Connect")
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("All (${relationships.size})") }
                            )
                        }
                        items(RelationshipCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
            }

            if (charactersState is UiState.Loading) {
                item { LoadingLoreState("Mapping Lore Book connections...") }
            } else if (characters.isEmpty()) {
                item {
                    EmptyLoreState(
                        title = "No Characters to Map Yet",
                        subtitle = "Add at least two characters in the Lore Book to map relationships between them."
                    )
                }
            } else {
                // Interactive Relationship Graph Canvas
                item(key = "graph_canvas") {
                    LoreCard(contentPadding = PaddingValues(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Interactive Graph • Tap a node",
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.textSecondary
                            )
                            Row {
                                IconButton(onClick = { zoom = (zoom - 0.25f).coerceIn(0.5f, 3f) }) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                                }
                                IconButton(onClick = { zoom = 1f; panOffset = Offset.Zero }) {
                                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset view")
                                }
                                IconButton(onClick = { zoom = (zoom + 0.25f).coerceIn(0.5f, 3f) }) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                                }
                            }
                        }

                        var canvasSize by remember { mutableStateOf(Pair(1f, 1f)) }
                        val nodePositionsNormalized = remember(characters) {
                            val n = characters.size
                            characters.mapIndexed { idx, ch ->
                                val angle = (2.0 * PI * idx / n) - (PI / 2.0)
                                val radiusRatio = if (n <= 4) 0.28f else if (n <= 10) 0.34f else 0.38f
                                ch.id to Offset(
                                    x = 0.5f + radiusRatio * cos(angle).toFloat(),
                                    y = 0.5f + radiusRatio * sin(angle).toFloat()
                                )
                            }.toMap()
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(theme.background.copy(alpha = 0.85f))
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, gestureZoom, _ ->
                                            zoom = (zoom * gestureZoom).coerceIn(0.5f, 3f)
                                            panOffset += pan
                                        }
                                    }
                                    .pointerInput(characters, zoom, panOffset) {
                                        detectTapGestures { tap ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val center = Offset(w / 2f, h / 2f)
                                            val hit = characters.firstOrNull { ch ->
                                                val norm = nodePositionsNormalized[ch.id] ?: return@firstOrNull false
                                                val base = Offset(norm.x * w, norm.y * h)
                                                val screenPos = center + (base - center) * zoom + panOffset
                                                hypot(tap.x - screenPos.x, tap.y - screenPos.y) <= 36.dp.toPx() * zoom
                                            }
                                            selectedCharacterId = hit?.id
                                        }
                                    }
                            ) {
                                canvasSize = size.width to size.height
                                val center = Offset(size.width / 2f, size.height / 2f)
                                fun toScreen(id: String): Offset? {
                                    val norm = nodePositionsNormalized[id] ?: return null
                                    val base = Offset(norm.x * size.width, norm.y * size.height)
                                    return center + (base - center) * zoom + panOffset
                                }

                                // Group edges by unordered pair so multiple relationships curve cleanly
                                val groupedPairs = filteredRelationships.groupBy { rel ->
                                    if (rel.fromCharacterId <= rel.toCharacterId) {
                                        rel.fromCharacterId to rel.toCharacterId
                                    } else {
                                        rel.toCharacterId to rel.fromCharacterId
                                    }
                                }

                                val textPaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    textSize = 26f
                                    color = theme.textPrimary.toArgb()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }

                                groupedPairs.values.forEach { pairEdges ->
                                    val count = pairEdges.size
                                    pairEdges.forEachIndexed { idx, rel ->
                                        val start = toScreen(rel.fromCharacterId) ?: return@forEachIndexed
                                        val end = toScreen(rel.toCharacterId) ?: return@forEachIndexed
                                        val dx = end.x - start.x
                                        val dy = end.y - start.y
                                        val len = hypot(dx, dy).coerceAtLeast(1f)
                                        val nx = -dy / len
                                        val ny = dx / len
                                        val offsetStep = (idx - (count - 1) / 2f) * 54f * zoom
                                        val ctrl = Offset(
                                            (start.x + end.x) / 2f + nx * offsetStep,
                                            (start.y + end.y) / 2f + ny * offsetStep
                                        )
                                        val catColor = categoryColor(RelationshipCategory.fromRaw(rel.category))
                                        val highlighted = selectedCharacterId == null ||
                                            selectedCharacterId == rel.fromCharacterId ||
                                            selectedCharacterId == rel.toCharacterId

                                        val path = Path().apply {
                                            moveTo(start.x, start.y)
                                            quadraticTo(ctrl.x, ctrl.y, end.x, end.y)
                                        }
                                        drawPath(
                                            path = path,
                                            color = catColor.copy(alpha = if (highlighted) 0.9f else 0.2f),
                                            style = Stroke(width = (3f * zoom).coerceIn(2f, 8f), cap = StrokeCap.Round)
                                        )

                                        val labelPos = Offset(
                                            0.25f * start.x + 0.5f * ctrl.x + 0.25f * end.x,
                                            0.25f * start.y + 0.5f * ctrl.y + 0.25f * end.y
                                        )
                                        if (highlighted) {
                                            drawContext.canvas.nativeCanvas.drawText(
                                                rel.label.take(18),
                                                labelPos.x,
                                                labelPos.y - 6f,
                                                textPaint
                                            )
                                        }
                                    }
                                }

                                val nodePaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    textSize = 28f
                                    color = theme.textPrimary.toArgb()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                }

                                characters.forEach { ch ->
                                    val pos = toScreen(ch.id) ?: return@forEach
                                    val isSel = ch.id == selectedCharacterId
                                    val r = (22.dp.toPx() * zoom).coerceIn(18f, 64f)
                                    drawCircle(
                                        color = if (isSel) theme.accent else theme.cardColor,
                                        radius = r,
                                        center = pos
                                    )
                                    drawCircle(
                                        color = if (isSel) theme.textPrimary else theme.accent,
                                        radius = r,
                                        center = pos,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                    val initial = ch.name.firstOrNull()?.uppercase() ?: "?"
                                    drawContext.canvas.nativeCanvas.drawText(
                                        initial,
                                        pos.x,
                                        pos.y + 10f,
                                        nodePaint
                                    )
                                    drawContext.canvas.nativeCanvas.drawText(
                                        ch.name.take(14),
                                        pos.x,
                                        pos.y + r + 28f,
                                        nodePaint
                                    )
                                }
                            }
                        }

                        // Category Legend
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            items(RelationshipCategory.entries) { cat ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor(cat))
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(cat.label, style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                                }
                            }
                        }
                    }
                }

                // Selected character banner
                selectedCharacterId?.let { selId ->
                    characters.firstOrNull { it.id == selId }?.let { ch ->
                        item(key = "selected_char_$selId") {
                            LoreCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Selected: ${ch.name}", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            ch.biography.ifBlank { "Tap Open to view timeline & lore." },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = theme.textSecondary,
                                            maxLines = 2
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = { onOpenCharacter(ch.id) }) {
                                        Text("Open Page")
                                    }
                                }
                            }
                        }
                    }
                }

                // Relationships Directory List
                item(key = "rel_list_header") {
                    Text(
                        "Mapped Connections (${filteredRelationships.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (filteredRelationships.isEmpty()) {
                    item {
                        LoreCard {
                            Text(
                                "No relationships in this category yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.textSecondary
                            )
                        }
                    }
                } else {
                    items(filteredRelationships, key = { it.id }) { rel ->
                        val cat = RelationshipCategory.fromRaw(rel.category)
                        LoreCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor(cat))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "${rel.fromCharacterName} — ${rel.label} — ${rel.toCharacterName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            cat.label + (rel.notes?.let { " • $it" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = theme.textSecondary
                                        )
                                    }
                                }
                                if (rel.createdByUid == viewModel.currentUserId || isAdmin) {
                                    IconButton(onClick = { viewModel.deleteRelationship(rel.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete relationship")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRelDialog && characters.size >= 2) {
        AddRelationshipDialog(
            initialFrom = characters.first(),
            allCharacters = characters,
            viewModel = viewModel,
            onDismiss = { showAddRelDialog = false }
        )
    }
}

@Composable
fun AddRelationshipDialog(
    initialFrom: LoreCharacter,
    allCharacters: List<LoreCharacter>,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalLoreTheme.current
    var fromChar by remember { mutableStateOf(initialFrom) }
    var toChar by remember {
        mutableStateOf(allCharacters.firstOrNull { it.id != initialFrom.id } ?: initialFrom)
    }
    var category by remember { mutableStateOf(RelationshipCategory.FRIEND) }
    var label by remember { mutableStateOf("Best Friend") }
    var notes by remember { mutableStateOf("") }

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
                    Text("Connect Characters", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("From Character", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allCharacters, key = { "from_${it.id}" }) { ch ->
                            FilterChip(
                                selected = fromChar.id == ch.id,
                                onClick = { fromChar = ch },
                                label = { Text(ch.name) }
                            )
                        }
                    }

                    Text("To Character", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allCharacters.filter { it.id != fromChar.id }, key = { "to_${it.id}" }) { ch ->
                            FilterChip(
                                selected = toChar.id == ch.id,
                                onClick = { toChar = ch },
                                label = { Text(ch.name) }
                            )
                        }
                    }

                    Text("Category", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RelationshipCategory.entries) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Custom Relationship Label *") },
                        placeholder = { Text("Sibling, Road-Trip Partner, Former Boss...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Story / Notes (optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (label.isNotBlank() && fromChar.id != toChar.id) {
                                viewModel.addRelationship(
                                    fromChar = fromChar,
                                    toChar = toChar,
                                    label = label,
                                    category = category,
                                    notes = notes.takeIf { it.isNotBlank() }
                                ) { onDismiss() }
                            }
                        },
                        enabled = label.isNotBlank() && fromChar.id != toChar.id,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                    ) { Text("Save Link") }
                }
            }
        }
    }
}
