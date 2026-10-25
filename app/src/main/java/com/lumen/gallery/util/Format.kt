package com.lumen.gallery.util

import com.lumen.gallery.data.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

object Format {
    private val dayThisYear = DateTimeFormatter.ofPattern("EEE, d MMM")
    private val dayOtherYear = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
    private val monthOnly = DateTimeFormatter.ofPattern("MMMM")
    private val monthAndYear = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy")
    private val fullDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
    private val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMMM")
    private val weekday = DateTimeFormatter.ofPattern("EEEE")
    private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEEE, d MMMM")
    private val year = DateTimeFormatter.ofPattern("yyyy")

    /** Serif group title in the timeline: "Today", "Yesterday", "23 October". */
    fun dayTitle(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> dayMonth.format(date)
    }

    /** Small caption beside a day title: the weekday, plus the year when it isn't this year. */
    fun daySubtitle(date: LocalDate, today: LocalDate): String = when {
        date == today || date == today.minusDays(1) -> weekdayDayMonth.format(date)
        date.year == today.year -> weekday.format(date)
        else -> "${weekday.format(date)} · ${date.year}"
    }

    fun monthName(month: YearMonth): String = monthOnly.format(month)

    fun dayMonth(millis: Long): String = dayMonth.format(millis.toLocalDate())

    fun year(millis: Long): String = year.format(millis.toLocalDate())

    fun dayHeader(date: LocalDate, today: LocalDate): String = when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> dayThisYear.format(date)
        else -> dayOtherYear.format(date)
    }

    fun monthHeader(month: YearMonth, today: LocalDate): String =
        if (month.year == today.year) monthOnly.format(month) else monthAndYear.format(month)

    fun monthYear(month: YearMonth): String = monthAndYear.format(month)

    fun longDate(millis: Long): String = longDate.format(millis.toLocalDate())

    fun fullDate(millis: Long): String = fullDate.format(millis.toLocalDate())

    fun time(millis: Long): String =
        time.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime())

    fun duration(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun size(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KB", "MB", "GB", "TB")
        var value = bytes / 1024.0
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return String.format(Locale.getDefault(), if (value >= 100) "%.0f %s" else "%.1f %s", value, units[unit])
    }

    fun resolution(width: Int, height: Int): String? =
        if (width > 0 && height > 0) "$width × $height" else null

    fun megapixels(width: Int, height: Int): String? {
        val pixels = width.toLong() * height
        return if (pixels >= 100_000) String.format(Locale.getDefault(), "%.1f MP", pixels / 1_000_000.0) else null
    }

    fun counts(items: List<MediaItem>): String {
        val videos = items.count { it.isVideo }
        val photos = items.size - videos
        return buildList {
            if (photos > 0) add(plural(photos, "photo"))
            if (videos > 0) add(plural(videos, "video"))
        }.joinToString(" · ").ifEmpty { "Empty" }
    }

    fun plural(count: Int, noun: String): String = "$count $noun${if (count == 1) "" else "s"}"
}
