package ge.renovatori.app.calculator

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.renovatori.app.faq.Faq
import ge.renovatori.app.faq.FaqItem
import ge.renovatori.app.ui.HtmlContent
import ge.renovatori.app.ui.LatestBlogCardsSection
import ge.renovatori.app.ui.LatestProjectsSection
import ge.renovatori.app.ui.LocalAppNavigator
import ge.renovatori.app.ui.PageTitle
import ge.renovatori.app.ui.Routes
import ge.renovatori.app.ui.formatMoney
import ge.renovatori.app.ui.requiredLabel
import ge.renovatori.app.ui.theme.success

private const val AREA_HELP_URL = "https://renovatori.ge/blog/69a88631b50c341432263980"
private const val POINTS_HELP_URL = "https://renovatori.ge/blog/698c9a0229b6ab9da1589692"

private val benefits = listOf(
    Triple("⏱️", "სწრაფი", "შედეგი წამებში, ხელით თვლის გარეშე"),
    Triple("💸", "რეალური ბიუჯეტი", "გათვლა ბაზრის ფასებზე დაყრდნობით"),
    Triple("📊", "დეტალური შედეგი", "სამუშაოების ტიპების მიხედვით დაყოფილი"),
    Triple("📱", "ყველა მოწყობილობაზე", "მუშაობს მობილურსა და კომპიუტერზე")
)

private val calculatorFaqs = listOf(
    Faq("calc-1", "ზუსტია კალკულატორი?", "დიახ, გათვლები ეფუძნება რეალურ ბაზრის ფასებს და პროფესიონალების მონაცემებს.", null),
    Faq("calc-2", "შემიძლია ფასის შენახვა?", "დიახ, შეგიძლია შედეგის შენახვა ან გაზიარება.", null)
)

/** Renovation calculator (web `/calculator` and its result page). */
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val result = state.result

    if (result != null) {
        // System back returns to the filled-in form instead of leaving the calculator.
        BackHandler(onBack = viewModel::closeResult)
        CalculatorResult(result, state.resultProjects, onEdit = viewModel::closeResult)
    } else {
        CalculatorForm(state, viewModel)
    }
}

@Composable
private fun CalculatorForm(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    val scrollState = rememberScrollState()
    var openFaqId by rememberSaveable { mutableStateOf<String?>(null) }

    // Each step starts at the top of the form card.
    LaunchedEffect(state.step) { scrollState.animateScrollTo(0) }

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)
    ) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("კალკულატორი", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                        Text(
                            "${state.step} / $CALCULATOR_STEPS",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { state.step / CALCULATOR_STEPS.toFloat() },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "კალკულატორის ფორმის პროგრესი" }
                )

                when (state.step) {
                    1 -> StepOne(state, viewModel)
                    2 -> StepTwo(state, viewModel)
                    3 -> StepThree(state, viewModel)
                    4 -> StepFour(state, viewModel)
                    5 -> StepFive(state, viewModel)
                    else -> StepSix(state, viewModel)
                }

                state.submitError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                StepButtons(state, viewModel)
            }
        }

        PageTitle("რემონტის კალკულატორი — გამოთვალე რემონტის ფასი ონლაინ")
        benefits.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (icon, title, text) ->
                    OutlinedCard(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(icon, style = MaterialTheme.typography.headlineSmall)
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        HtmlContent(calculatorInfoHtml)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ხშირად დასმული კითხვები", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            calculatorFaqs.forEach { faq ->
                FaqItem(faq, expanded = openFaqId == faq.id, onToggle = { openFaqId = if (openFaqId == faq.id) null else faq.id })
            }
        }
        LatestProjectsSection("ბოლოს შესრულებული პროექტები", state.latest.projects, showDescription = true)
        LatestBlogCardsSection("უახლესი ბლოგები", state.latest.blogs)
    }
}

@Composable
private fun StepOne(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    val form = state.form
    FormField(
        "📱 თქვენი ტელეფონის ნომერი",
        "ჩაწერე თქვენი აქტიური ტელეფონის ნომერი +995 ფორმატის გარეშე, მაგალითად 579123123",
        form.phoneNumber, state.errors["phoneNumber"], KeyboardType.Phone
    ) { value -> viewModel.edit("phoneNumber") { it.copy(phoneNumber = value.filter(Char::isDigit).take(9)) } }
    FormField(
        "📐 სახლის ფართობი (გარდა აივნისა)",
        "ჩაწერეთ თქვენი ბინის სრული ფართობი, იმ შეთხვევაში თუ გეგმავთ აბაზანის რემონტსაც - თუ არა, ჩაწერეთ აბაზანის ფართობის გამოკლებით.",
        form.totalArea, state.errors["totalArea"], helpUrl = AREA_HELP_URL, helpTitle = "როგორ დავთვალოთ ფართობი?"
    ) { value -> viewModel.edit("totalArea") { it.copy(totalArea = value) } }
    FormField(
        "🏢 სართული",
        "ჩაწერეთ სართული, რომელზეც მდებარეობს ბინა. თუ პირველ სართულზეა, მიუთითეთ 1.",
        form.floor, state.errors["floor"], KeyboardType.Number
    ) { value -> viewModel.edit("floor") { it.copy(floor = value) } }
}

@Composable
private fun StepTwo(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    val form = state.form
    SelectField(
        "🛗 ლიფტი",
        "აირჩიეთ ლიფტის ტიპი თქვენს კორპუსში. თუ ლიფტი არ არის, მონიშნეთ „არა“.",
        elevatorOptions, form.elevator
    ) { value -> viewModel.edit("elevator") { it.copy(elevator = value) } }
    FormField(
        "📏 ჭერის სიმაღლე (სანტიმეტრებში)",
        "ჩაწერეთ სუფთა სიმაღლე სანტიმეტრებში, მაგალითად 280 ან 300.",
        form.height, state.errors["height"], KeyboardType.Number
    ) { value -> viewModel.edit("height") { it.copy(height = value) } }
    FormField(
        "🚰 სველი წერტილები",
        "სველ წერტილებს წარმოადგენს, ცივი-ცხელი წყალი, კანალიზაცია, ტრაპი, შემრევი ონკანი, გათბობოის რადიატორი.",
        form.wetAreas, state.errors["wetAreas"], KeyboardType.Number,
        helpUrl = "$POINTS_HELP_URL#wetAreas", helpTitle = "რა არის სველი წერტილები?"
    ) { value -> viewModel.edit("wetAreas") { it.copy(wetAreas = value) } }
}

@Composable
private fun StepThree(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    val form = state.form
    FormField(
        "🚪 შიდა კარი (MDF-ის შიდა კარი)",
        "მიუთითეთ მხოლოდ შიდა MDF კარების რაოდენობა. თუ კარები არ იცვლება, ჩაწერეთ 0.",
        form.doors, state.errors["doors"], KeyboardType.Number,
        helpUrl = "$POINTS_HELP_URL#doorTypes", helpTitle = "რა არის MDF-ის კარი?"
    ) { value -> viewModel.edit("doors") { it.copy(doors = value) } }
    FormField(
        "🔥 ბოილერი (ცენტრალური გათბობის ქვაბი(ები))",
        "ჩაწერეთ ცენტრალური გათბობის ქვაბების რაოდენობა.",
        form.boilers, state.errors["boilers"], KeyboardType.Number
    ) { value -> viewModel.edit("boilers") { it.copy(boilers = value) } }
    FieldHeader("🧩 იატაკის მოხვეწა (ტიაშკა)", "აირჩიეთ „კი“, თუ საჭიროა იატაკის მოხვეწის (ტიაშკის) შესრულება.")
    RadioGroup(listOf(true to "კი", false to "არა"), form.screeding) { value ->
        viewModel.edit("screeding") { it.copy(screeding = value) }
    }
}

@Composable
private fun StepFour(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    FieldHeader(
        "♻️ დემონტაჟი (იატაკის აყრა, კედლების ჩამოხევა, კაფელის დემონტაჟი)",
        "მონიშნეთ იმ შემთხვევაში, თუ გასაკეთებელია სრული დემონტაჟი, კედლები, აბაზანა, იატაკი, ჭერი"
    )
    CheckboxRow("დემონტაჟი", state.form.removal) { value -> viewModel.edit("removal") { it.copy(removal = value) } }
}

@Composable
private fun StepFive(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    val uriHandler = LocalUriHandler.current
    FieldHeader(
        "არსებული მდგომარეობა (მწვანე, თეთრი, შავი კარკასი)",
        "მონიშნეთ ბინის მიმდინარე მდგომარეობა, რათა სწორად დაითვალოს სამუშაოების მოცულობა."
    )
    Column(Modifier.selectableGroup()) {
        carcasTypeOptions.forEach { (value, label, helpUrl) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioRow(label, selected = state.form.carcasType == value, modifier = Modifier.weight(1f)) {
                    viewModel.edit("carcasType") { it.copy(carcasType = value) }
                }
                // Each carcass type links to the post that explains it, like the web "?" icons.
                helpUrl?.let { url ->
                    IconButton(onClick = { uriHandler.openUri(url) }) {
                        Icon(Icons.Default.Info, contentDescription = "რა არის $label?", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    state.form.rooms.forEachIndexed { index, room ->
        RowCard("🛋️ ოთახი #${index + 1}", canRemove = state.form.rooms.size > 1, onRemove = { viewModel.removeRoom(room.key) }) {
            fun update(field: String, change: (RoomInput) -> RoomInput) = viewModel.edit("rooms.${room.key}.$field") { form ->
                form.copy(rooms = form.rooms.map { if (it.key == room.key) change(it) else it })
            }
            FormField(
                "📐 ფართობი", "ჩაწერეთ ოთახის იატაკის ფართობი კვადრატულ მეტრებში.",
                room.area, state.errors["rooms.${room.key}.area"], helpUrl = AREA_HELP_URL, helpTitle = "როგორ დავთვალოთ ფართობი?"
            ) { value -> update("area") { it.copy(area = value) } }
            SelectField("🧱 იატაკის ტიპი", "აირჩიეთ კონკრეტულად ამ ოთახისთვის იატაკის ტიპი.", floorTypeOptions, room.floorType) { value ->
                update("floorType") { it.copy(floorType = value) }
            }
            SelectField("🪜 ჭერის ტიპი", "აირჩიეთ კონკრეტულად ამ ოთახისთვის ჭერის ტიპი.", ceilingTypeOptions, room.ceilingType) { value ->
                update("ceilingType") { it.copy(ceilingType = value) }
            }
            FormField(
                "📏 პერიმეტრი (მეტრებში)", "კედლების ჯამური სიგრძე, მაგალითად: 4მ + 3მ + 4მ + 3მ = 14 მეტრი პერიმეტრი",
                room.perimeter, state.errors["rooms.${room.key}.perimeter"],
                helpUrl = "$POINTS_HELP_URL#perimeter", helpTitle = "როგორ დავთვალოთ ოთახის პერიმეტრი?"
            ) { value -> update("perimeter") { it.copy(perimeter = value) } }
            FormField(
                "⚡️ ელექტრო წერტილები", "ელექტრო წერტილები შეიძლება იყოს: ნათურა, ჩამრთველი, შტეფსელი",
                room.electricalPoints, state.errors["rooms.${room.key}.electricalPoints"], KeyboardType.Number,
                helpUrl = "$POINTS_HELP_URL#electricalPoints", helpTitle = "როგორ დავთვალოთ ელექტრო წერტილები?"
            ) { value -> update("electricalPoints") { it.copy(electricalPoints = value) } }
        }
    }
    FilledTonalButton(onClick = viewModel::addRoom) { Text("ოთახის დამატება") }
}

@Composable
private fun StepSix(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    state.form.bathrooms.forEachIndexed { index, bathroom ->
        RowCard("🛁 აბაზანა #${index + 1}", canRemove = state.form.bathrooms.size > 1, onRemove = { viewModel.removeBathroom(bathroom.key) }) {
            fun update(field: String, change: (BathroomInput) -> BathroomInput) = viewModel.edit("bathrooms.${bathroom.key}.$field") { form ->
                form.copy(bathrooms = form.bathrooms.map { if (it.key == bathroom.key) change(it) else it })
            }
            FormField(
                "📐 ფართობი მ²", "ჩაწერეთ აბაზანის იატაკის ფართობი კვადრატულ მეტრებში.",
                bathroom.area, state.errors["bathrooms.${bathroom.key}.area"], helpUrl = AREA_HELP_URL, helpTitle = "როგორ დავთვალოთ ფართობი?"
            ) { value -> update("area") { it.copy(area = value) } }
            SelectField("🪜 ჭერის ტიპი", "აირჩიეთ კონკრეტულად ამ აბაზანისთვის ჭერის ტიპი.", ceilingTypeOptions, bathroom.ceilingType) { value ->
                update("ceilingType") { it.copy(ceilingType = value) }
            }
            FormField(
                "📏 პერიმეტრი (მეტრებში)", "კედლების ჯამური სიგრძე, მაგალითად: 4მ + 3მ + 4მ + 3მ = 14 მეტრი პერიმეტრი",
                bathroom.perimeter, state.errors["bathrooms.${bathroom.key}.perimeter"],
                helpUrl = "$POINTS_HELP_URL#perimeter", helpTitle = "როგორ დავთვალოთ ოთახის პერიმეტრი?"
            ) { value -> update("perimeter") { it.copy(perimeter = value) } }
            FormField(
                "⚡️ ელექტრო წერტილები", "აბაზანის ელექტრო წერტილები შეიძლება იყოს: ნათურა, გამწოვი, ჩამრთველი, შტეფსელი",
                bathroom.electricalPoints, state.errors["bathrooms.${bathroom.key}.electricalPoints"], KeyboardType.Number,
                helpUrl = "$POINTS_HELP_URL#electricalPoints", helpTitle = "როგორ დავთვალოთ ელექტრო წერტილები?"
            ) { value -> update("electricalPoints") { it.copy(electricalPoints = value) } }
            CheckboxRow("🚽 ჩასაშენებელი უნიტაზი", bathroom.builtInToilet) { value -> update("builtInToilet") { it.copy(builtInToilet = value) } }
            Text(
                "მონიშნეთ მხოლოდ იმ შემთხვევაში, თუ იგეგმება ჩასაშენებელი უნიტაზის მონტაჟი.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    FilledTonalButton(onClick = viewModel::addBathroom) { Text("აბაზანის დამატება") }
}

@Composable
private fun StepButtons(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    if (state.step == 1) {
        Button(onClick = viewModel::next, modifier = Modifier.fillMaxWidth()) { Text("შემდეგი") }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = viewModel::back, enabled = !state.isSubmitting, modifier = Modifier.weight(1f)) { Text("უკან") }
        if (state.step < CALCULATOR_STEPS) {
            Button(onClick = viewModel::next, modifier = Modifier.weight(1f)) { Text("შემდეგი") }
        } else {
            // Web `btn-success` submit; disabled with a spinner while the server calculates.
            Button(
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.success),
                modifier = Modifier.weight(1f)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text("ფასის დადგენა")
                }
            }
        }
    }
}

/** Bold label with the red required asterisk, an optional "?" help link and the web's grey hint. */
@Composable
private fun FieldHeader(label: String, help: String, helpUrl: String? = null, helpTitle: String? = null) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(requiredLabel(label), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
            helpUrl?.let { url ->
                IconButton(onClick = { uriHandler.openUri(url) }) {
                    Icon(Icons.Default.Info, contentDescription = helpTitle, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormField(
    label: String,
    help: String,
    value: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    helpUrl: String? = null,
    helpTitle: String? = null,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeader(label, help, helpUrl, helpTitle)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label, სავალდებულო" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(label: String, help: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeader(label, help)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = options.firstOrNull { it.first == selected }?.second.orEmpty(),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = {
                        onSelect(value)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
private fun <T> RadioGroup(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(Modifier.selectableGroup()) {
        options.forEach { (value, label) -> RadioRow(label, selected == value) { onSelect(value) } }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, modifier: Modifier = Modifier, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(8.dp))
        Text(label)
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange).padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.size(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

/** Room / bathroom block with its "წაშლა" button (disabled for the last remaining one, like the web). */
@Composable
private fun RowCard(title: String, canRemove: Boolean, onRemove: () -> Unit, content: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
            OutlinedButton(
                onClick = onRemove,
                enabled = canRemove,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("წაშლა") }
        }
    }
}

/** Web calculator result page: total, per-m² price, breakdown, the user's parameters and next steps. */
@Composable
private fun CalculatorResult(result: CalculationResult, projects: List<ge.renovatori.app.project.ProjectCard>, onEdit: () -> Unit) {
    val navigator = LocalAppNavigator.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp)
            ) {
                Text("თქვენი რემონტის სავარაუდო ღირებულება", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                Text(formatMoney(result.clientTotal, 2), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("1 მ²-ის ფასი: ${formatMoney(result.clientPricePerArea, 2)}", textAlign = TextAlign.Center)
                Text(
                    "მიახლოებითი შეფასებაა — ზუსტ ფასს ადგილზე უფასო დათვალიერების შემდეგ დაგიდასტურებთ.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("რაში იხდით თანხას", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (result.breakdown.isEmpty()) {
                    Text("დეტალური ჩაშლა მიუწვდომელია.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    result.breakdown.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row {
                                Text(item.label, modifier = Modifier.weight(1f))
                                Text(formatMoney(item.amount, 2), fontWeight = FontWeight.SemiBold)
                            }
                            LinearProgressIndicator(
                                progress = { (item.percent / 100).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ჯამი", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            formatMoney(result.clientTotal, 2),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.success
                        )
                    }
                }
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("თქვენი პარამეტრები", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                result.parameters.forEachIndexed { index, (label, value) ->
                    if (index > 0) HorizontalDivider()
                    Row(Modifier.padding(vertical = 8.dp)) {
                        Text(label, modifier = Modifier.weight(1f))
                        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                    }
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("პარამეტრების შეცვლა") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Text("მზად ხართ რემონტის დასაწყებად?", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    "დაჯავშნეთ უფასო კონსულტაცია და მიიღეთ ზუსტი შეფასება ჩვენი სპეციალისტისგან.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { navigator.navigate(Routes.CONSULTATION) }, modifier = Modifier.fillMaxWidth()) { Text("კონსულტაციის დაჯავშნა") }
                OutlinedButton(onClick = { navigator.navigate(Routes.CONTACT) }, modifier = Modifier.fillMaxWidth()) { Text("დაგვიკავშირდით") }
            }
        }

        LatestProjectsSection("ჩვენი ნამუშევრები", projects, showDescription = true)
    }
}
