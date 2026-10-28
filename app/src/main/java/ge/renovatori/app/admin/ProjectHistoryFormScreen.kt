package ge.renovatori.app.admin

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import ge.renovatori.app.project.HistoryChanges
import ge.renovatori.app.project.HistoryEntry
import ge.renovatori.app.project.HistoryForm
import ge.renovatori.app.project.Project
import ge.renovatori.app.project.addHistory
import ge.renovatori.app.project.fetchProjectView
import ge.renovatori.app.project.updateHistory
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

private val objectId = Regex("^[0-9a-fA-F]{24}$")

data class HistoryFormUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val notFound: Boolean = false,
    val project: Project? = null,
    val entry: HistoryEntry? = null,
    val form: HistoryForm = HistoryForm(),
    val newPhotos: List<String> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val formError: String? = null,
    /** Whether the requested client e-mail went out (null when none was requested). */
    val savedEmailSent: Boolean? = null,
    val saved: Boolean = false
)

/** Add ([historyId] null) or edit a project history entry (admins only). */
class HistoryFormViewModel(
    private val appContext: Context,
    private val projectId: String,
    private val historyId: String?,
    private val api: ApiClient = ApiClient.shared
) : ViewModel() {
    private val _state = MutableStateFlow(HistoryFormUiState())
    val state: StateFlow<HistoryFormUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                // The admin project view carries the history list and the linked client.
                val view = api.fetchProjectView(projectId, signedIn = true)
                val entry = historyId?.let { id -> view.history.firstOrNull { it.id == id } }
                // Editing an entry that no longer exists.
                if (historyId != null && entry == null) {
                    _state.update { it.copy(isLoading = false, notFound = true) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        project = view.project,
                        entry = entry,
                        // Adding defaults to the project's client (web `defaultClientId`); editing starts from the entry.
                        form = entry?.let { saved -> HistoryForm(comment = saved.comment, timestamp = saved.timestamp, clientId = saved.clientId.orEmpty()) }
                            ?: HistoryForm(clientId = view.project.clientId.orEmpty())
                    )
                }
            } catch (e: ApiError) {
                _state.update { it.copy(isLoading = false, notFound = e.code == ApiError.CODE_NOT_FOUND, loadError = e.message) }
            }
        }
    }

    fun edit(field: String, change: (HistoryForm) -> HistoryForm) =
        _state.update { it.copy(form = change(it.form), errors = it.errors - field) }

    fun addPhotos(uris: List<Uri>) = _state.update { it.copy(newPhotos = (it.newPhotos + uris.map(Uri::toString)).distinct()) }

    fun removePhoto(uri: String) = _state.update { it.copy(newPhotos = it.newPhotos - uri) }

    fun submit() {
        val current = _state.value
        val clientId = current.form.clientId.trim()
        // Same rules as the API: the client is required when adding, and any id must be an ObjectId.
        val errors = buildMap {
            when {
                clientId.isEmpty() && historyId == null -> put("clientId", "მომხმარებლის ID სავალდებულოა.")
                clientId.isNotEmpty() && !objectId.matches(clientId) -> put("clientId", "მომხმარებლის ID არავალიდურია.")
            }
        }
        _state.update { it.copy(errors = errors, formError = null) }
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
                val result = if (historyId == null) api.addHistory(projectId, current.form, uploads)
                else api.updateHistory(historyId, current.form, uploads)
                HistoryChanges.changed()
                _state.update {
                    it.copy(isSubmitting = false, saved = true, savedEmailSent = if (current.form.sendClientEmail) result.clientEmailSent else null)
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

/** Web `/add-project-history/:projectId` and `/edit-project-history/:id`. */
@Composable
fun ProjectHistoryFormScreen(projectId: String, historyId: String?) {
    val context = LocalContext.current.applicationContext
    val viewModel: HistoryFormViewModel = viewModel(key = "history-$projectId-$historyId") { HistoryFormViewModel(context, projectId, historyId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val toastContext = LocalContext.current
    val project = state.project
    val isEdit = historyId != null

    if (state.saved) {
        LaunchedEffect(Unit) {
            val message = when (state.savedEmailSent) {
                true -> "შენახულია, დამკვეთს ელ-ფოსტა გაეგზავნა."
                false -> "შენახულია. ელ-ფოსტა არ გაიგზავნა."
                null -> "შენახულია."
            }
            Toast.makeText(toastContext, message, Toast.LENGTH_LONG).show()
            navigator.back()
        }
    }

    when {
        state.isLoading -> LoadingState()
        state.notFound -> NotFoundState("ჩანაწერი ვერ მოიძებნა.", "პროექტზე დაბრუნება") { navigator.navigate(Routes.project(projectId), replaceCurrent = true) }
        project == null -> ErrorState(state.loadError.orEmpty(), onRetry = viewModel::load)
        else -> HistoryForm(state, project, isEdit, viewModel)
    }
}

@Composable
private fun HistoryForm(state: HistoryFormUiState, project: Project, isEdit: Boolean, viewModel: HistoryFormViewModel) {
    val navigator = LocalAppNavigator.current
    val form = state.form
    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { viewModel.addPhotos(it) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        PageTitle(if (isEdit) "პროექტის ისტორიის რედაქტირება" else "პროექტის ისტორიის დამატება")
        OutlinedButton(onClick = { navigator.back() }) { Text("პროექტზე დაბრუნება") }
        Text("პროექტი: ${project.title}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Text(if (isEdit) "ახალი სურათები" else "სურათები", fontWeight = FontWeight.Bold)
        state.entry?.images?.takeIf { it.isNotEmpty() }?.let { existing ->
            Text("არსებული სურათები:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                existing.forEach { image ->
                    RemoteImage(
                        url = ApiConfig.imageUrl(image.thumbUrl),
                        contentDescription = "ისტორიის სურათი",
                        modifier = Modifier.size(width = 100.dp, height = 75.dp).clip(RoundedCornerShape(6.dp))
                    )
                }
            }
        }
        if (state.newPhotos.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.newPhotos.forEach { uri -> PickedHistoryPhoto(uri) { viewModel.removePhoto(uri) } }
            }
        }
        FilledTonalButton(onClick = { pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text("სურათების არჩევა")
        }

        FormTextField(AnnotatedString("კომენტარი"), form.comment, state.errors["comment"], hint = "ისტორიის აღწერა", minLines = 4) { v ->
            viewModel.edit("comment") { it.copy(comment = v) }
        }
        DateTimeField(
            "თარიღი / დრო", form.timestamp, state.errors["timestamp"],
            hint = if (isEdit) "ცარიელი დატოვება ინახავს არსებულ თარიღს." else "ცარიელი დატოვებისას ჩაიწერება მიმდინარე დრო."
        ) { v -> viewModel.edit("timestamp") { it.copy(timestamp = v) } }
        FormTextField(
            if (isEdit) AnnotatedString("მომხმარებლის ID") else requiredLabel("მომხმარებლის ID"),
            form.clientId, state.errors["clientId"],
            hint = "მომხმარებლის ID იპოვი ვებგვერდზე /users, „რედაქტირება“ → „მომხმარებლის ID“."
        ) { v -> viewModel.edit("clientId") { it.copy(clientId = v) } }
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
        FormSubmitButton(if (isEdit) "განახლება" else "ისტორიის დამატება", state.isSubmitting, viewModel::submit)
    }
}

@Composable
private fun PickedHistoryPhoto(uri: String, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, uri) { value = context.photoPreview(Uri.parse(uri))?.asImageBitmap() }
    Box(Modifier.size(width = 100.dp, height = 75.dp).clip(RoundedCornerShape(6.dp))) {
        bitmap?.let { Image(it, contentDescription = "არჩეული სურათი", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "სურათის მოშორება", tint = Color.White)
        }
    }
}
