package com.houdouri.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.houdouri.app.data.ShiftEntity
import com.houdouri.app.domain.BackupManager
import com.houdouri.app.domain.ShiftCalculator
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate

enum class HoudouriTab(val label: String) {
    HOME("الرئيسية"),
    HISTORY("السجل"),
    MANUAL("تعديل يدوي"),
    SETTINGS("الإعدادات")
}

enum class HistoryFilter(val label: String) {
    ALL("الكل"),
    WEEK("هذا الأسبوع"),
    MONTH("هذا الشهر")
}

private val Emerald600 = Color(0xFF059669)
private val Emerald500 = Color(0xFF10B981)
private val Emerald400 = Color(0xFF34D399)
private val Amber600 = Color(0xFFD97706)
private val Amber500 = Color(0xFFF59E0B)
private val Sky400 = Color(0xFF38BDF8)
private val Rose500 = Color(0xFFF43F5E)

@Composable
fun HoudouriAppScreen(
    uiState: HoudouriUiState,
    onPunchClick: () -> Unit,
    onSaveManualShift: (ShiftEntity, () -> Unit) -> Unit,
    onDeleteShift: (ShiftEntity) -> Unit,
    onSelectShiftForEdit: (ShiftEntity?) -> Unit,
    onOpenStaleShiftModal: (ShiftEntity) -> Unit,
    onDismissStaleShiftModal: () -> Unit,
    onResolveStaleShift: (ShiftEntity, Long) -> Unit,
    onSimulateStale16hShift: () -> Unit,
    onSimulateCrossMidnightShift: () -> Unit,
    onClearCooldown: () -> Unit,
    onDismissBiometricModal: () -> Unit,
    onWeekStartChange: (DayOfWeek) -> Unit,
    onMonthStartChange: (Int) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onExportCsvFile: () -> Unit,
    onExportJsonFile: () -> Unit,
    onImportBackupFile: () -> Unit,
    onImportRawBackupText: (String, Boolean) -> Unit,
    onResetSeedData: () -> Unit,
    onClearAllShifts: () -> Unit
) {
    var currentTab by remember { mutableStateOf(HoudouriTab.HOME) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حضوري",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { currentTab = HoudouriTab.HOME }
                    )

                    Button(
                        onClick = onPunchClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.openShift != null) Amber600 else Emerald600,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.openShift != null) "بصمة انصراف" else "بصمة حضور",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == HoudouriTab.HOME,
                        onClick = { currentTab = HoudouriTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(HoudouriTab.HOME.label, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = currentTab == HoudouriTab.HISTORY,
                        onClick = { currentTab = HoudouriTab.HISTORY },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text(HoudouriTab.HISTORY.label, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = currentTab == HoudouriTab.MANUAL,
                        onClick = { currentTab = HoudouriTab.MANUAL },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        label = { Text("تعديل", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = currentTab == HoudouriTab.SETTINGS,
                        onClick = { currentTab = HoudouriTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(HoudouriTab.SETTINGS.label, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                HoudouriTab.HOME -> HomeScreen(
                    uiState = uiState,
                    onPunchClick = onPunchClick,
                    onOpenStaleShiftModal = onOpenStaleShiftModal,
                    onSimulateStale16hShift = onSimulateStale16hShift,
                    onSimulateCrossMidnightShift = onSimulateCrossMidnightShift,
                    onClearCooldown = onClearCooldown
                )
                HoudouriTab.HISTORY -> HistoryScreen(
                    uiState = uiState,
                    onEditShift = { shift ->
                        onSelectShiftForEdit(shift)
                        currentTab = HoudouriTab.MANUAL
                    },
                    onDeleteShift = onDeleteShift,
                    onNavigateToManualAdd = {
                        onSelectShiftForEdit(null)
                        currentTab = HoudouriTab.MANUAL
                    }
                )
                HoudouriTab.MANUAL -> ManualEditScreen(
                    uiState = uiState,
                    onClearEditing = { onSelectShiftForEdit(null) },
                    onSelectShiftToEdit = { shift -> onSelectShiftForEdit(shift) },
                    onSaveShift = { shift ->
                        onSaveManualShift(shift) {
                            currentTab = HoudouriTab.HISTORY
                        }
                    },
                    onDeleteShift = onDeleteShift
                )
                HoudouriTab.SETTINGS -> SettingsScreen(
                    uiState = uiState,
                    onWeekStartChange = onWeekStartChange,
                    onMonthStartChange = onMonthStartChange,
                    onDarkModeChange = onDarkModeChange,
                    onExportCsvFile = onExportCsvFile,
                    onExportJsonFile = onExportJsonFile,
                    onImportBackupFile = onImportBackupFile,
                    onImportRawBackupText = onImportRawBackupText,
                    onResetSeedData = onResetSeedData,
                    onClearAllShifts = onClearAllShifts
                )
            }
        }
    }

    // نافذة التحقق البيومتري المطابقة للمعاينة
    uiState.activeBiometricRequest?.let { req ->
        BiometricPromptModalDialog(
            request = req,
            onDismiss = onDismissBiometricModal
        )
    }

    // نافذة تحديد وقت الانصراف الفعلي للشيفت المفتوح لأكثر من 16 ساعة
    uiState.staleOpenShiftPrompt?.let { staleShift ->
        StaleShiftModalDialog(
            staleShift = staleShift,
            onResolve = { endMs -> onResolveStaleShift(staleShift, endMs) },
            onDismiss = onDismissStaleShiftModal
        )
    }
}

@Composable
fun HomeScreen(
    uiState: HoudouriUiState,
    onPunchClick: () -> Unit,
    onOpenStaleShiftModal: (ShiftEntity) -> Unit,
    onSimulateStale16hShift: () -> Unit,
    onSimulateCrossMidnightShift: () -> Unit,
    onClearCooldown: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // المؤقت مربوط بدورة حياة الشاشة فقط ويتوقف تلقائياً عند إغلاقها (صفر بطارية بالخلفية)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }

    val openShift = uiState.openShift
    val liveOpenMinutes = if (openShift != null) {
        ShiftCalculator.calculateLiveShiftMinutes(openShift, nowMillis)
    } else 0L
    val isStale = ShiftCalculator.isOpenShiftOver16Hours(openShift, nowMillis, 16L)
    val (cooldownAllowed, cooldownRemainingSec) = ShiftCalculator.canPunchAfterCooldown(
        lastPunchEpochMillis = uiState.settings.lastPunchEpochMs,
        nowEpochMillis = nowMillis,
        cooldownSeconds = 30L
    )
    val totals = uiState.totals
    val todayKey = totals?.todayDate?.toString() ?: LocalDate.now(ShiftCalculator.DEFAULT_ZONE).toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. شاشة التأكيد الواضحة بعد كل تسجيل حضور أو انصراف
        uiState.confirmationBanner?.let { banner ->
            val (bgColor, borderColor, iconColor) = when (banner.type) {
                BannerType.CHECKIN -> Triple(
                    Emerald500.copy(alpha = 0.14f),
                    Emerald500.copy(alpha = 0.45f),
                    Emerald400
                )
                BannerType.CHECKOUT -> Triple(
                    Sky400.copy(alpha = 0.14f),
                    Sky400.copy(alpha = 0.45f),
                    Sky400
                )
                BannerType.WARNING -> Triple(
                    Amber500.copy(alpha = 0.15f),
                    Amber500.copy(alpha = 0.5f),
                    Amber500
                )
                BannerType.INFO -> Triple(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.outline,
                    Emerald400
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (banner.type == BannerType.WARNING) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "تأكيد البصمة · ساعة النظام (Africa/Cairo)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = banner.message,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        // 2. تنبيه الشيفت المفتوح لأكثر من 16 ساعة
        if (openShift != null && isStale) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Amber500.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Amber500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Amber500,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "شيفت مفتوح منذ أكثر من 16 ساعة (${ShiftCalculator.formatDurationShort(liveOpenMinutes)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "التطبيق لا يغلقه تلقائياً من نفسه — حدد وقت الانصراف الفعلي يدوياً لحساب الساعات بدقة.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Button(
                        onClick = { onOpenStaleShiftModal(openShift) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Amber600, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تحديد وقت الانصراف الفعلي", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. كارت حالة الدوام الآن والمدة الجارية
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حالة الدوام الآن · يعمل أوفلاين بالكامل",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = todayKey,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (openShift != null) {
                    val startFormatted = ShiftCalculator.formatTimeArabic(openShift.startTime, openShift.timezoneId)
                    Text(
                        text = "شيفت شغال من $startFormatted",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "المدة الجارية الآن (تُحسب فقط أثناء فتح الشاشة بدون أي خلفية):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ShiftCalculator.formatDurationShort(liveOpenMinutes),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald400
                        )
                    }
                } else {
                    Text(
                        text = "لا يوجد شيفت مفتوح",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اضغط على زر البصمة بالأسفل لتسجيل حضور جديد بالوقت الحالي من ساعة النظام.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = "المنطقة الزمنية: Africa/Cairo · صلاحية وحيدة: USE_BIOMETRIC · صفر استهلاك بطارية بالخلفية",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        // 4. كارت التحقق البيومتري المباشر والزر الكبير
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "التحقق البيومتري المباشر · BIOMETRIC_STRONG",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (openShift != null) "إغلاق الشيفت وتسجيل الانصراف" else "بدء شيفت جديد وتسجيل الحضور",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (openShift != null) {
                        "نجاح البصمة يقفل الشيفت المفتوح فوراً ويحسب مدته بالدقيقة ويضيفها لإجمالي اليوم والأسبوع والشهر."
                    } else {
                        "نجاح البصمة يفتح شيفت حضور جديداً بلحظة البصمة بالضبط."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onPunchClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (openShift != null) Amber600 else Emerald600,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (openShift != null) "بصمة تسجيل الانصراف" else "بصمة تسجيل الحضور",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (!cooldownAllowed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حماية الضغط المزدوج: متبقي $cooldownRemainingSec ثانية",
                            fontSize = 11.sp,
                            color = Amber500,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = onClearCooldown) {
                            Text(
                                text = "تجاوز الفاصل للتجربة",
                                fontSize = 11.sp,
                                color = Amber500,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. قسم إجمالي الساعات المحسوبة (3 كروت تفصيلية مطابقة للمعاينة)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجمالي الساعات المحسوبة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "الشيفت الليلي يُحسب على يوم بدايته",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SummaryDetailCard(
                title = "إجمالي اليوم",
                durationFormatted = ShiftCalculator.formatDurationShort(totals?.todayMinutes ?: 0L),
                subtitle = "تاريخ اليوم: $todayKey · ${totals?.todayMinutes ?: 0L} دقيقة"
            )

            val weekDayName = ShiftCalculator.dayOfWeekArabicName(uiState.settings.weekStartDay)
            val weekRangeText = if (totals != null) "${totals.weekStart} ← ${totals.weekEnd}" else ""
            SummaryDetailCard(
                title = "إجمالي الأسبوع (يبدأ $weekDayName)",
                durationFormatted = ShiftCalculator.formatDurationShort(totals?.weekMinutes ?: 0L),
                subtitle = "$weekRangeText · ${totals?.weekMinutes ?: 0L} دقيقة"
            )

            val monthTitle = if (uiState.settings.monthStartDay == 1) {
                "إجمالي الشهر (شهر ميلادي عادي)"
            } else {
                "إجمالي الشهر (يبدأ يوم ${uiState.settings.monthStartDay})"
            }
            val monthRangeText = if (totals != null) "${totals.monthStart} ← ${totals.monthEnd}" else ""
            SummaryDetailCard(
                title = monthTitle,
                durationFormatted = ShiftCalculator.formatDurationShort(totals?.monthMinutes ?: 0L),
                subtitle = "$monthRangeText · ${totals?.monthMinutes ?: 0L} دقيقة"
            )
        }

        // 6. شريط اختبار الحالات الدقيقة (Edge Cases) المطابق للمعاينة
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "اختبار الحالات الدقيقة (Edge Cases)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "جرّب سيناريو الشيفت المنسي لأكثر من 16 ساعة أو الشيفت العابر لمنتصف الليل بضغطة واحدة:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                OutlinedButton(
                    onClick = onSimulateStale16hShift,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "محاكاة شيفت مفتوح منذ 18 ساعة (>16س)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedButton(
                    onClick = onSimulateCrossMidnightShift,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "إضافة شيفت عابر لمنتصف الليل (10م ← 2:30ص)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryDetailCard(
    title: String,
    durationFormatted: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = durationFormatted,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HistoryScreen(
    uiState: HoudouriUiState,
    onEditShift: (ShiftEntity) -> Unit,
    onDeleteShift: (ShiftEntity) -> Unit,
    onNavigateToManualAdd: () -> Unit
) {
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    val totals = uiState.totals

    val filteredShifts = remember(uiState.shifts, filter, totals) {
        uiState.shifts.filter { shift ->
            if (filter == HistoryFilter.ALL || totals == null) return@filter true
            val d = ShiftCalculator.getShiftStartDate(shift)
            when (filter) {
                HistoryFilter.WEEK -> !d.isBefore(totals.weekStart) && !d.isAfter(totals.weekEnd)
                HistoryFilter.MONTH -> !d.isBefore(totals.monthStart) && !d.isAfter(totals.monthEnd)
                else -> true
            }
        }
    }

    val dayGroups = remember(filteredShifts) {
        ShiftCalculator.groupShiftsByDay(filteredShifts)
    }

    val filteredTotalMinutes = remember(dayGroups) {
        dayGroups.sumOf { it.totalMinutes }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // رأس السجل + الفلاتر (الكل / هذا الأسبوع / هذا الشهر)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "سجل الأيام والشيفتات",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "كل يوم يعرض جميع شيفتاته وإجمالي ساعاته. الشيفت العابر لمنتصف الليل يُنسب ليوم بدايته.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryFilter.entries.forEach { itemFilter ->
                            val selected = filter == itemFilter
                            Button(
                                onClick = { filter = itemFilter },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Emerald600 else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(itemFilter.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "إجمالي الفلتر المحدد (${dayGroups.size} يوم)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ShiftCalculator.formatDurationShort(filteredTotalMinutes),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald400
                            )
                        }

                        Button(
                            onClick = onNavigateToManualAdd,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald600,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة شيفت يدوي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (dayGroups.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "لا توجد شيفتات مسجلة في هذه الفترة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "سجل حضورك بالبصمة من الشاشة الرئيسية أو أضف شيفتاً يدوياً.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(dayGroups) { group ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // رأس اليوم
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = group.arabicDateLabel,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${group.date} · ${group.shifts.size} شيفت",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "إجمالي اليوم",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = ShiftCalculator.formatDurationShort(group.totalMinutes),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Emerald400
                                )
                            }
                        }

                        // قائمة الشيفتات داخل اليوم
                        group.shifts.forEachIndexed { idx, shift ->
                            if (idx > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                            val startFormatted = ShiftCalculator.formatTimeArabic(shift.startTime, shift.timezoneId)
                            val endFormatted = shift.endTime?.let {
                                ShiftCalculator.formatTimeArabic(it, shift.timezoneId)
                            } ?: "شيفت مفتوح حالياً"
                            val durationMins = ShiftCalculator.calculateShiftMinutes(shift)
                            val crossesMidnight = ShiftCalculator.isCrossMidnight(shift)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الحضور: $startFormatted ← الانصراف: $endFormatted",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (shift.endTime == null) "جارٍ..." else ShiftCalculator.formatDurationShort(durationMins),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (shift.endTime == null) Amber500 else Emerald400
                                    )
                                }

                                if (crossesMidnight) {
                                    Text(
                                        text = "عابر لمنتصف الليل — يُحسب بالكامل على يوم البداية (${group.date})",
                                        fontSize = 11.sp,
                                        color = Sky400,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (!shift.note.isNullOrBlank()) {
                                    Text(
                                        text = "ملاحظة: ${shift.note}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onEditShift(shift) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تعديل", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { onDeleteShift(shift) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Rose500.copy(alpha = 0.45f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حذف", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManualEditScreen(
    uiState: HoudouriUiState,
    onClearEditing: () -> Unit,
    onSelectShiftToEdit: (ShiftEntity) -> Unit,
    onSaveShift: (ShiftEntity) -> Unit,
    onDeleteShift: (ShiftEntity) -> Unit
) {
    val editingShift = uiState.editingShift
    val today = remember { LocalDate.now(ShiftCalculator.DEFAULT_ZONE) }

    var dateKey by remember(editingShift) {
        mutableStateOf(
            if (editingShift != null) {
                ShiftCalculator.getShiftStartDate(editingShift).toString()
            } else {
                today.toString()
            }
        )
    }
    var startTimeStr by remember(editingShift) {
        mutableStateOf(
            if (editingShift != null) {
                ShiftCalculator.formatTime24h(editingShift.startTime, editingShift.timezoneId)
            } else {
                "08:00"
            }
        )
    }
    var endTimeStr by remember(editingShift) {
        mutableStateOf(
            if (editingShift?.endTime != null) {
                ShiftCalculator.formatTime24h(editingShift.endTime, editingShift.timezoneId)
            } else {
                "16:30"
            }
        )
    }
    var isOpenShift by remember(editingShift) {
        mutableStateOf(editingShift != null && editingShift.endTime == null)
    }
    var crossesMidnight by remember(editingShift) {
        mutableStateOf(editingShift != null && ShiftCalculator.isCrossMidnight(editingShift))
    }
    var note by remember(editingShift) {
        mutableStateOf(editingShift?.note ?: "")
    }
    var errorMsg by remember(editingShift) { mutableStateOf<String?>(null) }

    val previewMinutes = remember(dateKey, startTimeStr, endTimeStr, isOpenShift, crossesMidnight) {
        if (isOpenShift) return@remember null
        val startMs = ShiftCalculator.buildEpochMillis(dateKey, startTimeStr, 0L) ?: return@remember null
        var endMs = ShiftCalculator.buildEpochMillis(
            dateKey,
            endTimeStr,
            if (crossesMidnight) 1L else 0L
        ) ?: return@remember null
        if (!crossesMidnight && endMs <= startMs) {
            endMs = ShiftCalculator.buildEpochMillis(dateKey, endTimeStr, 1L) ?: return@remember null
        }
        val diff = (endMs - startMs) / 60_000L
        if (diff > 0L) diff else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingShift != null) "تعديل شيفت مسجل" else "إضافة شيفت يدوي جديد",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "يتطلب الحفظ أو الحذف تأكيد بصمة الهاتف (BIOMETRIC_STRONG).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (editingShift != null) {
                        TextButton(onClick = onClearEditing) {
                            Text("إلغاء التعديل", color = Emerald400, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // أزرار اختيار سريع للتاريخ
                Text(
                    text = "تاريخ بداية الشيفت (YYYY-MM-DD)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { dateKey = today.toString() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("اليوم", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { dateKey = today.minusDays(1).toString() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("أمس", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { dateKey = today.minusDays(2).toString() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("قبل يومين", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = dateKey,
                    onValueChange = { dateKey = it },
                    label = { Text("تاريخ الشيفت (مثلاً 2026-11-11)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // أوقات الحضور والانصراف بصيغة HH:mm
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startTimeStr,
                        onValueChange = { startTimeStr = it },
                        label = { Text("وقت الحضور (HH:mm)") },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTimeStr,
                        onValueChange = { endTimeStr = it },
                        enabled = !isOpenShift,
                        label = { Text("وقت الانصراف (HH:mm)") },
                        placeholder = { Text("16:30") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // قوالب أوقات سريعة
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            startTimeStr = "08:00"
                            endTimeStr = "16:30"
                            crossesMidnight = false
                            isOpenShift = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("8ص ← 4:30م", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            startTimeStr = "22:00"
                            endTimeStr = "02:30"
                            crossesMidnight = true
                            isOpenShift = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("10م ← 2:30ص ليلي", fontSize = 11.sp)
                    }
                }

                // خيارات الشيفت المفتوح أو العابر لمنتصف الليل
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الشيفت لا يزال مفتوحاً (بدون انصراف)", fontSize = 13.sp)
                    Switch(
                        checked = isOpenShift,
                        onCheckedChange = { isOpenShift = it }
                    )
                }

                if (!isOpenShift) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ينتهي في اليوم التالي (عابر لمنتصف الليل)", fontSize = 13.sp)
                        Switch(
                            checked = crossesMidnight,
                            onCheckedChange = { crossesMidnight = it }
                        )
                    }
                }

                if (!isOpenShift && previewMinutes != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald500.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المدة المحسوبة تلقائياً:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${ShiftCalculator.formatDurationShort(previewMinutes)} (${ShiftCalculator.formatDurationVerbose(previewMinutes)})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة اختيارية (مثلاً: نسيت البصمة صباحاً)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let { err ->
                    Text(text = err, color = Rose500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        errorMsg = null
                        val startMs = ShiftCalculator.buildEpochMillis(dateKey, startTimeStr, 0L)
                        if (startMs == null) {
                            errorMsg = "صيغة التاريخ (YYYY-MM-DD) أو وقت الحضور (HH:mm) غير صحيحة."
                            return@Button
                        }
                        var endMs: Long? = null
                        if (!isOpenShift) {
                            var candidateEnd = ShiftCalculator.buildEpochMillis(
                                dateKey,
                                endTimeStr,
                                if (crossesMidnight) 1L else 0L
                            )
                            if (candidateEnd == null) {
                                errorMsg = "صيغة وقت الانصراف (HH:mm) غير صحيحة."
                                return@Button
                            }
                            if (!crossesMidnight && candidateEnd <= startMs) {
                                candidateEnd = ShiftCalculator.buildEpochMillis(dateKey, endTimeStr, 1L)
                            }
                            if (candidateEnd == null || candidateEnd <= startMs) {
                                errorMsg = "وقت الانصراف يجب أن يكون بعد وقت الحضور."
                                return@Button
                            }
                            endMs = candidateEnd
                        }

                        onSaveShift(
                            ShiftEntity(
                                id = editingShift?.id ?: 0L,
                                startTime = startMs,
                                endTime = endMs,
                                timezoneId = "Africa/Cairo",
                                note = note.trim().ifBlank { null }
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (editingShift != null) "بصمة الهاتف لحفظ التعديلات" else "بصمة الهاتف لإضافة الشيفت",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (editingShift != null) {
                    OutlinedButton(
                        onClick = { onDeleteShift(editingShift) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف هذا الشيفت بالبصمة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // قائمة اختيار سريع لتعديل أي شيفت سابق
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختر شيفتاً سابقاً لتعديله",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "اضغط على أي شيفت من القائمة لتحميل بياناته في النموذج أعلاه:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                uiState.shifts.take(10).forEach { s ->
                    val isSelected = editingShift?.id == s.id
                    val sDate = ShiftCalculator.getShiftStartDate(s)
                    val sStart = ShiftCalculator.formatTimeArabic(s.startTime, s.timezoneId)
                    val sEnd = s.endTime?.let { ShiftCalculator.formatTimeArabic(it, s.timezoneId) } ?: "مفتوح"
                    val sDur = if (s.endTime != null) {
                        ShiftCalculator.formatDurationShort(ShiftCalculator.calculateShiftMinutes(s))
                    } else "جارٍ"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Emerald500.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Emerald500 else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectShiftToEdit(s) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$sDate (${ShiftCalculator.formatDateArabic(sDate)})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$sStart ← $sEnd",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = sDur,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald400
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: HoudouriUiState,
    onWeekStartChange: (DayOfWeek) -> Unit,
    onMonthStartChange: (Int) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onExportCsvFile: () -> Unit,
    onExportJsonFile: () -> Unit,
    onImportBackupFile: () -> Unit,
    onImportRawBackupText: (String, Boolean) -> Unit,
    onResetSeedData: () -> Unit,
    onClearAllShifts: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showRawBackupDialog by remember { mutableStateOf(false) }
    var rawBackupInput by remember { mutableStateOf("") }
    var clipboardFeedback by remember { mutableStateOf<String?>(null) }

    val orderedDays = listOf(
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. المظهر والوضع الداكن
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المظهر والوضع الداكن (Material 3)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "واجهة عربية RTL كاملة مصممة لتوفير البطارية على شاشات OLED.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.settings.darkMode,
                    onCheckedChange = onDarkModeChange
                )
            }
        }

        // 2. بداية الأسبوع وبداية الشهر
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "إعدادات الحسابات (الأسبوع والشهر)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "بداية الأسبوع (الافتراضي: السبت)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // عرض جميع أيام الأسبوع الـ 7
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    orderedDays.chunked(4).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowDays.forEach { day ->
                                val selected = uiState.settings.weekStartDay == day
                                Button(
                                    onClick = { onWeekStartChange(day) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) Emerald600 else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = ShiftCalculator.dayOfWeekArabicName(day),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = "يوم بداية الشهر (شهر ميلادي عادي أو دورة رواتب مخصصة)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isStandard = uiState.settings.monthStartDay == 1
                    Button(
                        onClick = { onMonthStartChange(1) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStandard) Emerald600 else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isStandard) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("يوم 1 (ميلادي عادي)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    val is26 = uiState.settings.monthStartDay == 26
                    Button(
                        onClick = { onMonthStartChange(26) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (is26) Emerald600 else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (is26) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("يوم 26 ← 25", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تخصيص يوم بداية الشهر (1 إلى 28): يوم ${uiState.settings.monthStartDay}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onMonthStartChange((uiState.settings.monthStartDay - 1).coerceIn(1, 28))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                onMonthStartChange((uiState.settings.monthStartDay + 1).coerceIn(1, 28))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. النسخ الاحتياطي والاستيراد (CSV & JSON)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "النسخ الاحتياطي المحلي (CSV و JSON)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "صدّر سجل حضورك وانصرافك إلى ملف على تلفونك أو استورده في أي وقت بدون إنترنت (${uiState.shifts.size} شيفت مسجل).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onExportCsvFile,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onExportJsonFile,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onImportBackupFile,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استيراد ملف نسخة احتياطية (CSV أو JSON) من الهاتف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val json = BackupManager.exportToJson(uiState.shifts)
                            clipboardManager.setText(AnnotatedString(json))
                            clipboardFeedback = "تم نسخ بيانات JSON إلى الحافظة بنجاح!"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("نسخ JSON للحافظة", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { showRawBackupDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("لصق واستيراد نصي", fontSize = 11.sp)
                    }
                }

                clipboardFeedback?.let { msg ->
                    Text(text = msg, fontSize = 12.sp, color = Emerald400, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onResetSeedData,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("استعادة بيانات التجربة", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onClearAllShifts,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Rose500.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("مسح كل السجلات", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showRawBackupDialog) {
        Dialog(onDismissRequest = { showRawBackupDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("استيراد نسخة احتياطية نصية (CSV أو JSON)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(
                        value = rawBackupInput,
                        onValueChange = { rawBackupInput = it },
                        label = { Text("الصق محتوى ملف CSV أو JSON هنا") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val trimmed = rawBackupInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    val isJson = trimmed.startsWith("[") || trimmed.startsWith("{")
                                    onImportRawBackupText(trimmed, isJson)
                                    showRawBackupDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("استيراد الآن")
                        }
                        OutlinedButton(
                            onClick = { showRawBackupDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricPromptModalDialog(
    request: BiometricPromptState,
    onDismiss: () -> Unit
) {
    var failedAttemptMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "androidx.biometric.BiometricPrompt",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Emerald400
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            if (request.isCheckout) Amber500.copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f)
                        )
                        .border(
                            2.dp,
                            if (request.isCheckout) Amber500 else Emerald500,
                            CircleShape
                        )
                        .clickable { request.onConfirm() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = if (request.isCheckout) Amber500 else Emerald400,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Text(
                    text = request.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = request.subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                failedAttemptMsg?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = Amber500,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = request.onConfirm,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (request.isCheckout) Amber600 else Emerald600,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(request.actionLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            failedAttemptMsg = "لم يتم التعرف على البصمة — حاول مرة أخرى أو اضغط إلغاء للاطلاع فقط."
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("محاكاة فشل البصمة", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء (الاطلاع فقط)", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StaleShiftModalDialog(
    staleShift: ShiftEntity,
    onResolve: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val startDate = remember(staleShift) { ShiftCalculator.getShiftStartDate(staleShift).toString() }
    var customDate by remember(staleShift) { mutableStateOf(startDate) }
    var customTime by remember(staleShift) { mutableStateOf("16:30") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Amber500.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Amber500)
                    Text(
                        text = "شيفت مفتوح منذ أكثر من 16 ساعة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "بدأ هذا الشيفت في ${ShiftCalculator.formatTimeArabic(staleShift.startTime, staleShift.timezoneId)} بتاريخ $startDate. يرجى تحديد وقت الانصراف الفعلي يدوياً:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Text(
                    text = "إغلاق سريع بمدة قياسية:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8L to "8 ساعات", 9L to "9 ساعات", 10L to "10 ساعات").forEach { (hrs, label) ->
                        OutlinedButton(
                            onClick = { onResolve(staleShift.startTime + hrs * 3600_000L) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = "أو أدخل تاريخ ووقت الانصراف الفعلي بدقة:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customDate,
                        onValueChange = { customDate = it },
                        label = { Text("التاريخ YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customTime,
                        onValueChange = { customTime = it },
                        label = { Text("الوقت HH:mm") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                errorText?.let {
                    Text(it, color = Rose500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val endMs = ShiftCalculator.buildEpochMillis(customDate, customTime, 0L)
                            if (endMs == null || endMs <= staleShift.startTime) {
                                errorText = "يرجى إدخال وقت انصراف صحيح بعد وقت بداية الشيفت."
                            } else {
                                onResolve(endMs)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Amber600, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تأكيد وإغلاق الشيفت", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("لاحقاً")
                    }
                }
            }
        }
    }
}
