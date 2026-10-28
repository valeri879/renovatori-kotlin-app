package ge.renovatori.app.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.R
import ge.renovatori.app.blog.BlogCard
import ge.renovatori.app.faq.FaqViewModel
import ge.renovatori.app.faq.faqSection
import ge.renovatori.app.network.ApiConfig
import ge.renovatori.app.network.RemoteImage
import ge.renovatori.app.partner.Partner
import ge.renovatori.app.ui.CompanyInfo
import ge.renovatori.app.ui.formatDate
import ge.renovatori.app.ui.ContentCard
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.Routes
import kotlinx.coroutines.launch

private data class InfoCard(@param:DrawableRes val image: Int, val title: String, val text: String)

// Static copy of the web main page (views/pages/main-page/main-page.pug).
private val services = listOf(
    InfoCard(
        R.drawable.ic_service_calculator, "ხარჯთაღრიცხვა და კონსულტაცია",
        "გთავაზობთ პროფესიონალურ კონსულტაციას და დეტალურ ხარჯთაღრიცხვას — დაგეხმარებით ბიუჯეტის სწორ დაგეგმვაში და ყველაზე ეფექტური გადაწყვეტილების მიღებაში."
    ),
    InfoCard(
        R.drawable.ic_service_palette, "დიზაინი და დაგეგმარება",
        "დაგეხმარებით ინტერიერის ფუნქციურად და გემოვნებით მოწყობაში — ვქმნით ბალანსს კომფორტსა და თანამედროვე სტილს შორის."
    ),
    InfoCard(
        R.drawable.ic_service_helmet, "შიდა მოპირკეთება და სარემონტო სამუშაოები",
        "გთავაზობთ შიდა სარემონტო სამუშაოებს — შპალერი, საღებავი, გიფსოკარდონი, იატაკის მოპირკეთება და სხვა. შედეგი — ესთეტიკური, კომფორტული და დახვეწილი ინტერიერი."
    ),
    InfoCard(
        R.drawable.ic_service_paint_roll, "კოსმეტიკური რემონტი და განახლება",
        "თუ გსურთ უბრალოდ განაახლოთ უკვე გაკეთებული სივრცე — შევასრულებთ კოსმეტიკურ რემონტს სწრაფად, სუფთად და ხარისხიანად."
    ),
    InfoCard(
        R.drawable.ic_service_electric, "ელექტრო და სანტექნიკური სამუშაოები",
        "პროფესიონალი ოსტატების გუნდი უზრუნველყოფს ელექტრო და სანტექნიკური სისტემების მონტაჟს, შეცვლას და გამართულ მუშაობას უსაფრთხოების ყველა ნორმის დაცვით."
    ),
    InfoCard(
        R.drawable.ic_service_sofa, "ავეჯის დამზადება და მონტაჟი",
        "ვქმნით ინდივიდუალურ ავეჯს თქვენი სივრცისა და სტილის მიხედვით — სამზარეულოები, კარადები, თაროები და სხვა. ვუზრუნველყოფთ ზუსტ ზომებს, ხარისხიან მასალას და სუფთა მონტაჟს."
    )
)

private val completedWork = listOf(
    InfoCard(
        R.drawable.home_project_1, "პროფესიული მონტაჟი და აწყობა",
        "პროფესიონალური მონტაჟი — ავეჯი, ტექნიკა და ელემენტების აწყობა ზუსტი ზომებით და ხარისხიანი შესრულებით."
    ),
    InfoCard(
        R.drawable.home_project_2, "პროექტის მენეჯმენტი",
        "ვაკონტროლებთ ვადებს, ხშირად ვაწვდით პროგრესის ანგარიშს და ვმართავთ პროექტს სრულად, რათა ყველაფერი დროულად და შესაბამისად დაგეგმოს."
    ),
    InfoCard(
        R.drawable.home_project_3, "გარანტია და შემდგომი მომსახურება",
        "ვაძლევთ ხარისხის გარანტიას და ვატარებთ საჭირო შემდგომ მომსახურებას პრობლემების სწრაფად მოგვარებისთვის."
    )
)

// Each section is one LazyColumn item, so the price list has a stable index for the hero button.
private const val PRICE_SECTION_INDEX = 3
private val SECTION_SPACING = 32.dp

/** Home page (web `/`): hero, services, completed work, price comparison, latest posts and FAQ. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    faqViewModel: FaqViewModel = viewModel()
) {
    val state by homeViewModel.state.collectAsStateWithLifecycle()
    val faqState by faqViewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = faqState.isRefreshing,
        onRefresh = {
            homeViewModel.load()
            faqViewModel.refresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "hero") {
                Hero(onPricesClick = { scope.launch { listState.animateScrollToItem(PRICE_SECTION_INDEX) } })
            }
            item(key = "services") {
                Section(
                    "ჩვენ გთავაზობთ შემდეგ მომსახურებებს",
                    "მომსახურებები, რომლებსაც გთავაზობთ ჩვენი გუნდი."
                ) { services.forEach { ServiceCard(it) } }
            }
            item(key = "completed-work") {
                Section(
                    "ჩვენ მიერ შესრულებული პროექტები",
                    "პროექტები, რომლეიც ჩვენს მიერ შესრულდა"
                ) { completedWork.forEach { ImageCard(it) } }
            }
            item(key = "prices") {
                Section(
                    "კარკასული რემონტის ფასები",
                    "ჩვენ გთავაზობთ სრულ სარემონტო მომსახურებას მაქსიმალურად მისაღები ფასებით."
                ) { PriceComparison() }
            }
            // Shown only when the server has team members to show, like the web.
            if (state.partners.isNotEmpty()) {
                item(key = "team") {
                    Section(
                        "ჩვენი გუნდი",
                        "ჩვენი პარტნიორი პროფესიონალები, რომლებიც გამოცდილებით, პასუხისმგებლობით და ხარისხზე ორიენტირებული მუშაობით ქმნიან რენოვატორის სერვისს."
                    ) { TeamRow(state.partners) }
                }
            }
            item(key = "blogs") {
                Section(
                    "ბლოგები რემონტზე",
                    "წაიკითხეთ ბლოგები რემონტზე: ინტერიერის დიზაინი, სარემონტო ფასები, მასალების არჩევა და პრაქტიკული რჩევები ბინისა და სახლის განახლებისთვის."
                ) { LatestBlogs(state, onRetry = homeViewModel::loadBlogs) }
            }

            // FAQ rows are separate items (they expand), padded like the other sections.
            item(key = "faq-spacing") { Spacer(Modifier.height(SECTION_SPACING - 4.dp)) }
            if (faqState.isLoading) {
                item(key = "faq-loading") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            } else {
                faqSection(
                    faqState,
                    onToggle = faqViewModel::toggle,
                    itemModifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun Hero(onPricesClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val scrim = MaterialTheme.colorScheme.background

    Box(Modifier.fillMaxWidth().height(440.dp)) {
        Image(
            painter = painterResource(R.drawable.home_cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Same left-to-right fade as the web `.cover-block::after`, following light/dark theme.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(scrim.copy(alpha = 0.8f), scrim.copy(alpha = 0.1f))))
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Text(
                "რემონტი, რომელიც იწყება გეგმიდან და სრულდება სრულყოფილებით.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPricesClick, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Icon(painterResource(R.drawable.ic_dollar), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("ფასები")
                }
                // Web `btn-light`: a light filled button that dials the company phone.
                Button(
                    onClick = { uriHandler.openUri(CompanyInfo.PHONE_URI) },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(painterResource(R.drawable.ic_phone), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("კონტაქტი")
                }
            }
        }
    }
}

/** Shared [ge.renovatori.app.ui.Section] with the home page's horizontal padding and section spacing. */
@Composable
private fun Section(title: String, subtitle: String, content: @Composable () -> Unit) {
    ge.renovatori.app.ui.Section(
        title,
        subtitle,
        Modifier.padding(start = 16.dp, end = 16.dp, top = SECTION_SPACING),
        content
    )
}

@Composable
private fun ServiceCard(card: InfoCard) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painterResource(card.image),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(card.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ImageCard(card: InfoCard) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(card.image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(5f / 3f)
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(card.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


/** Web team Swiper as a horizontally scrolling row of cards. */
@Composable
private fun TeamRow(partners: List<Partner>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        // Let cards scroll edge to edge while the first/last still line up with the section padding.
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().layout { measurable, constraints ->
            val bleed = 16.dp.roundToPx()
            val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + 2 * bleed))
            layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
        }
    ) {
        items(partners, key = { it.id }) { PartnerCard(it) }
    }
}

@Composable
private fun PartnerCard(partner: Partner) {
    val name = partner.firstName.ifBlank { "პარტნიორი" }

    OutlinedCard(Modifier.width(200.dp)) {
        val photoModifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
        // Partners without a photo get the same neutral avatar as the web.
        if (partner.imageUrl != null) {
            RemoteImage(url = ApiConfig.imageUrl(partner.imageUrl), contentDescription = name, modifier = photoModifier)
        } else {
            Image(
                painterResource(R.drawable.unisex_avatar),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = photoModifier
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                partner.professions.joinToString(", ").ifBlank { "პროფესია არ არის მითითებული" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                // Two reserved lines keep cards the same height when one profession list wraps (web `h-100`).
                minLines = 2
            )
        }
    }
}

@Composable
private fun LatestBlogs(state: HomeUiState, onRetry: () -> Unit) {
    when {
        state.isLoadingBlogs -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.blogsError != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(state.blogsError, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            TextButton(onClick = onRetry) { Text("ხელახლა ცდა") }
        }

        state.latestBlogs.isEmpty() -> Text(
            "ბლოგები ჯერ არ მოიძებნა.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        else -> state.latestBlogs.forEach { BlogPreviewCard(it) }
    }
}

@Composable
private fun BlogPreviewCard(blog: BlogCard) {
    val navigator = LocalAppNavigator.current
    ContentCard(
        title = blog.title,
        onClick = { navigator.navigate(Routes.blog(blog.id)) },
        imageUrl = blog.coverImageUrl,
        description = blog.metaDescription.ifBlank { "სტატიის მოკლე აღწერა ხელმისაწვდომი არ არის." },
        footer = "📅 ${formatDate(blog.updatedAt ?: blog.createdAt)}",
        imageAspectRatio = 5f / 3f
    )
}
