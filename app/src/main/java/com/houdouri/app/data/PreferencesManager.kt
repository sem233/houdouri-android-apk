package com.houdouri.app.data

import android.content.Context
import java.time.DayOfWeek

data class UserSettings(
    val weekStartDay: DayOfWeek = DayOfWeek.SATURDAY,
    val monthStartDay: Int = 1, // 1 = شهر ميلادي عادي، أو مثلاً 26 لدورة (26 إلى 25)
    val darkMode: Boolean = true,
    val lastPunchEpochMs: Long? = null
)

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("houdouri_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): UserSettings {
        val dayValue = prefs.getInt("week_start_day", DayOfWeek.SATURDAY.value)
        val monthDay = prefs.getInt("month_start_day", 1).coerceIn(1, 28)
        val dark = prefs.getBoolean("dark_mode", true)
        val lastPunch = if (prefs.contains("last_punch_ms")) {
            prefs.getLong("last_punch_ms", 0L)
        } else null

        return UserSettings(
            weekStartDay = DayOfWeek.of(dayValue),
            monthStartDay = monthDay,
            darkMode = dark,
            lastPunchEpochMs = lastPunch
        )
    }

    fun setWeekStartDay(dayOfWeek: DayOfWeek) {
        prefs.edit().putInt("week_start_day", dayOfWeek.value).apply()
    }

    fun setMonthStartDay(day: Int) {
        prefs.edit().putInt("month_start_day", day.coerceIn(1, 28)).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun setLastPunchEpochMs(epochMs: Long) {
        prefs.edit().putLong("last_punch_ms", epochMs).apply()
    }
}
