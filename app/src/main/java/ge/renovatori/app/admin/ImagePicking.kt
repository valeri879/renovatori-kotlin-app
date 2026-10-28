package ge.renovatori.app.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import ge.renovatori.app.network.UploadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val MAX_DIMENSION_PX = 1920
private const val JPEG_QUALITY = 85

/**
 * Turns a picked photo into an upload: downscaled so neither side exceeds [MAX_DIMENSION_PX],
 * rotated upright from its EXIF orientation, and re-encoded as JPEG (the server stores every
 * upload as `.jpg`). Returns null when the file cannot be read as an image.
 */
suspend fun Context.photoUpload(uri: Uri, index: Int): UploadFile? = withContext(Dispatchers.IO) {
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        // Not an image (or unreadable): skip it instead of uploading garbage.
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_DIMENSION_PX) sampleSize *= 2
        val decoded = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: return@withContext null

        val rotation = contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, MAX_DIMENSION_PX.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val bytes = ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        UploadFile(fileName = "photo-${index + 1}.jpg", mimeType = "image/jpeg", bytes = bytes)
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        // Permission to the picked item was revoked.
        null
    }
}

/** Small preview bitmap of a picked photo (works on every supported Android version, unlike `loadThumbnail`). */
suspend fun Context.photoPreview(uri: Uri, maxPx: Int = 300): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxPx) sampleSize *= 2
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        }
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }
}
