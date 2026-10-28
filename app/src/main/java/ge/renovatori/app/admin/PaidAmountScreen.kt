package ge.renovatori.app.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.blog.NotFoundState
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.ui.ErrorState
import ge.renovatori.app.ui.LoadingState
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.requiredLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaidAmountUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val notFound: Boolean = false,
    val expense: ExpenseDetail? = null,
    val form: PaidAmountForm = PaidAmountForm(),
    val errors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val formError: String? = null,
    /** Set after saving: whether the requested client e-mail went out (null when none was requested). */
    val savedEmailSent: Boolean? = null,
    val saved: Boolean = false
)

class PaidAmountViewModel(private val expenseId: String, private val api: ApiClient = ApiClient.shared) : ViewModel() {
    private val _state = MutableStateFlow(PaidAmountUiState())
    val state: StateFlow<PaidAmountUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val expense = api.fetchExpense(expenseId)
                _state.update { it.copy(isLoading = false, expense = expense) }
            } catch (e: ApiError) {
                _state.update { it.copy(isLoading = false, notFound = e.code == ApiError.CODE_NOT_FOUND, loadError = e.message) }
            }
        }
    }

    fun edit(field: String, change: (PaidAmountForm) -> PaidAmountForm) =
        _state.update { it.copy(form = change(it.form), errors = it.errors - field) }

    fun submit() {
        val form = _state.value.form
        // Same rules and wording as POST /admin/expenses/:id/paid-amounts.
        val errors = buildMap {
            amountError(form.paidAmount, required = true, missing = "ჩარიცხული თანხა სავალდებულოა.")?.let { put("paidAmount", it) }
            amountError(form.paidAmountDollar, required = false, missing = "")?.let { put("paidAmountDollar", it) }
            if (form.paidAt == null) put("paidAt", "ჩარიცხვის თარიღი სავალდებულოა.")
            if (form.comment.trim().length > 2000) put("comment", "კომენტარი არ უნდა აღემატებოდეს 2000 სიმბოლოს.")
        }
        _state.update { it.copy(errors = errors, formError = null) }
        if (errors.isNotEmpty()) return

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                val (_, emailSent) = api.addPaidAmount(expenseId, form)
                ExpenseChanges.changed()
                _state.update {
                    it.copy(isSubmitting = false, saved = true, savedEmailSent = if (form.sendClientEmail) emailSent else null)
                }
            } catch (e: ApiError) {
                _state.update {
                    if (!e.details.isNullOrEmpty()) it.copy(isSubmitting = false, errors = e.details)
                    else it.copy(isSubmitting = false, formError = e.message)
                }
            }
        }
    }
}

/** Add payment (web `/admin/expenses/detail/:id/addPaidAmounts`, admins only, project „მიმდინარე“). */
@Composable
fun PaidAmountScreen(expenseId: String) {
    val viewModel: PaidAmountViewModel = viewModel(key = "paid-amount-$expenseId") { PaidAmountViewModel(expenseId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val expense = state.expense

    if (state.saved) {
        LaunchedEffect(Unit) {
            // The server only e-mails clients with a verified address; say when that didn't happen.
            val message = when (state.savedEmailSent) {
                true -> "ჩარიცხვა დაემატა, დამკვეთს ელ-ფოსტა გაეგზავნა."
                // Toasts show two lines at most: no linked client, or the client's e-mail is unverified.
                false -> "ჩარიცხვა დაემატა. ელ-ფოსტა არ გაიგზავნა."
                null -> "ჩარიცხვა დაემატა."
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            navigator.back()
        }
    }

    when {
        state.isLoading -> LoadingState()
        state.notFound -> NotFoundState("პროექტი ვერ მოიძებნა.", "სიაში დაბრუნება") { navigator.navigate(Routes.ADMIN_EXPENSES) }
        expense == null -> ErrorState(state.loadError.orEmpty(), onRetry = viewModel::load)
        else -> Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            val form = state.form
            PageTitle("ჩარიცხვის დამატება")
            OutlinedButton(onClick = { navigator.back() }) { Text("დეტალზე დაბრუნება") }
            ProjectContextCard(expense) { navigator.back() }
            // Payments can only be added while the project is in progress (server: 409 not_ongoing).
            if (!expense.canAddEntries) Text(NOT_ONGOING_HINT, color = MaterialTheme.colorScheme.error)
            state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            FormTextField(requiredLabel("ჩარიცხული თანხა (₾)"), form.paidAmount, state.errors["paidAmount"], KeyboardType.Decimal) { v ->
                viewModel.edit("paidAmount") { it.copy(paidAmount = v) }
            }
            FormTextField(AnnotatedString("ჩარიცხული თანხა ($)"), form.paidAmountDollar, state.errors["paidAmountDollar"], KeyboardType.Decimal) { v ->
                viewModel.edit("paidAmountDollar") { it.copy(paidAmountDollar = v) }
            }
            DateField(requiredLabel("ჩარიცხვის თარიღი"), form.paidAt, state.errors["paidAt"]) { date -> viewModel.edit("paidAt") { it.copy(paidAt = date) } }
            FormTextField(AnnotatedString("კომენტარი"), form.comment, state.errors["comment"], minLines = 4) { v -> viewModel.edit("comment") { it.copy(comment = v) } }
            TelegramCheckbox(form.sendTelegramNotification) { v -> viewModel.edit("sendTelegramNotification") { it.copy(sendTelegramNotification = v) } }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = form.sendClientEmail, role = Role.Checkbox) { v -> viewModel.edit("sendClientEmail") { it.copy(sendClientEmail = v) } }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(checked = form.sendClientEmail, onCheckedChange = null)
                Text("ელ-ფოსტაზე შეტყობინების გაგზავნა", modifier = Modifier.padding(start = 8.dp))
            }
            FormSubmitButton("დამატება", state.isSubmitting, viewModel::submit)
        }
    }
}
