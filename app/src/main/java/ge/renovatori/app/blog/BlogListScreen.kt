package ge.renovatori.app.blog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.LoadMoreEffect
import ge.renovatori.app.network.PagedLoader
import ge.renovatori.app.ui.ContentCard
import ge.renovatori.app.ui.EmptyText
import ge.renovatori.app.ui.ErrorState
import ge.renovatori.app.ui.LoadingState
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlogListViewModel(private val api: ApiClient = ApiClient.shared) : ViewModel() {

    private val _newestFirst = MutableStateFlow(true)
    val newestFirst: StateFlow<Boolean> = _newestFirst.asStateFlow()

    val pages = PagedLoader(viewModelScope, key = BlogCard::id) { page ->
        api.fetchBlogs(page = page, newestFirst = _newestFirst.value)
    }

    init {
        pages.reload()
    }

    /** Changing the order starts again from the first page, like the web sort links. */
    fun setNewestFirst(value: Boolean) {
        if (value == _newestFirst.value) return
        _newestFirst.value = value
        pages.reload()
    }
}

/** Blog list (web `/blog-list`): sort by date and an infinitely scrolling list of posts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogListScreen(viewModel: BlogListViewModel = viewModel()) {
    val state by viewModel.pages.state.collectAsStateWithLifecycle()
    val newestFirst by viewModel.newestFirst.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val listState = rememberLazyListState()

    LoadMoreEffect(listState, viewModel.pages::loadMore)

    PullToRefreshBox(
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
            item(key = "sort") { SortDropdown(newestFirst, viewModel::setNewestFirst) }

            when {
                state.isLoading -> item(key = "loading") { LoadingState(Modifier.padding(top = 48.dp)) }
                state.error != null && state.items.isEmpty() -> item(key = "error") {
                    ErrorState(state.error.orEmpty(), onRetry = { viewModel.pages.reload() })
                }
                state.items.isEmpty() -> item(key = "empty") { EmptyText("ბლოგები არ მოიძებნა.") }
                else -> {
                    items(state.items, key = { it.id }) { blog ->
                        ContentCard(
                            title = blog.title,
                            onClick = { navigator.navigate(Routes.blog(blog.id)) },
                            imageUrl = blog.coverImageUrl
                        )
                    }
                    listFooter(state.isLoadingMore, state.loadMoreError, onRetry = viewModel.pages::loadMore)
                }
            }
        }
    }
}

/** Spinner while the next page loads, or its error with a retry. Shared by the paginated lists. */
fun androidx.compose.foundation.lazy.LazyListScope.listFooter(isLoadingMore: Boolean, error: String?, onRetry: () -> Unit) {
    if (isLoadingMore) {
        item(key = "load-more") {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    } else if (error != null) {
        item(key = "load-more-error") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                TextButton(onClick = onRetry) { Text("ხელახლა ცდა") }
            }
        }
    }
}

/** Web "სორტირება: ახალი / ძველი" dropdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortDropdown(newestFirst: Boolean, onChange: (Boolean) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val options = listOf(true to "ახალი", false to "ძველი")

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.first { it.first == newestFirst }.second,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("სორტირება") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
