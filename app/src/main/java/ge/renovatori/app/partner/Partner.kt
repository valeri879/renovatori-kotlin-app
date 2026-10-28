package ge.renovatori.app.partner

import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.stringOrNull
import org.json.JSONObject

/** Team member from `GET /partners`. [imageUrl] is a relative "/uploads/small/..." path or null. */
data class Partner(
    val id: String,
    val firstName: String,
    val imageUrl: String?,
    val professions: List<String>
) {
    companion object {
        fun fromJson(json: JSONObject): Partner {
            val professions = json.optJSONArray("professions")
            return Partner(
                id = json.getString("id"),
                firstName = json.optString("firstName"),
                imageUrl = json.stringOrNull("imageUrl"),
                professions = List(professions?.length() ?: 0) { professions!!.getString(it) }
            )
        }
    }
}

/** Loads the partners shown on the main page, most recently updated first (server order). */
suspend fun ApiClient.fetchPartners(): List<Partner> {
    val data = get("/partners").getJSONArray("data")
    return List(data.length()) { Partner.fromJson(data.getJSONObject(it)) }
}
