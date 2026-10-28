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
import ge.renovatori.app.ui.HtmlContent
import ge.renovatori.app.ui.LatestBlogsSidebar
import ge.renovatori.app.ui.LatestContent
import ge.renovatori.app.ui.LatestProjectsSection
import ge.renovatori.app.ui.LoadingState
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.fetchLatestContent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val notFound: Boolean = false,
    val error: String? = null,
    val latest: LatestContent = LatestContent()
)

class ProjectDetailViewModel(private val id: String, private val api: ApiClient = ApiClient.shared) : ViewModel() {

    private val _state = MutableStateFlow(ProjectDetailUiState())
    val state: StateFlow<ProjectDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // The "latest" blocks load alongside the project; their failures never block the page.
            val latest = async { api.fetchLatestContent(projectCount = 3, blogCount = 10) }
            try {
                val project = api.fetchProject(id)
                _state.update { it.copy(isLoading = false, project = project, latest = latest.await()) }
            } catch (e: ApiError) {
                latest.cancel()
                _state.update {
                    it.copy(isLoading = false, notFound = e.code == ApiError.CODE_NOT_FOUND, error = e.message)
                }
            }
        }
    }
}

/**
 * Project page (web `/project/:id`), public view: photo gallery with a full-screen viewer, title,
 * location/area, HTML description, latest projects and posts. Client/admin-only blocks (finances,
 * history) need sign-in and are not part of the public app yet.
 */
@Composable
fun ProjectDetailScreen(id: String, viewModel: ProjectDetailViewModel = viewModel(key = "project-$id") { ProjectDetailViewModel(id) }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val project = state.project
    var viewerPage by rememberSaveable { mutableStateOf<Int?>(null) }

    when {
        state.isLoading -> LoadingState()
        state.notFound -> NotFoundState("პროექტი ვერ მოიძებნა.", "პროექტების სია") { navigator.navigate(Routes.PROJECT_LIST) }
        project == null -> ErrorState(state.error.orEmpty(), onRetry = viewModel::load)
        else -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                if (project.images.isNotEmpty()) {
                    Gallery(project.images, project.title, onOpen = { viewerPage = it })
                }
                PageTitle(project.title)
                HorizontalDivider()
                ProjectChips(project.location, project.area, withLabels = true)
                HtmlContent(project.description)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                LatestProjectsSection("ბოლო 3 პროექტი", state.latest.projects)
                LatestBlogsSidebar(state.latest.blogs)
            }

            viewerPage?.let { page ->
                FullScreenGallery(project.images, initialPage = page, onDismiss = { viewerPage = null })
            }
        }
    }
}

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

@Composable
private fun PageCounter(page: Int, total: Int, modifier: Modifier = Modifier) {
    Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Text("${page + 1} / $total", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** Full-screen dark viewer with pinch / double-tap zoom (web fullscreen modal + Swiper zoom). */
@Composable
private fun FullScreenGallery(images: List<ProjectImage>, initialPage: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val pagerState = rememberPagerState(initialPage = initialPage) { images.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                ZoomableImage(ApiConfig.imageUrl(images[page].url))
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding()) {
                Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = Color.White)
            }
            PageCounter(pagerState.currentPage, images.size, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp))
        }
    }
}

@Composable
private fun ZoomableImage(url: String?) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        // Back at 1x the photo snaps to center so the pager can take horizontal swipes again.
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    // Double tap toggles between fit and 2.5x.
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                    }
                })
            }
            // Panning is only taken while zoomed in, otherwise horizontal drags page the gallery.
            .transformable(transformState, canPan = { scale > 1f })
    ) {
        RemoteImage(
            url = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
        )
    }
}
