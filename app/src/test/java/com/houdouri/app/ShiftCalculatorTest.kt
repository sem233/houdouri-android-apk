package com.houdouri.app

import com.houdouri.app.data.ShiftEntity
import com.houdouri.app.domain.ShiftCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

class ShiftCalculatorTest {

    private fun cairoMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ShiftCalculator.DEFAULT_ZONE)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun calculatesSingleShiftDurationInMinutesAndFormatsAs8h28m() {
        val shift = ShiftEntity(
            id = 1L,
            startTime = cairoMillis(2026, 11, 10, 8, 2),
            endTime = cairoMillis(2026, 11, 10, 16, 30),
            timezoneId = "Africa/Cairo"
        )
        val minutes = ShiftCalculator.calculateShiftMinutes(shift)
        assertEquals(508L, minutes)
        assertEquals("8س 28د", ShiftCalculator.formatDurationShort(minutes))
        assertEquals("8 ساعات و28 دقيقة", ShiftCalculator.formatDurationVerbose(minutes))
    }

    @Test
    fun sumsMultipleShiftsInTheSameDay() {
        val shifts = listOf(
            ShiftEntity(
                id = 1L,
                startTime = cairoMillis(2026, 11, 10, 8, 0),
                endTime = cairoMillis(2026, 11, 10, 12, 15) // 255 د
            ),
            ShiftEntity(
                id = 2L,
                startTime = cairoMillis(2026, 11, 10, 13, 0),
                endTime = cairoMillis(2026, 11, 10, 17, 30) // 270 د
            )
        )
        val totals = ShiftCalculator.calculateTotals(
            shifts = shifts,
            nowEpochMillis = cairoMillis(2026, 11, 10, 19, 0)
        )
        assertEquals(525L, totals.todayMinutes)
        assertEquals("8س 45د", ShiftCalculator.formatDurationShort(totals.todayMinutes))
    }

    @Test
    fun crossMidnightShiftIsAttributedEntirelyToItsStartDay() {
        // يبدأ يوم 10 نوفمبر الساعة 10:00 مساءً وينتهي يوم 11 نوفمبر الساعة 02:30 صباحاً (270 دقيقة)
        val nightShift = ShiftEntity(
            id = 5L,
            startTime = cairoMillis(2026, 11, 10, 22, 0),
            endTime = cairoMillis(2026, 11, 11, 2, 30),
            timezoneId = "Africa/Cairo"
        )

        assertEquals(LocalDate.of(2026, 11, 10), ShiftCalculator.getShiftStartDate(nightShift))

        // عند الحساب ليوم 10 نوفمبر: تُحسب الـ 270 دقيقة كاملة في يوم 10 نوفمبر
        val totalsDay10 = ShiftCalculator.calculateTotals(
            shifts = listOf(nightShift),
            nowEpochMillis = cairoMillis(2026, 11, 10, 23, 30)
        )
        assertEquals(270L, totalsDay10.todayMinutes)

        // عند الحساب ليوم 11 نوفمبر: اليوم = 0، ولكن الأسبوع يحتفظ بالـ 270 دقيقة
        val totalsDay11 = ShiftCalculator.calculateTotals(
            shifts = listOf(nightShift),
            nowEpochMillis = cairoMillis(2026, 11, 11, 10, 0)
        )
        assertEquals(0L, totalsDay11.todayMinutes)
        assertEquals(270L, totalsDay11.weekMinutes)
    }

    @Test
    fun calculatesWeeklyRangeWithSaturdayDefaultAndCustomStartDay() {
        val tuesday = LocalDate.of(2026, 11, 10)
        val (satStart, satEnd) = ShiftCalculator.getWeekRange(tuesday, DayOfWeek.SATURDAY)
        assertEquals(LocalDate.of(2026, 11, 7), satStart)
        assertEquals(LocalDate.of(2026, 11, 13), satEnd)
    }

    @Test
    fun calculatesCustomMonthlyCycleFrom26thTo25th() {
        val nov10 = LocalDate.of(2026, 11, 10)
        val (start1, end1) = ShiftCalculator.getMonthRange(nov10, monthStartDay = 26)
        assertEquals(LocalDate.of(2026, 10, 26), start1)
        assertEquals(LocalDate.of(2026, 11, 25), end1)

        val nov27 = LocalDate.of(2026, 11, 27)
        val (start2, end2) = ShiftCalculator.getMonthRange(nov27, monthStartDay = 26)
        assertEquals(LocalDate.of(2026, 11, 26), start2)
        assertEquals(LocalDate.of(2026, 12, 25), end2)
    }

    @Test
    fun detectsOpenShiftOver16HoursAndEnforces30sDoublePunchCooldown() {
        val start = cairoMillis(2026, 11, 10, 6, 0)
        val openShift = ShiftEntity(id = 9L, startTime = start, endTime = null)

        assertFalse(ShiftCalculator.isOpenShiftOver16Hours(openShift, start + 8 * 3600_000L))
        assertTrue(ShiftCalculator.isOpenShiftOver16Hours(openShift, start + 16 * 3600_000L + 60_000L))

        val (allowed10s, rem10s) = ShiftCalculator.canPunchAfterCooldown(start, start + 10_000L, 30L)
        assertFalse(allowed10s)
        assertEquals(20L, rem10s)

        val (allowed31s, _) = ShiftCalculator.canPunchAfterCooldown(start, start + 31_000L, 30L)
        assertTrue(allowed31s)
    }
}
