package ge.renovatori.app.faq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FaqUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val faqs: List<Faq> = emptyList(),
    val error: String? = null,
    /** Only one entry is open at a time, like the web accordion (`data-bs-parent`). */
    val expandedId: String? = null
)

class FaqViewModel(private val api: ApiClient = ApiClient.shared) : ViewModel() {

    private val _state = MutableStateFlow(FaqUiState())
    val state: StateFlow<FaqUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** First load or retry after an error: shows the full-screen spinner. */
    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        fetch()
    }

    /** Pull-to-refresh: keeps the current list on screen while reloading. */
    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        fetch()
    }

    fun toggle(id: String) {
        _state.update { it.copy(expandedId = if (it.expandedId == id) null else id) }
    }

    private fun fetch() {
        viewModelScope.launch {
            try {
                val faqs = api.fetchFaqs()
                _state.update { current ->
                    // Keep the open entry if it still exists; otherwise open the first one, as the web does.
                    val expandedId = current.expandedId?.takeIf { id -> faqs.any { it.id == id } }
                        ?: faqs.firstOrNull()?.id
                    current.copy(isLoading = false, isRefreshing = false, faqs = faqs, error = null, expandedId = expandedId)
                }
            } catch (e: ApiError) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }
}
