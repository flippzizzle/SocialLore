package com.example.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder

data class GoogleFontMeta(
    val name: String,
    val category: String,
    val bundledResId: Int? = null
)

object FontCatalog {
    private val bundledMap = mapOf(
        "Playfair Display" to R.font.playfair_display,
        "Cinzel" to R.font.cinzel,
        "Cormorant Garamond" to R.font.cormorant_garamond,
        "Lora" to R.font.lora,
        "Merriweather" to R.font.merriweather,
        "Plus Jakarta Sans" to R.font.plus_jakarta_sans,
        "Inter" to R.font.inter,
        "Space Grotesk" to R.font.space_grotesk,
        "Outfit" to R.font.outfit,
        "Montserrat" to R.font.montserrat,
        "Poppins" to R.font.poppins,
        "Pacifico" to R.font.pacifico,
        "Dancing Script" to R.font.dancing_script,
        "Caveat" to R.font.caveat,
        "Permanent Marker" to R.font.permanent_marker,
        "Orbitron" to R.font.orbitron,
        "Press Start 2P" to R.font.press_start_2p,
        "JetBrains Mono" to R.font.jetbrains_mono,
        "Roboto Mono" to R.font.roboto_mono,
        "Fira Code" to R.font.fira_code
    )

    val allFonts: List<GoogleFontMeta> = listOf(
        // Bundled instant fonts
        GoogleFontMeta("Playfair Display", "Serif", R.font.playfair_display),
        GoogleFontMeta("Cinzel", "Serif", R.font.cinzel),
        GoogleFontMeta("Cormorant Garamond", "Serif", R.font.cormorant_garamond),
        GoogleFontMeta("Lora", "Serif", R.font.lora),
        GoogleFontMeta("Merriweather", "Serif", R.font.merriweather),
        GoogleFontMeta("Plus Jakarta Sans", "Sans-Serif", R.font.plus_jakarta_sans),
        GoogleFontMeta("Inter", "Sans-Serif", R.font.inter),
        GoogleFontMeta("Space Grotesk", "Sans-Serif", R.font.space_grotesk),
        GoogleFontMeta("Outfit", "Sans-Serif", R.font.outfit),
        GoogleFontMeta("Montserrat", "Sans-Serif", R.font.montserrat),
        GoogleFontMeta("Poppins", "Sans-Serif", R.font.poppins),
        GoogleFontMeta("Pacifico", "Handwriting", R.font.pacifico),
        GoogleFontMeta("Dancing Script", "Handwriting", R.font.dancing_script),
        GoogleFontMeta("Caveat", "Handwriting", R.font.caveat),
        GoogleFontMeta("Permanent Marker", "Display", R.font.permanent_marker),
        GoogleFontMeta("Orbitron", "Display", R.font.orbitron),
        GoogleFontMeta("Press Start 2P", "Display", R.font.press_start_2p),
        GoogleFontMeta("JetBrains Mono", "Monospace", R.font.jetbrains_mono),
        GoogleFontMeta("Roboto Mono", "Monospace", R.font.roboto_mono),
        GoogleFontMeta("Fira Code", "Monospace", R.font.fira_code),
        // Extended Google Fonts catalog loaded on demand
        GoogleFontMeta("Roboto", "Sans-Serif"),
        GoogleFontMeta("Open Sans", "Sans-Serif"),
        GoogleFontMeta("Lato", "Sans-Serif"),
        GoogleFontMeta("Nunito", "Sans-Serif"),
        GoogleFontMeta("Nunito Sans", "Sans-Serif"),
        GoogleFontMeta("Raleway", "Sans-Serif"),
        GoogleFontMeta("Ubuntu", "Sans-Serif"),
        GoogleFontMeta("Rubik", "Sans-Serif"),
        GoogleFontMeta("Work Sans", "Sans-Serif"),
        GoogleFontMeta("DM Sans", "Sans-Serif"),
        GoogleFontMeta("Manrope", "Sans-Serif"),
        GoogleFontMeta("Quicksand", "Sans-Serif"),
        GoogleFontMeta("Josefin Sans", "Sans-Serif"),
        GoogleFontMeta("Barlow", "Sans-Serif"),
        GoogleFontMeta("Mulish", "Sans-Serif"),
        GoogleFontMeta("Karla", "Sans-Serif"),
        GoogleFontMeta("Cabin", "Sans-Serif"),
        GoogleFontMeta("Comfortaa", "Display"),
        GoogleFontMeta("Source Sans 3", "Sans-Serif"),
        GoogleFontMeta("Fira Sans", "Sans-Serif"),
        GoogleFontMeta("Titillium Web", "Sans-Serif"),
        GoogleFontMeta("Exo 2", "Sans-Serif"),
        GoogleFontMeta("Rajdhani", "Sans-Serif"),
        GoogleFontMeta("Syne", "Display"),
        GoogleFontMeta("Lexend", "Sans-Serif"),
        GoogleFontMeta("Sora", "Sans-Serif"),
        GoogleFontMeta("Urbanist", "Sans-Serif"),
        GoogleFontMeta("Figtree", "Sans-Serif"),
        GoogleFontMeta("Onest", "Sans-Serif"),
        GoogleFontMeta("Instrument Sans", "Sans-Serif"),
        GoogleFontMeta("Bricolage Grotesque", "Display"),
        GoogleFontMeta("PT Serif", "Serif"),
        GoogleFontMeta("Libre Baskerville", "Serif"),
        GoogleFontMeta("EB Garamond", "Serif"),
        GoogleFontMeta("Crimson Text", "Serif"),
        GoogleFontMeta("Crimson Pro", "Serif"),
        GoogleFontMeta("Bitter", "Serif"),
        GoogleFontMeta("DM Serif Display", "Serif"),
        GoogleFontMeta("Bodoni Moda", "Serif"),
        GoogleFontMeta("Fraunces", "Serif"),
        GoogleFontMeta("Spectral", "Serif"),
        GoogleFontMeta("Vollkorn", "Serif"),
        GoogleFontMeta("Arvo", "Serif"),
        GoogleFontMeta("Cardo", "Serif"),
        GoogleFontMeta("Abril Fatface", "Display"),
        GoogleFontMeta("Bebas Neue", "Display"),
        GoogleFontMeta("Oswald", "Sans-Serif"),
        GoogleFontMeta("Anton", "Display"),
        GoogleFontMeta("Righteous", "Display"),
        GoogleFontMeta("Lobster", "Display"),
        GoogleFontMeta("Alfa Slab One", "Display"),
        GoogleFontMeta("Bungee", "Display"),
        GoogleFontMeta("Monoton", "Display"),
        GoogleFontMeta("Silkscreen", "Display"),
        GoogleFontMeta("VT323", "Monospace"),
        GoogleFontMeta("Audiowide", "Display"),
        GoogleFontMeta("Russo One", "Display"),
        GoogleFontMeta("Black Ops One", "Display"),
        GoogleFontMeta("Creepster", "Display"),
        GoogleFontMeta("UnifrakturMaguntia", "Display"),
        GoogleFontMeta("Cinzel Decorative", "Display"),
        GoogleFontMeta("Great Vibes", "Handwriting"),
        GoogleFontMeta("Sacramento", "Handwriting"),
        GoogleFontMeta("Satisfy", "Handwriting"),
        GoogleFontMeta("Indie Flower", "Handwriting"),
        GoogleFontMeta("Shadows Into Light", "Handwriting"),
        GoogleFontMeta("Amatic SC", "Handwriting"),
        GoogleFontMeta("Kalam", "Handwriting"),
        GoogleFontMeta("Patrick Hand", "Handwriting"),
        GoogleFontMeta("Cookie", "Handwriting"),
        GoogleFontMeta("Kaushan Script", "Handwriting"),
        GoogleFontMeta("Yellowtail", "Handwriting"),
        GoogleFontMeta("Courgette", "Handwriting"),
        GoogleFontMeta("Marck Script", "Handwriting"),
        GoogleFontMeta("Allura", "Handwriting"),
        GoogleFontMeta("Alex Brush", "Handwriting"),
        GoogleFontMeta("Gloria Hallelujah", "Handwriting"),
        GoogleFontMeta("Handlee", "Handwriting"),
        GoogleFontMeta("Source Code Pro", "Monospace"),
        GoogleFontMeta("IBM Plex Mono", "Monospace"),
        GoogleFontMeta("Space Mono", "Monospace"),
        GoogleFontMeta("Inconsolata", "Monospace"),
        GoogleFontMeta("Courier Prime", "Monospace"),
        GoogleFontMeta("Share Tech Mono", "Monospace"),
        GoogleFontMeta("Major Mono Display", "Monospace"),
        GoogleFontMeta("IBM Plex Sans", "Sans-Serif"),
        GoogleFontMeta("IBM Plex Serif", "Serif"),
        GoogleFontMeta("Noto Sans", "Sans-Serif"),
        GoogleFontMeta("Noto Serif", "Serif"),
        GoogleFontMeta("Archivo", "Sans-Serif"),
        GoogleFontMeta("Archivo Black", "Display"),
        GoogleFontMeta("Jost", "Sans-Serif"),
        GoogleFontMeta("Kanit", "Sans-Serif"),
        GoogleFontMeta("Prompt", "Sans-Serif"),
        GoogleFontMeta("Signika", "Sans-Serif"),
        GoogleFontMeta("Varela Round", "Sans-Serif"),
        GoogleFontMeta("Fredoka", "Display"),
        GoogleFontMeta("Baloo 2", "Display"),
        GoogleFontMeta("Chewy", "Display"),
        GoogleFontMeta("Luckiest Guy", "Display"),
        GoogleFontMeta("Bangla", "Display").copy(name = "Bangers"),
        GoogleFontMeta("Carter One", "Display"),
        GoogleFontMeta("Special Elite", "Display"),
        GoogleFontMeta("Syne Mono", "Monospace"),
        GoogleFontMeta("Cutive Mono", "Monospace"),
        GoogleFontMeta("Anonymous Pro", "Monospace"),
        GoogleFontMeta("Overpass Mono", "Monospace"),
        GoogleFontMeta("Red Hat Mono", "Monospace"),
        GoogleFontMeta("Red Hat Display", "Sans-Serif"),
        GoogleFontMeta("Yeseva One", "Display"),
        GoogleFontMeta("Prata", "Serif"),
        GoogleFontMeta("Marcellus", "Serif"),
        GoogleFontMeta("Cormorant", "Serif"),
        GoogleFontMeta("Gelasio", "Serif"),
        GoogleFontMeta("Alegreya", "Serif"),
        GoogleFontMeta("Alegreya Sans", "Sans-Serif"),
        GoogleFontMeta("Newsreader", "Serif"),
        GoogleFontMeta("Instrument Serif", "Serif"),
        GoogleFontMeta("Young Serif", "Serif"),
        GoogleFontMeta("Rock Salt", "Handwriting"),
        GoogleFontMeta("Homemade Apple", "Handwriting"),
        GoogleFontMeta("Reenie Beanie", "Handwriting"),
        GoogleFontMeta("Nothing You Could Do", "Handwriting"),
        GoogleFontMeta("Bad Script", "Handwriting"),
        GoogleFontMeta("Pinyon Script", "Handwriting"),
        GoogleFontMeta("Tangerine", "Handwriting"),
        GoogleFontMeta("Mr Dafoe", "Handwriting"),
        GoogleFontMeta("Zeyada", "Handwriting"),
        GoogleFontMeta("Covered By Your Grace", "Handwriting")
    ).distinctBy { it.name }

    private val client = OkHttpClient()
    private val memoryCache = mutableMapOf<String, FontFamily>()

    fun bundledFontFamily(fontName: String): FontFamily? {
        val resId = bundledMap[fontName] ?: return null
        return memoryCache.getOrPut(fontName) {
            FontFamily(
                Font(resId, FontWeight.Normal),
                Font(resId, FontWeight.Medium),
                Font(resId, FontWeight.SemiBold),
                Font(resId, FontWeight.Bold)
            )
        }
    }

    private fun sanitizedFileName(fontName: String): String =
        fontName.lowercase().replace(Regex("[^a-z0-9]+"), "_") + ".ttf"

    suspend fun resolveFontFamily(context: Context, fontName: String): FontFamily {
        bundledFontFamily(fontName)?.let { return it }
        memoryCache[fontName]?.let { return it }

        val dir = File(context.filesDir, "google_fonts").apply { mkdirs() }
        val localFile = File(dir, sanitizedFileName(fontName))
        if (localFile.exists() && localFile.length() > 1024) {
            return buildFileFontFamily(fontName, localFile)
        }

        val downloaded = withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(fontName, "UTF-8").replace("+", "%20")
                val cssReq = Request.Builder()
                    .url("https://fonts.googleapis.com/css2?family=$encoded")
                    .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 14)")
                    .build()
                val cssBody = client.newCall(cssReq).execute().use { resp ->
                    if (!resp.isSuccessful) null else resp.body?.string()
                } ?: return@withContext false

                val urlMatch = Regex("url\\((https://fonts\\.gstatic\\.com/[^)]+\\.ttf)\\)")
                    .find(cssBody)?.groupValues?.getOrNull(1)
                    ?: return@withContext false

                val fontReq = Request.Builder().url(urlMatch).build()
                client.newCall(fontReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext false
                    val bytes = resp.body?.bytes() ?: return@withContext false
                    if (bytes.size < 1024) return@withContext false
                    localFile.writeBytes(bytes)
                    true
                }
            } catch (_: Exception) {
                false
            }
        }

        return if (downloaded && localFile.exists()) {
            buildFileFontFamily(fontName, localFile)
        } else {
            bundledFontFamily("Plus Jakarta Sans") ?: FontFamily.SansSerif
        }
    }

    private fun buildFileFontFamily(fontName: String, file: File): FontFamily {
        return try {
            // Verify Typeface loads cleanly before constructing Compose FontFamily
            Typeface.createFromFile(file)
            FontFamily(
                Font(file, FontWeight.Normal),
                Font(file, FontWeight.Medium),
                Font(file, FontWeight.SemiBold),
                Font(file, FontWeight.Bold)
            ).also { memoryCache[fontName] = it }
        } catch (_: Exception) {
            bundledFontFamily("Plus Jakarta Sans") ?: FontFamily.SansSerif
        }
    }
}

@Composable
fun rememberGoogleFontFamily(fontName: String): FontFamily {
    val context = LocalContext.current
    var fontFamily by remember(fontName) {
        mutableStateOf(FontCatalog.bundledFontFamily(fontName) ?: FontFamily.SansSerif)
    }
    LaunchedEffect(fontName) {
        fontFamily = FontCatalog.resolveFontFamily(context, fontName)
    }
    return fontFamily
}
