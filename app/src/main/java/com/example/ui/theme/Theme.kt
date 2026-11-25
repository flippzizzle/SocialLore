package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalLoreTheme = staticCompositionLocalOf { ThemeLibrary.premadeThemes.first() }

val LoreShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

fun fontWeightFromInt(weight: Int): FontWeight = when {
    weight <= 300 -> FontWeight.Light
    weight <= 400 -> FontWeight.Normal
    weight <= 500 -> FontWeight.Medium
    weight <= 600 -> FontWeight.SemiBold
    weight <= 700 -> FontWeight.Bold
    else -> FontWeight.ExtraBold
}

fun buildLoreTypography(
    headingFamily: FontFamily,
    bodyFamily: FontFamily,
    scale: Float = 1f,
    headingWeight: FontWeight = FontWeight.Bold,
    bodyWeight: FontWeight = FontWeight.Normal
): Typography {
    val s = scale.coerceIn(0.75f, 1.6f)
    return Typography(
        displaySmall = TextStyle(
            fontFamily = headingFamily,
            fontWeight = headingWeight,
            fontSize = (32 * s).sp,
            lineHeight = (38 * s).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = headingFamily,
            fontWeight = headingWeight,
            fontSize = (26 * s).sp,
            lineHeight = (32 * s).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = headingFamily,
            fontWeight = headingWeight,
            fontSize = (22 * s).sp,
            lineHeight = (28 * s).sp
        ),
        titleLarge = TextStyle(
            fontFamily = headingFamily,
            fontWeight = headingWeight,
            fontSize = (20 * s).sp,
            lineHeight = (26 * s).sp
        ),
        titleMedium = TextStyle(
            fontFamily = headingFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * s).sp,
            lineHeight = (22 * s).sp
        ),
        titleSmall = TextStyle(
            fontFamily = headingFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = bodyWeight,
            fontSize = (15 * s).sp,
            lineHeight = (22 * s).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = bodyWeight,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = bodyWeight,
            fontSize = (12 * s).sp,
            lineHeight = (16 * s).sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * s).sp,
            lineHeight = (16 * s).sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * s).sp,
            lineHeight = (14 * s).sp
        )
    )
}

@Composable
fun SocialLoreTheme(
    themeSpec: LoreThemeSpec = ThemeLibrary.premadeThemes.first(),
    headingFontName: String? = null,
    bodyFontName: String? = null,
    fontScale: Float = 1f,
    headingWeightInt: Int = 700,
    bodyWeightInt: Int = 400,
    content: @Composable () -> Unit
) {
    val resolvedHeading = headingFontName?.takeIf { it.isNotBlank() } ?: themeSpec.defaultHeadingFont
    val resolvedBody = bodyFontName?.takeIf { it.isNotBlank() } ?: themeSpec.defaultBodyFont

    val headingFamily = rememberGoogleFontFamily(resolvedHeading)
    val bodyFamily = rememberGoogleFontFamily(resolvedBody)

    val onAccent = if (themeSpec.isDark) Color(0xFF0B0912) else Color.White

    val colorScheme = if (themeSpec.isDark) {
        darkColorScheme(
            primary = themeSpec.accent,
            onPrimary = onAccent,
            primaryContainer = themeSpec.cardVariantColor,
            onPrimaryContainer = themeSpec.textPrimary,
            secondary = themeSpec.secondaryAccent,
            onSecondary = onAccent,
            secondaryContainer = themeSpec.cardVariantColor,
            onSecondaryContainer = themeSpec.textPrimary,
            tertiary = themeSpec.secondaryAccent,
            background = themeSpec.background,
            onBackground = themeSpec.textPrimary,
            surface = themeSpec.cardColor,
            onSurface = themeSpec.textPrimary,
            surfaceVariant = themeSpec.cardVariantColor,
            onSurfaceVariant = themeSpec.textSecondary,
            outline = themeSpec.borderColor,
            outlineVariant = themeSpec.borderColor.copy(alpha = 0.5f),
            error = Color(0xFFF87171),
            onError = Color(0xFF1F0909)
        )
    } else {
        lightColorScheme(
            primary = themeSpec.accent,
            onPrimary = Color.White,
            primaryContainer = themeSpec.cardVariantColor,
            onPrimaryContainer = themeSpec.textPrimary,
            secondary = themeSpec.secondaryAccent,
            onSecondary = Color.White,
            secondaryContainer = themeSpec.cardVariantColor,
            onSecondaryContainer = themeSpec.textPrimary,
            tertiary = themeSpec.secondaryAccent,
            background = themeSpec.background,
            onBackground = themeSpec.textPrimary,
            surface = themeSpec.cardColor,
            onSurface = themeSpec.textPrimary,
            surfaceVariant = themeSpec.cardVariantColor,
            onSurfaceVariant = themeSpec.textSecondary,
            outline = themeSpec.borderColor,
            outlineVariant = themeSpec.borderColor.copy(alpha = 0.5f),
            error = Color(0xFFDC2626),
            onError = Color.White
        )
    }

    val typography = buildLoreTypography(
        headingFamily = headingFamily,
        bodyFamily = bodyFamily,
        scale = fontScale,
        headingWeight = fontWeightFromInt(headingWeightInt),
        bodyWeight = fontWeightFromInt(bodyWeightInt)
    )

    CompositionLocalProvider(LocalLoreTheme provides themeSpec) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = LoreShapes,
            content = content
        )
    }
}
