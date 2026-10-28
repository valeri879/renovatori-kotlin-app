package ge.renovatori.app.network

import ge.renovatori.app.BuildConfig

/**
 * API host configuration. The origin differs per build type (see `app/build.gradle.kts`);
 * image paths from the API are relative to the origin, not to `/api/v1`.
 */
object ApiConfig {
    val origin: String = BuildConfig.API_ORIGIN
    val baseUrl: String = "$origin/api/v1"

    /** Builds a displayable URL from a relative upload path, or null when the path is missing. */
    fun imageUrl(path: String?): String? = path?.takeIf { it.isNotBlank() }?.let { origin + it }
}
