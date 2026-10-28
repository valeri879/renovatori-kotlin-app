package ge.renovatori.app.project

import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.network.Paged
import ge.renovatori.app.network.Pagination
import ge.renovatori.app.network.doubleOrNull
import ge.renovatori.app.network.instantOrNull
import ge.renovatori.app.network.stringOrNull
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant

/** Project list item from `GET /projects`. [coverImageUrl] is a relative "/uploads/small/..." path. */
data class ProjectCard(
    val id: String,
    val title: String,
    val metaTitle: String,
    val metaDescription: String,
    val location: String,
    val area: Double?,
    val startDate: Instant?,
    val endDate: Instant?,
    val coverImageUrl: String?,
    val updatedAt: Instant?
) {
    companion object {
        fun fromJson(json: JSONObject) = ProjectCard(
            id = json.getString("id"),
            title = json.optString("title"),
            metaTitle = json.optString("metaTitle"),
            metaDescription = json.optString("metaDescription"),
            location = json.optString("location"),
            area = json.doubleOrNull("area"),
            startDate = json.instantOrNull("startDate"),
            endDate = json.instantOrNull("endDate"),
            coverImageUrl = json.stringOrNull("coverImageUrl"),
            updatedAt = json.instantOrNull("updatedAt")
        )
    }
}

/** Project photo: full-size [url] and [thumbUrl], both relative "/uploads/..." paths. */
data class ProjectImage(val url: String, val thumbUrl: String)

/** Full project from `GET /projects/:id`. [description] is editor HTML. */
data class Project(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val area: Double?,
    val images: List<ProjectImage>,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    /** Only sent to signed-in viewers; anonymous visitors only ever get published projects. */
    val isPublished: Boolean = true,
    /** Client account linked to the project (sent to admins, moderators and that client). */
    val clientId: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): Project {
            val images = json.optJSONArray("images")
            return Project(
                id = json.getString("id"),
                title = json.optString("title"),
                description = json.optString("description"),
                location = json.optString("location"),
                area = json.doubleOrNull("area"),
                images = List(images?.length() ?: 0) {
                    val image = images!!.getJSONObject(it)
                    ProjectImage(url = image.optString("url"), thumbUrl = image.optString("thumbUrl"))
                },
                startDate = json.instantOrNull("startDate"),
                endDate = json.instantOrNull("endDate"),
                createdAt = json.instantOrNull("createdAt"),
                updatedAt = json.instantOrNull("updatedAt"),
                isPublished = if (json.has("isPublished")) json.optBoolean("isPublished") else true,
                clientId = json.stringOrNull("clientId")
            )
        }
    }
}

/** What the viewer may see on the project page (`access` in `GET /projects/:id`). */
data class ProjectAccess(val canViewClientInfo: Boolean = false, val canViewHistory: Boolean = false, val canManage: Boolean = false)

data class ProjectClient(val firstName: String, val lastName: String, val email: String, val phoneNumber: String)

data class ClientPayment(val id: String, val paidAmountDollar: Double, val paidAt: Instant?)

/** Dollar finances of the linked expense project (web "პროექტის ფინანსები და პროგრესი"). */
data class FinancialSummary(
    val totalAmountDollar: Double,
    val remainingAmountDollar: Double,
    val totalPaidAmountDollar: Double,
    val paidAmounts: List<ClientPayment>,
    val progressPercent: Int,
    val progressLabel: String,
    val progressValid: Boolean
)

/** One project history entry (`serializeProjectHistory`). */
data class HistoryEntry(val id: String, val comment: String, val timestamp: Instant?, val clientId: String?, val images: List<ProjectImage>) {
    companion object {
        fun fromJson(json: JSONObject): HistoryEntry {
            val images = json.optJSONArray("images")
            return HistoryEntry(
                id = json.getString("id"),
                comment = json.optString("comment"),
                timestamp = json.instantOrNull("timestamp"),
                clientId = json.stringOrNull("clientId"),
                images = List(images?.length() ?: 0) {
                    val image = images!!.getJSONObject(it)
                    ProjectImage(url = image.optString("url"), thumbUrl = image.optString("thumbUrl"))
                }
            )
        }

        fun listFromJson(array: org.json.JSONArray?): List<HistoryEntry> =
            List(array?.length() ?: 0) { fromJson(array!!.getJSONObject(it)) }
    }
}

/** `GET /projects/:id` for the current viewer: the public project plus whatever their role unlocks. */
data class ProjectView(
    val project: Project,
    val access: ProjectAccess = ProjectAccess(),
    val client: ProjectClient? = null,
    val financialSummary: FinancialSummary? = null,
    val history: List<HistoryEntry> = emptyList()
) {
    companion object {
        fun fromJson(data: JSONObject): ProjectView {
            val access = data.optJSONObject("access")
            val client = data.optJSONObject("client")
            val finances = data.optJSONObject("financialSummary")
            return ProjectView(
                project = Project.fromJson(data.getJSONObject("project")),
                access = ProjectAccess(
                    canViewClientInfo = access?.optBoolean("canViewClientInfo") ?: false,
                    canViewHistory = access?.optBoolean("canViewHistory") ?: false,
                    canManage = access?.optBoolean("canManage") ?: false
                ),
                client = client?.let {
                    ProjectClient(it.optString("firstName"), it.optString("lastName"), it.optString("email"), it.optString("phoneNumber"))
                },
                financialSummary = finances?.let { f ->
                    val payments = f.optJSONArray("paidAmounts")
                    val progress = f.optJSONObject("progress")
                    FinancialSummary(
                        totalAmountDollar = f.optDouble("totalAmountDollar", 0.0),
                        remainingAmountDollar = f.optDouble("remainingAmountDollar", 0.0),
                        totalPaidAmountDollar = f.optDouble("totalPaidAmountDollar", 0.0),
                        paidAmounts = List(payments?.length() ?: 0) {
                            val payment = payments!!.getJSONObject(it)
                            ClientPayment(payment.getString("id"), payment.optDouble("paidAmountDollar", 0.0), payment.instantOrNull("paidAt"))
                        },
                        progressPercent = progress?.optInt("percent") ?: 0,
                        progressLabel = progress?.optString("label").orEmpty(),
                        progressValid = progress?.optBoolean("isValid") ?: false
                    )
                },
                history = HistoryEntry.listFromJson(data.optJSONArray("history"))
            )
        }
    }
}

/** Loads one page of published projects, newest first. */
suspend fun ApiClient.fetchProjects(page: Int = 1, pageSize: Int = 12): Paged<ProjectCard> {
    val root = get("/projects", mapOf("page" to page, "pageSize" to pageSize))
    val data = root.getJSONArray("data")
    return Paged(
        items = List(data.length()) { ProjectCard.fromJson(data.getJSONObject(it)) },
        pagination = Pagination.fromJson(root.getJSONObject("pagination"))
    )
}

/**
 * Loads a project for the current viewer. Signed in, the request carries the token so the server
 * adds client info, finances, history and drafts the viewer may see; a session the server rejects
 * (refresh failed and the user was signed out) falls back to the public view.
 * A missing or hidden id throws [ApiError] `not_found`.
 */
suspend fun ApiClient.fetchProjectView(id: String, signedIn: Boolean): ProjectView {
    val path = "/projects/${URLEncoder.encode(id, "UTF-8")}"
    return try {
        ProjectView.fromJson(get(path, authenticated = signedIn).getJSONObject("data"))
    } catch (e: ApiError) {
        if (!signedIn || e.status != 401) throw e
        ProjectView.fromJson(get(path).getJSONObject("data"))
    }
}

/** Area as the web prints it: "45" for whole numbers, "45.5" otherwise. */
fun formatArea(area: Double?): String = when {
    area == null -> "-"
    area % 1.0 == 0.0 -> area.toLong().toString()
    else -> area.toString()
}
