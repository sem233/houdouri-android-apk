package com.houdouri.app

import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.houdouri.app.ui.BiometricHelper
import com.houdouri.app.ui.HoudouriAppScreen
import com.houdouri.app.ui.MainViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            var didPromptOnLaunch by rememberSaveable { mutableStateOf(false) }

            // أول ما يفتح التطبيق تظهر تلقائياً نافذة بصمة التلفون مرة واحدة
            LaunchedEffect(Unit) {
                if (!didPromptOnLaunch) {
                    didPromptOnLaunch = true
                    triggerBiometricPunch()
                }
            }

            val colorScheme = if (uiState.settings.darkMode) {
                darkColorScheme()
            } else {
                lightColorScheme()
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
                            onSaveManualShift = { shift ->
                                BiometricHelper.authenticate(
                                    activity = this@MainActivity,
                                    title = "تأكيد حفظ التعديل اليدوي",
                                    subtitle = "يتطلب التعديل اليدوي بصمة الهاتف للحفظ",
                                    onSuccess = { viewModel.saveManualShiftAfterBiometric(shift) },
                                    onCancelOrFailed = { viewModel.onBiometricCancelled(it) }
                                )
                            },
                            onDeleteShift = { shift ->
                                BiometricHelper.authenticate(
                                    activity = this@MainActivity,
                                    title = "تأكيد حذف الشيفت",
                                    subtitle = "يتطلب الحذف بصمة الهاتف",
                                    onSuccess = { viewModel.deleteShiftAfterBiometric(shift) },
                                    onCancelOrFailed = { viewModel.onBiometricCancelled(it) }
                                )
                            },
                            onResolveStaleShift = { shift, endMs ->
                                viewModel.resolveStaleShiftManually(shift, endMs)
                            },
                            onWeekStartChange = viewModel::updateWeekStartDay,
                            onMonthStartChange = viewModel::updateMonthStartDay,
                            onDarkModeChange = viewModel::updateDarkMode
                        )
                    }
                }
            }
        }
    }

    private fun triggerBiometricPunch() {
        BiometricHelper.authenticate(
            activity = this,
            title = "بصمة الحضور والانصراف — حضوري",
            subtitle = "ضع بصمتك لتسجيل الحضور أو الانصراف بالوقت الحالي",
            onSuccess = { viewModel.onBiometricPunchSucceeded() },
            onCancelOrFailed = { reason -> viewModel.onBiometricCancelled(reason) }
        )
    }
}
