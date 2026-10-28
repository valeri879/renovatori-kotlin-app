package ge.renovatori.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ge.renovatori.app.ui.formatMoney
import ge.renovatori.app.ui.theme.success

/** Package comparison table (web `price-list.component.pug`) as weighted rows instead of an HTML table. */
@Composable
fun PriceComparison() {
    val uriHandler = LocalUriHandler.current

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Text("პაკეტების შედარება", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "აირჩიეთ თქვენთვის სასურველი კარკასის პაკეტი სერვისების მიხედვით.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
            Text("ოპცია", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(LABEL_WEIGHT).padding(start = 4.dp))
            pricePackages.forEach { pricePackage ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { uriHandler.openUri(pricePackage.blogUrl) }
                        .padding(4.dp)
                ) {
                    Text(
                        pricePackage.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${formatMoney(pricePackage.pricePerSquareMeter)}-დან / მ²",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        priceGroups.forEach { group ->
            Text(
                group.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            group.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(LABEL_WEIGHT).padding(start = 4.dp))
                    listOf(row.green, row.white, row.black).forEach { included ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Icon(
                                if (included) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (included) "შედის" else "არ შედის",
                                tint = if (included) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val LABEL_WEIGHT = 1.6f
