package ge.renovatori.app.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Close
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ge.renovatori.app.auth.AuthSession
import ge.renovatori.app.auth.AuthState
import ge.renovatori.app.auth.LoginScreen
import ge.renovatori.app.auth.User
import ge.renovatori.app.ui.theme.success
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Web status labels (`expenses-list-page.pug`): emoji + name; the legacy „დაწყებული“ reads as „დასაწყები“. */
fun statusLabel(status: String): String = when (status) {
    "დასაწყები", "დაწყებული" -> "🚀 დასაწყები"
    "მიმდინარე" -> "🔄 მიმდინარე"
    "დასრულებული" -> "✅ დასრულებული"
    "შეწყვეტილი" -> "⛔ შეწყვეტილი"
    else -> status.ifBlank { "-" }
}

/** Bootstrap `text-bg-*` colors of each status badge (primary / info / success / danger / secondary). */
private fun statusColors(status: String): Pair<Color, Color> = when (status) {
    "დასაწყები", "დაწყებული" -> Color(0xFF0D6EFD) to Color.White
    "მიმდინარე" -> Color(0xFF0DCAF0) to Color(0xFF000000)
    "დასრულებული" -> Color(0xFF198754) to Color.White
    "შეწყვეტილი" -> Color(0xFFDC3545) to Color.White
    else -> Color(0xFF6C757D) to Color.White
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (container, content) = statusColors(status)
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(statusLabel(status), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

/** Green for zero/positive, red for negative (web `text-success` / `text-danger`). */
@Composable
fun signColor(value: Double): Color = if (value >= 0) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error

/** Progress bar colored like the web `progressClass`: grey unknown/not started, blue running, yellow ≥ 80 %, green finished. */
@Composable
fun ProjectProgressBar(progress: ProjectProgress, modifier: Modifier = Modifier) {
    val color = when {
        !progress.isValid || progress.percent == 0 -> Color(0xFF6C757D)
        progress.percent >= 100 -> Color(0xFF198754)
        progress.percent >= 80 -> Color(0xFFFFC107)
        else -> MaterialTheme.colorScheme.primary
    }
    LinearProgressIndicator(
        progress = { progress.percent / 100f },
        color = color,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Admin pages are for staff only (admins full access, moderators read-only). Signed out shows the
 * login form, other users a "no access" note; the server enforces the same rules with 401/403.
 */
@Composable
fun StaffOnly(content: @Composable (User) -> Unit) {
    val authState by AuthSession.state.collectAsStateWithLifecycle()
    when (val auth = authState) {
        AuthState.Unauthenticated -> LoginScreen()
        is AuthState.Authenticated -> if (auth.user.isAdmin || auth.user.isModerator) {
            content(auth.user)
        } else {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("ამ გვერდზე წვდომა არ გაქვთ.", textAlign = TextAlign.Center)
            }
        }
    }
}

/** Confirmation before any delete (project rule: never delete without confirming). */
@Composable
fun ConfirmDeleteDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("წაშლა") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("გაუქმება") } }
    )
}

private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Read-only field that opens a [DatePicker] (web `input[type=date]`). Dates are calendar days, kept in UTC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: AnnotatedString, value: LocalDate?, error: String?, onChange: (LocalDate) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value?.format(dayFormatter).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // The text field is read-only; the whole area opens the picker.
        Box(
            Modifier
                .matchParentSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = true }
        )
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("არჩევა") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("გაუქმება") } }
        ) { DatePicker(state = state) }
    }
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/**
 * Optional date + time (web `input[type=datetime-local]`): a date picker, then a 24-hour time
 * picker, in the device time zone. [onChange] gets null when cleared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(label: String, value: Instant?, error: String?, hint: String?, onChange: (Instant?) -> Unit) {
    val zone = java.time.ZoneId.systemDefault()
    var step by rememberSaveable { mutableStateOf(0) } // 0 closed, 1 date, 2 time
    var pickedDate by rememberSaveable { mutableStateOf<String?>(null) }
    val local = value?.atZone(zone)?.toLocalDateTime()

    Box {
        OutlinedTextField(
            value = local?.format(dateTimeFormatter).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = error != null,
            supportingText = (error ?: hint)?.let { { Text(it) } },
            singleLine = true,
            trailingIcon = if (value != null) {
                { androidx.compose.material3.IconButton(onClick = { onChange(null) }) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Close, contentDescription = "გასუფთავება") } }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )
        // Tapping the text area (not the clear button) opens the pickers.
        Box(
            Modifier
                .padding(end = if (value != null) 56.dp else 0.dp)
                .matchParentSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { step = 1 }
        )
    }
    if (step == 1) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (local?.toLocalDate() ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    pickedDate = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    step = if (pickedDate != null) 2 else 0
                }) { Text("შემდეგი") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("გაუქმება") } }
        ) { DatePicker(state = state) }
    }
    if (step == 2) {
        val now = java.time.LocalTime.now()
        val state = androidx.compose.material3.rememberTimePickerState(
            initialHour = local?.hour ?: now.hour,
            initialMinute = local?.minute ?: now.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    val date = LocalDate.parse(pickedDate)
                    onChange(date.atTime(state.hour, state.minute).atZone(zone).toInstant())
                    step = 0
                }) { Text("არჩევა") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("გაუქმება") } },
            text = { androidx.compose.material3.TimePicker(state = state) }
        )
    }
}

/** Small label + value pair (web `.small.text-secondary` over `.fw-semibold`). */
@Composable
fun LabeledValue(label: String, value: String, valueColor: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
