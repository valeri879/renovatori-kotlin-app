package ge.renovatori.app.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ge.renovatori.app.blog.BlogCard
import ge.renovatori.app.blog.fetchBlogs
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.partner.Partner
import ge.renovatori.app.partner.fetchPartners
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Dynamic parts of the home page: the team and the three latest posts (the rest is static). */
data class HomeUiState(
    val isLoadingBlogs: Boolean = true,
    val latestBlogs: List<BlogCard> = emptyList(),
    val blogsError: String? = null,
    /** Empty while loading or after a failed load: the section is hidden then, as on the web when there is no team. */
    val partners: List<Partner> = emptyList()
)

class HomeViewModel(private val api: ApiClient = ApiClient.shared) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Loads (or reloads, for pull-to-refresh) every dynamic section. */
    fun load() {
        loadBlogs()
        loadPartners()
    }

    fun loadBlogs() {
        _state.update { it.copy(isLoadingBlogs = true, blogsError = null) }
        viewModelScope.launch {
            try {
                // Same query as the web main page: 3 newest published posts.
                val blogs = api.fetchBlogs(page = 1, pageSize = LATEST_BLOGS_COUNT).items
                _state.update { it.copy(isLoadingBlogs = false, latestBlogs = blogs) }
            } catch (e: ApiError) {
                _state.update { it.copy(isLoadingBlogs = false, blogsError = e.message) }
            }
        }
    }

    private fun loadPartners() {
        viewModelScope.launch {
            try {
                val partners = api.fetchPartners()
                _state.update { it.copy(partners = partners) }
            } catch (e: ApiError) {
                // The team is decorative: keep whatever is shown instead of surfacing an error.
                Log.w(TAG, "Loading partners failed: ${e.code}")
            }
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
        const val LATEST_BLOGS_COUNT = 3
    }
}
