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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class BannerType {
    CHECKIN,
    CHECKOUT,
    WARNING,
    INFO
}

data class ConfirmationBanner(
    val message: String,
    val type: BannerType,
    val timestampMillis: Long
)

data class BiometricPromptState(
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val isCheckout: Boolean = false,
    val onConfirm: () -> Unit
)

data class HoudouriUiState(
    val shifts: List<ShiftEntity> = emptyList(),
    val openShift: ShiftEntity? = null,
    val settings: UserSettings = UserSettings(),
    val totals: SummaryTotals? = null,
    val confirmationBanner: ConfirmationBanner? = null,
    val staleOpenShiftPrompt: ShiftEntity? = null,
    val activeBiometricRequest: BiometricPromptState? = null,
    val editingShift: ShiftEntity? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).shiftDao()
    private val prefs = PreferencesManager(application)

    private val _settings = MutableStateFlow(prefs.loadSettings())
    private val _confirmationBanner = MutableStateFlow<ConfirmationBanner?>(null)
    private val _staleOpenShiftPrompt = MutableStateFlow<ShiftEntity?>(null)
    private val _activeBiometricRequest = MutableStateFlow<BiometricPromptState?>(null)
    private val _editingShift = MutableStateFlow<ShiftEntity?>(null)

    init {
        viewModelScope.launch {
            if (!prefs.isInitialSeedDone()) {
                val existing = dao.getAllShiftsOnce()
                if (existing.isEmpty()) {
                    dao.insertAll(createRealisticSeedShifts())
                }
                prefs.markInitialSeedDone()
            }
        }
    }

    private fun createRealisticSeedShifts(): List<ShiftEntity> {
        val zone = ShiftCalculator.DEFAULT_ZONE
        val today = LocalDate.now(zone)

        fun epochAt(daysOffset: Long, hour: Int, minute: Int): Long {
            return ZonedDateTime.of(today.plusDays(daysOffset), LocalTime.of(hour, minute), zone)
                .toInstant()
                .toEpochMilli()
        }

        return listOf(
            ShiftEntity(
                startTime = epochAt(-1, 8, 2),
                endTime = epochAt(-1, 16, 30), // 8س 28د (508 دقيقة)
                timezoneId = "Africa/Cairo",
                note = "وردية الأمس الرسمية"
            ),
            ShiftEntity(
                startTime = epochAt(-2, 22, 0),
                endTime = epochAt(-1, 2, 30), // شيفت عابر لمنتصف الليل (4س 30د يُحسب على يوم بدايته)
                timezoneId = "Africa/Cairo",
                note = "شيفت مسائي عابر لمنتصف الليل"
            ),
            ShiftEntity(
                startTime = epochAt(-3, 8, 15),
                endTime = epochAt(-3, 16, 15), // 8س 0د
                timezoneId = "Africa/Cairo",
                note = "دوام كامل"
            )
        )
    }

    val uiState: StateFlow<HoudouriUiState> = combine(
        dao.observeAllShifts(),
        _settings,
        _confirmationBanner,
        _staleOpenShiftPrompt,
        combine(_activeBiometricRequest, _editingShift) { bio, edit -> bio to edit }
    ) { shifts, settings, banner, stalePrompt, (bioReq, editShift) ->
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
            staleOpenShiftPrompt = stalePrompt,
            activeBiometricRequest = bioReq,
            editingShift = editShift
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HoudouriUiState()
    )

    fun requestPunchBiometric(onTriggerHardwareSensor: (String, String, () -> Unit) -> Unit) {
        viewModelScope.launch {
            val openShift = dao.getOpenShift()
            val title = if (openShift != null) "بصمة تسجيل الانصراف" else "بصمة تسجيل الحضور"
            val subtitle = if (openShift != null) {
                val startFormatted = ShiftCalculator.formatTimeArabic(openShift.startTime, openShift.timezoneId)
                "يوجد شيفت مفتوح منذ $startFormatted. نجاح البصمة سيغلق الشيفت ويحسب مدته بالوقت الحالي."
            } else {
                "لا يوجد شيفت مفتوح حالياً. نجاح البصمة سيسجل حضورك فوراً بالوقت الحالي من ساعة النظام."
            }
            val actionLabel = if (openShift != null) {
                "نجاح البصمة — تسجيل الانصراف الآن"
            } else {
                "نجاح البصمة — تسجيل الحضور الآن"
            }

            val confirmAction: () -> Unit = {
                _activeBiometricRequest.value = null
                onBiometricPunchSucceeded()
            }

            _activeBiometricRequest.value = BiometricPromptState(
                title = title,
                subtitle = subtitle,
                actionLabel = actionLabel,
                isCheckout = openShift != null,
                onConfirm = confirmAction
            )

            onTriggerHardwareSensor(title, subtitle, confirmAction)
        }
    }

    fun requestCustomBiometric(
        title: String,
        subtitle: String,
        actionLabel: String,
        onConfirmed: () -> Unit,
        onTriggerHardwareSensor: (String, String, () -> Unit) -> Unit
    ) {
        val confirmWrapper: () -> Unit = {
            _activeBiometricRequest.value = null
            onConfirmed()
        }
        _activeBiometricRequest.value = BiometricPromptState(
            title = title,
            subtitle = subtitle,
            actionLabel = actionLabel,
            isCheckout = false,
            onConfirm = confirmWrapper
        )
        onTriggerHardwareSensor(title, subtitle, confirmWrapper)
    }

    fun dismissBiometricModalWithCancelMessage() {
        val wasOpen = _activeBiometricRequest.value != null
        _activeBiometricRequest.value = null
        if (wasOpen) {
            _confirmationBanner.value = ConfirmationBanner(
                message = "تم إلغاء البصمة — لم يتم تسجيل أي حركة (وضع الاطلاع على الساعات فقط).",
                type = BannerType.INFO,
                timestampMillis = System.currentTimeMillis()
            )
        }
    }

    /**
     * يُستدعى فور نجاح بصمة الهاتف
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
                _confirmationBanner.value = ConfirmationBanner(
                    message = "لا يمكن التسجيل مرتين متتاليتين بالخطأ — يرجى الانتظار $remainingSec ثانية.",
                    type = BannerType.WARNING,
                    timestampMillis = now
                )
                return@launch
            }

            val openShift = dao.getOpenShift()
            val currentTz = "Africa/Cairo"

            if (openShift == null) {
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
                    message = "تم تسجيل الحضور $formattedTime",
                    type = BannerType.CHECKIN,
                    timestampMillis = now
                )
            } else {
                if (ShiftCalculator.isOpenShiftOver16Hours(openShift, now, 16L)) {
                    _staleOpenShiftPrompt.value = openShift
                    _confirmationBanner.value = ConfirmationBanner(
                        message = "هذا الشيفت مفتوح منذ أكثر من 16 ساعة — يرجى تحديد وقت الانصراف الفعلي يدوياً.",
                        type = BannerType.WARNING,
                        timestampMillis = now
                    )
                    return@launch
                }

                val closedShift = openShift.copy(endTime = now)
                dao.updateShift(closedShift)
                prefs.setLastPunchEpochMs(now)
                _settings.value = prefs.loadSettings()

                val durationMins = ShiftCalculator.calculateShiftMinutes(closedShift)
                val formattedTime = ShiftCalculator.formatTimeArabic(now, closedShift.timezoneId)
                val formattedDuration = ShiftCalculator.formatDurationVerbose(durationMins)

                _confirmationBanner.value = ConfirmationBanner(
                    message = "تم الانصراف $formattedTime — مدة الشيفت $formattedDuration",
                    type = BannerType.CHECKOUT,
                    timestampMillis = now
                )
            }
        }
    }

    fun clearCooldownForTesting() {
        prefs.setLastPunchEpochMs(null)
        _settings.value = prefs.loadSettings()
    }

    fun openStaleShiftModal(shift: ShiftEntity) {
        _staleOpenShiftPrompt.value = shift
    }

    fun dismissStaleShiftModal() {
        _staleOpenShiftPrompt.value = null
    }

    fun resolveStaleShiftManually(shift: ShiftEntity, actualEndTimeMillis: Long) {
        viewModelScope.launch {
            if (actualEndTimeMillis <= shift.startTime) {
                _confirmationBanner.value = ConfirmationBanner(
                    message = "وقت الانصراف يجب أن يكون بعد وقت الحضور.",
                    type = BannerType.WARNING,
                    timestampMillis = System.currentTimeMillis()
                )
                return@launch
            }
            val closed = shift.copy(endTime = actualEndTimeMillis)
            dao.updateShift(closed)
            _staleOpenShiftPrompt.value = null
            val durationMins = ShiftCalculator.calculateShiftMinutes(closed)
            val formattedTime = ShiftCalculator.formatTimeArabic(actualEndTimeMillis, closed.timezoneId)
            val formattedDuration = ShiftCalculator.formatDurationVerbose(durationMins)
            _confirmationBanner.value = ConfirmationBanner(
                message = "تم الانصراف $formattedTime — مدة الشيفت $formattedDuration",
                type = BannerType.CHECKOUT,
                timestampMillis = actualEndTimeMillis
            )
        }
    }

    fun simulateStale16hShift() {
        viewModelScope.launch {
            val all = dao.getAllShiftsOnce()
            for (s in all) {
                if (s.endTime == null) {
                    dao.updateShift(s.copy(endTime = s.startTime + 8L * 3600_000L))
                }
            }
            val eighteenHoursAgo = System.currentTimeMillis() - 18L * 3600_000L
            val staleShift = ShiftEntity(
                startTime = eighteenHoursAgo,
                endTime = null,
                timezoneId = "Africa/Cairo",
                note = "شيفت منسي منذ 18 ساعة"
            )
            val newId = dao.insertShift(staleShift)
            prefs.setLastPunchEpochMs(null)
            _settings.value = prefs.loadSettings()
            _staleOpenShiftPrompt.value = staleShift.copy(id = newId)
        }
    }

    fun simulateCrossMidnightShift() {
        viewModelScope.launch {
            val zone = ShiftCalculator.DEFAULT_ZONE
            val yesterday = LocalDate.now(zone).minusDays(1)
            val startMs = ZonedDateTime.of(yesterday, LocalTime.of(22, 0), zone).toInstant().toEpochMilli()
            val endMs = startMs + (4L * 60L + 30L) * 60_000L // 4h 30m crossing midnight to 02:30 AM
            val nightShift = ShiftEntity(
                startTime = startMs,
                endTime = endMs,
                timezoneId = "Africa/Cairo",
                note = "شيفت ليلي عابر لمنتصف الليل (يُحسب على يوم بدايته)"
            )
            dao.insertShift(nightShift)
            _confirmationBanner.value = ConfirmationBanner(
                message = "تمت إضافة شيفت عابر لمنتصف الليل (10:00 م ← 02:30 ص) — حُسبت مدته (4 ساعات و30 دقيقة) بالكامل على يوم بدايته.",
                type = BannerType.INFO,
                timestampMillis = System.currentTimeMillis()
            )
        }
    }

    fun selectShiftForEditing(shift: ShiftEntity?) {
        _editingShift.value = shift
    }

    fun saveManualShiftAfterBiometric(shift: ShiftEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val isExisting = shift.id != 0L
            if (!isExisting) {
                dao.insertShift(shift)
            } else {
                dao.updateShift(shift)
            }
            _editingShift.value = null
            _confirmationBanner.value = ConfirmationBanner(
                message = if (isExisting) {
                    "تم حفظ تعديل الشيفت بنجاح بعد تأكيد البصمة."
                } else {
                    "تمت إضافة الشيفت اليدوي بنجاح بعد تأكيد البصمة."
                },
                type = BannerType.CHECKIN,
                timestampMillis = System.currentTimeMillis()
            )
            onSaved()
        }
    }

    fun deleteShiftAfterBiometric(shift: ShiftEntity) {
        viewModelScope.launch {
            dao.deleteShift(shift)
            if (_editingShift.value?.id == shift.id) {
                _editingShift.value = null
            }
            _confirmationBanner.value = ConfirmationBanner(
                message = "تم حذف الشيفت بعد تأكيد البصمة.",
                type = BannerType.INFO,
                timestampMillis = System.currentTimeMillis()
            )
        }
    }

    fun resetSeedData() {
        viewModelScope.launch {
            dao.clearAllShifts()
            dao.insertAll(createRealisticSeedShifts())
            _confirmationBanner.value = ConfirmationBanner(
                message = "تمت استعادة السجلات الافتراضية بنجاح.",
                type = BannerType.INFO,
                timestampMillis = System.currentTimeMillis()
            )
        }
    }

    fun clearAllShifts() {
        viewModelScope.launch {
            dao.clearAllShifts()
            _confirmationBanner.value = ConfirmationBanner(
                message = "تم مسح جميع السجلات بنجاح.",
                type = BannerType.INFO,
                timestampMillis = System.currentTimeMillis()
            )
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
                if (imported.isNotEmpty()) {
                    dao.clearAllShifts()
                    dao.insertAll(imported)
                }
                _confirmationBanner.value = ConfirmationBanner(
                    message = "تم استيراد ${imported.size} شيفت من النسخة الاحتياطية بنجاح.",
                    type = BannerType.INFO,
                    timestampMillis = System.currentTimeMillis()
                )
            }.onFailure {
                _confirmationBanner.value = ConfirmationBanner(
                    message = "تعذر قراءة ملف النسخة الاحتياطية — تأكد من صحة تنسيق الملف.",
                    type = BannerType.WARNING,
                    timestampMillis = System.currentTimeMillis()
                )
            }
        }
    }
}
