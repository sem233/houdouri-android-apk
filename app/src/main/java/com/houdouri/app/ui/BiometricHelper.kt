package com.houdouri.app.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

object BiometricHelper {

    fun authenticate(
        activity: AppCompatActivity,
        title: String = "تأكيد البصمة — حضوري",
        subtitle: String = "ضع بصمتك لتسجيل الحضور أو الانصراف",
        onSuccess: () -> Unit,
        onCancelOrFailed: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricManager = BiometricManager.from(activity)

        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onCancelOrFailed("البصمة القوية (BIOMETRIC_STRONG) غير مفعلة على هذا الجهاز.")
            return
        }

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onCancelOrFailed("تم إلغاء البصمة — وضع الاطلاع فقط")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // يظل الـ Prompt مفتوحاً لمحاولة أخرى أو الإلغاء
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("إلغاء (الاطلاع فقط)")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(promptInfo)
    }
}
