package ge.renovatori.app.auth

import ge.renovatori.app.network.instantOrNull
import ge.renovatori.app.network.stringOrNull
import org.json.JSONObject
import java.time.Instant

/** Signed-in user (`serializeUser` in the backend's api-serializers.js). */
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    /** Upload file name; shown from "/uploads/small/<name>" like the web profile. */
    val profileImage: String?,
    val isAdmin: Boolean,
    val isModerator: Boolean,
    val isVerified: Boolean,
    val createdAt: Instant?
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("firstName", firstName)
        .put("lastName", lastName)
        .put("email", email)
        .put("phoneNumber", phoneNumber)
        .put("profileImage", profileImage ?: JSONObject.NULL)
        .put("isAdmin", isAdmin)
        .put("isModerator", isModerator)
        .put("isVerified", isVerified)
        .put("createdAt", createdAt?.toString() ?: JSONObject.NULL)

    companion object {
        fun fromJson(json: JSONObject) = User(
            id = json.getString("id"),
            firstName = json.optString("firstName"),
            lastName = json.optString("lastName"),
            email = json.optString("email"),
            phoneNumber = json.optString("phoneNumber"),
            profileImage = json.stringOrNull("profileImage"),
            isAdmin = json.optBoolean("isAdmin"),
            isModerator = json.optBoolean("isModerator"),
            isVerified = json.optBoolean("isVerified"),
            createdAt = json.instantOrNull("createdAt")
        )
    }
}

/** `/auth/login`, `/signup` and `/refresh` payload. Refresh tokens rotate: always keep the newest. */
data class TokenBundle(val accessToken: String, val refreshToken: String, val expiresIn: Int, val user: User) {
    companion object {
        fun fromJson(json: JSONObject) = TokenBundle(
            accessToken = json.getString("accessToken"),
            refreshToken = json.getString("refreshToken"),
            expiresIn = json.optInt("expiresIn"),
            user = User.fromJson(json.getJSONObject("user"))
        )
    }
}

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val user: User) : AuthState
}
