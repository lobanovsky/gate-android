package ru.housekpr.gate.services

import android.content.Context

data class AppDependencies(
    val api: GateApi,
    val storage: SecureStorage,
    val biometricSupport: BiometricSupport
) {
    companion object {
        fun from(context: Context): AppDependencies {
            val appContext = context.applicationContext
            return AppDependencies(
                api = OkHttpGateApi(),
                storage = SecurePrefsStorage(appContext),
                biometricSupport = AndroidBiometricSupport(appContext)
            )
        }
    }
}
