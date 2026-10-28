package ge.renovatori.app.blog

import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.Paged
import ge.renovatori.app.network.Pagination
import ge.renovatori.app.network.instantOrNull
import ge.renovatori.app.network.stringOrNull
import org.json.JSONObject
import java.time.Instant

/** Blog list item from `GET /blog`. [coverImageUrl] is a relative "/uploads/small/..." path. */
data class BlogCard(
    val id: String,
    val title: String,
    val metaDescription: String,
    val coverImageUrl: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?
) {
    companion object {
        fun fromJson(json: JSONObject) = BlogCard(
            id = json.getString("id"),
            title = json.optString("title"),
            metaDescription = json.optString("metaDescription"),
            coverImageUrl = json.stringOrNull("coverImageUrl"),
            createdAt = json.instantOrNull("createdAt"),
            updatedAt = json.instantOrNull("updatedAt")
        )
    }
}

/** Loads one page of published posts; [newestFirst] maps to `sort=desc` (server default). */
suspend fun ApiClient.fetchBlogs(page: Int = 1, pageSize: Int = 12, newestFirst: Boolean = true): Paged<BlogCard> {
    val root = get("/blog", mapOf("page" to page, "pageSize" to pageSize, "sort" to if (newestFirst) "desc" else "asc"))
    val data = root.getJSONArray("data")
    return Paged(
        items = List(data.length()) { BlogCard.fromJson(data.getJSONObject(it)) },
        pagination = Pagination.fromJson(root.getJSONObject("pagination"))
    )
}

/** Full post from `GET /blog/:id`. [description] is editor HTML; [coverImageUrl] is the full-size "/uploads/..." path. */
data class Blog(
    val id: String,
    val title: String,
    val description: String,
    val coverImageUrl: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?
) {
    companion object {
        fun fromJson(json: JSONObject) = Blog(
            id = json.getString("id"),
            title = json.optString("title"),
            description = json.optString("description"),
            coverImageUrl = json.stringOrNull("coverImageUrl"),
            createdAt = json.instantOrNull("createdAt"),
            updatedAt = json.instantOrNull("updatedAt")
        )
    }
}

/** Loads one published post; a missing or unpublished id throws [ge.renovatori.app.network.ApiError] `not_found`. */
suspend fun ApiClient.fetchBlog(id: String): Blog =
    Blog.fromJson(get("/blog/${java.net.URLEncoder.encode(id, "UTF-8")}").getJSONObject("data").getJSONObject("blog"))
