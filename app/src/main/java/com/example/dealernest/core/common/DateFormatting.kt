package com.example.dealernest.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// java.time isn't available on minSdk 24 without core library desugaring (not configured), so
// this uses the older java.text API instead - fine for our simple yyyy-MM-dd needs.
private const val API_DATE_PATTERN = "yyyy-MM-dd"

fun Long.toApiDateString(): String = SimpleDateFormat(API_DATE_PATTERN, Locale.US).format(Date(this))

fun todayApiDateString(): String = SimpleDateFormat(API_DATE_PATTERN, Locale.US).format(Date())

fun daysAgoApiDateString(days: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -days)
    return SimpleDateFormat(API_DATE_PATTERN, Locale.US).format(calendar.time)
}

/** "2026-07-21" -> "Tue" - used to label chart bars. Returns the input unchanged if unparseable. */
fun String.toShortWeekdayLabel(): String {
    val parsed = runCatching { SimpleDateFormat(API_DATE_PATTERN, Locale.US).parse(this) }.getOrNull() ?: return this
    return SimpleDateFormat("EEE", Locale.US).format(parsed)
}
