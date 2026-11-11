package com.houdouri.app.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

object BiometricHelper {

    fun isHardwareBiometricAvailable(activity: AppCompatActivity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        val strong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (strong == BiometricManager.BIOMETRIC_SUCCESS) return true
        val weak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return weak == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: AppCompatActivity,
        title: String = "تأكيد البصمة — حضوري",
        subtitle: String = "ضع بصمتك لتسجيل الحضور أو الانصراف",
        onSuccess: () -> Unit,
        onCancelOrFailed: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricManager = BiometricManager.from(activity)

        val authenticators = if (
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        }

        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // لا نغلق النافذة التفاعلية، بل نسمح للمستخدم بالتأكيد من نافذة البصمة التفاعلية على الشاشة
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
                    onCancelOrFailed("تم إلغاء البصمة — لم يتم تسجيل أي حركة (وضع الاطلاع على الساعات فقط).")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("إلغاء (الاطلاع فقط)")
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)
            .build()

        runCatching {
            prompt.authenticate(promptInfo)
        }
    }
}
