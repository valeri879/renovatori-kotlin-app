package ge.renovatori.app.calculator

import ge.renovatori.app.network.ApiClient
import org.json.JSONArray
import org.json.JSONObject

// Option values are sent exactly as the web form posts them; the backend compares them literally
// (helpers/calculator.helper.js in the backend).
val elevatorOptions = listOf("passenger" to "სამგზავრო", "cargo" to "სატვირთო", "none" to "არა")
val floorTypeOptions = listOf("laminate" to "ლამინატი", "tiles" to "მეტლახი / გრანიტი")
val ceilingTypeOptions = listOf("french" to "გასაჭიმი (ფრანგული)", "dryWall" to "თაბაშირ მუყაო")
val carcasTypeOptions = listOf(
    Triple("green", "მწვანე კარკასი", "https://renovatori.ge/blog/693e40002ed62d127ab05108"),
    Triple("white", "თეთრი კარკასი", "https://renovatori.ge/blog/693e40342ed62d127ab0510e"),
    Triple("black", "შავი კარკასი", "https://renovatori.ge/blog/693d8d61291bc84d00b6f427"),
    Triple("old", "ძველი რემონტი", null)
)

/** One living room as typed in the form (numbers stay strings until validation). */
data class RoomInput(
    val key: Int,
    val area: String = "",
    val floorType: String = "laminate",
    val ceilingType: String = "french",
    val perimeter: String = "",
    val electricalPoints: String = ""
)

data class BathroomInput(
    val key: Int,
    val area: String = "",
    val ceilingType: String = "french",
    val perimeter: String = "",
    val electricalPoints: String = "",
    val builtInToilet: Boolean = false
)

/** Every field of the web calculator form, with the web's defaults. */
data class CalculatorForm(
    val phoneNumber: String = "",
    val totalArea: String = "",
    val floor: String = "",
    val elevator: String = "passenger",
    val height: String = "",
    val wetAreas: String = "",
    val doors: String = "",
    val boilers: String = "",
    val screeding: Boolean = false,
    val removal: Boolean = false,
    val carcasType: String = "green",
    val rooms: List<RoomInput> = listOf(RoomInput(key = 0)),
    val bathrooms: List<BathroomInput> = listOf(BathroomInput(key = 0))
)

/** Parses a user-typed number, accepting a decimal comma; null when empty or invalid. */
fun parseNumber(value: String): Double? = value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }

/** Builds the `POST /calculator` body (shape documented in the backend's compose-content-task.md). */
fun CalculatorForm.toRequestJson(): JSONObject = JSONObject().apply {
    put("phoneNumber", phoneNumber.trim())
    put("totalArea", parseNumber(totalArea))
    put("floor", parseNumber(floor))
    put("elevator", elevator)
    put("height", parseNumber(height))
    put("wetAreas", parseNumber(wetAreas))
    put("doors", parseNumber(doors))
    put("boilers", parseNumber(boilers))
    put("screedingOption", if (screeding) "1" else "0")
    // Unchecked checkboxes are simply absent, like an HTML form post.
    if (removal) put("removal", "on")
    put("carcasType", carcasType)
    put("rooms", JSONArray(rooms.map { room ->
        JSONObject()
            .put("area", parseNumber(room.area))
            .put("floorType", room.floorType)
            .put("ceilingType", room.ceilingType)
            .put("perimeter", parseNumber(room.perimeter))
            .put("electricalPoints", parseNumber(room.electricalPoints))
    }))
    put("bathrooms", JSONArray(bathrooms.map { bathroom ->
        JSONObject()
            .put("area", parseNumber(bathroom.area))
            .put("ceilingType", bathroom.ceilingType)
            .put("perimeter", parseNumber(bathroom.perimeter))
            .put("electricalPoints", parseNumber(bathroom.electricalPoints))
            .apply { if (bathroom.builtInToilet) put("builtInToilet", "on") }
    }))
}

data class BreakdownItem(val label: String, val amount: Double, val percent: Double)

/** `POST /calculator` result plus the "your parameters" rows of the web result page. */
data class CalculationResult(
    val clientTotal: Double,
    val clientPricePerArea: Double,
    val breakdown: List<BreakdownItem>,
    val parameters: List<Pair<String, String>>
)

suspend fun ApiClient.calculate(form: CalculatorForm): CalculationResult =
    parseResult(post("/calculator", form.toRequestJson()).getJSONObject("data"), form)

private fun parseResult(data: JSONObject, form: CalculatorForm): CalculationResult {
    val info = data.optJSONObject("info") ?: JSONObject()
    val breakdown = data.optJSONArray("breakdown") ?: JSONArray()
    val ceilingTypes = form.rooms.map { it.ceilingType } + form.bathrooms.map { it.ceilingType }

    // Same rows and wording as views/pages/calculator-result-page/calculator-result-page.pug.
    val parameters = listOf(
        "ბინის ფართობი:" to "${formatNumber(info.opt("totalArea"))} მ²",
        "ოთახები:" to form.rooms.size.toString(),
        "აბაზანა:" to form.bathrooms.size.toString(),
        "კედლები:" to "${formatNumber(info.opt("wallsTotalArea"))} მ²",
        "აბაზანის კედლები:" to "${formatNumber(info.opt("bathroomWallsTotalArea"))} მ²",
        "ჭერის სიმაღლე:" to "${formatNumber(info.opt("height"))} სმ",
        "ჭერის ტიპი:" to "${ceilingTypes.count { it == "french" }} გასაჭიმი, ${ceilingTypes.count { it == "dryWall" }} თაბაშირ მუყაო",
        "იატაკის ტიპი:" to "${form.rooms.count { it.floorType == "laminate" }} ლამინატი, ${form.rooms.count { it.floorType == "tiles" }} მეტლახი/გრანიტი",
        "MDF-ის კარი:" to formatNumber(info.opt("doors")),
        "ბოილერი (გათბობის ქვები):" to formatNumber(info.opt("boilers")),
        "სველი წერტილები:" to formatNumber(info.opt("wetPoints")),
        "ელექტრო წერტილები:" to formatNumber(info.opt("electricalPoints")),
        "სართული:" to formatNumber(info.opt("floor")),
        "ლიფტი:" to (elevatorOptions.firstOrNull { it.first == info.optString("elevator") }?.second ?: "-")
    )

    return CalculationResult(
        clientTotal = data.optDouble("clientTotal", 0.0),
        clientPricePerArea = data.optDouble("clientPricePerArea", 0.0),
        breakdown = List(breakdown.length()) {
            val item = breakdown.getJSONObject(it)
            BreakdownItem(item.optString("label"), item.optDouble("amount", 0.0), item.optDouble("percent", 0.0))
        },
        parameters = parameters
    )
}

/** Prints an `info` value like the web: whole numbers without decimals, others to 2 places, "-" when missing. */
private fun formatNumber(value: Any?): String {
    val number = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: return value.ifBlank { "-" }
        else -> return "-"
    }
    return if (number % 1.0 == 0.0) number.toLong().toString() else "%.2f".format(java.util.Locale.US, number).trimEnd('0').trimEnd('.')
}
