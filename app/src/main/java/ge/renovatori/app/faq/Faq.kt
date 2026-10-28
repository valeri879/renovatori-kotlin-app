package ge.renovatori.app.faq

import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.instantOrNull
import org.json.JSONObject
import java.time.Instant

/** FAQ entry from `GET /faq`. [description] is HTML (rendered with `!=` on the web). */
data class Faq(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Instant?
) {
    companion object {
        fun fromJson(json: JSONObject) = Faq(
            id = json.getString("id"),
            title = json.optString("title"),
            description = json.optString("description"),
            timestamp = json.instantOrNull("timestamp")
        )
    }
}

/** Loads every FAQ entry, newest first (server order). */
suspend fun ApiClient.fetchFaqs(): List<Faq> {
    val data = get("/faq").getJSONArray("data")
    return List(data.length()) { Faq.fromJson(data.getJSONObject(it)) }
}
