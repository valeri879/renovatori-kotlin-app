package ge.renovatori.app.admin

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import ge.renovatori.app.ui.LoadingState
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.requiredLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneOffset

/** Web limit on the number of photos per pick (the form itself has none; keeps uploads reasonable). */
private const val MAX_PHOTOS_PER_PICK = 10

data class ExpenseEntryUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val notFound: Boolean = false,
    val expense: ExpenseDetail? = null,
    /** The row being edited (null when adding). */
    val entry: ExpenseEntry? = null,
    val expenseTypes: List<ExpenseType> = emptyList(),
    val form: ExpenseEntryForm = ExpenseEntryForm(),
    /** New photos picked on this screen (content URIs as strings, so they survive rotation). */
    val newPhotos: List<String> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val formError: String? = null,
    val saved: Boolean = false
)

/** Add ([entryId] null) or edit one row of an expense project. */
class ExpenseEntryViewModel(
    private val appContext: Context,
    private val expenseId: String,
    private val entryId: String?,
    private val api: ApiClient = ApiClient.shared
) : ViewModel() {
    private val _state = MutableStateFlow(ExpenseEntryUiState())
    val state: StateFlow<ExpenseEntryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val types = async { api.fetchExpenseTypes() }
                val expense = api.fetchExpense(expenseId)
                val entry = entryId?.let { id -> expense.expenses.firstOrNull { it.id == id } }
                // Editing a row that no longer exists.
                if (entryId != null && entry == null) {
                    _state.update { it.copy(isLoading = false, notFound = true) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        expense = expense,
                        entry = entry,
                        expenseTypes = types.await(),
                        // Editing starts from the saved values; dates are calendar days stored at UTC midnight.
                        form = entry?.let { saved ->
                            ExpenseEntryForm(
                                expenseTypeId = saved.expenseType?.id.orEmpty(),
                                expenseDate = saved.expenseDate?.atZone(ZoneOffset.UTC)?.toLocalDate(),
                                amount = formatAmountInput(saved.amount),
                                comment = saved.comment
                            )
                        } ?: it.form
                    )
                }
            } catch (e: ApiError) {
                _state.update { it.copy(isLoading = false, notFound = e.code == ApiError.CODE_NOT_FOUND, loadError = e.message) }
            }
        }
    }

    fun edit(field: String, change: (ExpenseEntryForm) -> ExpenseEntryForm) =
        _state.update { it.copy(form = change(it.form), errors = it.errors - field) }

    fun addPhotos(uris: List<Uri>) = _state.update { it.copy(newPhotos = (it.newPhotos + uris.map(Uri::toString)).distinct()) }

    fun removePhoto(uri: String) = _state.update { it.copy(newPhotos = it.newPhotos - uri) }

    fun submit() {
        val current = _state.value
        val errors = buildMap {
            if (current.form.expenseDate == null) put("expenseDate", "ხარჯის თარიღი სავალდებულოა.")
            amountError(current.form.amount, required = true, missing = "თანხა სავალდებულოა.")?.let { put("amount", it) }
            if (current.form.comment.trim().length > 2000) put("comment", "კომენტარი არ უნდა აღემატებოდეს 2000 სიმბოლოს.")
        }
        _state.update { it.copy(errors = errors, formError = null) }
        // Same rules as the API; only valid rows are sent.
        if (errors.isNotEmpty()) return

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val uploads = current.newPhotos.mapIndexedNotNull { index, uri -> appContext.photoUpload(Uri.parse(uri), index) }
            // A picked file that isn't a readable image is reported instead of silently dropped.
            if (uploads.size < current.newPhotos.size) {
                _state.update { it.copy(isSubmitting = false, formError = "ზოგიერთი სურათის წაკითხვა ვერ მოხერხდა. წაშალეთ და თავიდან აირჩიეთ.") }
                return@launch
            }
            try {
                api.saveExpenseEntry(expenseId, entryId, current.form, uploads)
                ExpenseChanges.changed()
                _state.update { it.copy(isSubmitting = false, saved = true) }
            } catch (e: ApiError) {
                _state.update {
                    // Field errors under the fields; 409 not_ongoing and others above the form.
                    if (!e.details.isNullOrEmpty()) it.copy(isSubmitting = false, errors = e.details)
                    else it.copy(isSubmitting = false, formError = e.message)
                }
            }
        }
    }
}

/** "120" for whole amounts, "120.5" otherwise, so the edit field shows what was typed. */
private fun formatAmountInput(amount: Double): String = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

/**
 * Add expense row (web `/admin/expenses/detail/:id/addExpenses`) or edit one
 * (`.../editExpenses?expenseId=…`). Admins only; adding needs a project „მიმდინარე“.
 */
@Composable
fun ExpenseEntryScreen(expenseId: String, entryId: String?) {
    val context = LocalContext.current.applicationContext
    val viewModel: ExpenseEntryViewModel = viewModel(key = "expense-entry-$expenseId-$entryId") {
        ExpenseEntryViewModel(context, expenseId, entryId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val expense = state.expense
    val isEdit = entryId != null

    // Saved: back to the project page, which reloads (web: redirect to detail).
    if (state.saved) LaunchedEffect(Unit) { navigator.back() }

    when {
        state.isLoading -> LoadingState()
        state.notFound -> NotFoundState("ჩანაწერი ვერ მოიძებნა.", "დეტალზე დაბრუნება") { navigator.navigate(Routes.adminExpense(expenseId), replaceCurrent = true) }
        expense == null -> ErrorState(state.loadError.orEmpty(), onRetry = viewModel::load)
        else -> EntryForm(state, expense, isEdit, viewModel)
    }
}

@Composable
private fun EntryForm(state: ExpenseEntryUiState, expense: ExpenseDetail, isEdit: Boolean, viewModel: ExpenseEntryViewModel) {
    val navigator = LocalAppNavigator.current
    val form = state.form
    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS_PER_PICK)) { uris ->
        viewModel.addPhotos(uris)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        PageTitle(if (isEdit) "ხარჯის ჩანაწერის რედაქტირება" else "ხარჯის ჩანაწერის დამატება")
        OutlinedButton(onClick = { navigator.back() }) { Text("დეტალზე დაბრუნება") }
        ProjectContextCard(expense) { navigator.back() }
        // Adding is only possible while the project is in progress; say so up front (the server answers 409).
        if (!isEdit && !expense.canAddEntries) {
            Text(NOT_ONGOING_HINT, color = MaterialTheme.colorScheme.error)
        }
        state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        ExpenseTypeDropdown(state.expenseTypes, form.expenseTypeId) { id -> viewModel.edit("expenseType") { it.copy(expenseTypeId = id) } }
        DateField(requiredLabel("ხარჯის თარიღი"), form.expenseDate, state.errors["expenseDate"]) { date -> viewModel.edit("expenseDate") { it.copy(expenseDate = date) } }
        FormTextField(requiredLabel("თანხა (₾)"), form.amount, state.errors["amount"], KeyboardType.Decimal) { v -> viewModel.edit("amount") { it.copy(amount = v) } }
        FormTextField(AnnotatedString("კომენტარი"), form.comment, state.errors["comment"], minLines = 4) { v -> viewModel.edit("comment") { it.copy(comment = v) } }

        Text("სურათები", fontWeight = FontWeight.Bold)
        state.entry?.images?.takeIf { it.isNotEmpty() }?.let { existing ->
            Text("მიმდინარე სურათები", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                existing.forEach { image ->
                    RemoteImage(
                        url = ApiConfig.imageUrl(image.thumbUrl),
                        contentDescription = "ხარჯის სურათი",
                        modifier = Modifier.size(width = 100.dp, height = 75.dp).clip(RoundedCornerShape(6.dp))
                    )
                }
            }
        }
        if (state.newPhotos.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.newPhotos.forEach { uri -> PickedPhoto(uri) { viewModel.removePhoto(uri) } }
            }
        }
        FilledTonalButton(onClick = { pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text("სურათების არჩევა")
        }
        Text(
            if (isEdit) "სურვილისამებრ შეგიძლია დაამატო ახალი ფოტოები (არსებულებს დაემატება)." else "ატვირთვა არ არის სავალდებულო.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TelegramCheckbox(form.sendTelegramNotification) { v -> viewModel.edit("sendTelegramNotification") { it.copy(sendTelegramNotification = v) } }
        FormSubmitButton(if (isEdit) "განახლება" else "დამატება", state.isSubmitting, viewModel::submit)
    }
}

/** Web `expenseContextCard` mixin: which project this row belongs to; tapping returns to it. */
@Composable
internal fun ProjectContextCard(expense: ExpenseDetail, onClick: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🏗️", style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text("მიმდინარე პროექტი", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(expense.projectName.ifBlank { "-" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusBadge(expense.projectStatus)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseTypeDropdown(types: List<ExpenseType>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val placeholder = "აირჩიე ხარჯის ტიპი"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = types.firstOrNull { it.id == selectedId }?.title ?: placeholder,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("ხარჯის ტიპი") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // The type is optional, like the web's empty first option.
            (listOf(ExpenseType("", placeholder)) + types).forEach { type ->
                DropdownMenuItem(text = { Text(type.title) }, onClick = {
                    onSelect(type.id)
                    expanded = false
                })
            }
        }
    }
}

/** Preview of a picked (not yet uploaded) photo with a remove button. */
@Composable
private fun PickedPhoto(uri: String, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = context.photoPreview(Uri.parse(uri))?.asImageBitmap()
    }
    Box(Modifier.size(width = 100.dp, height = 75.dp).clip(RoundedCornerShape(6.dp))) {
        bitmap?.let { Image(it, contentDescription = "არჩეული სურათი", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "სურათის მოშორება", tint = Color.White)
        }
    }
}
