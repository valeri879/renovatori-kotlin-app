package ge.renovatori.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ge.renovatori.app.network.ApiConfig
import ge.renovatori.app.network.RemoteImage
import java.time.Instant

/** Centered section heading + optional subtitle (web `h2.text-center` + `p.text-center`) above its content. */
@Composable
fun Section(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            subtitle?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
        content()
    }
}

/** Page title, left-aligned like the web `h1.fw-bold`. */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Full-screen error with a retry button, used when a screen has nothing else to show. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(message, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 480.dp))
        Button(onClick = onRetry) { Text("ხელახლა ცდა") }
    }
}

/** Centered secondary text for empty lists (web `p.text-center` / `alert-secondary`). */
@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp)
    )
}

/** Form label with the red asterisk the web uses for required fields. */
fun requiredLabel(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    append(" ")
    withStyle(SpanStyle(color = Color.Red)) { append("*") }
}

/** Card with an optional 16:9 cover, bold title, optional description and a date footer (web content cards). */
@Composable
fun ContentCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    description: String? = null,
    footer: String? = null,
    imageAspectRatio: Float = 16f / 9f
) {
    OutlinedCard(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)) {
        // Cards without a cover show no image, like the web.
        if (imageUrl != null) {
            RemoteImage(
                url = ApiConfig.imageUrl(imageUrl),
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().aspectRatio(imageAspectRatio)
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        footer?.let {
            HorizontalDivider()
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

/** Compact row with a small thumbnail (web "ბოლო 10 ბლოგი" sidebar item). */
@Composable
fun CompactContentRow(title: String, imageUrl: String?, date: Instant?, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)
    ) {
        // Items without a cover keep the text aligned to the start, like the web.
        if (imageUrl != null) {
            RemoteImage(
                url = ApiConfig.imageUrl(imageUrl),
                contentDescription = null,
                modifier = Modifier.size(width = 88.dp, height = 66.dp).clip(RoundedCornerShape(6.dp))
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(formatDate(date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private sealed interface HtmlBlock {
    data class Text(val html: String) : HtmlBlock
    data class Image(val src: String, val alt: String?) : HtmlBlock
    data object Divider : HtmlBlock
}

private val voidBlockTag = Regex("""<(img|hr)\b[^>]*>""", RegexOption.IGNORE_CASE)
private val srcAttribute = Regex("""\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val altAttribute = Regex("""\balt\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val anyTag = Regex("<[^>]+>")
private val blockEnd = Regex("""(?<=</(?:p|h[1-6]|ul|ol|blockquote|div)>)""", RegexOption.IGNORE_CASE)

/**
 * Splits editor HTML at `<img>` and `<hr>`, which `fromHtml` cannot draw, so they can be shown natively.
 * Segments with no visible text (e.g. the `</p><p>` around an image) are dropped.
 */
private fun parseHtmlBlocks(html: String): List<HtmlBlock> {
    val blocks = mutableListOf<HtmlBlock>()
    var cursor = 0
    fun addText(segment: String) {
        // One block per paragraph/heading/list: fromHtml leaves no space between them, the Column adds it.
        segment.split(blockEnd).forEach { part ->
            if (part.replace(anyTag, "").isNotBlank()) blocks += HtmlBlock.Text(part)
        }
    }
    voidBlockTag.findAll(html).forEach { match ->
        addText(html.substring(cursor, match.range.first))
        if (match.groupValues[1].equals("hr", ignoreCase = true)) {
            blocks += HtmlBlock.Divider
        } else {
            srcAttribute.find(match.value)?.groupValues?.get(1)?.let { src ->
                blocks += HtmlBlock.Image(src, altAttribute.find(match.value)?.groupValues?.get(1))
            }
        }
        cursor = match.range.last + 1
    }
    addText(html.substring(cursor))
    return blocks
}

/**
 * Renders editor HTML (blog posts, project descriptions, FAQ answers, policies): text through
 * `AnnotatedString.fromHtml` (headings, lists, bold, links), images and dividers natively.
 * Links go through `LocalUriHandler`, so renovatori.ge links open inside the app.
 */
@Composable
fun HtmlContent(html: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyLarge) {
    val blocks = remember(html) { parseHtmlBlocks(html) }
    val linkColor = MaterialTheme.colorScheme.primary

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block) {
                is HtmlBlock.Text -> {
                    val text = remember(block.html, linkColor) {
                        val parsed = AnnotatedString.fromHtml(
                            block.html,
                            linkStyles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                        )
                        // Block tags leave leading/trailing newlines that read as empty space.
                        val start = parsed.text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                        parsed.subSequence(start, parsed.text.trimEnd().length.coerceAtLeast(start))
                    }
                    Text(text, style = style)
                }

                is HtmlBlock.Image -> RemoteImage(
                    // Editor uploads are site-relative ("/uploads/..."); absolute URLs are used as-is.
                    url = if (block.src.startsWith("/")) ApiConfig.origin + block.src else block.src,
                    contentDescription = block.alt,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(RoundedCornerShape(8.dp))
                )

                HtmlBlock.Divider -> HorizontalDivider()
            }
        }
    }
}
