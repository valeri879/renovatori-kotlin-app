package ge.renovatori.app.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.project.ProjectCard
import ge.renovatori.app.project.fetchProjects
import ge.renovatori.app.ui.LatestContent
import ge.renovatori.app.ui.fetchLatestContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val CALCULATOR_STEPS = 6

data class CalculatorUiState(
    val step: Int = 1,
    val form: CalculatorForm = CalculatorForm(),
    /** Field errors keyed like "phoneNumber" or "rooms.0.area" (the room/bathroom `key`). */
    val errors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val result: CalculationResult? = null,
    /** Under the form: 4 latest projects and posts, like the web calculator page. */
    val latest: LatestContent = LatestContent(),
    /** Under the result: the web's 6 latest projects ("ჩვენი ნამუშევრები"). */
    val resultProjects: List<ProjectCard> = emptyList()
)

private const val REQUIRED = "სავალდებულო ველი."
private const val INVALID_NUMBER = "შეიყვანეთ სწორი რიცხვი."

/**
 * Six-step calculator form (web `/calculator`). The form lives here so it survives rotation and
 * going back from the result, like the browser keeps the web form.
 */
class CalculatorViewModel(private val api: ApiClient = ApiClient.shared) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()
    private var nextRowKey = 1

    init {
        viewModelScope.launch {
            val latest = api.fetchLatestContent(projectCount = 4, blogCount = 4)
            _state.update { it.copy(latest = latest) }
        }
    }

    /** Applies [change] to the form and clears the error of the edited field [errorKey]. */
    fun edit(errorKey: String, change: (CalculatorForm) -> CalculatorForm) {
        _state.update { it.copy(form = change(it.form), errors = it.errors - errorKey) }
    }

    fun next() {
        val errors = validate(_state.value.form, _state.value.step)
        _state.update {
            // Stay on the step until its required fields are valid, like the web `reportValidity`.
            if (errors.isEmpty()) it.copy(step = (it.step + 1).coerceAtMost(CALCULATOR_STEPS), errors = emptyMap()) else it.copy(errors = errors)
        }
    }

    fun back() {
        _state.update { it.copy(step = (it.step - 1).coerceAtLeast(1), errors = emptyMap(), submitError = null) }
    }

    fun addRoom() = _state.update { it.copy(form = it.form.copy(rooms = it.form.rooms + RoomInput(key = nextRowKey++))) }

    fun addBathroom() =
        _state.update { it.copy(form = it.form.copy(bathrooms = it.form.bathrooms + BathroomInput(key = nextRowKey++))) }

    /** Removes a room; the last one stays, since the calculation needs at least one. */
    fun removeRoom(key: Int) = _state.update { state ->
        if (state.form.rooms.size <= 1) state else state.copy(form = state.form.copy(rooms = state.form.rooms.filterNot { it.key == key }))
    }

    /** Removes a bathroom; the web form keeps at least one as well. */
    fun removeBathroom(key: Int) = _state.update { state ->
        if (state.form.bathrooms.size <= 1) state
        else state.copy(form = state.form.copy(bathrooms = state.form.bathrooms.filterNot { it.key == key }))
    }

    fun submit() {
        val form = _state.value.form
        // Re-check every step; jump to the first one with a problem.
        val invalidStep = (1..CALCULATOR_STEPS).firstOrNull { validate(form, it).isNotEmpty() }
        if (invalidStep != null) {
            _state.update { it.copy(step = invalidStep, errors = validate(form, invalidStep)) }
            return
        }

        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            try {
                val result = api.calculate(form)
                _state.update { it.copy(isSubmitting = false, result = result) }
                loadResultProjects()
            } catch (e: ApiError) {
                _state.update { it.copy(isSubmitting = false, submitError = e.message) }
            }
        }
    }

    /** Back from the result to the filled-in form. */
    fun closeResult() = _state.update { it.copy(result = null) }

    private suspend fun loadResultProjects() {
        // The result already shows; its project list is optional.
        try {
            val projects = api.fetchProjects(page = 1, pageSize = 6).items
            _state.update { it.copy(resultProjects = projects) }
        } catch (e: ApiError) {
            _state.update { it.copy(resultProjects = emptyList()) }
        }
    }
}

/** Required-field rules of one step (the web marks every field with a red asterisk). */
fun validate(form: CalculatorForm, step: Int): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    fun number(key: String, value: String) {
        when {
            value.isBlank() -> errors[key] = REQUIRED
            parseNumber(value) == null -> errors[key] = INVALID_NUMBER
        }
    }

    when (step) {
        1 -> {
            // Same 9-digit rule and message as the backend.
            if (!Regex("""^\d{9}$""").matches(form.phoneNumber.trim())) {
                errors["phoneNumber"] = "გთხოვთ მიუთითოთ ტელეფონის ნომერი 9 ციფრის ფორმატში."
            }
            number("totalArea", form.totalArea)
            number("floor", form.floor)
        }
        2 -> {
            number("height", form.height)
            number("wetAreas", form.wetAreas)
        }
        3 -> {
            number("doors", form.doors)
            number("boilers", form.boilers)
        }
        5 -> form.rooms.forEach { room ->
            number("rooms.${room.key}.area", room.area)
            number("rooms.${room.key}.perimeter", room.perimeter)
            number("rooms.${room.key}.electricalPoints", room.electricalPoints)
        }
        6 -> form.bathrooms.forEach { bathroom ->
            number("bathrooms.${bathroom.key}.area", bathroom.area)
            number("bathrooms.${bathroom.key}.perimeter", bathroom.perimeter)
            number("bathrooms.${bathroom.key}.electricalPoints", bathroom.electricalPoints)
        }
    }
    return errors
}
