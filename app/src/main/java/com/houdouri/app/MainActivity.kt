package com.houdouri.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.houdouri.app.domain.ShiftCalculator
import com.houdouri.app.ui.BiometricHelper
import com.houdouri.app.ui.HoudouriAppScreen
import com.houdouri.app.ui.MainViewModel

private val HoudouriDarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),          // Emerald 500
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF064E3B), // Emerald 900
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFFF59E0B),        // Amber 500
    onSecondary = Color(0xFF020617),
    background = Color(0xFF020617),       // Slate 950 (مطابق للمعاينة)
    onBackground = Color(0xFFF8FAFC),     // Slate 50
    surface = Color(0xFF0F172A),          // Slate 900 (مطابق للكروت في المعاينة)
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),   // Slate 800
    onSurfaceVariant = Color(0xFF94A3B8), // Slate 400
    outline = Color(0xFF1E293B)           // Slate 800 border
)

private val HoudouriLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),          // Emerald 600
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFFD97706),        // Amber 600
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),       // Slate 50
    onBackground = Color(0xFF0F172A),     // Slate 900
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),   // Slate 100
    onSurfaceVariant = Color(0xFF64748B), // Slate 500
    outline = Color(0xFFE2E8F0)           // Slate 200 border
)

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            var didPromptOnLaunch by rememberSaveable { mutableStateOf(false) }
            var pendingExportContent by remember { mutableStateOf("") }

            val createCsvLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("text/csv")
            ) { uri: Uri? ->
                if (uri != null && pendingExportContent.isNotEmpty()) {
                    writeTextToUri(uri, pendingExportContent)
                }
            }

            val createJsonLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri: Uri? ->
                if (uri != null && pendingExportContent.isNotEmpty()) {
                    writeTextToUri(uri, pendingExportContent)
                }
            }

            val openBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    val content = readTextFromUri(uri)
                    if (!content.isNullOrBlank()) {
                        val isJson = content.trimStart().startsWith("[") || content.trimStart().startsWith("{")
                        viewModel.importBackupContent(content, isJson)
                    }
                }
            }

            // أول ما يفتح التطبيق تظهر تلقائياً نافذة بصمة التلفون مرة واحدة
            LaunchedEffect(Unit) {
                if (!didPromptOnLaunch) {
                    didPromptOnLaunch = true
                    triggerBiometricPunch()
                }
            }

            val colorScheme = if (uiState.settings.darkMode) {
                HoudouriDarkColorScheme
            } else {
                HoudouriLightColorScheme
            }

            // اتجاه عربي بالكامل مع RTL
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        HoudouriAppScreen(
                            uiState = uiState,
                            onPunchClick = { triggerBiometricPunch() },
                            onSaveManualShift = { shift, onDone ->
                                val isEdit = shift.id != 0L
                                viewModel.requestCustomBiometric(
                                    title = if (isEdit) "تأكيد البصمة لحفظ تعديل الشيفت" else "تأكيد البصمة لإضافة شيفت يدوي",
                                    subtitle = "التعديل اليدوي محمي ببصمة الهاتف لضمان دقة سجل الحضور والانصراف الشخصي.",
                                    actionLabel = "نجاح البصمة — حفظ الشيفت الآن",
                                    onConfirmed = { viewModel.saveManualShiftAfterBiometric(shift, onDone) },
                                    onTriggerHardwareSensor = { title, sub, onSuccess ->
                                        BiometricHelper.authenticate(
                                            activity = this@MainActivity,
                                            title = title,
                                            subtitle = sub,
                                            onSuccess = onSuccess,
                                            onCancelOrFailed = { viewModel.dismissBiometricModalWithCancelMessage() }
                                        )
                                    }
                                )
                            },
                            onDeleteShift = { shift ->
                                val timeFormatted = ShiftCalculator.formatTimeArabic(shift.startTime, shift.timezoneId)
                                viewModel.requestCustomBiometric(
                                    title = "تأكيد البصمة لحذف الشيفت",
                                    subtitle = "هل تريد حذف شيفت $timeFormatted؟ يتطلب الحذف بصمة الهاتف.",
                                    actionLabel = "نجاح البصمة — تأكيد الحذف",
                                    onConfirmed = { viewModel.deleteShiftAfterBiometric(shift) },
                                    onTriggerHardwareSensor = { title, sub, onSuccess ->
                                        BiometricHelper.authenticate(
                                            activity = this@MainActivity,
                                            title = title,
                                            subtitle = sub,
                                            onSuccess = onSuccess,
                                            onCancelOrFailed = { viewModel.dismissBiometricModalWithCancelMessage() }
                                        )
                                    }
                                )
                            },
                            onSelectShiftForEdit = { shift ->
                                viewModel.selectShiftForEditing(shift)
                            },
                            onOpenStaleShiftModal = { shift ->
                                viewModel.openStaleShiftModal(shift)
                            },
                            onDismissStaleShiftModal = {
                                viewModel.dismissStaleShiftModal()
                            },
                            onResolveStaleShift = { shift, endMs ->
                                viewModel.resolveStaleShiftManually(shift, endMs)
                            },
                            onSimulateStale16hShift = {
                                viewModel.simulateStale16hShift()
                            },
                            onSimulateCrossMidnightShift = {
                                viewModel.simulateCrossMidnightShift()
                            },
                            onClearCooldown = {
                                viewModel.clearCooldownForTesting()
                            },
                            onDismissBiometricModal = {
                                viewModel.dismissBiometricModalWithCancelMessage()
                            },
                            onWeekStartChange = viewModel::updateWeekStartDay,
                            onMonthStartChange = viewModel::updateMonthStartDay,
                            onDarkModeChange = viewModel::updateDarkMode,
                            onExportCsvFile = {
                                viewModel.exportCsv { csv ->
                                    pendingExportContent = csv
                                    createCsvLauncher.launch("houdouri_backup.csv")
                                }
                            },
                            onExportJsonFile = {
                                viewModel.exportJson { json ->
                                    pendingExportContent = json
                                    createJsonLauncher.launch("houdouri_backup.json")
                                }
                            },
                            onImportBackupFile = {
                                openBackupLauncher.launch(arrayOf("*/*"))
                            },
                            onImportRawBackupText = { text, isJson ->
                                viewModel.importBackupContent(text, isJson)
                            },
                            onResetSeedData = {
                                viewModel.resetSeedData()
                            },
                            onClearAllShifts = {
                                viewModel.clearAllShifts()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun triggerBiometricPunch() {
        viewModel.requestPunchBiometric { title, subtitle, onSuccess ->
            BiometricHelper.authenticate(
                activity = this,
                title = title,
                subtitle = subtitle,
                onSuccess = onSuccess,
                onCancelOrFailed = { viewModel.dismissBiometricModalWithCancelMessage() }
            )
        }
    }

    private fun writeTextToUri(uri: Uri, content: String) {
        runCatching {
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String? {
        return runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull()
    }
}
