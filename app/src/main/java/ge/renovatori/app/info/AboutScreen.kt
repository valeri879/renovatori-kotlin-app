package ge.renovatori.app.info

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ge.renovatori.app.R
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.Routes

private data class NumberedItem(val number: String, val title: String, val text: String)
private data class IconItem(@param:DrawableRes val icon: Int, val title: String, val text: String)

// Copy of views/pages/about-page/about-page.pug.
private val values = listOf(
    NumberedItem("01", "დაგეგმვა", "სამუშაოს დაწყებამდე ვადგენთ ეტაპებს, ვადებს და ბიუჯეტს."),
    NumberedItem("02", "გამჭვირვალობა", "მომხმარებელი ხედავს რა კეთდება, რატომ კეთდება და რა ჯდება."),
    NumberedItem("03", "კონტროლი", "პროცესი, ხარჯები, გადახდები და ისტორია ერთ სივრცეში იყრის თავს.")
)

private val differences = listOf(
    IconItem(R.drawable.ic_clock, "შეთანხმებული ვადები", "დეტალური გრაფიკი გვეხმარება, სამუშაოები პროგნოზირებადად და ორგანიზებულად წარიმართოს."),
    IconItem(R.drawable.ic_service_calculator, "გამჭვირვალე ხარჯები", "წინასწარ ჩანს სამუშაოების მოცულობა, მასალები და თითოეული ეტაპის ღირებულება."),
    IconItem(R.drawable.ic_service_helmet, "პასუხისმგებლიანი გუნდი", "თითოეულ ობიექტს ვუდგებით ისე, როგორც სივრცეს, სადაც მომავალში ადამიანმა კომფორტულად უნდა იცხოვროს."),
    IconItem(R.drawable.ic_phone, "მუდმივი კომუნიკაცია", "მომხმარებელი პროცესის მიღმა არ რჩება: გადაწყვეტილებები, ცვლილებები და პროგრესი შეთანხმებულად მიდის.")
)

private val dashboardCards = listOf(
    "ფინანსები და პროგრესი" to "ჯამური ღირებულება, ჩასარიცხი თანხა და გადახდები",
    "ჩარიცხვების ისტორია" to "თითოეული გადახდა თარიღით",
    "პროექტის ისტორია" to "კომენტარები და ფოტო განახლებები",
    "პირადი ინფორმაცია" to "დაშვებული მომხმარებლებისთვის"
)

private val processSteps = listOf(
    NumberedItem("1", "კონსულტაცია და დათვალიერება", "ვისმენთ მოთხოვნებს, ვაფასებთ სივრცეს და ვადგენთ საწყის სამუშაო მოცულობას."),
    NumberedItem("2", "ხარჯთაღრიცხვა და გრაფიკი", "ვამზადებთ ბიუჯეტს, ეტაპებს და ვადებს, რომ გადაწყვეტილება ინფორმირებულად მიიღოთ."),
    NumberedItem("3", "სამუშაოების მართვა", "ვაკოორდინირებთ გუნდს, მასალებს და შესრულების ხარისხს შეთანხმებული გეგმის მიხედვით."),
    NumberedItem("4", "ხარისხის შემოწმება და ჩაბარება", "დასრულებულ პროექტს ვამოწმებთ დეტალებში და მომხმარებელთან ერთად ვაბარებთ შედეგს.")
)

/** About page (web `/about`). */
@Composable
fun AboutScreen() {
    val navigator = LocalAppNavigator.current

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        AboutHero(
            onConsultation = { navigator.navigate(Routes.CONSULTATION) },
            onProjects = { navigator.navigate(Routes.PROJECT_LIST) }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(40.dp),
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                values.forEach { value ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(value.number, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(value.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(value.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            LabeledSection(
                label = "ვინ ვართ ჩვენ",
                title = "სარემონტო გუნდი, რომელიც პროცესს თავიდან ბოლომდე მართავს",
                text = "რენოვატორი შეიქმნა ერთი მარტივი მიზეზით: რემონტი არ უნდა იყოს გაურკვეველი, დაუსრულებელი და სტრესული პროცესი. ჩვენ ვაერთიანებთ დაგეგმვას, ხარჯთაღრიცხვას, სამუშაოების კოორდინაციას და ხარისხის კონტროლს ერთ სისტემურ სერვისში."
            ) {
                differences.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(
                            painterResource(item.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(item.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            LabeledSection(
                label = "კლიენტის სივრცე",
                title = "პროექტის ისტორია და ხარჯები ერთ ადგილზე",
                text = "რემონტის პროცესში კლიენტს არ უნდა უწევდეს ინფორმაციის გამოცნობა. ამიტომ პროექტის დეტალურ გვერდზე ვაჩვენებთ იმ მონაცემებს, რაც რეალურ კონტროლს ქმნის: ფინანსურ მდგომარეობას, გადახდებს, ვადებს და შესრულებული სამუშაოების ისტორიას."
            ) {
                OutlinedButton(onClick = { navigator.navigate(Routes.PROJECT_LIST) }) { Text("პროექტების ნახვა") }
                ClientDashboardPreview()
            }

            LabeledSection(
                label = "პროცესი",
                title = "როგორ ვმუშაობთ",
                text = "ჩვენი პროცესი მარტივია, მაგრამ მკაფიო. ჯერ ვარკვევთ საჭიროებებს, შემდეგ ვგეგმავთ სამუშაოებს და მხოლოდ ამის შემდეგ იწყება შესრულება."
            ) {
                processSteps.forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Text(step.number, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(step.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(step.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "გეგმავთ რემონტს?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    "მოგვწერეთ ან დაგვიტოვეთ მოთხოვნა. დაგეხმარებით რეალური ბიუჯეტის, ვადებისა და სამუშაო მოცულობის წინასწარ განსაზღვრაში.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { navigator.navigate(Routes.CONSULTATION) }) { Text("კონსულტაციის მოთხოვნა") }
                    OutlinedButton(onClick = { navigator.navigate(Routes.PRICES) }) { Text("ფასების ნახვა") }
                }
            }
        }
    }
}

@Composable
private fun AboutHero(onConsultation: () -> Unit, onProjects: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Image(
            painterResource(R.drawable.home_cover),
            contentDescription = "თანამედროვე გარემონტებული ინტერიერი",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        // Web `.about-hero-overlay` (mobile variant): dark top-to-bottom fade under white text.
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.86f), Color.Black.copy(alpha = 0.52f))))
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 40.dp)) {
            Text("რენოვატორი", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                "რემონტი, რომელსაც თავიდან ბოლომდე აკონტროლებთ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "ვეხმარებით მომხმარებლებს ბინის, სახლისა და კომერციული სივრცის რემონტში ისე, რომ პროცესი იყოს დაგეგმილი, ხარჯები გამჭვირვალე და შედეგი შეთანხმებულ ხარისხთან ახლოს.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConsultation) { Text("კონსულტაცია") }
                // Web `btn-outline-light` on the dark photo.
                OutlinedButton(onClick = onProjects, border = BorderStroke(1.dp, Color.White)) {
                    Text("პროექტები", color = Color.White)
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                    .padding(20.dp)
            ) {
                Text("სერვისის იდეა", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                Text("რემონტი არ უნდა იყოს გაურკვეველი, დაუსრულებელი და სტრესული პროცესი.", color = Color.White)
            }
        }
    }
}

/** Section with the web's small primary label above a heading and lead text, then [content]. */
@Composable
private fun LabeledSection(label: String, title: String, text: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

/** Illustrative card of what a client sees on their project page (web `.about-dashboard`). */
@Composable
private fun ClientDashboardPreview() {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(6.dp)) {
                Text(
                    "ხილულია მხოლოდ კლიენტისთვის",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Text("პროექტის გამჭვირვალე მართვა", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    Text(
                        "პროექტის დასრულების პროგრესი",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text("შეთანხმებული ვადებით", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { 0.68f }, modifier = Modifier.fillMaxWidth().height(8.dp))
            }
            dashboardCards.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, value) ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
