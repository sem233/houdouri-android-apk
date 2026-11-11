package com.houdouri.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.houdouri.app.data.AppDatabase
import com.houdouri.app.data.PreferencesManager
import com.houdouri.app.data.ShiftEntity
import com.houdouri.app.data.UserSettings
import com.houdouri.app.domain.BackupManager
import com.houdouri.app.domain.ShiftCalculator
import com.houdouri.app.domain.SummaryTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.TimeZone

data class ConfirmationBanner(
    val title: String,
    val isCheckIn: Boolean,
    val timestampMillis: Long
)

data class HoudouriUiState(
    val shifts: List<ShiftEntity> = emptyList(),
    val openShift: ShiftEntity? = null,
    val settings: UserSettings = UserSettings(),
    val totals: SummaryTotals? = null,
    val confirmationBanner: ConfirmationBanner? = null,
    val statusMessage: String? = null,
    val staleOpenShiftPrompt: ShiftEntity? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).shiftDao()
    private val prefs = PreferencesManager(application)

    private val _settings = MutableStateFlow(prefs.loadSettings())
    private val _confirmationBanner = MutableStateFlow<ConfirmationBanner?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _staleOpenShiftPrompt = MutableStateFlow<ShiftEntity?>(null)

    val uiState: StateFlow<HoudouriUiState> = combine(
        dao.observeAllShifts(),
        _settings,
        _confirmationBanner,
        _statusMessage,
        _staleOpenShiftPrompt
    ) { shifts, settings, banner, msg, stalePrompt ->
        val now = System.currentTimeMillis()
        val openShift = shifts.firstOrNull { it.endTime == null }
        val totals = ShiftCalculator.calculateTotals(
            shifts = shifts,
            nowEpochMillis = now,
            weekStartDay = settings.weekStartDay,
            monthStartDay = settings.monthStartDay
        )
        HoudouriUiState(
            shifts = shifts,
            openShift = openShift,
            settings = settings,
            totals = totals,
            confirmationBanner = banner,
            statusMessage = msg,
            staleOpenShiftPrompt = stalePrompt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HoudouriUiState()
    )

    /**
     * يُستدعى فور نجاح بصمة الهاتف (BiometricPrompt.onAuthenticationSucceeded)
     */
    fun onBiometricPunchSucceeded() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val currentSettings = _settings.value

            // 1. التحقق من فاصل الـ 30 ثانية لمنع الضغط المزدوج بالخطأ
            val (allowed, remainingSec) = ShiftCalculator.canPunchAfterCooldown(
                lastPunchEpochMillis = currentSettings.lastPunchEpochMs,
                nowEpochMillis = now,
                cooldownSeconds = 30L
            )
            if (!allowed) {
                _statusMessage.value = "يرجى الانتظار $remainingSec ثانية قبل التسجيل مرة أخرى لتجنب الضغط المزدوج."
                return@launch
            }

            val openShift = dao.getOpenShift()
            val currentTz = TimeZone.getDefault().id.ifBlank { "Africa/Cairo" }

            if (openShift == null) {
                // لا يوجد شيفت مفتوح -> تسجيل حضور جديد بالوقت الحالي
                val newShift = ShiftEntity(
                    startTime = now,
                    endTime = null,
                    timezoneId = currentTz
                )
                dao.insertShift(newShift)
                prefs.setLastPunchEpochMs(now)
                _settings.value = prefs.loadSettings()

                val formattedTime = ShiftCalculator.formatTimeArabic(now, currentTz)
                _confirmationBanner.value = ConfirmationBanner(
                    title = "تم تسجيل الحضور $formattedTime",
                    isCheckIn = true,
                    timestampMillis = now
                )
                _statusMessage.value = null
            } else {
                // يوجد شيفت مفتوح -> نفحص أولاً هل مر عليه أكثر من 16 ساعة؟
                if (ShiftCalculator.isOpenShiftOver16Hours(openShift, now, 16L)) {
                    _staleOpenShiftPrompt.value = openShift
                    _statusMessage.value = "تنبيه: هذا الشيفت مفتوح منذ أكثر من 16 ساعة. يرجى تحديد وقت الانصراف الفعلي يدوياً."
                    return@launch
                }

                // إغلاق الشيفت وحساب مدته
                val closedShift = openShift.copy(endTime = now)
                dao.updateShift(closedShift)
                prefs.setLastPunchEpochMs(now)
                _settings.value = prefs.loadSettings()

                val durationMins = ShiftCalculator.calculateShiftMinutes(closedShift)
                val formattedTime = ShiftCalculator.formatTimeArabic(now, closedShift.timezoneId)
                val formattedDuration = ShiftCalculator.formatDurationVerbose(durationMins)

                _confirmationBanner.value = ConfirmationBanner(
                    title = "تم الانصراف $formattedTime — مدة الشيفت $formattedDuration",
                    isCheckIn = false,
                    timestampMillis = now
                )
                _statusMessage.value = null
            }
        }
    }

    fun onBiometricCancelled(reason: String) {
        _statusMessage.value = reason
    }

    fun resolveStaleShiftManually(shift: ShiftEntity, actualEndTimeMillis: Long) {
        viewModelScope.launch {
            if (actualEndTimeMillis <= shift.startTime) {
                _statusMessage.value = "وقت الانصراف يجب أن يكون بعد وقت الحضور."
                return@launch
            }
            val closed = shift.copy(endTime = actualEndTimeMillis)
            dao.updateShift(closed)
            _staleOpenShiftPrompt.value = null
            val durationMins = ShiftCalculator.calculateShiftMinutes(closed)
            val formattedTime = ShiftCalculator.formatTimeArabic(actualEndTimeMillis, closed.timezoneId)
            val formattedDuration = ShiftCalculator.formatDurationVerbose(durationMins)
            _confirmationBanner.value = ConfirmationBanner(
                title = "تم الانصراف $formattedTime — مدة الشيفت $formattedDuration",
                isCheckIn = false,
                timestampMillis = actualEndTimeMillis
            )
        }
    }

    fun saveManualShiftAfterBiometric(shift: ShiftEntity) {
        viewModelScope.launch {
            if (shift.id == 0L) {
                dao.insertShift(shift)
            } else {
                dao.updateShift(shift)
            }
            _statusMessage.value = "تم حفظ التعديل اليدوي بنجاح بعد تأكيد البصمة."
        }
    }

    fun deleteShiftAfterBiometric(shift: ShiftEntity) {
        viewModelScope.launch {
            dao.deleteShift(shift)
            _statusMessage.value = "تم حذف الشيفت بعد تأكيد البصمة."
        }
    }

    fun updateWeekStartDay(day: DayOfWeek) {
        prefs.setWeekStartDay(day)
        _settings.value = prefs.loadSettings()
    }

    fun updateMonthStartDay(day: Int) {
        prefs.setMonthStartDay(day)
        _settings.value = prefs.loadSettings()
    }

    fun updateDarkMode(enabled: Boolean) {
        prefs.setDarkMode(enabled)
        _settings.value = prefs.loadSettings()
    }

    fun exportCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val all = dao.getAllShiftsOnce()
            onReady(BackupManager.exportToCsv(all))
        }
    }

    fun exportJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val all = dao.getAllShiftsOnce()
            onReady(BackupManager.exportToJson(all))
        }
    }

    fun importBackupContent(content: String, isJson: Boolean) {
        viewModelScope.launch {
            runCatching {
                val imported = if (isJson) {
                    BackupManager.importFromJson(content)
                } else {
                    BackupManager.importFromCsv(content)
                }
                dao.insertAll(imported)
                _statusMessage.value = "تم استيراد ${imported.size} شيفت بنجاح."
            }.onFailure {
                _statusMessage.value = "تعذر قراءة ملف النسخة الاحتياطية."
            }
        }
    }
}
