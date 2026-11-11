package com.houdouri.app.domain

import com.houdouri.app.data.ShiftEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.max

data class SummaryTotals(
    val todayMinutes: Long,
    val weekMinutes: Long,
    val monthMinutes: Long,
    val todayDate: LocalDate,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val monthStart: LocalDate,
    val monthEnd: LocalDate
)

data class DayShiftGroup(
    val date: LocalDate,
    val totalMinutes: Long,
    val shifts: List<ShiftEntity>
)

object ShiftCalculator {

    val DEFAULT_ZONE: ZoneId = ZoneId.of("Africa/Cairo")
    private val ARABIC_LOCALE = Locale("ar", "EG")
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", ARABIC_LOCALE)

    /**
     * يحسب مدة الشيفت بالدقيقة بدون تقريب مبالغ فيه.
     */
    fun calculateShiftMinutes(shift: ShiftEntity): Long {
        val end = shift.endTime ?: return 0L
        val diffMillis = end - shift.startTime
        if (diffMillis <= 0L) return 0L
        return diffMillis / 60_000L
    }

    /**
     * يحسب المدة الجارية للشيفت المفتوح لحظة فتح الشاشة فقط.
     */
    fun calculateLiveShiftMinutes(shift: ShiftEntity, nowEpochMillis: Long): Long {
        val end = shift.endTime ?: nowEpochMillis
        val diffMillis = end - shift.startTime
        return max(0L, diffMillis / 60_000L)
    }

    /**
     * صيغة مختصرة للكروت والسجل: "8س 28د"
     */
    fun formatDurationShort(totalMinutes: Long): String {
        val safe = max(0L, totalMinutes)
        val hours = safe / 60L
        val minutes = safe % 60L
        return "${hours}س ${minutes}د"
    }

    /**
     * صيغة واضحة لشاشة التأكيد: "8 ساعات و28 دقيقة"
     */
    fun formatDurationVerbose(totalMinutes: Long): String {
        val safe = max(0L, totalMinutes)
        val hours = safe / 60L
        val minutes = safe % 60L
        return "${hours} ساعات و${minutes} دقيقة"
    }

    /**
     * تنسيق الوقت بالعربية مثل: "08:02 ص" أو "04:30 م"
     */
    fun formatTimeArabic(epochMillis: Long, timezoneId: String = "Africa/Cairo"): String {
        val zone = runCatching { ZoneId.of(timezoneId) }.getOrDefault(DEFAULT_ZONE)
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .format(TIME_FORMATTER)
            .replace("AM", "ص")
            .replace("PM", "م")
    }

    /**
     * الشيفت الذي يعبر منتصف الليل يُحسب بالكامل على يوم بدايته.
     */
    fun getShiftStartDate(shift: ShiftEntity): LocalDate {
        val zone = runCatching { ZoneId.of(shift.timezoneId) }.getOrDefault(DEFAULT_ZONE)
        return Instant.ofEpochMilli(shift.startTime).atZone(zone).toLocalDate()
    }

    /**
     * يحسب بداية ونهاية الأسبوع بناءً على اليوم المختار (الافتراضي: السبت).
     */
    fun getWeekRange(
        referenceDate: LocalDate,
        weekStartDay: DayOfWeek = DayOfWeek.SATURDAY
    ): Pair<LocalDate, LocalDate> {
        val start = referenceDate.with(TemporalAdjusters.previousOrSame(weekStartDay))
        val end = start.plusDays(6)
        return start to end
    }

    /**
     * يحسب بداية ونهاية الشهر الميلادي العادي (يوم 1) أو المخصص (مثلاً من 26 إلى 25).
     */
    fun getMonthRange(
        referenceDate: LocalDate,
        monthStartDay: Int = 1
    ): Pair<LocalDate, LocalDate> {
        val safeDay = monthStartDay.coerceIn(1, 28)
        if (safeDay == 1) {
            val ym = YearMonth.from(referenceDate)
            return ym.atDay(1) to ym.atEndOfMonth()
        }

        return if (referenceDate.dayOfMonth >= safeDay) {
            val start = referenceDate.withDayOfMonth(safeDay)
            val end = referenceDate.plusMonths(1).withDayOfMonth(safeDay - 1)
            start to end
        } else {
            val start = referenceDate.minusMonths(1).withDayOfMonth(safeDay)
            val end = referenceDate.withDayOfMonth(safeDay - 1)
            start to end
        }
    }

    /**
     * يحسب إجمالي ساعات اليوم والأسبوع والشهر.
     */
    fun calculateTotals(
        shifts: List<ShiftEntity>,
        nowEpochMillis: Long,
        weekStartDay: DayOfWeek = DayOfWeek.SATURDAY,
        monthStartDay: Int = 1,
        timezoneId: String = "Africa/Cairo"
    ): SummaryTotals {
        val zone = runCatching { ZoneId.of(timezoneId) }.getOrDefault(DEFAULT_ZONE)
        val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()
        val (weekStart, weekEnd) = getWeekRange(today, weekStartDay)
        val (monthStart, monthEnd) = getMonthRange(today, monthStartDay)

        var todayMins = 0L
        var weekMins = 0L
        var monthMins = 0L

        for (shift in shifts) {
            if (shift.endTime == null) continue
            val duration = calculateShiftMinutes(shift)
            val shiftDate = getShiftStartDate(shift)

            if (shiftDate == today) {
                todayMins += duration
            }
            if (!shiftDate.isBefore(weekStart) && !shiftDate.isAfter(weekEnd)) {
                weekMins += duration
            }
            if (!shiftDate.isBefore(monthStart) && !shiftDate.isAfter(monthEnd)) {
                monthMins += duration
            }
        }

        return SummaryTotals(
            todayMinutes = todayMins,
            weekMinutes = weekMins,
            monthMinutes = monthMins,
            todayDate = today,
            weekStart = weekStart,
            weekEnd = weekEnd,
            monthStart = monthStart,
            monthEnd = monthEnd
        )
    }

    /**
     * يفحص إذا كان الشيفت المفتوح قد تجاوز 16 ساعة ليطلب من المستخدم إدخال وقت الانصراف الفعلي يدوياً.
     */
    fun isOpenShiftOver16Hours(
        openShift: ShiftEntity?,
        nowEpochMillis: Long,
        maxHours: Long = 16L
    ): Boolean {
        if (openShift == null || openShift.endTime != null) return false
        val elapsedMillis = nowEpochMillis - openShift.startTime
        return elapsedMillis > maxHours * 3600_000L
    }

    /**
     * يمنع تسجيل بصمتين متتاليتين بالخطأ خلال أقل من 30 ثانية.
     */
    fun canPunchAfterCooldown(
        lastPunchEpochMillis: Long?,
        nowEpochMillis: Long,
        cooldownSeconds: Long = 30L
    ): Pair<Boolean, Long> {
        if (lastPunchEpochMillis == null) return true to 0L
        val elapsedMillis = nowEpochMillis - lastPunchEpochMillis
        val requiredMillis = cooldownSeconds * 1000L
        if (elapsedMillis >= requiredMillis) return true to 0L
        val remainingSec = ((requiredMillis - elapsedMillis) + 999L) / 1000L
        return false to remainingSec
    }

    /**
     * يجمع الشيفتات حسب اليوم (الأحدث أولاً) مع جمع ساعات كل يوم.
     */
    fun groupShiftsByDay(shifts: List<ShiftEntity>): List<DayShiftGroup> {
        return shifts
            .groupBy { getShiftStartDate(it) }
            .map { (date, dayShifts) ->
                val sorted = dayShifts.sortedByDescending { it.startTime }
                val total = sorted.sumOf { calculateShiftMinutes(it) }
                DayShiftGroup(date = date, totalMinutes = total, shifts = sorted)
            }
            .sortedByDescending { it.date }
    }
}
