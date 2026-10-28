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
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile/edit"
    const val PASSWORD_CHANGE = "profile/password-change"
    const val ADMIN_EXPENSES = "admin/expenses"
    const val ADMIN_EXPENSE_ADD = "admin/expense-add"
    const val ADMIN_EXPENSE_DETAIL = "admin/expense/{id}"
    const val ADMIN_EXPENSE_ENTRY_ADD = "admin/expense/{id}/entry-add"
    const val ADMIN_EXPENSE_ENTRY_EDIT = "admin/expense/{id}/entry/{entryId}"
    const val ADMIN_EXPENSE_PAID_ADD = "admin/expense/{id}/paid-add"
    const val PROJECT_HISTORY_ADD = "project/{id}/history-add"
    const val PROJECT_HISTORY_EDIT = "project/{id}/history/{historyId}"

    fun blog(id: String) = "blog/$id"
    fun project(id: String) = "project/$id"
    fun adminExpense(id: String) = "admin/expense/$id"
    fun adminExpenseEntryAdd(id: String) = "admin/expense/$id/entry-add"
    fun adminExpenseEntryEdit(id: String, entryId: String) = "admin/expense/$id/entry/$entryId"
    fun adminExpensePaidAdd(id: String) = "admin/expense/$id/paid-add"
    fun projectHistoryAdd(id: String) = "project/$id/history-add"
    fun projectHistoryEdit(id: String, historyId: String) = "project/$id/history/$historyId"
}

/** Lets any screen open another route without threading callbacks through every composable. */
interface AppNavigator {
    /** Opens [route]; [replaceCurrent] drops the current screen first (e.g. a submitted form). */
    fun navigate(route: String, replaceCurrent: Boolean = false)

    /** Returns to the previous screen (system Back). */
    fun back()
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
    // Admin expense pages (routes/admin/expenses.route.js), e.g. links shared from the web.
    if (segments.firstOrNull() == "admin") {
        val query = uri.rawQuery.orEmpty().split('&').mapNotNull { it.split('=', limit = 2).takeIf { pair -> pair.size == 2 } }
            .associate { (key, value) -> key to java.net.URLDecoder.decode(value, "UTF-8") }
        return when {
            segments == listOf("admin", "expenses-list") -> Routes.ADMIN_EXPENSES
            segments == listOf("admin", "expenses", "add") -> Routes.ADMIN_EXPENSE_ADD
            segments.size == 4 && segments[1] == "expenses" && segments[2] == "detail" -> Routes.adminExpense(segments[3])
            segments.size == 5 && segments[2] == "detail" && segments[4] == "addExpenses" -> Routes.adminExpenseEntryAdd(segments[3])
            segments.size == 5 && segments[2] == "detail" && segments[4] == "addPaidAmounts" -> Routes.adminExpensePaidAdd(segments[3])
            segments.size == 5 && segments[2] == "detail" && segments[4] == "editExpenses" ->
                query["expenseId"]?.let { Routes.adminExpenseEntryEdit(segments[3], it) }
            else -> null
        }
    }
    return when {
        segments.isEmpty() -> Routes.HOME
        segments.size == 2 && segments[0] == "blog" -> Routes.blog(segments[1])
        segments.size == 2 && segments[0] == "project" -> Routes.project(segments[1])
        segments.size == 2 && segments[0] == "add-project-history" -> Routes.projectHistoryAdd(segments[1])
        segments == listOf("profile", "edit") -> Routes.PROFILE_EDIT
        segments == listOf("profile", "password-change") -> Routes.PASSWORD_CHANGE
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
            "login" -> Routes.LOGIN
            "signup" -> Routes.SIGNUP
            "profile" -> Routes.PROFILE
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
