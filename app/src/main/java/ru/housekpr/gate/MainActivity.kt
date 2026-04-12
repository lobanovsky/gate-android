package ru.housekpr.gate

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.housekpr.gate.services.AppDependencies
import ru.housekpr.gate.ui.GateApp
import ru.housekpr.gate.ui.theme.GateTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dependencies = AppDependencies.from(applicationContext)

        setContent {
            GateTheme {
                val appViewModel: AppViewModel = viewModel(
                    factory = AppViewModel.factory(dependencies)
                )
                val state by appViewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    appViewModel.bootstrap()
                }

                GateApp(
                    state = state,
                    onDismissAlert = appViewModel::dismissAlert,
                    onLogin = appViewModel::login,
                    onRegister = appViewModel::register,
                    onRecoverPassword = appViewModel::recoverPassword,
                    onBiometricLogin = {
                        val biometricManager = BiometricManager.from(context)
                        val canAuthenticate = biometricManager.canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK
                        )
                        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                            appViewModel.onBiometricUnavailable()
                        } else {
                            val prompt = BiometricPrompt(
                                this@MainActivity,
                                ContextCompat.getMainExecutor(context),
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(
                                        result: BiometricPrompt.AuthenticationResult
                                    ) {
                                        appViewModel.loginWithStoredCredentials()
                                    }

                                    override fun onAuthenticationError(
                                        errorCode: Int,
                                        errString: CharSequence
                                    ) {
                                        if (
                                            errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                                            errorCode != BiometricPrompt.ERROR_CANCELED
                                        ) {
                                            appViewModel.onBiometricFailure(errString.toString())
                                        }
                                    }
                                }
                            )

                            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                .setTitle(state.biometricOption?.buttonTitle ?: "Войти по биометрии")
                                .setSubtitle(state.biometricOption?.reason ?: "Подтвердите вход")
                                .setNegativeButtonText("Отмена")
                                .build()
                            prompt.authenticate(promptInfo)
                        }
                    },
                    onRefresh = appViewModel::loadDevices,
                    onOpenGate = appViewModel::openGate,
                    onDial = appViewModel::dialGate,
                    onLogout = appViewModel::logout,
                    buttonTitle = appViewModel::buttonTitle,
                    isActionDisabled = appViewModel::isActionDisabled,
                    isActionInProgress = appViewModel::isActionInProgress,
                    isActionWaiting = appViewModel::isActionWaiting,
                    onDialFailure = appViewModel::onDialFailure,
                    onLinkOpenFailure = appViewModel::onLinkOpenFailure
                )
            }
        }
    }
}
