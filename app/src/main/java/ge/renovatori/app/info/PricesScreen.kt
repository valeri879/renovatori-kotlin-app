package ge.renovatori.app.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ge.renovatori.app.home.PriceComparison
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes

/** Prices page (web `/prices`): intro, the "indicative prices" note and the package comparison. */
@Composable
fun PricesScreen() {
    val navigator = LocalAppNavigator.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        PageTitle("რემონტის ფასები")
        Text("ქვემოთ მოცემულია რემონტის საშუალო ფასები, რომლებიც მოიცავს თეთრი, შავი და მწვანე კარკასის სამუშაოებს.")
        // Web `alert-warning`: Bootstrap's warning palette works in both themes.
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD), contentColor = Color(0xFF664D03))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "მოცემული ფასები არის საორიენტაციო. საბოლოო ღირებულება განისაზღვრება ინდივიდუალურად — მომხმარებლის მოთხოვნებისა და ობიექტის თავისებურებების გათვალისწინებით.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { navigator.navigate(Routes.CALCULATOR) }) { Text("გამოიყენეთ კალკულატორი") }
            }
        }
        PriceComparison()
    }
}
