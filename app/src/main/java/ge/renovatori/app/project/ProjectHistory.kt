package ge.renovatori.app.project

import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.UploadFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder
import java.time.Instant

// Project history is admin-only to write (backend routes/api/v1/admin/project-history.route.js).

/** Fields of the web add/edit history form; a null [timestamp] means "now" when adding, "unchanged" when editing. */
data class HistoryForm(
    val comment: String = "",
    val timestamp: Instant? = null,
    val clientId: String = "",
    val sendClientEmail: Boolean = false
)

/** Server answer to a history write: the project's whole history and whether the client e-mail went out. */
data class HistoryResult(val history: List<HistoryEntry>, val clientEmailSent: Boolean)

private fun encode(id: String) = URLEncoder.encode(id, "UTF-8")

private fun HistoryForm.fields() = mapOf(
    "comment" to comment.trim(),
    // ISO-8601 instant; the web posts a local datetime, the API accepts both.
    "timestamp" to (timestamp?.toString() ?: ""),
    "clientId" to clientId.trim(),
    "sendClientEmail" to sendClientEmail.toString()
)

suspend fun ApiClient.addHistory(projectId: String, form: HistoryForm, images: List<UploadFile>): HistoryResult {
    val data = multipart("POST", "/admin/projects/${encode(projectId)}/history", form.fields(), mapOf("images" to images), authenticated = true)
        .getJSONObject("data")
    return HistoryResult(HistoryEntry.listFromJson(data.optJSONArray("history")), data.optBoolean("clientEmailSent"))
}

/** Updates an entry; new [images] are appended, an empty client id unlinks it, an empty date keeps it. */
suspend fun ApiClient.updateHistory(historyId: String, form: HistoryForm, images: List<UploadFile>): HistoryResult {
    val data = multipart("PUT", "/admin/project-history/${encode(historyId)}", form.fields(), mapOf("images" to images), authenticated = true)
        .getJSONObject("data")
    return HistoryResult(HistoryEntry.listFromJson(data.optJSONArray("history")), data.optBoolean("clientEmailSent"))
}

suspend fun ApiClient.deleteHistory(historyId: String): List<HistoryEntry> =
    HistoryEntry.listFromJson(delete("/admin/project-history/${encode(historyId)}", authenticated = true).getJSONObject("data").optJSONArray("history"))

/** Bumped after any history write so an open project page reloads. */
object HistoryChanges {
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()
    fun changed() {
        _version.value += 1
    }
}
