package com.fitbudget.app.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * All persisted dates are stored as [LocalDate.toEpochDay] so that a "day" always means the
 * user's local calendar day. Nothing in the app depends on a server clock.
 */
object DateTimeUtils {

    val zone: ZoneId get() = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now()

    fun todayEpochDay(): Long = today().toEpochDay()

    fun date(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun nowMillis(): Long = System.currentTimeMillis()

    fun localDateTimeOf(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    fun toMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(zone).toInstant().toEpochMilli()

    /** Minutes from midnight, the format used to persist reminder / wake / sleep times. */
    fun minutesOfDay(hour: Int, minute: Int): Int = hour * 60 + minute

    fun hourOf(minutesOfDay: Int): Int = (minutesOfDay / 60).coerceIn(0, 23)

    fun minuteOf(minutesOfDay: Int): Int = (minutesOfDay % 60).coerceIn(0, 59)

    fun timeOf(minutesOfDay: Int): LocalTime =
        LocalTime.of(hourOf(minutesOfDay), minuteOf(minutesOfDay))

    fun formatTime(hour: Int, minute: Int): String {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val suffix = if (safeHour < 12) "AM" else "PM"
        val display = when {
            safeHour == 0 -> 12
            safeHour > 12 -> safeHour - 12
            else -> safeHour
        }
        return "%d:%02d %s".format(display, safeMinute, suffix)
    }

    fun formatMinutesOfDay(minutesOfDay: Int): String =
        formatTime(hourOf(minutesOfDay), minuteOf(minutesOfDay))

    private val dayMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    private val fullFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val weekdayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")

    fun formatShortDate(date: LocalDate): String = date.format(dayMonthFormatter)

    fun formatFullDate(date: LocalDate): String = date.format(fullFormatter)

    fun formatMonth(month: YearMonth): String = month.atDay(1).format(monthFormatter)

    fun formatWeekday(date: LocalDate): String = date.format(weekdayFormatter)

    fun relativeDayLabel(epochDay: Long): String {
        val date = date(epochDay)
        return when (epochDay) {
            todayEpochDay() -> "Today"
            todayEpochDay() - 1 -> "Yesterday"
            todayEpochDay() + 1 -> "Tomorrow"
            else -> formatShortDate(date)
        }
    }

    fun greeting(time: LocalTime = LocalTime.now()): String = when (time.hour) {
        in 0..4 -> "Good night"
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }

    /** Inclusive range of epoch days ending today, e.g. [dayRange] of 7 = last 7 days. */
    fun lastDays(days: Int, endEpochDay: Long = todayEpochDay()): LongRange {
        val safeDays = days.coerceAtLeast(1)
        return (endEpochDay - safeDays + 1)..endEpochDay
    }

    fun monthRange(month: YearMonth): LongRange {
        val start = month.atDay(1).toEpochDay()
        val end = month.atEndOfMonth().toEpochDay()
        return start..end
    }

    fun monthOf(epochDay: Long): YearMonth = YearMonth.from(date(epochDay))

    fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    /** Bit mask helpers: bit 0 = Monday … bit 6 = Sunday. */
    const val ALL_DAYS_MASK = 0b1111111

    fun maskOf(days: Set<DayOfWeek>): Int =
        days.fold(0) { acc, day -> acc or (1 shl (day.value - 1)) }

    fun daysOf(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

    fun maskContains(mask: Int, day: DayOfWeek): Boolean = mask and (1 shl (day.value - 1)) != 0

    fun describeMask(mask: Int): String {
        val normalised = mask and ALL_DAYS_MASK
        return when {
            normalised == 0 -> "Never"
            normalised == ALL_DAYS_MASK -> "Every day"
            normalised == 0b0011111 -> "Weekdays"
            normalised == 0b1100000 -> "Weekends"
            else -> DayOfWeek.entries
                .filter { maskContains(normalised, it) }
                .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
        }
    }
}
