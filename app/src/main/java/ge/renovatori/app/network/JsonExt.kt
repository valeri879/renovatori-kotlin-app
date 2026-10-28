package ge.renovatori.app.network

import org.json.JSONObject
import java.time.Instant

// org.json's optString returns "null"/"" for missing values, so nullable fields are read explicitly.

fun JSONObject.stringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else getString(key)

fun JSONObject.doubleOrNull(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

/** Reads an ISO-8601 date; malformed values are treated as missing rather than crashing the screen. */
fun JSONObject.instantOrNull(key: String): Instant? =
    stringOrNull(key)?.let { runCatching { Instant.parse(it) }.getOrNull() }
