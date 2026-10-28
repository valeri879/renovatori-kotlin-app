package ge.renovatori.app.project

import ge.renovatori.app.network.ApiClient
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
    val images: List<ProjectImage>
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
                }
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

/** Loads one published project; a missing or unpublished id throws [ge.renovatori.app.network.ApiError] `not_found`. */
suspend fun ApiClient.fetchProject(id: String): Project =
    Project.fromJson(get("/projects/${URLEncoder.encode(id, "UTF-8")}").getJSONObject("data").getJSONObject("project"))

/** Area as the web prints it: "45" for whole numbers, "45.5" otherwise. */
fun formatArea(area: Double?): String = when {
    area == null -> "-"
    area % 1.0 == 0.0 -> area.toLong().toString()
    else -> area.toString()
}
