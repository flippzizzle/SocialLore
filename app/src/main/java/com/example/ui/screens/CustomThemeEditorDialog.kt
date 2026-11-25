package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomThemeEntity
import com.example.ui.SocialLoreViewModel
import com.example.ui.components.FontPickerDialog
import com.example.ui.components.LoreCard
import com.example.ui.theme.LocalLoreTheme
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.toLoreThemeSpec
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun CustomThemeEditorDialog(
    existing: CustomThemeEntity?,
    viewModel: SocialLoreViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalLoreTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(existing?.name ?: "My Lore Palette") }
    var darkTheme by remember { mutableStateOf(existing?.darkTheme ?: true) }
    var bgHex by remember { mutableStateOf(existing?.bgColorHex ?: "#0E0C1A") }
    var gradStartHex by remember { mutableStateOf(existing?.gradientStartHex ?: "#1D1338") }
    var gradEndHex by remember { mutableStateOf(existing?.gradientEndHex ?: "#090812") }
    var cardHex by remember { mutableStateOf(existing?.cardColorHex ?: "#1B1633") }
    var textHex by remember { mutableStateOf(existing?.textColorHex ?: "#F4F1FF") }
    var accentHex by remember { mutableStateOf(existing?.accentColorHex ?: "#A855F7") }
    var borderHex by remember { mutableStateOf(existing?.borderColorHex ?: "#5B3FA8") }
    var borderWidth by remember { mutableFloatStateOf((existing?.borderWidthDp ?: 1.5).toFloat()) }
    var cardOpacity by remember { mutableFloatStateOf((existing?.cardOpacity ?: 0.92).toFloat()) }
    var bgImageUrl by remember { mutableStateOf(existing?.bgImageUrl) }
    var headingFont by remember { mutableStateOf(existing?.headingFont ?: "Playfair Display") }
    var bodyFont by remember { mutableStateOf(existing?.bodyFont ?: "Plus Jakarta Sans") }
    var applyImmediately by remember { mutableStateOf(true) }
    var fontPickerTarget by remember { mutableStateOf<String?>(null) }

    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                ImageUtils.encodeUriToDataUrl(context, uri)
                    .onSuccess { bgImageUrl = it }
                    .onFailure { viewModel.showMessage(it.localizedMessage ?: "Failed to load background") }
            }
        }
    }

    val previewEntity = CustomThemeEntity(
        id = existing?.id ?: "preview",
        name = name,
        darkTheme = darkTheme,
        bgColorHex = bgHex,
        gradientStartHex = gradStartHex,
        gradientEndHex = gradEndHex,
        cardColorHex = cardHex,
        textColorHex = textHex,
        accentColorHex = accentHex,
        borderColorHex = borderHex,
        borderWidthDp = borderWidth.toDouble(),
        cardOpacity = cardOpacity.toDouble(),
        bgImageUrl = bgImageUrl,
        headingFont = headingFont,
        bodyFont = bodyFont
    )
    val previewSpec = previewEntity.toLoreThemeSpec()

    val quickAccents = listOf("#A855F7", "#EC4899", "#38BDF8", "#34D399", "#F59E0B", "#EF4444", "#E2E8F0")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(640.dp),
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
                        if (existing == null) "Custom Theme Studio" else "Edit Custom Theme",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                // Live Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(previewSpec.gradientStart, previewSpec.gradientEnd)))
                        .padding(10.dp)
                ) {
                    LoreCard(theme = previewSpec, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                        Text(
                            previewSpec.name.ifBlank { "Preview" },
                            style = MaterialTheme.typography.titleMedium,
                            color = previewSpec.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Live theme preview • Card opacity ${(cardOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = previewSpec.textPrimary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Theme Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode Base", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = {
                                darkTheme = it
                                if (!it && textHex == "#F4F1FF") {
                                    bgHex = "#F8FAFC"
                                    gradStartHex = "#FFFFFF"
                                    gradEndHex = "#E2E8F0"
                                    cardHex = "#FFFFFF"
                                    textHex = "#0F172A"
                                } else if (it && bgHex == "#F8FAFC") {
                                    bgHex = "#0E0C1A"
                                    gradStartHex = "#1D1338"
                                    gradEndHex = "#090812"
                                    cardHex = "#1B1633"
                                    textHex = "#F4F1FF"
                                }
                            }
                        )
                    }

                    Text("Quick Accent Swatches", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickAccents) { hex ->
                            val color = parseHexColor(hex, Color.Magenta)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        accentHex = hex
                                        borderHex = hex
                                    }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = bgHex, onValueChange = { bgHex = it }, label = { Text("Background") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = cardHex, onValueChange = { cardHex = it }, label = { Text("Card") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = gradStartHex, onValueChange = { gradStartHex = it }, label = { Text("Gradient Start") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = gradEndHex, onValueChange = { gradEndHex = it }, label = { Text("Gradient End") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = accentHex, onValueChange = { accentHex = it }, label = { Text("Accent") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = textHex, onValueChange = { textHex = it }, label = { Text("Text") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = borderHex, onValueChange = { borderHex = it }, label = { Text("Border") }, singleLine = true, modifier = Modifier.weight(1f))
                    }

                    Text("Border Width: ${String.format("%.1f", borderWidth)}dp", style = MaterialTheme.typography.labelMedium)
                    Slider(value = borderWidth, onValueChange = { borderWidth = it }, valueRange = 0f..6f)

                    Text("Card Opacity: ${(cardOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(value = cardOpacity, onValueChange = { cardOpacity = it }, valueRange = 0.35f..1f)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { fontPickerTarget = "heading" }, modifier = Modifier.weight(1f)) {
                            Text("Heading: $headingFont", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(onClick = { fontPickerTarget = "body" }, modifier = Modifier.weight(1f)) {
                            Text("Body: $bodyFont", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (bgImageUrl == null) "Background Image" else "Change Background")
                        }
                        if (bgImageUrl != null) {
                            TextButton(onClick = { bgImageUrl = null }) { Text("Clear Image") }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Apply theme immediately", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = applyImmediately, onCheckedChange = { applyImmediately = it })
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveCustomTheme(previewEntity, applyImmediately) { onDismiss() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        modifier = Modifier.testTag("save_custom_theme_button")
                    ) { Text("Save Theme") }
                }
            }
        }
    }

    fontPickerTarget?.let { target ->
        FontPickerDialog(
            title = if (target == "heading") "Choose Heading Font" else "Choose Body Font",
            selectedFont = if (target == "heading") headingFont else bodyFont,
            onSelect = { chosen ->
                if (target == "heading") headingFont = chosen else bodyFont = chosen
                fontPickerTarget = null
            },
            onDismiss = { fontPickerTarget = null }
        )
    }
}
