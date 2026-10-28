package ge.renovatori.app.auth

import android.util.Patterns
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignupForm(
    val email: String = "",
    val phoneNumber: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val password: String = "",
    val repeatPassword: String = ""
)

data class SignupUiState(
    val isSubmitting: Boolean = false,
    /** Keyed by API field name (`email`, `phoneNumber`, …) plus `repeatPassword`. */
    val errors: Map<String, String> = emptyMap(),
    val error: String? = null
)

class SignupViewModel : ViewModel() {
    private val _state = MutableStateFlow(SignupUiState())
    val state: StateFlow<SignupUiState> = _state.asStateFlow()

    fun submit(form: SignupForm) {
        val errors = validate(form)
        _state.update { it.copy(errors = errors, error = null) }
        // Client checks mirror the backend, so most mistakes never reach the server.
        if (errors.isNotEmpty()) return

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                AuthSession.signup(form.firstName, form.lastName, form.phoneNumber, form.email, form.password)
                _state.update { it.copy(isSubmitting = false) }
            } catch (e: ApiError) {
                _state.update { state ->
                    when {
                        // Duplicate e-mail belongs to the e-mail field.
                        e.code == "email_taken" -> state.copy(isSubmitting = false, errors = mapOf("email" to e.message))
                        // Per-field server validation goes under each field.
                        !e.details.isNullOrEmpty() -> state.copy(isSubmitting = false, errors = e.details)
                        else -> state.copy(isSubmitting = false, error = e.message)
                    }
                }
            }
        }
    }

    fun clearError(field: String) = _state.update { it.copy(errors = it.errors - field) }

    /** Same rules and wording as `POST /auth/signup`, plus the web form's repeated password. */
    private fun validate(form: SignupForm): Map<String, String> = buildMap {
        when {
            form.email.isBlank() -> put("email", "ელ-ფოსტა აუცილებელია")
            !Patterns.EMAIL_ADDRESS.matcher(form.email.trim()).matches() -> put("email", "ელ-ფოსტის ფორმატი არასწორია")
        }
        when {
            form.phoneNumber.isBlank() -> put("phoneNumber", "ტელეფონის ნომერი აუცილებელია")
            !Regex("""^\d{9}$""").matches(form.phoneNumber.trim()) -> put("phoneNumber", "ტელეფონის ნომერი უნდა შედგებოდეს 9 ციფრისგან")
        }
        if (form.firstName.isBlank()) put("firstName", "სახელი აუცილებელია")
        if (form.lastName.isBlank()) put("lastName", "გვარი აუცილებელია")
        when {
            form.password.isEmpty() -> put("password", "პაროლი აუცილებელია")
            form.password.length < 8 -> put("password", "პაროლი უნდა იყოს მინიმუმ 8 სიმბოლო")
        }
        if (form.repeatPassword != form.password) put("repeatPassword", "პაროლები არ ემთხვევა")
    }
}

/** Signup (web `/signup`). Field order and labels follow the web form; the web captcha has no API counterpart. */
@Composable
fun SignupScreen(viewModel: SignupViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    // Not saveable: passwords never go into the saved-instance-state bundle.
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    val submit = { viewModel.submit(SignupForm(email, phoneNumber, firstName, lastName, password, repeatPassword)) }

    AuthCard {
        PageTitle("რეგისტრაცია")
        Text("სისტემაში შესასვლელად გაიარეთ რეგისტრაცია")
        AuthTextField("ელ-ფოსტა", email, { email = it; viewModel.clearError("email") }, state.errors["email"], KeyboardType.Email)
        AuthTextField(
            "ტელეფონის ნომერი", phoneNumber,
            { value -> phoneNumber = value.filter(Char::isDigit).take(9); viewModel.clearError("phoneNumber") },
            state.errors["phoneNumber"], KeyboardType.Phone
        )
        AuthTextField(
            "სახელი", firstName, { firstName = it; viewModel.clearError("firstName") },
            state.errors["firstName"], capitalization = KeyboardCapitalization.Words
        )
        AuthTextField(
            "გვარი", lastName, { lastName = it; viewModel.clearError("lastName") },
            state.errors["lastName"], capitalization = KeyboardCapitalization.Words
        )
        PasswordField("პაროლი", password, { password = it; viewModel.clearError("password") }, state.errors["password"])
        PasswordField(
            "გაიმეორე პაროლი", repeatPassword, { repeatPassword = it; viewModel.clearError("repeatPassword") },
            state.errors["repeatPassword"], imeAction = ImeAction.Done, onDone = submit
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        SubmitButton("რეგისტრაცია", state.isSubmitting, onClick = submit)
        Text(
            "თუ უკვე გაქვს ანგარიში, გაიარე ავტორიზაცია.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { navigator.navigate(Routes.LOGIN) }, modifier = Modifier.fillMaxWidth()) { Text("შესვლა") }
    }
}
