package ge.renovatori.app.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ge.renovatori.app.ui.HtmlContent
import ge.renovatori.app.ui.PageTitle

/** Text-only page (cookies / privacy policy) rendered from its HTML. */
@Composable
fun StaticPageScreen(page: StaticPage) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        PageTitle(page.title)
        HtmlContent(page.html)
    }
}
