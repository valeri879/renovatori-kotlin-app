package ge.renovatori.app.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.blog.NotFoundState
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiConfig
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.network.RemoteImage
import ge.renovatori.app.ui.ErrorState
import ge.renovatori.app.ui.FullScreenImageViewer
import ge.renovatori.app.ui.HtmlContent
import ge.renovatori.app.ui.LatestBlogsSidebar
import ge.renovatori.app.ui.LatestContent
import ge.renovatori.app.ui.LatestProjectsSection
import ge.renovatori.app.ui.LoadingState
import ge.renovatori.app.ui.PageCounter
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.fetchLatestContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import ge.renovatori.app.auth.AuthSession
import ge.renovatori.app.auth.AuthState
import ge.renovatori.app.ui.formatDate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val view: ProjectView? = null,
    val notFound: Boolean = false,
    val error: String? = null,
    val latest: LatestContent = LatestContent(),
    val isDeletingHistory: Boolean = false,
    val historyError: String? = null
)

class ProjectDetailViewModel(private val id: String, private val api: ApiClient = ApiClient.shared) : ViewModel() {

    private val _state = MutableStateFlow(ProjectDetailUiState())
    val state: StateFlow<ProjectDetailUiState> = _state.asStateFlow()

    init {
        // Signing in or out changes what this viewer may see, so the page reloads (also the first load).
        viewModelScope.launch {
            AuthSession.state.map { (it as? AuthState.Authenticated)?.user?.id }.distinctUntilChanged().collect { load() }
        }
        // History added or edited on its form shows up when coming back.
        viewModelScope.launch { HistoryChanges.version.drop(1).collect { load(refreshing = true) } }
    }

    fun load(refreshing: Boolean = false) {
        _state.update { it.copy(isLoading = !refreshing && it.view == null, isRefreshing = refreshing, error = null, notFound = false) }
        viewModelScope.launch {
            val signedIn = AuthSession.state.value is AuthState.Authenticated
            // The "latest" blocks load alongside the project; their failures never block the page.
            val latest = async { api.fetchLatestContent(projectCount = 3, blogCount = 10) }
            try {
                val view = api.fetchProjectView(id, signedIn)
                _state.update { it.copy(isLoading = false, isRefreshing = false, view = view, latest = latest.await()) }
            } catch (e: ApiError) {
                latest.cancel()
                _state.update {
                    it.copy(isLoading = false, isRefreshing = false, notFound = e.code == ApiError.CODE_NOT_FOUND, error = e.message)
                }
            }
        }
    }

    fun deleteHistory(historyId: String) {
        _state.update { it.copy(isDeletingHistory = true, historyError = null) }
        viewModelScope.launch {
            try {
                val history = api.deleteHistory(historyId)
                _state.update { state -> state.copy(isDeletingHistory = false, view = state.view?.copy(history = history)) }
            } catch (e: ApiError) {
                _state.update { it.copy(isDeletingHistory = false, historyError = e.message) }
            }
        }
    }
}

/**
 * Project page (web `/project/:id`): photo gallery with a full-screen viewer, title, location/area,
 * HTML description, latest projects and posts. Signed-in admins, moderators and the linked client
 * also get dates, client info and finances; admins and the client get the history (admins manage it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(id: String, viewModel: ProjectDetailViewModel = viewModel(key = "project-$id") { ProjectDetailViewModel(id) }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val view = state.view
    var viewer by rememberSaveable { mutableStateOf<Pair<String, Int>?>(null) }

    when {
        state.isLoading -> LoadingState()
        state.notFound -> NotFoundState("პროექტი ვერ მოიძებნა.", "პროექტების სია") { navigator.navigate(Routes.PROJECT_LIST) }
        view == null -> ErrorState(state.error.orEmpty(), onRetry = { viewModel.load() })
        else -> {
            val project = view.project
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.load(refreshing = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    if (project.images.isNotEmpty()) {
                        Gallery(project.images, project.title, onOpen = { viewer = GALLERY to it })
                    }
                    PageTitle(project.title)
                    // Dates are shown only to viewers with client access, like the web.
                    if (view.access.canViewClientInfo) {
                        Text("📅 დაწყება: ${formatDate(project.startDate)}")
                        Text("🏁 დასრულება: ${formatDate(project.endDate)}")
                    }
                    HorizontalDivider()
                    ProjectChips(project.location, project.area, withLabels = true)
                    if (view.access.canViewClientInfo) {
                        ClientInfoCard(view.client)
                        view.financialSummary?.let { FinanceCard(it, project) }
                    }
                    if (view.access.canManage) AdminMeta(project)
                    HtmlContent(project.description)
                    if (view.access.canViewHistory) {
                        HistoryCard(
                            history = view.history,
                            canManage = view.access.canManage,
                            isDeleting = state.isDeletingHistory,
                            error = state.historyError,
                            onAdd = { navigator.navigate(Routes.projectHistoryAdd(project.id)) },
                            onEdit = { entry -> navigator.navigate(Routes.projectHistoryEdit(project.id, entry.id)) },
                            onDelete = viewModel::deleteHistory,
                            onOpenImage = { entry, index -> viewer = entry.id to index }
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    LatestProjectsSection("ბოლო 3 პროექტი", state.latest.projects)
                    LatestBlogsSidebar(state.latest.blogs)
                }
            }

            viewer?.let { (source, index) ->
                val images = if (source == GALLERY) project.images else view.history.firstOrNull { it.id == source }?.images.orEmpty()
                FullScreenImageViewer(images.map { ApiConfig.imageUrl(it.url) }, initialPage = index, onDismiss = { viewer = null })
            }
        }
    }
}

/** Key of the main photo gallery in the viewer state (history entries use their id). */
private const val GALLERY = "gallery"

/** Swipeable photos with an "n / total" counter (web Swiper); tapping opens the full-screen viewer. */
@Composable
private fun Gallery(images: List<ProjectImage>, title: String, onOpen: (Int) -> Unit) {
    val pagerState = rememberPagerState { images.size }

    Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(8.dp))) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            RemoteImage(
                url = ApiConfig.imageUrl(images[page].url),
                contentDescription = "$title — ფოტო ${page + 1}",
                modifier = Modifier.fillMaxSize().clickable { onOpen(page) }
            )
        }
        PageCounter(pagerState.currentPage, images.size, Modifier.align(Alignment.BottomCenter).padding(8.dp))
    }
}

