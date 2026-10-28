package ge.renovatori.app.faq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.ui.ErrorState
import ge.renovatori.app.ui.HtmlContent
import ge.renovatori.app.ui.LoadingState

/** FAQ page (web `/faq`): heading, then an accordion of entries with an HTML body. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(viewModel: FaqViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.isLoading -> LoadingState()

        // Without anything to show, an error takes the whole screen with a retry button.
        state.error != null && state.faqs.isEmpty() -> ErrorState(state.error.orEmpty(), onRetry = viewModel::load)

        else -> PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                faqSection(state, onToggle = viewModel::toggle)
            }
        }
    }
}

/**
 * Heading + accordion rows (web `faq.component.pug`), shared by the FAQ page and the home page.
 * Loading/full-screen error are left to the caller; a failed refresh shows its error above the list.
 */
fun LazyListScope.faqSection(state: FaqUiState, onToggle: (String) -> Unit, itemModifier: Modifier = Modifier) {
    item(key = "faq-header") { FaqHeader(itemModifier) }

    state.error?.let { error ->
        item(key = "faq-error") {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = itemModifier)
        }
    }

    // An empty collection shows the web's empty message; a failed load already shows its error.
    if (state.faqs.isEmpty() && !state.isLoading && state.error == null) {
        item(key = "faq-empty") {
            Text(
                "FAQ ინფორმაცია ცარიელია.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = itemModifier.fillMaxWidth().padding(vertical = 24.dp)
            )
        }
    }

    items(state.faqs, key = { "faq-${it.id}" }) { faq ->
        FaqItem(
            faq = faq,
            expanded = faq.id == state.expandedId,
            onToggle = { onToggle(faq.id) },
            modifier = itemModifier
        )
    }
}

@Composable
private fun FaqHeader(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Text(
            "ხშირად დასმული კითხვები",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "კითხვები, რომლებსაც ყველაზე ხშირად გვისვამენ ჩვენი მომხმარებლები",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FaqItem(faq: Faq, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "faqArrow")

    OutlinedCard(modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = if (expanded) "გახსნილი" else "დახურული" }
                .padding(16.dp)
        ) {
            Text(
                faq.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(arrowRotation)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider()
                HtmlContent(faq.description, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
