package ge.renovatori.app.auth

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.R
import ge.renovatori.app.admin.ConfirmDeleteDialog
import ge.renovatori.app.admin.photoPreview
import ge.renovatori.app.admin.photoUpload
import ge.renovatori.app.network.ApiConfig
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.network.RemoteImage
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val isSubmitting: Boolean = false,
    val isDeletingPhoto: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val formError: String? = null,
    val saved: Boolean = false
)

class ProfileEditViewModel(private val appContext: Context) : ViewModel() {
    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    fun clearError(field: String) = _state.update { it.copy(errors = it.errors - field) }

    fun submit(firstName: String, lastName: String, phoneNumber: String, photoUri: String?) {
        // Same rules and wording as the user model, checked before the request.
        val errors = buildMap {
            nameError(firstName, "სახელი")?.let { put("firstName", it) }
            nameError(lastName, "გვარი")?.let { put("lastName", it) }
            when {
                phoneNumber.isBlank() -> put("phoneNumber", "ტელეფონის ნომერი აუცილებელია")
                !Regex("""^\d{9}$""").matches(phoneNumber.trim()) -> put("phoneNumber", "ტელეფონის ნომერი უნდა შეიცავდეს 9 ციფრს")
            }
        }
        _state.update { it.copy(errors = errors, formError = null) }
        if (errors.isNotEmpty()) return

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val photo = photoUri?.let { appContext.photoUpload(Uri.parse(it), 0) }
            // A picked file that couldn't be read is reported, not silently skipped.
            if (photoUri != null && photo == null) {
                _state.update { it.copy(isSubmitting = false, formError = "სურათის წაკითხვა ვერ მოხერხდა. აირჩიეთ სხვა.") }
                return@launch
            }
            try {
                AuthSession.updateProfile(firstName, lastName, phoneNumber, photo)
                _state.update { it.copy(isSubmitting = false, saved = true) }
            } catch (e: ApiError) {
                _state.update {
                    if (!e.details.isNullOrEmpty()) it.copy(isSubmitting = false, errors = e.details)
                    else it.copy(isSubmitting = false, formError = e.message)
                }
            }
        }
    }

    fun deletePhoto() {
        _state.update { it.copy(isDeletingPhoto = true, formError = null) }
        viewModelScope.launch {
            try {
                AuthSession.deleteProfileImage()
                _state.update { it.copy(isDeletingPhoto = false) }
            } catch (e: ApiError) {
                _state.update { it.copy(isDeletingPhoto = false, formError = e.message) }
            }
        }
    }

    private fun nameError(value: String, label: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "$label აუცილებელია"
            trimmed.length < 2 -> "$label უნდა შეიცავდეს მინიმუმ 2 სიმბოლოს"
            trimmed.length > 50 -> "$label არ შეიძლება აღემატებოდეს 50 სიმბოლოს"
            else -> null
        }
    }
}

/** Edit profile (web `/profile/edit`): photo (replace / delete), name, read-only e-mail, phone. */
@Composable
fun ProfileEditScreen(user: User) {
    val context = LocalContext.current
    val viewModel: ProfileEditViewModel = viewModel { ProfileEditViewModel(context.applicationContext) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    var firstName by rememberSaveable { mutableStateOf(user.firstName) }
    var lastName by rememberSaveable { mutableStateOf(user.lastName) }
    var phoneNumber by rememberSaveable { mutableStateOf(user.phoneNumber) }
    var newPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) newPhoto = uri.toString() }

    if (state.saved) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "პროფილი განახლდა.", Toast.LENGTH_SHORT).show()
            navigator.back()
        }
    }

    AuthCard {
        PageTitle("პროფილის რედაქტირება")
        OutlinedButton(onClick = { navigator.back() }) { Text("უკან დაბრუნება") }
        state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Avatar(user, newPhoto)
            // A newly picked photo can be dropped before saving; a saved one is deleted on the server.
            if (newPhoto != null) {
                OutlinedButton(onClick = { newPhoto = null }) { Text("არჩეული ფოტოს გაუქმება") }
            } else if (user.profileImage != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !state.isDeletingPhoto,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("ფოტოს წაშლა") }
            }
            FilledTonalButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text("პროფილის ფოტო")
            }
        }

        AuthTextField(
            "სახელი", firstName, { firstName = it; viewModel.clearError("firstName") },
            state.errors["firstName"], capitalization = KeyboardCapitalization.Words
        )
        AuthTextField(
            "გვარი", lastName, { lastName = it; viewModel.clearError("lastName") },
            state.errors["lastName"], capitalization = KeyboardCapitalization.Words
        )
        OutlinedTextField(
            value = user.email,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("ელ-ფოსტა") },
            supportingText = { Text("ელ-ფოსტის შეცვლა ამ ფორმიდან შეუძლებელია.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        AuthTextField(
            "ტელეფონი", phoneNumber,
            { value -> phoneNumber = value.filter(Char::isDigit).take(9); viewModel.clearError("phoneNumber") },
            state.errors["phoneNumber"], KeyboardType.Phone
        )
        if (state.errors["phoneNumber"] == null) {
            Text("შეიყვანეთ 9 ციფრი, მაგალითად 599123456.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SubmitButton("შენახვა", state.isSubmitting) { viewModel.submit(firstName, lastName, phoneNumber, newPhoto) }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            message = "ნამდვილად გსურთ პროფილის ფოტოს წაშლა?",
            onConfirm = {
                confirmDelete = false
                viewModel.deletePhoto()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

/** 120 dp round photo: the picked one, else the saved one, else the web's neutral avatar. */
@Composable
private fun Avatar(user: User, newPhoto: String?) {
    val context = LocalContext.current
    val modifier = Modifier.size(120.dp).clip(CircleShape)
    when {
        newPhoto != null -> {
            val bitmap by produceState<ImageBitmap?>(null, newPhoto) { value = context.photoPreview(Uri.parse(newPhoto), 480)?.asImageBitmap() }
            Box(modifier) { bitmap?.let { Image(it, contentDescription = "ახალი ფოტო", contentScale = ContentScale.Crop, modifier = Modifier.size(120.dp)) } }
        }
        user.profileImage != null -> RemoteImage(ApiConfig.imageUrl("/uploads/small/${user.profileImage}"), "${user.firstName} ${user.lastName}", modifier)
        else -> Image(painterResource(R.drawable.unisex_avatar), "${user.firstName} ${user.lastName}", modifier, contentScale = ContentScale.Crop)
    }
}
