package ge.renovatori.app.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Shared JSON client for `/api/v1` on top of [HttpURLConnection]. Every call runs on
 * [Dispatchers.IO] and returns the envelope root, or throws [ApiError].
 *
 * Public screens only for now; the auth task adds Bearer injection and 401 refresh here.
 */
class ApiClient(private val baseUrl: String = ApiConfig.baseUrl) {

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap()): JSONObject =
        request("GET", path + queryString(query), null)

    suspend fun post(path: String, body: JSONObject): JSONObject =
        request("POST", path, body)

    private suspend fun request(method: String, path: String, body: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = try {
                URL(baseUrl + path).openConnection() as HttpURLConnection
            } catch (e: IOException) {
                throw networkError(method, path, e)
            }
            try {
                connection.requestMethod = method
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/json")

                // Only requests with a payload send a JSON body.
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }

                val status = connection.responseCode
                // Error statuses carry their envelope on the error stream.
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                parseEnvelope(status, text)
            } catch (e: IOException) {
                throw networkError(method, path, e)
            } finally {
                connection.disconnect()
            }
        }

    private fun queryString(query: Map<String, Any?>): String {
        val pairs = query.filterValues { it != null }
            .map { (key, value) -> "${encode(key)}=${encode(value.toString())}" }
        return if (pairs.isEmpty()) "" else "?" + pairs.joinToString("&")
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    /** Logs the transport cause (URL path only, never headers or bodies) and maps it to a user-facing error. */
    private fun networkError(method: String, path: String, cause: IOException): ApiError {
        Log.w(TAG, "$method $path failed", cause)
        return ApiError(0, ApiError.CODE_NETWORK, "სერვერთან კავშირი ვერ მოხერხდა. შეამოწმეთ ინტერნეტი.")
    }

    companion object {
        private const val TAG = "ApiClient"
        private const val TIMEOUT_MS = 15_000

        /** Single instance reused by every screen. */
        val shared = ApiClient()
    }
}
