package ge.renovatori.app.network

import org.json.JSONObject

/** Error returned by the API envelope (`{ error: { message, code, details? } }`) or by the transport. */
data class ApiError(
    val status: Int,
    val code: String,
    override val message: String,
    val details: Map<String, String>? = null
) : Exception(message) {
    companion object {
        /** Transport failure (no connection, timeout); never reached the server. */
        const val CODE_NETWORK = "network"
        /** The server answered with something that is not a JSON envelope. */
        const val CODE_INVALID_RESPONSE = "invalid_response"
        const val CODE_NOT_FOUND = "not_found"
    }
}

/** Pagination block sent next to `data` on list endpoints. */
data class Pagination(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val pageSize: Int,
    val hasPrev: Boolean,
    val hasNext: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject) = Pagination(
            currentPage = json.optInt("currentPage", 1),
            totalPages = json.optInt("totalPages", 1),
            totalItems = json.optInt("totalItems", 0),
            pageSize = json.optInt("pageSize", 0),
            hasPrev = json.optBoolean("hasPrev", false),
            hasNext = json.optBoolean("hasNext", false)
        )
    }
}

/** One page of a list endpoint. */
data class Paged<T>(val items: List<T>, val pagination: Pagination)

/**
 * Parses a response body into the envelope root. Returns the root object on success
 * (callers read `data` / `pagination`), throws [ApiError] for an error envelope or a non-JSON body.
 */
internal fun parseEnvelope(status: Int, body: String): JSONObject {
    val root = try {
        JSONObject(body)
    } catch (e: Exception) {
        throw ApiError(status, ApiError.CODE_INVALID_RESPONSE, "სერვერის პასუხი ვერ დამუშავდა.")
    }

    // An error envelope (or any non-2xx status) is surfaced as ApiError with the server's message.
    val error = root.optJSONObject("error")
    if (error != null || status !in 200..299) {
        val detailsJson = error?.optJSONObject("details")
        val details = detailsJson?.keys()?.asSequence()?.associateWith { detailsJson.optString(it) }
        throw ApiError(
            status = status,
            code = error?.optString("code")?.ifBlank { null } ?: "error",
            message = error?.optString("message")?.ifBlank { null } ?: "დაფიქსირდა შეცდომა ($status).",
            details = details
        )
    }
    return root
}
