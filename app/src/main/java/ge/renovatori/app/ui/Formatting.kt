package ge.renovatori.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Same output as the web `formatDate` helper: "dd/MM/yyyy" in the device time zone, "-" when missing. */
fun formatDate(instant: Instant?): String =
    instant?.atZone(ZoneId.systemDefault())?.format(dateFormatter) ?: "-"

/** Same output as the web `formatMoney` helper for whole lari amounts: "1 500 ₾". */
fun formatMoney(amount: Int): String =
    "%,d".format(Locale.US, amount).replace(',', ' ') + " ₾"

/** Web `formatMoney(value, { fractionDigits })`: "20 887.50 ₾". */
fun formatMoney(amount: Double, fractionDigits: Int): String =
    "%,.${fractionDigits}f".format(Locale.US, amount).replace(',', ' ') + " ₾"
