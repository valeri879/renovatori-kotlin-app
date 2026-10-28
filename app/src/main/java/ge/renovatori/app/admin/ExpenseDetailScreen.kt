package ge.renovatori.app.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import ge.renovatori.app.ui.LoadingState
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.formatDate
import ge.renovatori.app.ui.formatDateTime
import ge.renovatori.app.ui.formatDollars
import ge.renovatori.app.ui.formatMoney
import ge.renovatori.app.ui.formatPhone
import ge.renovatori.app.ui.phoneUri
import ge.renovatori.app.ui.theme.success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Web hint on the disabled "add" buttons of a project that is not „მიმდინარე“. */
const val NOT_ONGOING_HINT = "ხარჯის/ჩარიცხვის დასამატებლად ან რედაქტირებისთვის შეცვალეთ პროექტის სტატუსი „მიმდინარე\"-ზე"

data class ExpenseDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val expense: ExpenseDetail? = null,
    val notFound: Boolean = false,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    val deleted: Boolean = false
)

class ExpenseDetailViewModel(private val id: String, private val api: ApiClient = ApiClient.shared) : ViewModel() {
    private val _state = MutableStateFlow(ExpenseDetailUiState())
    val state: StateFlow<ExpenseDetailUiState> = _state.asStateFlow()

    init {
        load()
        // Rows added or edited on the entry form show up when coming back.
        viewModelScope.launch { ExpenseChanges.version.drop(1).collect { load(refreshing = true) } }
    }

    fun load(refreshing: Boolean = false) {
        _state.update { it.copy(isLoading = !refreshing && it.expense == null, isRefreshing = refreshing, error = null) }
        viewModelScope.launch {
            try {
                val expense = api.fetchExpense(id)
                _state.update { it.copy(isLoading = false, isRefreshing = false, expense = expense) }
            } catch (e: ApiError) {
                _state.update {
                    it.copy(isLoading = false, isRefreshing = false, notFound = e.code == ApiError.CODE_NOT_FOUND, error = e.message)
                }
            }
        }
    }

    fun delete() {
        _state.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            try {
                api.deleteExpense(id)
                ExpenseChanges.changed()
                _state.update { it.copy(isDeleting = false, deleted = true) }
            } catch (e: ApiError) {
                _state.update { it.copy(isDeleting = false, deleteError = e.message) }
            }
        }
    }
}

// Section order in the list below, for the web "section-nav" jump buttons.
private const val SECTION_INFO = 2
private const val SECTION_FINANCES = 3
private const val SECTION_PROGRESS = 4
private const val SECTION_TYPES = 5
private const val SECTION_PAID = 6
private const val SECTION_EXPENSES = 7

private val typeColors = listOf(
    Color(0xFF0D6EFD), Color(0xFF198754), Color(0xFFDC3545), Color(0xFFFFC107), Color(0xFF0DCAF0),
    Color(0xFF6C757D), Color(0xFF212529), Color(0xFF9EC5FE), Color(0xFFA3CFBB), Color(0xFFF1AEB5),
    Color(0xFFFFE69C), Color(0xFF9EEAF9), Color(0xFFC4C8CB), Color(0xFFADB5BD)
)
private const val UNTYPED_TITLE = "ტიპი არ არის მითითებული"

/** Expense project page (web `/admin/expenses/detail/:id`). Admin-only actions follow `permissions`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(id: String, viewModel: ExpenseDetailViewModel = viewModel(key = "expense-$id") { ExpenseDetailViewModel(id) }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val expense = state.expense

    // Deleted: back to the list, like the web redirect.
    if (state.deleted) LaunchedEffect(Unit) { navigator.navigate(Routes.ADMIN_EXPENSES) }

    when {
        state.isLoading -> LoadingState()
        state.notFound -> NotFoundState("პროექტი ვერ მოიძებნა.", "სიაში დაბრუნება") { navigator.navigate(Routes.ADMIN_EXPENSES) }
        expense == null -> ErrorState(state.error.orEmpty(), onRetry = { viewModel.load() })
        else -> PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.load(refreshing = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            ExpenseDetailContent(
                expense = expense,
                isDeleting = state.isDeleting,
                deleteError = state.deleteError,
                onDelete = viewModel::delete
            )
        }
    }
}

@Composable
private fun ExpenseDetailContent(expense: ExpenseDetail, isDeleting: Boolean, deleteError: String?, onDelete: () -> Unit) {
    val navigator = LocalAppNavigator.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var typeFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var viewer by rememberSaveable { mutableStateOf<Pair<String, Int>?>(null) }
    val rows = expense.expenses.filter { typeFilter == null || (it.expenseType?.title ?: UNTYPED_TITLE) == typeFilter }
    val canAdd = expense.canManage && expense.canAddEntries

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            // Room for the floating "add" button at the bottom.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ხარჯების დეტალები", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(expense.projectName.ifBlank { "-" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    StatusBadge(expense.projectStatus)
                    // Deleting is for admins only (server: 403 for moderators).
                    if (expense.canManage) {
                        Button(
                            onClick = { confirmDelete = true },
                            enabled = !isDeleting,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                        ) { Text("წაშლა") }
                    }
                    deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            item(key = "nav") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ℹ️ შესახებ" to SECTION_INFO, "💰 ფინანსები" to SECTION_FINANCES, "📈 პროგრესი" to SECTION_PROGRESS,
                        "📊 ტიპები" to SECTION_TYPES, "💵 ჩარიცხვები" to SECTION_PAID, "🧾 ხარჯები" to SECTION_EXPENSES
                    ).forEach { (label, index) ->
                        AssistChip(onClick = { scope.launch { listState.animateScrollToItem(index) } }, label = { Text(label) })
                    }
                }
            }
            item(key = "info") { InfoCard(expense) }
            item(key = "finances") { FinanceCards(expense) }
            item(key = "progress") {
                SectionCard("პროექტის პროგრესი") {
                    Row {
                        Text("${formatDate(expense.startDate)} - ${formatDate(expense.endDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(expense.progress.label, fontWeight = FontWeight.Bold)
                    }
                    ProjectProgressBar(expense.progress, Modifier.height(10.dp))
                    Text("${expense.progress.percent}%", style = MaterialTheme.typography.labelSmall)
                }
            }
            item(key = "types") { TypeBreakdown(expense.expenseTypeSummary, typeFilter) { typeFilter = if (typeFilter == it) null else it } }
            item(key = "paid") {
                PaidAmountsCard(expense, canAdd = canAdd, onAdd = { navigator.navigate(Routes.adminExpensePaidAdd(expense.id)) })
            }
            item(key = "expenses-header") {
                ExpensesHeader(
                    expense = expense,
                    rows = rows,
                    typeFilter = typeFilter,
                    canAdd = canAdd,
                    onAdd = { navigator.navigate(Routes.adminExpenseEntryAdd(expense.id)) },
                    onClearFilter = { typeFilter = null }
                )
            }
            if (rows.isEmpty()) {
                item(key = "expenses-empty") {
                    Text("ხარჯის ჩანაწერები ჯერ არ დამატებულა.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            itemsIndexed(rows, key = { _, entry -> entry.id }) { index, entry ->
                ExpenseRow(
                    entry = entry,
                    number = index + 1,
                    canEdit = expense.canManage,
                    onEdit = { navigator.navigate(Routes.adminExpenseEntryEdit(expense.id, entry.id)) },
                    onOpenImage = { image -> viewer = entry.id to image }
                )
            }
        }

        // Web fixed bottom bar (admins): add expense / add payment, disabled unless the project is „მიმდინარე“.
        if (expense.canManage) {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { navigator.navigate(Routes.adminExpenseEntryAdd(expense.id)) },
                        enabled = canAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                        modifier = Modifier.weight(1f)
                    ) { Text("ხარჯის დამატება") }
                    Button(
                        onClick = { navigator.navigate(Routes.adminExpensePaidAdd(expense.id)) },
                        enabled = canAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF198754), contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) { Text("ჩარიცხვის დამატება") }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            message = "ნამდვილად გსურთ ამ ჩანაწერის წაშლა? ეს ოპერაცია აღარ დაბრუნდება.",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
    viewer?.let { (entryId, index) ->
        val images = expense.expenses.firstOrNull { it.id == entryId }?.images.orEmpty()
        FullScreenImageViewer(images.map { ApiConfig.imageUrl(it.url) }, initialPage = index, onDismiss = { viewer = null })
    }
}

@Composable
private fun SectionCard(title: String, trailing: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** Web accordion "პროექტის დეტალური ინფორმაცია" (collapsed by default). */
@Composable
private fun InfoCard(expense: ExpenseDetail) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val navigator = LocalAppNavigator.current

    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp)
        ) {
            Text("პროექტის დეტალური ინფორმაცია", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.rotate(if (expanded) 180f else 0f))
        }
        AnimatedVisibility(expanded) {
            Column {
                HorizontalDivider()
                InfoRow("პროექტის სახელი", expense.projectName.ifBlank { "-" })
                InfoRow("პროექტის სტატუსი", statusLabel(expense.projectStatus))
                InfoRow("საჯარო პროექტის ID", expense.publicProjectId ?: "-", onClick = expense.publicProjectId?.let { { navigator.navigate(Routes.project(it)) } })
                InfoRow("პროექტის მისამართი", expense.projectAddress.ifBlank { "-" })
                InfoRow("დამკვეთის ID", expense.clientId ?: "-")
                InfoRow("დამკვეთის სახელი/გვარი", expense.clientName.ifBlank { "-" })
                val tel = phoneUri(expense.clientPhone)
                InfoRow("დამკვეთის ტელეფონი", if (tel != null) formatPhone(expense.clientPhone) else "-", onClick = tel?.let { { uriHandler.openUri(it) } })
                InfoRow(
                    "დამკვეთის ელფოსტა", expense.clientEmail.ifBlank { "-" },
                    onClick = expense.clientEmail.takeIf { it.isNotBlank() }?.let { { uriHandler.openUri("mailto:$it") } }
                )
                InfoRow("ფართობი", "${formatNumber(expense.area)} მ²")
                InfoRow("ფასი კვ.მ-ზე", formatMoney(expense.pricePerSquareArea, 2))
                InfoRow("ფასი კვ.მ-ზე ($)", formatDollars(expense.pricePerSquareAreaDollar))
                InfoRow("დარჩენილი თანხა", formatMoney(expense.remainingAmount, 2))
                InfoRow("დარჩენილი თანხა ($)", formatDollars(expense.remainingAmountDollar))
                InfoRow("პროექტის დაწყება", formatDate(expense.startDate))
                InfoRow("პროექტის დასრულება", formatDate(expense.endDate))
                InfoRow("შექმნის თარიღი", formatDateTime(expense.createdAt))
                InfoRow("განახლების თარიღი", formatDateTime(expense.updatedAt), divider = false)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null, divider: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.45f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
            modifier = Modifier.weight(0.55f)
        )
    }
    if (divider) HorizontalDivider()
}

/** The six web summary cards, two per row. */
@Composable
private fun FinanceCards(expense: ExpenseDetail) {
    val cards = listOf(
        Triple("ჯამური ღირებულება", "პროექტის სრული ღირებულება ლარში", formatMoney(expense.totalAmount, 2) to Color.Unspecified),
        Triple("ჯამური ღირებულება ($)", "პროექტის სრული ღირებულება დოლარში", formatDollars(expense.totalAmountDollar) to Color.Unspecified),
        Triple("ჯამური ხარჯი", "ყველა ხარჯის ჯამი ლარში", formatMoney(expense.totalExpenses, 2) to MaterialTheme.colorScheme.error),
        Triple("მიმდინარე თანხა", "ჩარიცხვების ჯამი მინუს ხარჯები", formatMoney(expense.currentAmount, 2) to signColor(expense.currentAmount)),
        Triple("დარჩენილი თანხა ($)", "დოლარში დარჩენილი გადასახდელი თანხა", formatDollars(expense.remainingAmountDollar) to MaterialTheme.colorScheme.error),
        Triple("მთლიანი მოგება", "ჯამური ღირებულება მინუს ხარჯები", formatMoney(expense.totalProfit, 2) to signColor(expense.totalProfit))
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (title, hint, value) ->
                    OutlinedCard(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value.first, style = MaterialTheme.typography.titleMedium, color = value.second)
                        }
                    }
                }
            }
        }
    }
}

/** Web "ხარჯების პროგრესი ტიპების მიხედვით": stacked bar + legend; tapping a type filters the rows. */
@Composable
private fun TypeBreakdown(summary: List<TypeShare>, selected: String?, onSelect: (String) -> Unit) {
    SectionCard("ხარჯების პროგრესი ტიპების მიხედვით") {
        if (summary.isEmpty()) {
            Text("ხარჯის ტიპების მიხედვით პროგრესი ჯერ ვერ გამოითვლება.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp))) {
            summary.forEachIndexed { index, share ->
                Box(Modifier.weight(share.percent.toFloat().coerceAtLeast(0.5f)).fillMaxSize().background(typeColors[index % typeColors.size]))
            }
        }
        summary.forEachIndexed { index, share ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected == share.title) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .clickable { onSelect(share.title) }
                    .padding(8.dp)
            ) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(typeColors[index % typeColors.size]))
                Column {
                    Text(share.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${formatMoney(share.amount, 2)} · ${Math.round(share.percent)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Web "ჩარიცხვები" table as rows (adding here; edit/delete come later). */
@Composable
private fun PaidAmountsCard(expense: ExpenseDetail, canAdd: Boolean, onAdd: () -> Unit) {
    SectionCard(
        "ჩარიცხვები",
        trailing = { Text("ჯამი: ${formatDollars(expense.totalPaidAmountDollar)}", color = MaterialTheme.colorScheme.success, fontWeight = FontWeight.Bold) }
    ) {
        // Admins add payments from here too (web header button); the not-ongoing hint sits under the expenses header.
        if (expense.canManage) {
            OutlinedButton(onClick = onAdd, enabled = canAdd) { Text("ჩარიცხვის დამატება") }
        }
        if (expense.paidAmounts.isEmpty()) {
            Text("ჩარიცხვები ჯერ არ დამატებულა.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        expense.paidAmounts.forEachIndexed { index, paid ->
            if (index > 0) HorizontalDivider()
            Row(verticalAlignment = Alignment.Top) {
                Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${formatMoney(paid.paidAmount, 2)} · ${formatDollars(paid.paidAmountDollar)}",
                        color = MaterialTheme.colorScheme.success,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(paid.comment.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall)
                }
                Text(formatDate(paid.paidAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
        Row {
            Text("ჯამი", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${formatMoney(expense.totalPaidAmount, 2)} · ${formatDollars(expense.totalPaidAmountDollar)}", color = MaterialTheme.colorScheme.success, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExpensesHeader(
    expense: ExpenseDetail,
    rows: List<ExpenseEntry>,
    typeFilter: String?,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onClearFilter: () -> Unit
) {
    val total = rows.sumOf { it.amount }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ხარჯები", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("ჯამი: ${formatMoney(total, 2)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            if (expense.canManage) {
                OutlinedButton(onClick = onAdd, enabled = canAdd) { Text("ხარჯის ჩანაწერის დამატება") }
            }
        }
        // Web tooltip on the disabled button, shown as text since there is no hover on a phone.
        if (expense.canManage && !canAdd) {
            Text(NOT_ONGOING_HINT, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        typeFilter?.let { filter ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp)
            ) {
                Text("ფილტრი: $filter (${rows.size})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onClearFilter) { Text("✕ ფილტრის მოხსნა") }
            }
        }
    }
}

@Composable
private fun ExpenseRow(entry: ExpenseEntry, number: Int, canEdit: Boolean, onEdit: () -> Unit, onOpenImage: (Int) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#$number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                Text(entry.expenseType?.title ?: "-", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(formatMoney(entry.amount, 2), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            Text("📅 ${formatDate(entry.expenseDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (entry.comment.isNotBlank()) Text(entry.comment, style = MaterialTheme.typography.bodyMedium)
            if (entry.images.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.images.forEachIndexed { index, image ->
                        RemoteImage(
                            url = ApiConfig.imageUrl(image.thumbUrl),
                            contentDescription = "ხარჯის სურათი ${index + 1}",
                            modifier = Modifier.size(width = 80.dp, height = 60.dp).clip(RoundedCornerShape(6.dp)).clickable { onOpenImage(index) }
                        )
                    }
                }
            }
            // Editing is for admins; the server allows it in any status, like the web.
            if (canEdit) {
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color(0xFF212529)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("რედაქტირება") }
            }
        }
    }
}

/** "45" for whole numbers, "45.5" otherwise (web prints the raw number). */
private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
