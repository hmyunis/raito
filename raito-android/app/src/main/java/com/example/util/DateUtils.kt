package com.example.util

import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Date
import java.util.Locale

object DateUtils {
  private val yyyyMMddFormatter: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

  private val yyyyMMddHHmmFormatter: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

  private val mmmDdYyyyFormatter: SimpleDateFormat
    get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

  private val mmmDdYyyyHHmmFormatter: SimpleDateFormat
    get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

  private val mmmDdYyyyHmmAFormatter: SimpleDateFormat
    get() = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())

  private val hhmmFormatter: SimpleDateFormat
    get() = SimpleDateFormat("HH:mm", Locale.getDefault())

  private val hmmAFormatter: SimpleDateFormat
    get() = SimpleDateFormat("h:mm a", Locale.getDefault())

  private val mmDdYyyyFormatter: SimpleDateFormat
    get() = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

  private val mmDdYyyyHHmmFormatter: SimpleDateFormat
    get() = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())

  fun formatYyyyMMdd(date: Date = Date()): String {
    return yyyyMMddFormatter.format(date)
  }

  /**
   * Helper to parse a date string formatted as "yyyy-MM-dd"
   */
  fun parseYyyyMMdd(dateStr: String): Date? {
    return runCatching { yyyyMMddFormatter.parse(dateStr) }.getOrNull()
  }

  fun formatYyyyMMddHHmm(date: Date = Date()): String {
    return yyyyMMddHHmmFormatter.format(date)
  }

  fun formatMmmDdYyyy(date: Date = Date()): String {
    return mmmDdYyyyFormatter.format(date)
  }

  fun formatMmmDdYyyyHHmm(date: Date = Date(), use24Hour: Boolean = true): String {
    return if (use24Hour) {
      mmmDdYyyyHHmmFormatter.format(date)
    } else {
      mmmDdYyyyHmmAFormatter.format(date).lowercase(Locale.getDefault())
    }
  }

  /**
   * Converts a "yyyy-MM-dd" date string into "MMM dd, yyyy"
   */
  fun formatIsoToMmmDdYyyy(isoDateStr: String): String {
    val parsed = parseYyyyMMdd(isoDateStr) ?: return isoDateStr
    return formatMmmDdYyyy(parsed)
  }

  fun formatTimeForDisplay(timeStr: String, use24Hour: Boolean): String {
    if (use24Hour) return timeStr
    val parsed = runCatching { hhmmFormatter.parse(timeStr) }.getOrNull() ?: return timeStr
    return hmmAFormatter.format(parsed).lowercase(Locale.getDefault())
  }

  fun formatDateTimeStringForDisplay(value: String?, use24Hour: Boolean): String {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return raw

    val parseAttempts = listOf(
      yyyyMMddHHmmFormatter to "dateTime",
      mmDdYyyyHHmmFormatter to "dateTime",
      yyyyMMddFormatter to "date",
      mmDdYyyyFormatter to "date",
      hhmmFormatter to "time"
    )

    for ((formatter, type) in parseAttempts) {
      val parsed = parseExact(formatter, raw) ?: continue
      return when (type) {
        "dateTime" -> formatMmmDdYyyyHHmm(parsed, use24Hour)
        "date" -> formatMmmDdYyyy(parsed)
        "time" -> formatTimeForDisplay(raw, use24Hour)
        else -> raw
      }
    }

    return raw
  }

  private fun parseExact(formatter: SimpleDateFormat, raw: String): Date? {
    formatter.isLenient = false
    val position = ParsePosition(0)
    val parsed = formatter.parse(raw, position) ?: return null
    return if (position.index == raw.length) parsed else null
  }
}
