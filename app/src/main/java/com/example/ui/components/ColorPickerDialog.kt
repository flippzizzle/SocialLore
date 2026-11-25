package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalLoreTheme
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.toHexString

data class NamedPaletteColor(
    val name: String,
    val hex: String
)

object CuratedColorPalette {
    // 120 curated shades organized by hue & tone family (neutrals, metallics, pastels, jewel tones, neons, deep shades)
    val colors: List<NamedPaletteColor> = listOf(
        // Neutrals & Grayscale (12)
        NamedPaletteColor("Pure White", "#FFFFFF"),
        NamedPaletteColor("Snow Mist", "#F8FAFC"),
        NamedPaletteColor("Alabaster", "#F4F1EA"),
        NamedPaletteColor("Warm Ivory", "#FDF6E3"),
        NamedPaletteColor("Pearl Gray", "#E2E8F0"),
        NamedPaletteColor("Cool Silver", "#CBD5E1"),
        NamedPaletteColor("Pewter", "#94A3B8"),
        NamedPaletteColor("Slate Gray", "#64748B"),
        NamedPaletteColor("Graphite", "#334155"),
        NamedPaletteColor("Charcoal", "#1E293B"),
        NamedPaletteColor("Obsidian", "#0F172A"),
        NamedPaletteColor("Midnight Black", "#050508"),

        // Metallics, Champagne & Earthy Neutrals (12)
        NamedPaletteColor("Champagne", "#F7E7CE"),
        NamedPaletteColor("Warm Sand", "#E6CCB2"),
        NamedPaletteColor("Rose Gold", "#E0A9A9"),
        NamedPaletteColor("Blush Bronze", "#D49B94"),
        NamedPaletteColor("Soft Gold", "#F2D16B"),
        NamedPaletteColor("Metallic Gold", "#D4AF37"),
        NamedPaletteColor("Antique Brass", "#B8860B"),
        NamedPaletteColor("Copper Glow", "#B87333"),
        NamedPaletteColor("Terracotta", "#C86D51"),
        NamedPaletteColor("Cinnamon", "#9C523B"),
        NamedPaletteColor("Mocha", "#6F4E37"),
        NamedPaletteColor("Espresso", "#2C1D16"),

        // Pinks, Rose & Berry (12)
        NamedPaletteColor("Pastel Pink", "#FFD6E7"),
        NamedPaletteColor("Sakura", "#FBCFE8"),
        NamedPaletteColor("Soft Rose", "#F9A8D4"),
        NamedPaletteColor("Flamingo", "#F472B6"),
        NamedPaletteColor("Hot Pink", "#EC4899"),
        NamedPaletteColor("Neon Magenta", "#FF2A85"),
        NamedPaletteColor("Raspberry", "#E11D48"),
        NamedPaletteColor("Strawberry", "#FB7185"),
        NamedPaletteColor("Dusty Rose", "#B56576"),
        NamedPaletteColor("Mauve", "#9D5C78"),
        NamedPaletteColor("Cranberry", "#9F1239"),
        NamedPaletteColor("Wine Velvet", "#581C32"),

        // Reds & Coral (12)
        NamedPaletteColor("Peach Cream", "#FED7AA"),
        NamedPaletteColor("Soft Coral", "#FDA4AF"),
        NamedPaletteColor("Coral Pop", "#FF6B6B"),
        NamedPaletteColor("Electric Coral", "#FF4D6D"),
        NamedPaletteColor("Scarlet", "#EF4444"),
        NamedPaletteColor("Crimson", "#DC2626"),
        NamedPaletteColor("Ruby Red", "#B91C1C"),
        NamedPaletteColor("Garnet", "#881337"),
        NamedPaletteColor("Burgundy", "#6B0F1A"),
        NamedPaletteColor("Deep Maroon", "#4C0519"),
        NamedPaletteColor("Oxblood", "#3B0A14"),
        NamedPaletteColor("Velvet Noir", "#1F0910"),

        // Orange, Amber & Gold (12)
        NamedPaletteColor("Apricot", "#FFE4C4"),
        NamedPaletteColor("Buttercream", "#FEF3C7"),
        NamedPaletteColor("Honey Pastel", "#FDE68A"),
        NamedPaletteColor("Warm Amber", "#FBBF24"),
        NamedPaletteColor("Marigold", "#F59E0B"),
        NamedPaletteColor("Tangerine", "#FB923C"),
        NamedPaletteColor("Neon Orange", "#FF6B00"),
        NamedPaletteColor("Burnt Orange", "#EA580C"),
        NamedPaletteColor("Rust", "#C2410C"),
        NamedPaletteColor("Ochre", "#9A3412"),
        NamedPaletteColor("Bronze Dark", "#431407"),
        NamedPaletteColor("Umber Night", "#1E120A"),

        // Yellows & Chartreuse (12)
        NamedPaletteColor("Lemon Mist", "#FEF9C3"),
        NamedPaletteColor("Pastel Canary", "#FEF08A"),
        NamedPaletteColor("Sunbeam", "#FDE047"),
        NamedPaletteColor("Vivid Yellow", "#FACC15"),
        NamedPaletteColor("Electric Lime", "#D9F99D"),
        NamedPaletteColor("Neon Chartreuse", "#A3E635"),
        NamedPaletteColor("Acid Green", "#39FF14"),
        NamedPaletteColor("Olive Sage", "#84A98C"),
        NamedPaletteColor("Moss", "#65A30D"),
        NamedPaletteColor("Muted Olive", "#4D7C0F"),
        NamedPaletteColor("Deep Moss", "#365314"),
        NamedPaletteColor("Dark Pine", "#1A2E05"),

        // Greens & Emerald (12)
        NamedPaletteColor("Mint Whisper", "#D1FAE5"),
        NamedPaletteColor("Pastel Mint", "#A7F3D0"),
        NamedPaletteColor("Seafoam", "#6EE7B7"),
        NamedPaletteColor("Jade", "#34D399"),
        NamedPaletteColor("Emerald", "#10B981"),
        NamedPaletteColor("Kelly Green", "#22C55E"),
        NamedPaletteColor("Forest Sage", "#52796F"),
        NamedPaletteColor("Malachite", "#059669"),
        NamedPaletteColor("Hunter Green", "#047857"),
        NamedPaletteColor("Deep Emerald", "#065F46"),
        NamedPaletteColor("Evergreen", "#064E3B"),
        NamedPaletteColor("Night Canopy", "#04231A"),

        // Teals, Aqua & Cyan (12)
        NamedPaletteColor("Ice Aqua", "#CFFAFE"),
        NamedPaletteColor("Pastel Cyan", "#A5F3FC"),
        NamedPaletteColor("Turquoise", "#5EEAD4"),
        NamedPaletteColor("Bright Teal", "#2DD4BF"),
        NamedPaletteColor("Cyber Cyan", "#00F0FF"),
        NamedPaletteColor("Electric Aqua", "#06B6D4"),
        NamedPaletteColor("Peacock Teal", "#0D9488"),
        NamedPaletteColor("Muted Teal", "#2A6F7B"),
        NamedPaletteColor("Deep Lagoon", "#0E7490"),
        NamedPaletteColor("Ocean Depth", "#155E75"),
        NamedPaletteColor("Abyss Teal", "#0B2B33"),
        NamedPaletteColor("Trench Dark", "#04141D"),

        // Blues & Steel (12)
        NamedPaletteColor("Powder Blue", "#DBEAFE"),
        NamedPaletteColor("Periwinkle Sky", "#BAE6FD"),
        NamedPaletteColor("Sky Glow", "#7DD3FC"),
        NamedPaletteColor("Cerulean", "#38BDF8"),
        NamedPaletteColor("Electric Blue", "#00B4FF"),
        NamedPaletteColor("Steel Blue", "#4682B4"),
        NamedPaletteColor("Cadet Blue", "#5B84B1"),
        NamedPaletteColor("Azure", "#3B82F6"),
        NamedPaletteColor("Cobalt", "#2563EB"),
        NamedPaletteColor("Sapphire", "#1D4ED8"),
        NamedPaletteColor("Navy Ink", "#1E3A8A"),
        NamedPaletteColor("Midnight Navy", "#0A1320"),

        // Purples, Lavender & Plum (12)
        NamedPaletteColor("Soft Lavender", "#EDE9FE"),
        NamedPaletteColor("Lilac", "#DDD6FE"),
        NamedPaletteColor("Wisteria", "#C4B5FD"),
        NamedPaletteColor("Orchid Mist", "#E9D5FF"),
        NamedPaletteColor("Amethyst", "#C084FC"),
        NamedPaletteColor("Electric Violet", "#A855F7"),
        NamedPaletteColor("Royal Purple", "#7C3AED"),
        NamedPaletteColor("Neon Orchid", "#E879F9"),
        NamedPaletteColor("Plum", "#7E22CE"),
        NamedPaletteColor("Deep Plum", "#581C87"),
        NamedPaletteColor("Twilight Indigo", "#1B1633"),
        NamedPaletteColor("Cosmic Violet", "#0E0C1A")
    )
}

fun normalizeHexOrNull(raw: String): String? {
    val clean = raw.trim().removePrefix("#").uppercase()
    if (clean.length != 6 && clean.length != 8) return null
    if (!clean.all { it in '0'..'9' || it in 'A'..'F' }) return null
    return "#$clean"
}

private fun colorToHsvArray(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv
}

@Composable
fun ColorSwatchField(
    label: String,
    colorHex: String,
    fallbackColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val theme = LocalLoreTheme.current
    val resolvedColor = parseHexColor(colorHex, fallbackColor)
    val displayHex = normalizeHexOrNull(colorHex) ?: resolvedColor.toHexString()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = theme.cardVariantColor,
        border = BorderStroke(1.dp, theme.borderColor.copy(alpha = 0.65f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(resolvedColor)
                    .border(
                        width = 1.5.dp,
                        color = if (resolvedColor.luminance() > 0.7f) Color(0xFF475569) else Color.White.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayHex,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = theme.textSecondary,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.Colorize,
                contentDescription = "Pick $label color",
                tint = theme.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoreColorPickerDialog(
    title: String,
    initialColorHex: String,
    fallbackColor: Color,
    onLivePreview: ((String) -> Unit)? = null,
    onApply: (String) -> Unit,
    onCancel: () -> Unit
) {
    val theme = LocalLoreTheme.current
    val initialColor = remember(initialColorHex, fallbackColor) {
        parseHexColor(initialColorHex, fallbackColor)
    }
    val normalizedInitialHex = remember(initialColor) {
        normalizeHexOrNull(initialColorHex) ?: initialColor.toHexString()
    }
    val initialHsv = remember(initialColor) { colorToHsvArray(initialColor) }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember { mutableStateOf(normalizedInitialHex) }
    var hexError by remember { mutableStateOf<String?>(null) }

    val selectedColor = remember(hue, saturation, brightness) {
        Color.hsv(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), brightness.coerceIn(0f, 1f))
    }
    val selectedHex = remember(selectedColor) { selectedColor.toHexString() }

    fun syncFromVisual(newH: Float, newS: Float, newV: Float) {
        hue = newH.coerceIn(0f, 360f)
        saturation = newS.coerceIn(0f, 1f)
        brightness = newV.coerceIn(0f, 1f)
        val updatedHex = Color.hsv(hue, saturation, brightness).toHexString()
        hexInput = updatedHex
        hexError = null
        onLivePreview?.invoke(updatedHex)
    }

    fun syncFromHex(raw: String) {
        hexInput = raw
        val normalized = normalizeHexOrNull(raw)
        if (normalized == null) {
            hexError = "Enter a valid hex code like #A855F7"
            return
        }
        val parsed = parseHexColor(normalized, selectedColor)
        val hsv = colorToHsvArray(parsed)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        hexError = null
        onLivePreview?.invoke(normalized)
    }

    val latestHue by rememberUpdatedState(hue)
    val latestSaturation by rememberUpdatedState(saturation)
    val latestBrightness by rememberUpdatedState(brightness)

    fun cancelAndRestore() {
        onLivePreview?.invoke(normalizedInitialHex)
        onCancel()
    }

    Dialog(
        onDismissRequest = { cancelAndRestore() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .heightIn(max = 670.dp),
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
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { cancelAndRestore() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel color selection")
                    }
                }

                // Live Before vs Selected Color Preview Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(initialColor)
                                .border(1.dp, theme.borderColor, RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Previous", style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(selectedColor)
                            .border(1.5.dp, theme.borderColor, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewTextColor = if (selectedColor.luminance() > 0.55f) Color(0xFF0F172A) else Color.White
                        Text(
                            text = "Selected • $selectedHex",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                            color = previewTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Visual Color Picker: Saturation / Brightness Square + Rainbow Hue Slider
                    Text("Visual Spectrum Picker", style = MaterialTheme.typography.titleSmall)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, theme.borderColor, RoundedCornerShape(14.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("color_sv_square")
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val s = (offset.x / size.width).coerceIn(0f, 1f)
                                        val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                                        syncFromVisual(latestHue, s, v)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val s = (change.position.x / size.width).coerceIn(0f, 1f)
                                        val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                        syncFromVisual(latestHue, s, v)
                                    }
                                }
                        ) {
                            val pureHueColor = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)
                            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, pureHueColor)))
                            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

                            val cx = saturation.coerceIn(0f, 1f) * size.width
                            val cy = (1f - brightness.coerceIn(0f, 1f)) * size.height
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.65f),
                                radius = 12.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 10.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                    }

                    // Rainbow Hue Slider
                    val rainbowColors = remember {
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    }
                    Column {
                        Text("Hue (${hue.toInt()}°)", style = MaterialTheme.typography.labelMedium, color = theme.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, theme.borderColor, RoundedCornerShape(18.dp))
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("color_hue_slider")
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val newHue = ((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                                            syncFromVisual(newHue, latestSaturation, latestBrightness)
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, _ ->
                                            change.consume()
                                            val newHue = ((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                                            syncFromVisual(newHue, latestSaturation, latestBrightness)
                                        }
                                    }
                            ) {
                                drawRect(brush = Brush.horizontalGradient(rainbowColors))
                                val hx = (hue.coerceIn(0f, 360f) / 360f) * size.width
                                drawCircle(
                                    color = Color.White,
                                    radius = 13.dp.toPx(),
                                    center = Offset(hx, size.height / 2f)
                                )
                                drawCircle(
                                    color = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f),
                                    radius = 9.dp.toPx(),
                                    center = Offset(hx, size.height / 2f)
                                )
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    radius = 13.dp.toPx(),
                                    center = Offset(hx, size.height / 2f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = theme.borderColor.copy(alpha = 0.35f))

                    // 2. Manual Hex Code Entry
                    Text("Manual Hex Code", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { syncFromHex(it) },
                        label = { Text("Hex Color (#RRGGBB)") },
                        placeholder = { Text("#A855F7") },
                        singleLine = true,
                        isError = hexError != null,
                        supportingText = hexError?.let { msg -> { Text(msg) } },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(selectedColor)
                                    .border(1.dp, theme.borderColor, CircleShape)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("color_hex_input")
                    )

                    HorizontalDivider(color = theme.borderColor.copy(alpha = 0.35f))

                    // 3. Premade Color Palette (120 curated shades)
                    Text(
                        "Premade Color Palette (${CuratedColorPalette.colors.size} Shades)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CuratedColorPalette.colors.forEach { preset ->
                            val swatchColor = parseHexColor(preset.hex, Color.Gray)
                            val isSelected = selectedHex.equals(preset.hex, ignoreCase = true)
                            val checkTint = if (swatchColor.luminance() > 0.55f) Color(0xFF0F172A) else Color.White
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) theme.accent else theme.borderColor.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { syncFromHex(preset.hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "${preset.name} selected",
                                        tint = checkTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { cancelAndRestore() },
                        modifier = Modifier.testTag("color_picker_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalHex = normalizeHexOrNull(hexInput) ?: selectedHex
                            onApply(finalHex)
                        },
                        enabled = hexError == null,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        modifier = Modifier.testTag("color_picker_apply_button")
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
