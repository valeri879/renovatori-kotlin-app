package ge.renovatori.app.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.blog.listFooter
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.network.LoadMoreEffect
import ge.renovatori.app.network.PagedLoader
import ge.renovatori.app.ui.EmptyText
import ge.renovatori.app.ui.ErrorState
import ge.renovatori.app.ui.LoadingState
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.formatMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Fallback when `/admin/expenses/statuses` can't be loaded (backend `expenseStatuses`). */
private val defaultStatuses = listOf("დასაწყები", "მიმდინარე", "დასრულებული", "შეწყვეტილი")

class ExpenseListViewModel(private val api: ApiClient = ApiClient.shared) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _statuses = MutableStateFlow(defaultStatuses)
    val statuses: StateFlow<List<String>> = _statuses.asStateFlow()

    /** One-off message (e.g. a failed delete), shown until dismissed. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val pages = PagedLoader(viewModelScope, key = ExpenseCard::id) { page -> api.fetchExpenses(page, _status.value) }

    init {
        pages.reload()
        viewModelScope.launch {
            try {
                _statuses.value = api.fetchExpenseStatuses()
            } catch (e: ApiError) {
                // The built-in list matches the server; nothing to report.
            }
        }
        // Adds and deletes elsewhere (form, detail page) refresh the list.
        viewModelScope.launch { ExpenseChanges.version.drop(1).collect { pages.reload(refreshing = true) } }
    }

    /** Status filter (web "ყველა" + one button per status); null lists every project. */
    fun setStatus(value: String?) {
        if (value == _status.value) return
        _status.value = value
        pages.reload()
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                api.deleteExpense(id)
                ExpenseChanges.changed()
            } catch (e: ApiError) {
                _message.value = e.message
            }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}

/** Admin project list (web `/admin/expenses-list`): status filter, finance cards, view / delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(isAdmin: Boolean, viewModel: ExpenseListViewModel = viewModel()) {
    val state by viewModel.pages.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val listState = rememberLazyListState()
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    LoadMoreEffect(listState, viewModel.pages::loadMore)
    // A new filter starts at the top of the list.
    LaunchedEffect(status) { listState.scrollToItem(0) }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.pages.reload(refreshing = true) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PageTitle("პროექტების სია", Modifier.weight(1f))
                        // Moderators only view; adding is for admins (server: 403 for others).
                        if (isAdmin) {
                            Button(onClick = { navigator.navigate(Routes.ADMIN_EXPENSE_ADD) }) { Text("პროექტის დამატება") }
                        }
                    }
                    if (!state.isLoading && state.error == null) {
                        Text("სულ ჩანაწერები: ${state.totalItems}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = status == null, onClick = { viewModel.setStatus(null) }, label = { Text("ყველა") })
                        statuses.forEach { value ->
                            FilterChip(selected = status == value, onClick = { viewModel.setStatus(value) }, label = { Text(statusLabel(value)) })
                        }
                    }
                    message?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable { viewModel.dismissMessage() })
                    }
                }
            }

            when {
                state.isLoading -> item(key = "loading") { LoadingState(Modifier.padding(top = 48.dp)) }
                state.error != null && state.items.isEmpty() -> item(key = "error") {
                    ErrorState(state.error.orEmpty(), onRetry = { viewModel.pages.reload() })
                }
                state.items.isEmpty() -> item(key = "empty") { EmptyText("ჩანაწერები არ მოიძებნა.") }
                else -> {
                    itemsIndexed(state.items, key = { _, card -> card.id }) { index, card ->
                        ExpenseCardView(
                            card = card,
                            number = index + 1,
                            isAdmin = isAdmin,
                            onOpen = { navigator.navigate(Routes.adminExpense(card.id)) },
                            onDelete = { pendingDelete = card.id }
                        )
                    }
                    listFooter(state.isLoadingMore, state.loadMoreError, onRetry = viewModel.pages::loadMore)
                }
            }
        }
    }

    pendingDelete?.let { id ->
        ConfirmDeleteDialog(
            message = "ნამდვილად გსურთ ჩანაწერის წაშლა? ეს მოქმედება შეუქცევადია.",
            onConfirm = {
                pendingDelete = null
                viewModel.delete(id)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun ExpenseCardView(card: ExpenseCard, number: Int, isAdmin: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("#$number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(card.projectName.ifBlank { "-" }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            StatusBadge(card.projectStatus)
        }
        HorizontalDivider()
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                LabeledValue("ჯამური ღირებულება", formatMoney(card.totalAmount, 2), modifier = Modifier.weight(1f))
                LabeledValue("ჯამური ხარჯი", formatMoney(card.totalExpenses, 2), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
            Row {
                LabeledValue("დარჩენილი თანხა", formatMoney(card.remainingAmount, 2), signColor(1.0), Modifier.weight(1f))
                LabeledValue("მიმდინარე თანხა", formatMoney(card.currentAmount, 2), signColor(card.currentAmount), Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    Text("პროექტის პროგრესი", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(card.progress.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ProjectProgressBar(card.progress)
            }
        }
        HorizontalDivider()
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("ნახვა") }
            if (isAdmin) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    modifier = Modifier.weight(1f)
                ) { Text("წაშლა") }
            }
        }
    }
}
