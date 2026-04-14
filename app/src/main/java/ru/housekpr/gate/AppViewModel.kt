package ru.housekpr.gate

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.housekpr.gate.models.ApiError
import ru.housekpr.gate.models.AppAlert
import ru.housekpr.gate.models.BiometricOption
import ru.housekpr.gate.models.Credentials
import ru.housekpr.gate.models.GateActionId
import ru.housekpr.gate.models.GateArea
import ru.housekpr.gate.models.GateDirection
import ru.housekpr.gate.models.GateSection
import ru.housekpr.gate.models.RegistrationPayload
import ru.housekpr.gate.models.UserDevices
import ru.housekpr.gate.models.UserSession
import ru.housekpr.gate.services.AppDependencies
import ru.housekpr.gate.services.BiometricSupport
import ru.housekpr.gate.services.GateApi
import ru.housekpr.gate.services.GateLayoutBuilder
import ru.housekpr.gate.services.SecureStorage
import ru.housekpr.gate.services.StorageError
import kotlinx.coroutines.delay

data class AppUiState(
    val isBusy: Boolean = false,
    val isLoadingDevices: Boolean = false,
    val isAuthenticated: Boolean = false,
    val sections: List<GateSection> = emptyList(),
    val biometricOption: BiometricOption? = null,
    val alert: AppAlert? = null,
    val inFlightAction: GateActionId? = null,
    val cooldownActions: Set<GateActionId> = emptySet()
)

class AppViewModel(
    private val api: GateApi,
    private val storage: SecureStorage,
    private val biometricSupport: BiometricSupport,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _state = MutableStateFlow(
        AppUiState(
            biometricOption = biometricSupport.currentOption(storage.hasCredentials())
        )
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var session: UserSession? = null
    private var userDevices: UserDevices? = null

    fun bootstrap() {
        if (session != null) return
        runCatching {
            storage.loadSession()
        }.onSuccess { savedSession ->
            savedSession?.let {
                logDebug("Bootstrap restored saved session")
                session = it
                _state.update { state -> state.copy(isAuthenticated = true) }
                loadDevices()
            }
        }.onFailure { error ->
            logError("Bootstrap failed while reading stored session", error)
            present(error, "Не удалось восстановить сессию")
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return
        launchBusy {
            runCatching {
                logDebug("Login started for ${email.trim()}")
                api.login(Credentials(email.trim(), password))
            }.onSuccess { userSession ->
                logDebug("Backend login succeeded for ${email.trim()}")
                runCatching {
                    applySession(userSession)
                    logDebug("Session persisted successfully")
                    storage.saveCredentials(Credentials(email.trim(), password))
                    logDebug("Credentials persisted successfully")
                    refreshBiometricOption()
                    loadDevices()
                }.onFailure { error ->
                    logError("Login post-processing failed", error)
                    present(error, "Не удалось завершить вход")
                }
            }.onFailure { error ->
                logError("Login failed before session persistence", error)
                present(error, "Не удалось войти")
            }
        }
    }

    fun register(email: String, phoneNumber: String) {
        if (email.isBlank() || phoneNumber.isBlank()) return
        launchBusy {
            runCatching {
                val response = api.register(RegistrationPayload(email.trim(), phoneNumber))
                val credentials = Credentials(email.trim(), response.password)
                val session = api.login(credentials)
                Triple(session, credentials, response)
            }.onSuccess { (userSession, credentials, _) ->
                applySession(userSession)
                storage.saveCredentials(credentials)
                refreshBiometricOption()
                loadDevices()
            }.onFailure { error ->
                present(error, "Не удалось зарегистрироваться")
            }
        }
    }

    fun recoverPassword(email: String, onSuccess: (() -> Unit)? = null) {
        if (email.isBlank()) return
        launchBusy {
            runCatching {
                api.recoverPassword(email.trim())
            }.onSuccess { response ->
                _state.update {
                    it.copy(
                        alert = AppAlert(
                            title = "Готово",
                            message = response.message ?: "Пароль отправлен на \"$email\""
                        )
                    )
                }
                onSuccess?.invoke()
            }.onFailure { error ->
                present(error, "Не удалось восстановить пароль")
            }
        }
    }

    fun loginWithStoredCredentials() {
        val credentials = runCatching { storage.loadCredentials() }
            .onFailure { error ->
                logError("Biometric login failed while loading saved credentials", error)
                present(error, "Не удалось войти по биометрии")
                refreshBiometricOption()
            }
            .getOrNull() ?: return

        launchBusy {
            runCatching {
                api.login(credentials)
            }.onSuccess { userSession ->
                runCatching {
                    applySession(userSession)
                    loadDevices()
                }.onFailure { error ->
                    logError("Biometric login post-processing failed", error)
                    present(error, "Не удалось завершить вход")
                }
            }.onFailure { error ->
                present(error, "Не удалось войти")
            }
        }
    }

    fun onBiometricUnavailable() {
        _state.update {
            it.copy(
                alert = AppAlert(
                    "Не удалось войти по биометрии",
                    "Биометрический вход недоступен."
                )
            )
        }
    }

    fun onBiometricFailure(message: String) {
        _state.update {
            it.copy(
                alert = AppAlert(
                    "Не удалось войти по биометрии",
                    message.ifBlank { "Не удалось использовать биометрию." }
                )
            )
        }
    }

    fun loadDevices() {
        val currentSession = session ?: return
        if (_state.value.isLoadingDevices) return

        viewModelScope.launch(mainDispatcher) {
            _state.update { it.copy(isLoadingDevices = true) }
            runCatching {
                api.fetchDevices(currentSession.token)
            }.onSuccess { devices ->
                userDevices = devices
                _state.update {
                    it.copy(
                        isLoadingDevices = false,
                        sections = GateLayoutBuilder.build(devices)
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoadingDevices = false) }
                handleAuthorizedError(error, "Не удалось загрузить устройства")
            }
        }
    }

    fun openGate(area: GateArea, direction: GateDirection) {
        val actionId = GateActionId(area, direction)
        val currentSession = session ?: return
        val currentDevices = userDevices ?: return
        val currentAction = _state.value.sections
            .firstOrNull { it.area == area }
            ?.actions
            ?.get(direction) ?: return

        if (_state.value.inFlightAction != null || actionId in _state.value.cooldownActions) return

        viewModelScope.launch(mainDispatcher) {
            _state.update { it.copy(isBusy = true, inFlightAction = actionId) }
            runCatching {
                api.open(currentAction.device, currentDevices.userId, currentSession.token)
            }.onSuccess {
                _state.update {
                    it.copy(
                        isBusy = false,
                        inFlightAction = null,
                        cooldownActions = it.cooldownActions + actionId
                    )
                }
                delay(2_000)
                _state.update {
                    it.copy(cooldownActions = it.cooldownActions - actionId)
                }
            }.onFailure { error ->
                _state.update { it.copy(isBusy = false, inFlightAction = null) }
                handleAuthorizedError(error, "Не удалось выполнить команду")
            }
        }
    }

    fun dialGate(area: GateArea, direction: GateDirection): Intent {
        return Intent(Intent.ACTION_DIAL, GateActionId(area, direction).dialUri())
    }

    fun logout() {
        session = null
        userDevices = null
        storage.clearSession()
        _state.update {
            it.copy(
                isBusy = false,
                isLoadingDevices = false,
                isAuthenticated = false,
                sections = emptyList(),
                inFlightAction = null,
                cooldownActions = emptySet(),
                biometricOption = biometricSupport.currentOption(storage.hasCredentials())
            )
        }
    }

    fun onDialFailure() {
        _state.update {
            it.copy(
                alert = AppAlert(
                    "Не удалось начать звонок",
                    "Проверьте, доступен ли телефонный вызов на этом устройстве."
                )
            )
        }
    }

    fun onLinkOpenFailure() {
        _state.update {
            it.copy(
                alert = AppAlert(
                    "Не удалось открыть ссылку",
                    "Проверьте, доступен ли браузер на этом устройстве."
                )
            )
        }
    }

    fun dismissAlert() {
        _state.update { it.copy(alert = null) }
    }

    fun buttonTitle(area: GateArea, direction: GateDirection): String {
        val actionId = GateActionId(area, direction)
        return if (
            _state.value.inFlightAction == actionId ||
            actionId in _state.value.cooldownActions
        ) {
            "Ждите..."
        } else {
            when (direction) {
                GateDirection.ENTER -> "Заехать"
                GateDirection.EXIT -> "Выехать"
            }
        }
    }

    fun isActionDisabled(area: GateArea, direction: GateDirection, hasDevice: Boolean): Boolean {
        val actionId = GateActionId(area, direction)
        return !hasDevice ||
            _state.value.inFlightAction == actionId ||
            actionId in _state.value.cooldownActions
    }

    fun isActionInProgress(area: GateArea, direction: GateDirection): Boolean {
        val actionId = GateActionId(area, direction)
        return _state.value.inFlightAction == actionId
    }

    fun isActionWaiting(area: GateArea, direction: GateDirection): Boolean {
        val actionId = GateActionId(area, direction)
        return _state.value.inFlightAction == actionId || actionId in _state.value.cooldownActions
    }

    private fun applySession(userSession: UserSession) {
        session = userSession
        storage.saveSession(userSession)
        _state.update { it.copy(isAuthenticated = true) }
    }

    private fun refreshBiometricOption() {
        _state.update {
            it.copy(
                biometricOption = biometricSupport.currentOption(storage.hasCredentials())
            )
        }
    }

    private fun handleAuthorizedError(error: Throwable, fallbackTitle: String) {
        if (error is ApiError.Unauthorized) {
            logout()
            _state.update {
                it.copy(
                    alert = AppAlert(
                        "Сессия истекла",
                        "Сессия истекла. Выполните вход повторно."
                    )
                )
            }
            return
        }

        present(error, fallbackTitle)
    }

    private fun present(error: Throwable, title: String) {
        logError(
            "Presenting error. title=$title type=${error::class.java.name} message=${error.message} cause=${error.cause?.message}",
            error
        )
        val message = when (error) {
            is ApiError.InvalidBaseUrl -> "Некорректный URL backend."
            is ApiError.InvalidResponse -> "Сервер вернул неожиданный ответ."
            is ApiError.Unauthorized -> "Сессия истекла. Выполните вход повторно."
            is ApiError.ServerError -> error.details
            is ApiError.Transport -> error.details
            is StorageError -> error.message ?: "Ошибка защищённого хранилища."
            else -> error.localizedMessage ?: "Неизвестная ошибка"
        }
        _state.update {
            it.copy(alert = AppAlert(title, message))
        }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch(mainDispatcher) {
            _state.update { it.copy(isBusy = true) }
            try {
                block()
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun logDebug(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private fun logError(message: String, error: Throwable) {
        runCatching { Log.e(TAG, message, error) }
    }

    companion object {
        private const val TAG = "AppViewModel"

        fun factory(dependencies: AppDependencies): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(
                        api = dependencies.api,
                        storage = dependencies.storage,
                        biometricSupport = dependencies.biometricSupport
                    ) as T
                }
            }
        }
    }
}
