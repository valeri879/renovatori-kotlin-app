package ge.renovatori.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.ui.CompanyInfo
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isSubmitting: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    /** Server / network error shown above the button. */
    val error: String? = null,
    /** Wrong credentials: offer password recovery, like the web form. */
    val showPasswordReset: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun submit(email: String, password: String) {
        val emailError = if (email.isBlank()) "ელ-ფოსტა სავალდებულოა." else null
        val passwordError = if (password.isEmpty()) "პაროლი სავალდებულოა." else null
        _state.update { it.copy(emailError = emailError, passwordError = passwordError, error = null) }
        // Don't call the server until both required fields are filled.
        if (emailError != null || passwordError != null) return

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                // Success flips AuthSession.state; the screen navigates from there.
                AuthSession.login(email, password)
                _state.update { it.copy(isSubmitting = false) }
            } catch (e: ApiError) {
                _state.update {
                    it.copy(isSubmitting = false, error = e.message, showPasswordReset = e.code == "invalid_credentials")
                }
            }
        }
    }

    fun clearFieldError(field: String) = _state.update {
        if (field == "email") it.copy(emailError = null) else it.copy(passwordError = null)
    }
}

/** Login (web `/login`): e-mail + password; links to signup and, after a failed attempt, password reset. */
@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val uriHandler = LocalUriHandler.current
    var email by rememberSaveable { mutableStateOf("") }
    // Not saveable: passwords never go into the saved-instance-state bundle.
    var password by remember { mutableStateOf("") }
    val submit = { viewModel.submit(email, password) }

    AuthCard {
        PageTitle("ავტორიზაცია")
        Text("სისტემაში შესასვლელად გაიარეთ ავტორიზაცია")
        AuthTextField(
            "ელ-ფოსტა", email,
            { email = it; viewModel.clearFieldError("email") },
            state.emailError, keyboardType = KeyboardType.Email
        )
        PasswordField(
            "პაროლი", password,
            { password = it; viewModel.clearFieldError("password") },
            state.passwordError, imeAction = ImeAction.Done, onDone = submit
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        // Password reset is not in the app yet; the web flow handles it.
        if (state.showPasswordReset) {
            TextButton(onClick = { uriHandler.openUri("${CompanyInfo.WEBSITE_URL}/password-reset") }) { Text("პაროლის აღდგენა") }
        }
        SubmitButton("შესვლა", state.isSubmitting, onClick = submit)
        Text(
            "თუ არ გაქვს ანგარიში, გაიარე რეგისტრაცია.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { navigator.navigate(Routes.SIGNUP) }, modifier = Modifier.fillMaxWidth()) { Text("რეგისტრაცია") }
    }
}

/** Centered, width-limited card around an auth form (web `.col-md-6.col-lg-4 .card`). */
@Composable
fun AuthCard(content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        OutlinedCard(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}

/** Full-width primary button that shows a spinner and blocks repeat taps while [isSubmitting]. */
@Composable
fun SubmitButton(text: String, isSubmitting: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth()) {
        if (isSubmitting) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(text)
        }
    }
}
