package ge.renovatori.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.UriHandler
import ge.renovatori.app.network.ApiConfig
import java.net.URI

/** Navigation routes; detail routes take the API id as `{id}`. */
object Routes {
    const val HOME = "home"
    const val CONSULTATION = "consultation"
    const val CALCULATOR = "calculator"
    const val ABOUT = "about"
    const val PRICES = "prices"
    const val BLOG_LIST = "blog"
    const val BLOG_DETAIL = "blog/{id}"
    const val PROJECT_LIST = "projects"
    const val PROJECT_DETAIL = "project/{id}"
    const val FAQ = "faq"
    const val CONTACT = "contact"
    const val COOKIES_POLICY = "cookies-policy"
    const val PRIVACY_POLICY = "privacy-policy"

    fun blog(id: String) = "blog/$id"
    fun project(id: String) = "project/$id"
}

/** Lets any screen open another route without threading callbacks through every composable. */
fun interface AppNavigator {
    fun navigate(route: String)
}

val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> { error("AppNavigator not provided") }

private val siteHosts = setOf("renovatori.ge", "www.renovatori.ge")

/**
 * Maps a website URL (web routes in the backend's `app.js`) to the matching app route, or null
 * when the page is not part of the app (e.g. consultation ordering, which needs login).
 */
fun appRouteForUrl(url: String): String? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    // Only the public site (or the configured API origin, for dev builds) maps to app screens.
    val isSiteUrl = uri.scheme in setOf("http", "https") &&
        (uri.host in siteHosts || "${uri.scheme}://${uri.rawAuthority}" == ApiConfig.origin)
    if (!isSiteUrl) return null

    val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
    return when {
        segments.isEmpty() -> Routes.HOME
        segments.size == 2 && segments[0] == "blog" -> Routes.blog(segments[1])
        segments.size == 2 && segments[0] == "project" -> Routes.project(segments[1])
        segments.size == 1 -> when (segments[0]) {
            "consultation" -> Routes.CONSULTATION
            "calculator" -> Routes.CALCULATOR
            "about" -> Routes.ABOUT
            "prices" -> Routes.PRICES
            "blog-list" -> Routes.BLOG_LIST
            "projects" -> Routes.PROJECT_LIST
            "faq" -> Routes.FAQ
            "contact" -> Routes.CONTACT
            "cookies-policy" -> Routes.COOKIES_POLICY
            "privacy-policy" -> Routes.PRIVACY_POLICY
            else -> null
        }
        else -> null
    }
}

/** Opens site links as app screens; everything else (tel:, mailto:, other sites) goes to [fallback]. */
class AppUriHandler(private val navigator: AppNavigator, private val fallback: UriHandler) : UriHandler {
    override fun openUri(uri: String) {
        val route = appRouteForUrl(uri)
        if (route != null) {
            navigator.navigate(route)
        } else {
            // No app can handle the link (e.g. no browser on the device): ignore rather than crash.
            runCatching { fallback.openUri(uri) }
        }
    }
}
