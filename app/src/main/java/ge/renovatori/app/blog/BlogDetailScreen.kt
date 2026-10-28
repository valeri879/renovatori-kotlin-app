package ge.renovatori.app.blog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiConfig
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.network.RemoteImage
import ge.renovatori.app.ui.ErrorState
import ge.renovatori.app.ui.HtmlContent
import ge.renovatori.app.ui.LatestBlogsSidebar
import ge.renovatori.app.ui.LatestContent
import ge.renovatori.app.ui.LatestProjectsSection
import ge.renovatori.app.ui.LoadingState
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.fetchLatestContent
import ge.renovatori.app.ui.formatDate
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlogDetailUiState(
    val isLoading: Boolean = true,
    val blog: Blog? = null,
    val notFound: Boolean = false,
    val error: String? = null,
    val latest: LatestContent = LatestContent()
)

class BlogDetailViewModel(private val id: String, private val api: ApiClient = ApiClient.shared) : ViewModel() {

    private val _state = MutableStateFlow(BlogDetailUiState())
    val state: StateFlow<BlogDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // The sidebar loads alongside the post; its failures never block the article.
            val latest = async { api.fetchLatestContent(projectCount = 3, blogCount = 10, excludeBlogId = id) }
            try {
                val blog = api.fetchBlog(id)
                _state.update { it.copy(isLoading = false, blog = blog, latest = latest.await()) }
            } catch (e: ApiError) {
                latest.cancel()
                _state.update {
                    it.copy(isLoading = false, notFound = e.code == ApiError.CODE_NOT_FOUND, error = e.message)
                }
            }
        }
    }
}

/** Blog post (web `/blog/:id`): cover, title, date, HTML body, latest projects and posts. */
@Composable
fun BlogDetailScreen(id: String, viewModel: BlogDetailViewModel = viewModel(key = "blog-$id") { BlogDetailViewModel(id) }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val blog = state.blog

    when {
        state.isLoading -> LoadingState()
        // The web redirects unknown posts to the list; the app explains and offers the list.
        state.notFound -> NotFoundState("ბლოგი ვერ მოიძებნა.", "ბლოგების სია") { navigator.navigate(Routes.BLOG_LIST) }
        blog == null -> ErrorState(state.error.orEmpty(), onRetry = viewModel::load)
        else -> Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            if (blog.coverImageUrl != null) {
                RemoteImage(
                    url = ApiConfig.imageUrl(blog.coverImageUrl),
                    contentDescription = "${blog.title} სურათი",
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
                )
            }
            PageTitle(blog.title)
            Text(
                "📅 ${formatDate(blog.updatedAt ?: blog.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            HtmlContent(blog.description)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            LatestProjectsSection("ბოლო 3 პროექტი", state.latest.projects)
            LatestBlogsSidebar(state.latest.blogs)
        }
    }
}

/** Message + button for a missing (or unpublished) item. */
@Composable
fun NotFoundState(message: String, actionLabel: String, onAction: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(message, textAlign = TextAlign.Center)
        Button(onClick = onAction) { Text(actionLabel) }
    }
}
