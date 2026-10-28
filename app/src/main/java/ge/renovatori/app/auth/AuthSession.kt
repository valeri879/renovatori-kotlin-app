package ge.renovatori.app.auth

import android.content.Context
import android.os.Build
import android.util.Log
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.network.TokenSource
import ge.renovatori.app.network.UploadFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * App-wide sign-in state and token lifecycle (compose-auth-task.md in the backend repo):
 * login/signup store the encrypted token pair, `/auth/me` refreshes the user on launch, a rejected
 * access token is refreshed once under a [Mutex], and logout revokes the refresh token server-side.
 */
object AuthSession : TokenSource {

    private const val TAG = "AuthSession"

    private val api = ApiClient.shared
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val refreshMutex = Mutex()
    private lateinit var store: TokenStore

    @Volatile
    private var session: StoredSession? = null

    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** "Manufacturer Model", sent as `device` so the backend can tell sessions apart. */
    private val device = "${Build.MANUFACTURER} ${Build.MODEL}"

    /** Restores a stored session (shown immediately from the cached user) and re-checks it with `/me`. */
    fun init(context: Context) {
        store = TokenStore(context.applicationContext)
        api.tokenSource = this
        session = store.load()
        session?.let { stored ->
            _state.value = AuthState.Authenticated(stored.user)
            scope.launch { refreshUser() }
        }
    }

    suspend fun login(email: String, password: String) {
        val body = JSONObject().put("email", email.trim()).put("password", password).put("device", device)
        start(TokenBundle.fromJson(api.post("/auth/login", body).getJSONObject("data")))
    }

    suspend fun signup(firstName: String, lastName: String, phoneNumber: String, email: String, password: String) {
        val body = JSONObject()
            .put("firstName", firstName.trim())
            .put("lastName", lastName.trim())
            .put("phoneNumber", phoneNumber.trim())
            .put("email", email.trim())
            .put("password", password)
            .put("device", device)
        start(TokenBundle.fromJson(api.post("/auth/signup", body).getJSONObject("data")))
    }

    /** Reloads the user from `/auth/me`; a session the server no longer accepts signs out. */
    suspend fun refreshUser() {
        try {
            val user = User.fromJson(api.get("/auth/me", authenticated = true).getJSONObject("data").getJSONObject("user"))
            session?.let { persist(it.copy(user = user)) }
            _state.value = AuthState.Authenticated(user)
        } catch (e: ApiError) {
            // Offline keeps the cached user; a rejected or deleted account ends the session.
            if (e.status == 401 || e.status == 404) signOutLocally()
        }
    }

    /**
     * Saves name and phone, and a new photo when [photo] is given (web `/profile/edit`); the stored
     * user is replaced with the server's copy so every screen shows the change.
     */
    suspend fun updateProfile(firstName: String, lastName: String, phoneNumber: String, photo: UploadFile?) {
        val fields = mapOf("firstName" to firstName.trim(), "lastName" to lastName.trim(), "phoneNumber" to phoneNumber.trim())
        val files = if (photo != null) mapOf("profileImage" to listOf(photo)) else emptyMap()
        applyUser(api.multipart("PUT", "/profile/me", fields, files, authenticated = true))
    }

    /** Removes the profile photo (web `/profile/delete-image`). */
    suspend fun deleteProfileImage() {
        applyUser(api.delete("/profile/me/image", authenticated = true))
    }

    /**
     * Changes the password. The server signs out every other session and returns a new token pair
     * for this device, which replaces the stored one.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        val body = JSONObject()
            .put("currentPassword", currentPassword)
            .put("newPassword", newPassword)
            .put("confirmPassword", confirmPassword)
            .put("device", device)
        start(TokenBundle.fromJson(api.post("/auth/password-change", body, authenticated = true).getJSONObject("data")))
    }

    private suspend fun applyUser(root: JSONObject) {
        val user = User.fromJson(root.getJSONObject("data").getJSONObject("user"))
        session?.let { persist(it.copy(user = user)) }
        _state.value = AuthState.Authenticated(user)
    }

    /** Revokes the refresh token on the server (best effort) and forgets the session on this device. */
    suspend fun logout() {
        val refreshToken = session?.refreshToken
        signOutLocally()
        if (refreshToken != null) {
            try {
                api.post("/auth/logout", JSONObject().put("refreshToken", refreshToken))
            } catch (e: ApiError) {
                // Offline: the token still expires server-side after 30 days.
                Log.w(TAG, "Logout request failed: ${e.code}")
            }
        }
    }

    override fun accessToken(): String? = session?.accessToken

    override suspend fun refreshAfterRejection(failedAccessToken: String): Boolean = refreshMutex.withLock {
        val current = session ?: return@withLock false
        // Another request already refreshed while this one waited for the lock: just retry.
        if (current.accessToken != failedAccessToken) return@withLock true

        try {
            val body = JSONObject().put("refreshToken", current.refreshToken).put("device", device)
            val bundle = TokenBundle.fromJson(api.post("/auth/refresh", body).getJSONObject("data"))
            // Refresh tokens rotate: the old one is already invalid, so persist the new pair first.
            persist(StoredSession(bundle.accessToken, bundle.refreshToken, bundle.user))
            _state.value = AuthState.Authenticated(bundle.user)
            true
        } catch (e: ApiError) {
            // 401 invalid_refresh / expired_refresh: the session is over. Other errors (offline) keep it.
            if (e.status == 401) signOutLocally()
            false
        }
    }

    private suspend fun start(bundle: TokenBundle) {
        persist(StoredSession(bundle.accessToken, bundle.refreshToken, bundle.user))
        _state.value = AuthState.Authenticated(bundle.user)
    }

    private suspend fun persist(newSession: StoredSession) {
        session = newSession
        withContext(Dispatchers.IO) { store.save(newSession) }
    }

    private suspend fun signOutLocally() {
        session = null
        _state.value = AuthState.Unauthenticated
        withContext(Dispatchers.IO) { store.clear() }
    }
}
