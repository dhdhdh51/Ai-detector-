package com.fitbudget.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class DateTimeUtilsTest {

    @Test
    fun `greeting changes across the day`() {
        assertEquals("Good morning", DateTimeUtils.greeting(LocalTime.of(7, 0)))
        assertEquals("Good afternoon", DateTimeUtils.greeting(LocalTime.of(13, 30)))
        assertEquals("Good evening", DateTimeUtils.greeting(LocalTime.of(18, 0)))
        assertEquals("Good night", DateTimeUtils.greeting(LocalTime.of(23, 0)))
        assertEquals("Good night", DateTimeUtils.greeting(LocalTime.of(2, 0)))
    }

    @Test
    fun `minutes of day round trips`() {
        val minutes = DateTimeUtils.minutesOfDay(13, 45)
        assertEquals(825, minutes)
        assertEquals(13, DateTimeUtils.hourOf(minutes))
        assertEquals(45, DateTimeUtils.minuteOf(minutes))
    }

    @Test
    fun `time formatting uses a 12 hour clock`() {
        assertEquals("8:00 AM", DateTimeUtils.formatTime(8, 0))
        assertEquals("1:00 PM", DateTimeUtils.formatTime(13, 0))
        assertEquals("12:00 AM", DateTimeUtils.formatTime(0, 0))
        assertEquals("12:30 PM", DateTimeUtils.formatTime(12, 30))
        assertEquals("10:30 PM", DateTimeUtils.formatTime(22, 30))
    }

    @Test
    fun `time formatting clamps nonsense input instead of throwing`() {
        assertEquals("11:59 PM", DateTimeUtils.formatTime(99, 99))
    }

    @Test
    fun `day masks encode and decode`() {
        val weekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        val mask = DateTimeUtils.maskOf(weekdays)
        assertEquals(weekdays, DateTimeUtils.daysOf(mask))
        assertTrue(DateTimeUtils.maskContains(mask, DayOfWeek.MONDAY))
        assertFalse(DateTimeUtils.maskContains(mask, DayOfWeek.SUNDAY))
        assertEquals("Weekdays", DateTimeUtils.describeMask(mask))
    }

    @Test
    fun `all days mask is described as every day`() {
        assertEquals("Every day", DateTimeUtils.describeMask(DateTimeUtils.ALL_DAYS_MASK))
        assertEquals("Never", DateTimeUtils.describeMask(0))
        assertEquals(
            "Weekends",
            DateTimeUtils.describeMask(
                DateTimeUtils.maskOf(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
            )
        )
    }

    @Test
    fun `last days range is inclusive and ends today`() {
        val range = DateTimeUtils.lastDays(7, endEpochDay = 100)
        assertEquals(94L, range.first)
        assertEquals(100L, range.last)
        assertEquals(7, range.count())
    }

    @Test
    fun `relative labels name today and yesterday`() {
        val today = DateTimeUtils.todayEpochDay()
        assertEquals("Today", DateTimeUtils.relativeDayLabel(today))
        assertEquals("Yesterday", DateTimeUtils.relativeDayLabel(today - 1))
        assertEquals("Tomorrow", DateTimeUtils.relativeDayLabel(today + 1))
    }

    @Test
    fun `month range covers the whole month`() {
        val month = java.time.YearMonth.of(2025, 2)
        val range = DateTimeUtils.monthRange(month)
        assertEquals(28, range.count())
    }

    @Test
    fun `epoch day conversion round trips`() {
        val date = java.time.LocalDate.of(2025, 9, 22)
        assertEquals(date, DateTimeUtils.date(date.toEpochDay()))
    }
}
