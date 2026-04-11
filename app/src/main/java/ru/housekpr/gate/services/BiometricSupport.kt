package ru.housekpr.gate.services

import android.content.Context
import androidx.biometric.BiometricManager
import ru.housekpr.gate.models.BiometricOption

interface BiometricSupport {
    fun currentOption(hasStoredCredentials: Boolean): BiometricOption?
}

class AndroidBiometricSupport(
    private val context: Context
) : BiometricSupport {
    override fun currentOption(hasStoredCredentials: Boolean): BiometricOption? {
        if (!hasStoredCredentials) return null

        val manager = BiometricManager.from(context)
        val status = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (status != BiometricManager.BIOMETRIC_SUCCESS) return null

        return BiometricOption(
            buttonTitle = "Войти по биометрии",
            reason = "Подтвердите вход в приложение через биометрию."
        )
    }
}
