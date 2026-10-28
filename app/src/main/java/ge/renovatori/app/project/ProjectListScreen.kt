package ge.renovatori.app.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.blog.listFooter
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiConfig
import ge.renovatori.app.network.LoadMoreEffect
import ge.renovatori.app.network.PagedLoader
import ge.renovatori.app.network.RemoteImage
import ge.renovatori.app.ui.EmptyText
import ge.renovatori.app.ui.ErrorState
import ge.renovatori.app.ui.LoadingState
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.formatDate

class ProjectListViewModel(private val api: ApiClient = ApiClient.shared) : ViewModel() {
    val pages = PagedLoader(viewModelScope, key = ProjectCard::id) { page -> api.fetchProjects(page = page) }

    init {
        pages.reload()
    }
}

/** Project list (web `/projects`): infinitely scrolling cards, newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(viewModel: ProjectListViewModel = viewModel()) {
    val state by viewModel.pages.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val listState = rememberLazyListState()

    LoadMoreEffect(listState, viewModel.pages::loadMore)

    when {
        state.isLoading -> LoadingState()
        state.error != null && state.items.isEmpty() -> ErrorState(state.error.orEmpty(), onRetry = { viewModel.pages.reload() })
        else -> PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.pages.reload(refreshing = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Empty only after a successful load; errors are handled above.
                if (state.items.isEmpty()) {
                    item(key = "empty") { EmptyText("არ არის დამატებული პროექტები.") }
                }
                items(state.items, key = { it.id }) { project ->
                    ProjectListCard(project) { navigator.navigate(Routes.project(project.id)) }
                }
                listFooter(state.isLoadingMore, state.loadMoreError, onRetry = viewModel.pages::loadMore)
            }
        }
    }
}

@Composable
private fun ProjectListCard(project: ProjectCard, onClick: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        if (project.coverImageUrl != null) {
            RemoteImage(
                url = ApiConfig.imageUrl(project.coverImageUrl),
                contentDescription = project.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (project.metaTitle.isNotBlank()) {
                Text(project.metaTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(project.metaDescription, style = MaterialTheme.typography.bodyMedium)
            ProjectChips(project.location, project.area)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("📅 დაწყება: ${formatDate(project.startDate)}", style = MaterialTheme.typography.bodyMedium)
            Text("🏁 დასრულება: ${formatDate(project.endDate)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Location and area badges (web `badge text-bg-light`); [withLabels] adds the detail page's prefixes. */
@Composable
fun ProjectChips(location: String, area: Double?, withLabels: Boolean = false) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Badge(if (withLabels) "📍მისამართი: $location" else "📍 $location")
        Badge(if (withLabels) "📐ფართი: ${formatArea(area)} მ²" else "📐 ${formatArea(area)} მ²")
    }
}

@Composable
private fun Badge(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
