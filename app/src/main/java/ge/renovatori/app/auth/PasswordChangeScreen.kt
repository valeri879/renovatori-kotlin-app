package ge.renovatori.app.auth

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PasswordChangeUiState(
    val isSubmitting: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val error: String? = null,
    val success: Boolean = false
)

class PasswordChangeViewModel : ViewModel() {
    private val _state = MutableStateFlow(PasswordChangeUiState())
    val state: StateFlow<PasswordChangeUiState> = _state.asStateFlow()

    fun clearError(field: String) = _state.update { it.copy(errors = it.errors - field) }

    /** Sends the change; [onDone] runs after success (the screen clears the fields). */
    fun submit(current: String, new: String, confirm: String, onDone: () -> Unit) {
        // Client copy of the server rules (same messages); "different from the old one" needs the server.
        val errors = buildMap {
            if (current.isEmpty()) put("currentPassword", "მიმდინარე პაროლი აუცილებელია")
            when {
                new.isEmpty() -> put("newPassword", "ახალი პაროლი აუცილებელია")
                new.length < 8 -> put("newPassword", "ახალი პაროლი უნდა შედგებოდეს მინიმუმ 8 სიმბოლოსგან")
            }
            when {
                confirm.isEmpty() -> put("confirmPassword", "გაიმეორეთ ახალი პაროლი")
                new.isNotEmpty() && new != confirm -> put("confirmPassword", "ახალი პაროლები არ ემთხვევა")
            }
        }
        _state.update { it.copy(errors = errors, error = null, success = false) }
        if (errors.isNotEmpty()) return

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                AuthSession.changePassword(current, new, confirm)
                _state.update { it.copy(isSubmitting = false, success = true) }
                onDone()
            } catch (e: ApiError) {
                _state.update {
                    if (!e.details.isNullOrEmpty()) it.copy(isSubmitting = false, errors = e.details)
                    else it.copy(isSubmitting = false, error = e.message)
                }
            }
        }
    }
}

/** Change password (web `/profile/password-change`); other devices are signed out by the server. */
@Composable
fun PasswordChangeScreen(viewModel: PasswordChangeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    // Not saveable: passwords never go into the saved-instance-state bundle.
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val submit = {
        viewModel.submit(current, new, confirm) {
            current = ""
            new = ""
            confirm = ""
        }
    }

    AuthCard {
        PageTitle("პაროლის შეცვლა")
        OutlinedButton(onClick = { navigator.back() }) { Text("უკან დაბრუნება") }
        // Web `alert-success`.
        if (state.success) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFD1E7DD), contentColor = Color(0xFF0F5132))) {
                Text(
                    "პაროლი წარმატებით შეიცვალა. დადასტურება გამოგზავნილია თქვენს ელ-ფოსტაზე. სხვა მოწყობილობებზე სესიები დასრულდა.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PasswordField("მიმდინარე პაროლი", current, { current = it; viewModel.clearError("currentPassword") }, state.errors["currentPassword"])
        PasswordField("ახალი პაროლი", new, { new = it; viewModel.clearError("newPassword") }, state.errors["newPassword"])
        if (state.errors["newPassword"] == null) {
            Text("მინიმუმ 8 სიმბოლო.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PasswordField(
            "გაიმეორეთ ახალი პაროლი", confirm, { confirm = it; viewModel.clearError("confirmPassword") },
            state.errors["confirmPassword"], imeAction = ImeAction.Done, onDone = submit
        )
        SubmitButton("პაროლის შეცვლა", state.isSubmitting, onClick = submit)
    }
}
