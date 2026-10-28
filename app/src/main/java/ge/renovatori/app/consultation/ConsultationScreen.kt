package ge.renovatori.app.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.faq.FaqViewModel
import ge.renovatori.app.faq.faqSection
import ge.renovatori.app.ui.CompanyInfo
import ge.renovatori.app.ui.Section
import ge.renovatori.app.ui.formatMoney
import kotlinx.coroutines.launch
import java.net.URLEncoder

private data class Benefit(val title: String, val text: String)
private data class ConsultationPackage(val title: String, val price: Int, val items: List<String>, val highlighted: Boolean)

// Copy of views/pages/consultation-page/consultation-page.pug.
private val benefits = listOf(
    Benefit("💰 ბიუჯეტის განსაზღვრა", "თავიდან აიცილეთ ზედმეტი ხარჯები."),
    Benefit("🧠 პროფესიონალის რჩევები", "მიიღეთ გადაწყვეტილება გამოცდილების საფუძველზე."),
    Benefit("🏠 შეფასება ადგილზე", "რეალურ პირობებში ვაანალიზებთ ყველაფერს."),
    Benefit("📋 დეტალური ხარჯთაღრიცხვა", "მიიღეთ გამჭვირვალე, გასაგები და სანდო დოკუმენტი სამუშაოების ჩამონათვალითა და ფასებით."),
    Benefit("⏱ დროის დაზოგვა", "თავიდან იცილებთ შეცდომებს, რომლებიც გრაფიკს დაგიგრძელებთ."),
    Benefit("🤝 ნდობა და გამჭვირვალობა", "თქვენ ზუსტად იცით, რაში იხარჯება თანხა და რა შედეგს უნდა ელოდოთ.")
)

private val steps = listOf(
    Benefit("დაჯავშნა", "აირჩით სასურველი დრო ონლაინ ან სატელეფონო კონტაქტით."),
    Benefit("ვიზიტი ადგილზე", "სპეციალისტი ახორციელებს საჭირო წარმოჩენას და ზომებს."),
    Benefit("მოსმენა და ანალიზი", "თქვენი მოთხოვნების და პრიორიტეტების გამოკვლევა."),
    Benefit("დეტალური ხარჯთაღრიცხვა", "PDF/Excel- ფორმატში; სამუშაოების ეტაპობრივი გეგმა.")
)

private val packages = listOf(
    ConsultationPackage(
        "სტარტ პაკეტი", 150,
        listOf("📋 ობიექტზე ვიზიტი", "📐 სარემონტო სივრცის აზომვა", "🧰 ძირითადი ხარჯთაღრიცხვა", "💬 მოკლე რეკომენდაციები"),
        highlighted = false
    ),
    ConsultationPackage(
        "სტანდარტ პაკეტი", 250,
        listOf("📋 ობიექტის ვიზიტი და კონსულტაცია", "📐 სარემონტო სივრცის აზომვა", "📊 დეტალური ხარჯთაღრიცხვა (PDF)", "📸 ფოტო დოკუმენტაცია", "🕒 შედეგები 48 საათში"),
        highlighted = true
    ),
    ConsultationPackage(
        "პრემიუმ პაკეტი", 400,
        listOf("🏠 სრული ობიექტის ანალიზი", "📐 სარემონტო სივრცის აზომვა", "📊 ზუსტი ხარჯთაღრიცხვა და მასალების სია", "📅 სამუშაო გეგმა + შემდგომი კონსულტაცია"),
        highlighted = false
    )
)

// Index of the pricing item in the list below, for the "book" button in the intro.
private const val PRICING_INDEX = 3

/**
 * Consultation page (web `/consultation`). Ordering needs a signed-in user, which the app does not
 * have yet, so "დაჯავშნა" opens the web order form for the chosen package.
 */
@Composable
fun ConsultationScreen(faqViewModel: FaqViewModel = viewModel()) {
    val faqState by faqViewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val itemPadding = Modifier.padding(horizontal = 16.dp)

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "intro") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = itemPadding.fillMaxWidth()
            ) {
                Text("რემონტის კონსულტაცია", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "ზუსტი ხარჯთაღრიცხვა და პროფესიონალური რჩევები",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    "ჩვენი სპეციალისტი ეწვევა თქვენს ობიექტს, მოისმენს თქვენს საჭიროებებს და დაგეხმარებათ სწორი გადაწყვეტილების მიღებაში — სანამ დაიწყებთ რემონტს.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { scope.launch { listState.animateScrollToItem(PRICING_INDEX) } }) { Text("კონსულტაციის დაჯავშნა") }
            }
        }
        item(key = "benefits") {
            Section("რატომ ღირს წინასწარი კონსულტაცია?", modifier = itemPadding.padding(top = 16.dp)) {
                benefits.forEach { InfoCard(it) }
            }
        }
        item(key = "steps") {
            Section("როგორ მუშაობს სერვისი?", modifier = itemPadding.padding(top = 16.dp)) {
                steps.forEach { InfoCard(it) }
            }
        }
        item(key = "pricing") {
            Section("ფასი და პირობები", modifier = itemPadding.padding(top = 16.dp)) {
                packages.forEach { consultationPackage ->
                    PackageCard(consultationPackage) {
                        val type = URLEncoder.encode(consultationPackage.title, "UTF-8")
                        uriHandler.openUri("${CompanyInfo.WEBSITE_URL}/consultation/order?consultationType=$type")
                    }
                }
                // Web `alert-info`.
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "თუ სარემონტო მომსახურებას რენოვატორის გუნდს მიანდობთ, კონსულტაციის ფასი ავტომატურად ჩაითვლება მომსახურებაში და დამატებით არაფერს გადაიხდით.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        item(key = "faq-spacing") { Box(Modifier.padding(top = 16.dp)) }
        if (faqState.isLoading) {
            item(key = "faq-loading") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        } else {
            faqSection(faqState, onToggle = faqViewModel::toggle, itemModifier = itemPadding)
        }
    }
}

@Composable
private fun InfoCard(benefit: Benefit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(benefit.title, style = MaterialTheme.typography.titleMedium)
            Text(benefit.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PackageCard(consultationPackage: ConsultationPackage, onBook: () -> Unit) {
    // The recommended package gets the web's thick primary border and a filled button.
    val border = if (consultationPackage.highlighted) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ElevatedCard(Modifier.fillMaxWidth().then(if (border != null) Modifier.border(border, CardDefaults.elevatedShape) else Modifier)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(consultationPackage.title, style = MaterialTheme.typography.titleMedium)
            Text(formatMoney(consultationPackage.price), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            consultationPackage.items.forEach {
                Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (consultationPackage.highlighted) {
                Button(onClick = onBook, modifier = Modifier.fillMaxWidth()) { Text("დაჯავშნა") }
            } else {
                OutlinedButton(onClick = onBook, modifier = Modifier.fillMaxWidth()) { Text("დაჯავშნა") }
            }
        }
    }
}
