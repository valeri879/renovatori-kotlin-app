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

/** Web `formatPhone`: "+995 579 37 33 73" for a 9-digit (or +995-prefixed) number, "-" otherwise. */
fun formatPhone(value: String): String {
    val national = nationalPhone(value) ?: return "-"
    return "+995 ${national.substring(0, 3)} ${national.substring(3, 5)} ${national.substring(5, 7)} ${national.substring(7)}"
}

/** `tel:` link for [value], or null when it is not a Georgian mobile number. */
fun phoneUri(value: String): String? = nationalPhone(value)?.let { "tel:+995$it" }

private fun nationalPhone(value: String): String? {
    val digits = value.filter(Char::isDigit)
    return when {
        digits.length == 12 && digits.startsWith("995") -> digits.substring(3)
        digits.length == 9 -> digits
        else -> null
    }
}

/** Web `formatMoney(value, { currency: '$', fractionDigits: 2 })`: "$1 234.50". */
fun formatDollars(amount: Double): String =
    "$" + "%,.2f".format(Locale.US, amount).replace(',', ' ')

/** Web `formatDate(value, { includeTime: true })`: "dd/MM/yyyy HH:mm" in the device time zone. */
fun formatDateTime(instant: Instant?): String =
    instant?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "-"
