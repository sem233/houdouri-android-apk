package com.houdouri.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.houdouri.app.data.ShiftEntity
import com.houdouri.app.domain.ShiftCalculator
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class HoudouriTab(val label: String) {
    HOME("الرئيسية"),
    HISTORY("السجل"),
    MANUAL("تعديل يدوي"),
    SETTINGS("الإعدادات")
}

@Composable
fun HoudouriAppScreen(
    uiState: HoudouriUiState,
    onPunchClick: () -> Unit,
    onSaveManualShift: (ShiftEntity) -> Unit,
    onDeleteShift: (ShiftEntity) -> Unit,
    onResolveStaleShift: (ShiftEntity, Long) -> Unit,
    onWeekStartChange: (DayOfWeek) -> Unit,
    onMonthStartChange: (Int) -> Unit,
    onDarkModeChange: (Boolean) -> Unit
) {
    var currentTab by remember { mutableStateOf(HoudouriTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == HoudouriTab.HOME,
                    onClick = { currentTab = HoudouriTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(HoudouriTab.HOME.label) }
                )
                NavigationBarItem(
                    selected = currentTab == HoudouriTab.HISTORY,
                    onClick = { currentTab = HoudouriTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(HoudouriTab.HISTORY.label) }
                )
                NavigationBarItem(
                    selected = currentTab == HoudouriTab.MANUAL,
                    onClick = { currentTab = HoudouriTab.MANUAL },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    label = { Text(HoudouriTab.MANUAL.label) }
                )
                NavigationBarItem(
                    selected = currentTab == HoudouriTab.SETTINGS,
                    onClick = { currentTab = HoudouriTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(HoudouriTab.SETTINGS.label) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentTab) {
                HoudouriTab.HOME -> HomeScreen(
                    uiState = uiState,
                    onPunchClick = onPunchClick,
                    onResolveStaleShift = onResolveStaleShift
                )
                HoudouriTab.HISTORY -> HistoryScreen(
                    uiState = uiState,
                    onDeleteShift = onDeleteShift
                )
                HoudouriTab.MANUAL -> ManualEditScreen(
                    onSaveShift = onSaveManualShift
                )
                HoudouriTab.SETTINGS -> SettingsScreen(
                    uiState = uiState,
                    onWeekStartChange = onWeekStartChange,
                    onMonthStartChange = onMonthStartChange,
                    onDarkModeChange = onDarkModeChange
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    uiState: HoudouriUiState,
    onPunchClick: () -> Unit,
    onResolveStaleShift: (ShiftEntity, Long) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // المؤقت يعمل فقط عندما تكون الشاشة الرئيسية مفتوحة ومرئية ويوجد شيفت مفتوح
    // ويتوقف تلقائياً فور إغلاق الشاشة (صفر استهلاك بطارية في الخلفية)
    LaunchedEffect(lifecycleOwner, uiState.openShift?.id) {
        if (uiState.openShift != null) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    nowMillis = System.currentTimeMillis()
                    delay(15_000L)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        uiState.confirmationBanner?.let { banner ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = banner.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        uiState.statusMessage?.let { msg ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        uiState.staleOpenShiftPrompt?.let { staleShift ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "شيفت مفتوح منذ أكثر من 16 ساعة",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اختر إغلاقه على مدة 8 ساعات افتراضية أو عدّله من شاشة التعديل اليدوي:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            onResolveStaleShift(staleShift, staleShift.startTime + 8L * 3600_000L)
                        }
                    ) {
                        Text("إغلاق على 8 ساعات")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val openShift = uiState.openShift
                if (openShift == null) {
                    Text(
                        text = "لا يوجد شيفت مفتوح",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val startText = ShiftCalculator.formatTimeArabic(openShift.startTime, openShift.timezoneId)
                    val liveMins = ShiftCalculator.calculateLiveShiftMinutes(openShift, nowMillis)
                    Text(
                        text = "شيفت شغال من $startText",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ShiftCalculator.formatDurationShort(liveMins),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Button(
            onClick = onPunchClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (uiState.openShift == null) "بصمة تسجيل حضور" else "بصمة تسجيل انصراف",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val totals = uiState.totals
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryStatCard(
                title = "إجمالي اليوم",
                value = ShiftCalculator.formatDurationShort(totals?.todayMinutes ?: 0L),
                modifier = Modifier.weight(1f)
            )
            SummaryStatCard(
                title = "إجمالي الأسبوع",
                value = ShiftCalculator.formatDurationShort(totals?.weekMinutes ?: 0L),
                modifier = Modifier.weight(1f)
            )
            SummaryStatCard(
                title = "إجمالي الشهر",
                value = ShiftCalculator.formatDurationShort(totals?.monthMinutes ?: 0L),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SummaryStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HistoryScreen(uiState: HoudouriUiState, onDeleteShift: (ShiftEntity) -> Unit) {
    val groups = remember(uiState.shifts) { ShiftCalculator.groupShiftsByDay(uiState.shifts) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(groups) { group ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = group.date.toString(), fontWeight = FontWeight.Bold)
                        Text(
                            text = ShiftCalculator.formatDurationShort(group.totalMinutes),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    group.shifts.forEach { shift ->
                        val startStr = ShiftCalculator.formatTimeArabic(shift.startTime, shift.timezoneId)
                        val endStr = shift.endTime?.let { ShiftCalculator.formatTimeArabic(it, shift.timezoneId) } ?: "مفتوح"
                        val durStr = ShiftCalculator.formatDurationShort(ShiftCalculator.calculateShiftMinutes(shift))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "$startStr ← $endStr ($durStr)")
                            IconButton(onClick = { onDeleteShift(shift) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف بالبصمة")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManualEditScreen(onSaveShift: (ShiftEntity) -> Unit) {
    var startHour by remember { mutableStateOf("08") }
    var startMinute by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("16") }
    var endMinute by remember { mutableStateOf("30") }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("إضافة شيفت يدوي لليوم (يتطلب البصمة للحفظ)", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startHour,
                onValueChange = { startHour = it },
                label = { Text("ساعة الحضور (0-23)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = startMinute,
                onValueChange = { startMinute = it },
                label = { Text("دقيقة الحضور") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = endHour,
                onValueChange = { endHour = it },
                label = { Text("ساعة الانصراف (0-23)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endMinute,
                onValueChange = { endMinute = it },
                label = { Text("دقيقة الانصراف") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("ملاحظة اختيارية") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val today = LocalDate.now(ShiftCalculator.DEFAULT_ZONE)
                val sh = startHour.toIntOrNull()?.coerceIn(0, 23) ?: 8
                val sm = startMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                val eh = endHour.toIntOrNull()?.coerceIn(0, 23) ?: 16
                val em = endMinute.toIntOrNull()?.coerceIn(0, 59) ?: 30

                val startZdt = ZonedDateTime.of(today, LocalTime.of(sh, sm), ShiftCalculator.DEFAULT_ZONE)
                var endZdt = ZonedDateTime.of(today, LocalTime.of(eh, em), ShiftCalculator.DEFAULT_ZONE)
                if (!endZdt.isAfter(startZdt)) {
                    endZdt = endZdt.plusDays(1) // شيفت عابر لمنتصف الليل
                }

                onSaveShift(
                    ShiftEntity(
                        startTime = startZdt.toInstant().toEpochMilli(),
                        endTime = endZdt.toInstant().toEpochMilli(),
                        timezoneId = ShiftCalculator.DEFAULT_ZONE.id,
                        note = note.ifBlank { null }
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تأكيد البصمة وحفظ الشيفت")
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: HoudouriUiState,
    onWeekStartChange: (DayOfWeek) -> Unit,
    onMonthStartChange: (Int) -> Unit,
    onDarkModeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("الإعدادات والدورة الشهرية", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الوضع الداكن")
            Switch(checked = uiState.settings.darkMode, onCheckedChange = onDarkModeChange)
        }

        Text("بداية الأسبوع (الافتراضي: السبت)", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onWeekStartChange(DayOfWeek.SATURDAY) }) {
                Text("السبت")
            }
            OutlinedButton(onClick = { onWeekStartChange(DayOfWeek.SUNDAY) }) {
                Text("الأحد")
            }
            OutlinedButton(onClick = { onWeekStartChange(DayOfWeek.MONDAY) }) {
                Text("الإثنين")
            }
        }

        Text("بداية الشهر (1 = ميلادي عادي، أو 26 لدورة 26 إلى 25)", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onMonthStartChange(1) }) {
                Text("يوم 1 (عادي)")
            }
            OutlinedButton(onClick = { onMonthStartChange(26) }) {
                Text("يوم 26 ← 25")
            }
        }
    }
}
