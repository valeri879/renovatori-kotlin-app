package ge.renovatori.app.network

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagedState<T>(
    val items: List<T> = emptyList(),
    /** First page in flight with nothing to show yet (full-screen spinner). */
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** First-page failure; shown full screen when [items] is empty. */
    val error: String? = null,
    /** Next-page failure; shown at the end of the list with a retry. */
    val loadMoreError: String? = null,
    val nextPage: Int? = null
)

/**
 * Infinite-scroll pagination over a `{ data, pagination }` endpoint: [reload] fetches page 1,
 * [loadMore] appends `nextPage` while the server reports `hasNext`. Items are de-duplicated by
 * [key], since new posts can shift page boundaries between requests.
 */
class PagedLoader<T>(
    private val scope: CoroutineScope,
    private val key: (T) -> String,
    private val fetch: suspend (page: Int) -> Paged<T>
) {
    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()
    private var job: Job? = null

    /** Loads page 1; [refreshing] keeps the current list visible under the pull-to-refresh indicator. */
    fun reload(refreshing: Boolean = false) {
        job?.cancel()
        _state.update {
            it.copy(isLoading = !refreshing, isRefreshing = refreshing, isLoadingMore = false, error = null, loadMoreError = null)
        }
        job = scope.launch {
            try {
                val page = fetch(1)
                _state.value = PagedState(
                    items = page.items.distinctBy(key),
                    isLoading = false,
                    nextPage = page.pagination.currentPage + 1
                ).let { if (page.pagination.hasNext) it else it.copy(nextPage = null) }
            } catch (e: ApiError) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        val nextPage = current.nextPage
        // Nothing more to load, or a request is already running.
        if (nextPage == null || current.isLoading || current.isRefreshing || current.isLoadingMore) return

        _state.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        job = scope.launch {
            try {
                val page = fetch(nextPage)
                _state.update {
                    it.copy(
                        items = (it.items + page.items).distinctBy(key),
                        isLoadingMore = false,
                        nextPage = if (page.pagination.hasNext) page.pagination.currentPage + 1 else null
                    )
                }
            } catch (e: ApiError) {
                _state.update { it.copy(isLoadingMore = false, loadMoreError = e.message) }
            }
        }
    }
}

/** Calls [onLoadMore] when the list is scrolled within a few items of its end. */
@Composable
fun LoadMoreEffect(listState: LazyListState, onLoadMore: () -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}
