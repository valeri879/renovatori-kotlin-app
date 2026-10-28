package ge.renovatori.app.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.requiredLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val objectIdPattern = Regex("^[0-9a-fA-F]{24}$")

data class ExpenseAddUiState(
    val form: ExpenseProjectForm = ExpenseProjectForm(),
    val errors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val formError: String? = null,
    /** Set once the server created the project; the screen then opens its detail page. */
    val createdId: String? = null
)

class ExpenseAddViewModel(private val api: ApiClient = ApiClient.shared) : ViewModel() {
    private val _state = MutableStateFlow(ExpenseAddUiState())
    val state: StateFlow<ExpenseAddUiState> = _state.asStateFlow()

    fun edit(field: String, change: (ExpenseProjectForm) -> ExpenseProjectForm) =
        _state.update { it.copy(form = change(it.form), errors = it.errors - field) }

    fun submit() {
        val form = _state.value.form
        val errors = validate(form)
        _state.update { it.copy(errors = errors, formError = null) }
        // Same rules as the server's readProjectBody; only valid forms are sent.
        if (errors.isNotEmpty()) return

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                val created = api.createExpense(form)
                ExpenseChanges.changed()
                _state.update { it.copy(isSubmitting = false, createdId = created.id) }
            } catch (e: ApiError) {
                _state.update {
                    // Field problems go under their fields, anything else above the form (web `formError`).
                    if (!e.details.isNullOrEmpty()) it.copy(isSubmitting = false, errors = e.details)
                    else it.copy(isSubmitting = false, formError = e.message)
                }
            }
        }
    }

    /** Client copy of `readProjectBody` in the backend's api/v1/admin/expenses.route.js. */
    private fun validate(form: ExpenseProjectForm): Map<String, String> = buildMap {
        val name = form.projectName.trim()
        when {
            name.isEmpty() -> put("projectName", "პროექტის სახელი სავალდებულოა.")
            name.length !in 2..255 -> put("projectName", "პროექტის სახელი უნდა იყოს 2-დან 255 სიმბოლომდე.")
        }
        amountError(form.totalAmount, required = true, missing = "ჯამური ღირებულება სავალდებულოა.")?.let { put("totalAmount", it) }
        amountError(form.totalAmountDollar, required = false, missing = "")?.let { put("totalAmountDollar", it) }
        amountError(form.area, required = true, missing = "ფართობი სავალდებულოა.", negative = "ფართობი არ შეიძლება იყოს უარყოფითი.")?.let { put("area", it) }
        if (form.projectAddress.trim().length > 500) put("projectAddress", "მისამართი არ უნდა აღემატებოდეს 500 სიმბოლოს.")
        if (form.clientName.trim().length > 255) put("clientName", "სახელი არ უნდა აღემატებოდეს 255 სიმბოლოს.")
        if (form.clientPhone.trim().length > 50) put("clientPhone", "ტელეფონი არ უნდა აღემატებოდეს 50 სიმბოლოს.")
        if (form.clientEmail.trim().length > 255) put("clientEmail", "ელფოსტა არ უნდა აღემატებოდეს 255 სიმბოლოს.")
        if (form.clientId.isNotBlank() && !objectIdPattern.matches(form.clientId.trim())) put("clientId", "დამკვეთის ID არავალიდურია.")
        if (form.publicProjectId.isNotBlank() && !objectIdPattern.matches(form.publicProjectId.trim())) {
            put("publicProjectId", "საჯარო პროექტის ID არავალიდურია.")
        }
    }
}

/** Error for an amount field, or null when it is valid (a decimal comma is accepted, like the server). */
internal fun amountError(
    value: String,
    required: Boolean,
    missing: String,
    negative: String = "თანხა არ შეიძლება იყოს უარყოფითი."
): String? {
    if (value.isBlank()) return if (required) missing else null
    val number = value.trim().replace(',', '.').toDoubleOrNull() ?: return "შეიყვანეთ სწორი რიცხვი."
    return if (number < 0) negative else null
}

/** Add project (web `/admin/expenses/add`, admins only). On success opens the new project. */
@Composable
fun ExpenseAddScreen(viewModel: ExpenseAddViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val form = state.form

    // Created: replace this form with the project's page, like the web redirect.
    state.createdId?.let { id ->
        androidx.compose.runtime.LaunchedEffect(id) { navigator.navigate(Routes.adminExpense(id), replaceCurrent = true) }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PageTitle("ხარჯის დამატება", Modifier.weight(1f))
            OutlinedButton(onClick = { navigator.navigate(Routes.ADMIN_EXPENSES) }) { Text("სიაში დაბრუნება") }
        }
        state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        FormTextField(requiredLabel("პროექტის სახელი"), form.projectName, state.errors["projectName"]) { v -> viewModel.edit("projectName") { it.copy(projectName = v) } }
        StatusDropdown(form.projectStatus) { v -> viewModel.edit("projectStatus") { it.copy(projectStatus = v) } }
        FormTextField(
            AnnotatedString("საჯარო პროექტის ID"), form.publicProjectId, state.errors["publicProjectId"],
            hint = "გამოიყენება ამ ხარჯის საჯარო პროექტის გვერდთან დასაკავშირებლად."
        ) { v -> viewModel.edit("publicProjectId") { it.copy(publicProjectId = v) } }
        FormTextField(requiredLabel("პროექტის ჯამური ღირებულება (₾)"), form.totalAmount, state.errors["totalAmount"], KeyboardType.Decimal) { v ->
            viewModel.edit("totalAmount") { it.copy(totalAmount = v) }
        }
        FormTextField(AnnotatedString("პროექტის ჯამური ღირებულება ($)"), form.totalAmountDollar, state.errors["totalAmountDollar"], KeyboardType.Decimal) { v ->
            viewModel.edit("totalAmountDollar") { it.copy(totalAmountDollar = v) }
        }
        FormTextField(requiredLabel("ფართობი (მ²)"), form.area, state.errors["area"], KeyboardType.Decimal) { v -> viewModel.edit("area") { it.copy(area = v) } }
        DateField(requiredLabel("პროექტის დაწყების თარიღი"), form.startDate, state.errors["startDate"]) { v -> viewModel.edit("startDate") { it.copy(startDate = v) } }
        DateField(requiredLabel("პროექტის დასრულების თარიღი"), form.endDate, state.errors["endDate"]) { v -> viewModel.edit("endDate") { it.copy(endDate = v) } }
        FormTextField(AnnotatedString("პროექტის მისამართი"), form.projectAddress, state.errors["projectAddress"]) { v -> viewModel.edit("projectAddress") { it.copy(projectAddress = v) } }
        FormTextField(AnnotatedString("დამკვეთის სახელი/გვარი"), form.clientName, state.errors["clientName"]) { v -> viewModel.edit("clientName") { it.copy(clientName = v) } }
        FormTextField(AnnotatedString("დამკვეთის ტელეფონი"), form.clientPhone, state.errors["clientPhone"], KeyboardType.Phone) { v -> viewModel.edit("clientPhone") { it.copy(clientPhone = v) } }
        FormTextField(AnnotatedString("დამკვეთის ელფოსტა"), form.clientEmail, state.errors["clientEmail"], KeyboardType.Email) { v -> viewModel.edit("clientEmail") { it.copy(clientEmail = v) } }
        FormTextField(
            AnnotatedString("დამკვეთის ID"), form.clientId, state.errors["clientId"],
            hint = "მომხმარებლის ID იპოვი ვებგვერდზე /users, „რედაქტირება“ → „მომხმარებლის ID“."
        ) { v -> viewModel.edit("clientId") { it.copy(clientId = v) } }
        TelegramCheckbox(form.sendTelegramNotification) { v -> viewModel.edit("sendTelegramNotification") { it.copy(sendTelegramNotification = v) } }
        FormSubmitButton("შენახვა", state.isSubmitting, viewModel::submit)
    }
}

@Composable
internal fun FormTextField(
    label: AnnotatedString,
    value: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    hint: String? = null,
    minLines: Int = 1,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        // The web `form-text` hint shows until there is an error to report.
        supportingText = (error ?: hint)?.let { { Text(it) } },
        singleLine = minLines == 1,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = statusLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(requiredLabel("პროექტის სტატუსი")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("დასაწყები", "მიმდინარე", "დასრულებული", "შეწყვეტილი").forEach { status ->
                DropdownMenuItem(text = { Text(statusLabel(status)) }, onClick = {
                    onSelect(status)
                    expanded = false
                })
            }
        }
    }
}

/** Web "Telegram-ზე შეტყობინების გაგზავნა" checkbox; off by default. */
@Composable
internal fun TelegramCheckbox(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange).padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text("Telegram-ზე შეტყობინების გაგზავნა", modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
internal fun FormSubmitButton(text: String, isSubmitting: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth()) {
        if (isSubmitting) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) else Text(text)
    }
}
