package ge.renovatori.app.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal remote image loader (HttpURLConnection + BitmapFactory + in-memory LRU cache), used
 * instead of a third-party library. No disk cache: images are re-downloaded after process death.
 */
object ImageLoader {
    private const val TAG = "ImageLoader"
    private const val MAX_DIMENSION_PX = 1440

    // A quarter of the app heap, sized in KB.
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 4).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun cached(url: String): Bitmap? = cache.get(url)

    /** Downloads and decodes [url], downsampled so neither side exceeds [MAX_DIMENSION_PX]. */
    suspend fun load(url: String): Bitmap? {
        cache.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val bytes = download(url)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sampleSize = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_DIMENSION_PX) sampleSize *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also { cache.put(url, it) }
            } catch (e: IOException) {
                Log.w(TAG, "Image download failed: $url", e)
                null
            }
        }
    }

    private fun download(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            // Non-2xx responses carry an error page, not an image.
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}

private sealed interface RemoteImageState {
    data object Loading : RemoteImageState
    data object Error : RemoteImageState
    data class Ready(val bitmap: ImageBitmap) : RemoteImageState
}

/**
 * Shows the image at [url] (already absolute, see [ApiConfig.imageUrl]) with a placeholder while
 * loading and a warning icon when it fails or [url] is null. Give it a size via [modifier].
 */
@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val state by produceState<RemoteImageState>(
        initialValue = url?.let(ImageLoader::cached)?.let { RemoteImageState.Ready(it.asImageBitmap()) }
            ?: RemoteImageState.Loading,
        key1 = url
    ) {
        // Missing URL or failed download both end in the error placeholder.
        value = url?.let { ImageLoader.load(it) }?.let { RemoteImageState.Ready(it.asImageBitmap()) }
            ?: RemoteImageState.Error
    }

    when (val current = state) {
        is RemoteImageState.Ready -> Image(
            bitmap = current.bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )

        RemoteImageState.Loading -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))

        RemoteImageState.Error -> Box(
            modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "სურათი ვერ ჩაიტვირთა",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
