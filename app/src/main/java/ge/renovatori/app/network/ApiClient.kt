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
 * Supplies the Bearer token for authenticated requests and renews it after a 401
 * (implemented by [ge.renovatori.app.auth.AuthSession]).
 */
interface TokenSource {
    fun accessToken(): String?

    /**
     * Gets a new access token after [failedAccessToken] was rejected. Returns true when a different
     * token is now available (refreshed here, or already by a concurrent caller), false when the
     * session is over and the user has to sign in again.
     */
    suspend fun refreshAfterRejection(failedAccessToken: String): Boolean
}

/** A file part of a multipart request (e.g. an expense photo). */
class UploadFile(val fileName: String, val mimeType: String, val bytes: ByteArray)

/** Request payloads: a JSON object, or form fields plus files (`multipart/form-data`, like a web form with `enctype`). */
sealed interface RequestBody {
    class Json(val json: JSONObject) : RequestBody
    class Multipart(val fields: Map<String, String>, val files: Map<String, List<UploadFile>>) : RequestBody
}

/**
 * Shared JSON client for `/api/v1` on top of [HttpURLConnection]. Every call runs on
 * [Dispatchers.IO] and returns the envelope root, or throws [ApiError].
 *
 * Only calls made with `authenticated = true` carry `Authorization: Bearer`; public screens stay
 * anonymous, because some public endpoints change their answer for staff tokens (e.g. drafts in
 * `/projects`) and reject expired ones with 401.
 */
class ApiClient(private val baseUrl: String = ApiConfig.baseUrl) {

    @Volatile
    var tokenSource: TokenSource? = null

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap(), authenticated: Boolean = false): JSONObject =
        send("GET", path + queryString(query), null, authenticated)

    suspend fun post(path: String, body: JSONObject, authenticated: Boolean = false): JSONObject =
        send("POST", path, RequestBody.Json(body), authenticated)

    suspend fun put(path: String, body: JSONObject, authenticated: Boolean = false): JSONObject =
        send("PUT", path, RequestBody.Json(body), authenticated)

    suspend fun delete(path: String, authenticated: Boolean = false): JSONObject =
        send("DELETE", path, null, authenticated)

    /** Sends form [fields] and [files] as `multipart/form-data` with [method] (POST or PUT). */
    suspend fun multipart(
        method: String,
        path: String,
        fields: Map<String, String>,
        files: Map<String, List<UploadFile>>,
        authenticated: Boolean = false
    ): JSONObject = send(method, path, RequestBody.Multipart(fields, files), authenticated)

    /** Sends the request; an authenticated one whose token was rejected is refreshed and retried once. */
    private suspend fun send(method: String, path: String, body: RequestBody?, authenticated: Boolean): JSONObject {
        val source = tokenSource.takeIf { authenticated }
        val token = source?.accessToken()
        // An authenticated call without a session fails like the server would, without a round trip.
        if (authenticated && token == null) {
            throw ApiError(401, CODE_UNAUTHORIZED, "ავტორიზაცია სავალდებულოა.")
        }

        return try {
            request(method, path, body, token)
        } catch (e: ApiError) {
            val rejectedToken = token != null && e.status == 401 && e.code in REFRESHABLE_CODES
            if (source == null || !rejectedToken || !source.refreshAfterRejection(token!!)) throw e
            request(method, path, body, source.accessToken())
        }
    }

    private suspend fun request(method: String, path: String, body: RequestBody?, bearerToken: String?): JSONObject =
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
                bearerToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }

                // Only requests with a payload write a body.
                when (body) {
                    is RequestBody.Json -> {
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        connection.outputStream.use { it.write(body.json.toString().toByteArray(Charsets.UTF_8)) }
                    }
                    is RequestBody.Multipart -> {
                        val boundary = "----RenovatoriBoundary" + System.nanoTime()
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                        connection.outputStream.use { writeMultipart(it, boundary, body) }
                    }
                    null -> Unit
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

    private fun writeMultipart(out: java.io.OutputStream, boundary: String, body: RequestBody.Multipart) {
        fun line(text: String = "") = out.write("$text\r\n".toByteArray(Charsets.UTF_8))
        body.fields.forEach { (name, value) ->
            line("--$boundary")
            line("Content-Disposition: form-data; name=\"$name\"")
            line()
            line(value)
        }
        body.files.forEach { (name, files) ->
            files.forEach { file ->
                line("--$boundary")
                line("Content-Disposition: form-data; name=\"$name\"; filename=\"${file.fileName}\"")
                line("Content-Type: ${file.mimeType}")
                line()
                out.write(file.bytes)
                line()
            }
        }
        line("--$boundary--")
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
        private const val CODE_UNAUTHORIZED = "unauthorized"
        /** 401 codes from `api-auth.middleware.js` that a fresh access token can fix. */
        private val REFRESHABLE_CODES = setOf("invalid_token", CODE_UNAUTHORIZED)

        /** Single instance reused by every screen. */
        val shared = ApiClient()
    }
}
