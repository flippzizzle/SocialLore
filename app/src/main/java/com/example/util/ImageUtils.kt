package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageUtils {
    /**
     * Reads an image from a Photo Picker Uri, scales it down to fit Firestore's document budget,
     * and encodes it as a compact JPEG data URI (~30KB - 140KB).
     */
    suspend fun encodeUriToDataUrl(
        context: Context,
        uri: Uri,
        maxDimension: Int = 720,
        targetMaxBytes: Int = 130_000
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Unable to read image dimensions"))
            }

            var sampleSize = 1
            while (max(origW / sampleSize, origH / sampleSize) > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext Result.failure(IllegalStateException("Unable to decode selected image"))

            val scale = minOf(
                1f,
                maxDimension.toFloat() / max(decoded.width, decoded.height).toFloat()
            )
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).roundToInt().coerceAtLeast(1),
                    (decoded.height * scale).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }

            var quality = 82
            val out = ByteArrayOutputStream()
            do {
                out.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                quality -= 10
            } while (out.size() > targetMaxBytes && quality >= 30)

            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()

            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            Result.success("data:image/jpeg;base64,$b64")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeDataUrlToBytes(dataUrl: String?): ByteArray? {
        if (dataUrl.isNullOrBlank()) return null
        val commaIndex = dataUrl.indexOf(',')
        if (!dataUrl.startsWith("data:image") || commaIndex == -1) return null
        return try {
            Base64.decode(dataUrl.substring(commaIndex + 1), Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }
}
