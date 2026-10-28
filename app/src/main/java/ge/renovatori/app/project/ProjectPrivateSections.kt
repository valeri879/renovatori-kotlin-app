package ge.renovatori.app.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ge.renovatori.app.admin.ConfirmDeleteDialog
import ge.renovatori.app.admin.ProjectProgress
import ge.renovatori.app.admin.ProjectProgressBar
import ge.renovatori.app.network.ApiConfig
import ge.renovatori.app.network.RemoteImage
import ge.renovatori.app.ui.formatDate
import ge.renovatori.app.ui.formatDateTime
import ge.renovatori.app.ui.formatDollars
import ge.renovatori.app.ui.formatPhone
import ge.renovatori.app.ui.phoneUri
import ge.renovatori.app.ui.theme.success
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Web accordion (open by default) with the "visible only to you" note under its title. */
@Composable
private fun PrivateAccordion(title: String, content: @Composable () -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text("ხილულია მხოლოდ თქვენთვის", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.rotate(if (expanded) 180f else 0f))
        }
        AnimatedVisibility(expanded) {
            Column {
                HorizontalDivider()
                content()
            }
        }
    }
}

@Composable
private fun Row2(label: String, value: String, valueColor: Color = Color.Unspecified, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.45f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else valueColor,
            modifier = Modifier.weight(0.55f)
        )
    }
}

/** Web "დამკვეთის ინფორმაცია"; every row shows "-" when the project has no linked client. */
@Composable
fun ClientInfoCard(client: ProjectClient?) {
    val uriHandler = LocalUriHandler.current
    PrivateAccordion("დამკვეთის ინფორმაცია") {
        Row2("სახელი", client?.firstName?.ifBlank { null } ?: "-")
        HorizontalDivider()
        Row2("გვარი", client?.lastName?.ifBlank { null } ?: "-")
        HorizontalDivider()
        val email = client?.email?.ifBlank { null }
        Row2("ელ-ფოსტა", email ?: "-", onClick = email?.let { { uriHandler.openUri("mailto:$it") } })
        HorizontalDivider()
        val tel = client?.phoneNumber?.let(::phoneUri)
        Row2("ტელეფონი", if (tel != null) formatPhone(client.phoneNumber) else "-", onClick = tel?.let { { uriHandler.openUri(it) } })
    }
}

/** Web "პროექტის ფინანსები და პროგრესი" (dollar totals, each payment, date progress). */
@Composable
fun FinanceCard(summary: FinancialSummary, project: Project) {
    PrivateAccordion("პროექტის ფინანსები და პროგრესი") {
        Row2("ჯამური ღირებულება", formatDollars(summary.totalAmountDollar))
        HorizontalDivider()
        Row2("ჩასარიცხი თანხა", formatDollars(summary.remainingAmountDollar), MaterialTheme.colorScheme.error)
        HorizontalDivider(thickness = 2.dp)
        if (summary.paidAmounts.isEmpty()) {
            Text(
                "ინდივიდუალური ჩარიცხვები ჯერ არ არის დამატებული.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        summary.paidAmounts.forEach { payment ->
            Row2(formatDate(payment.paidAt), formatDollars(payment.paidAmountDollar), MaterialTheme.colorScheme.success)
            HorizontalDivider()
        }
        Row2("ჯამი", formatDollars(summary.totalPaidAmountDollar), MaterialTheme.colorScheme.success)
        HorizontalDivider(thickness = 2.dp)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("პროექტის დაწყება: ${formatDate(project.startDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("პროექტის დასრულება: ${formatDate(project.endDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(summary.progressLabel, fontWeight = FontWeight.Bold)
            ProjectProgressBar(ProjectProgress(summary.progressPercent, summary.progressLabel, summary.progressValid))
            Text("${summary.progressPercent}%", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private val secondsFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

/** Web admin lines above the description: created / updated time and publish status. */
@Composable
fun AdminMeta(project: Project) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        project.createdAt?.let { Text("დამატებულია: ${it.atZone(ZoneId.systemDefault()).format(secondsFormatter)}") }
        // The web prints the update time only when it differs from creation.
        if (project.updatedAt != null && project.updatedAt != project.createdAt) {
            Text("განახლებულია: ${project.updatedAt.atZone(ZoneId.systemDefault()).format(secondsFormatter)}")
        }
        Surface(
            color = if (project.isPublished) Color(0xFF198754) else Color(0xFF6C757D),
            contentColor = Color.White,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                if (project.isPublished) "გამოქვეყნებული" else "გამოუქვეყნებელი",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** Web "პროექტის ისტორია": entries newest first with photos; admins add, edit and delete (with confirmation). */
@Composable
fun HistoryCard(
    history: List<HistoryEntry>,
    canManage: Boolean,
    isDeleting: Boolean,
    error: String?,
    onAdd: () -> Unit,
    onEdit: (HistoryEntry) -> Unit,
    onDelete: (String) -> Unit,
    onOpenImage: (HistoryEntry, Int) -> Unit
) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    PrivateAccordion("პროექტის ისტორია") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (canManage) {
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("ისტორიის დამატება") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (history.isEmpty()) {
                Text("ამ პროექტზე ისტორიის ჩანაწერები ჯერ არ არის დამატებული.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            history.forEachIndexed { index, entry ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${history.size - index}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                                Text(
                                    formatDateTime(entry.timestamp),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (entry.comment.isNotBlank()) Text(entry.comment)
                        if (entry.images.isNotEmpty()) {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                entry.images.forEachIndexed { imageIndex, image ->
                                    RemoteImage(
                                        url = ApiConfig.imageUrl(image.thumbUrl),
                                        contentDescription = "ისტორიის სურათი ${imageIndex + 1}",
                                        modifier = Modifier
                                            .size(width = 96.dp, height = 72.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { onOpenImage(entry, imageIndex) }
                                    )
                                }
                            }
                        }
                        // Admins only (the server rejects others with 403).
                        if (canManage) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onEdit(entry) }, modifier = Modifier.weight(1f)) { Text("რედაქტირება") }
                                OutlinedButton(
                                    onClick = { pendingDelete = entry.id },
                                    enabled = !isDeleting,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) { Text("წაშლა") }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { id ->
        ConfirmDeleteDialog(
            message = "ნამდვილად გსურთ ამ ისტორიის ჩანაწერის წაშლა? ეს მოქმედება შეუქცევადია.",
            onConfirm = {
                pendingDelete = null
                onDelete(id)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
