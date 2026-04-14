package ru.housekpr.gate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.housekpr.gate.models.ApiError
import ru.housekpr.gate.models.BiometricOption
import ru.housekpr.gate.models.Credentials
import ru.housekpr.gate.models.Device
import ru.housekpr.gate.models.GateArea
import ru.housekpr.gate.models.GateDirection
import ru.housekpr.gate.models.MessageResponse
import ru.housekpr.gate.models.RegistrationPayload
import ru.housekpr.gate.models.RegistrationResponse
import ru.housekpr.gate.models.UserDevices
import ru.housekpr.gate.models.UserInfo
import ru.housekpr.gate.models.UserSession
import ru.housekpr.gate.models.Zone
import ru.housekpr.gate.services.BiometricSupport
import ru.housekpr.gate.services.GateApi
import ru.housekpr.gate.services.SecureStorage

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Test
    fun bootstrapLoadsSavedSessionAndDevices() = runTest(dispatcher) {
        val storage = FakeStorage().apply { saveSession(sampleSession()) }
        val api = FakeApi(devices = sampleDevices())
        val viewModel = AppViewModel(api, storage, FakeBiometricSupport(), dispatcher)

        viewModel.bootstrap()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isAuthenticated)
        assertEquals(2, viewModel.state.value.sections.size)
    }

    @Test
    fun loginStoresCredentialsAndLoadsDevices() = runTest(dispatcher) {
        val storage = FakeStorage()
        val api = FakeApi(devices = sampleDevices())
        val viewModel = AppViewModel(api, storage, FakeBiometricSupport(), dispatcher)

        viewModel.login("test@example.com", "secret")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isAuthenticated)
        assertTrue(storage.hasCredentials())
        assertEquals(2, viewModel.state.value.sections.size)
    }

    @Test
    fun logoutClearsAuthenticatedState() = runTest(dispatcher) {
        val storage = FakeStorage().apply { saveSession(sampleSession()) }
        val api = FakeApi(devices = sampleDevices())
        val viewModel = AppViewModel(api, storage, FakeBiometricSupport(), dispatcher)

        viewModel.bootstrap()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.logout()

        assertFalse(viewModel.state.value.isAuthenticated)
        assertTrue(viewModel.state.value.sections.isEmpty())
    }

    @Test
    fun openGateAddsAndRemovesCooldown() = runTest(dispatcher) {
        val storage = FakeStorage().apply { saveSession(sampleSession()) }
        val api = FakeApi(devices = sampleDevices())
        val viewModel = AppViewModel(api, storage, FakeBiometricSupport(), dispatcher)

        viewModel.bootstrap()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openGate(GateArea.COURTYARD, GateDirection.ENTER)
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.state.value.cooldownActions.isNotEmpty())
        advanceTimeBy(2_001)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.cooldownActions.isEmpty())
    }

    @Test
    fun unauthorizedDevicesRequestLogsOutUser() = runTest(dispatcher) {
        val storage = FakeStorage().apply { saveSession(sampleSession()) }
        val api = FakeApi(fetchDevicesError = ApiError.Unauthorized)
        val viewModel = AppViewModel(api, storage, FakeBiometricSupport(), dispatcher)

        viewModel.bootstrap()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isAuthenticated)
        assertEquals("Сессия истекла", viewModel.state.value.alert?.title)
    }
}

private class FakeApi(
    private val session: UserSession = sampleSession(),
    private val devices: UserDevices = sampleDevices(),
    private val fetchDevicesError: Throwable? = null
) : GateApi {
    override suspend fun login(credentials: Credentials): UserSession = session
    override suspend fun register(payload: RegistrationPayload): RegistrationResponse =
        RegistrationResponse(password = "generated", user = session.user)
    override suspend fun recoverPassword(email: String): MessageResponse = MessageResponse("ok")
    override suspend fun fetchDevices(token: String): UserDevices {
        fetchDevicesError?.let { throw it }
        return devices
    }
    override suspend fun open(device: Device, userId: String, token: String) = Unit
}

private class FakeStorage : SecureStorage {
    private var session: UserSession? = null
    private var credentials: Credentials? = null

    override fun loadSession(): UserSession? = session
    override fun saveSession(session: UserSession) { this.session = session }
    override fun clearSession() { session = null }
    override fun saveCredentials(credentials: Credentials) { this.credentials = credentials }
    override fun loadCredentials(): Credentials? = credentials
    override fun hasCredentials(): Boolean = credentials != null
}

private class FakeBiometricSupport : BiometricSupport {
    override fun currentOption(hasStoredCredentials: Boolean): BiometricOption? {
        return if (hasStoredCredentials) {
            BiometricOption("Войти по биометрии", "Подтвердите вход")
        } else {
            null
        }
    }
}

private fun sampleSession(): UserSession {
    return UserSession(
        token = "token",
        user = UserInfo(id = 1, email = "test@example.com")
    )
}

private fun sampleDevices(): UserDevices {
    return UserDevices(
        userId = "42",
        zones = listOf(
            Zone(
                id = 1,
                name = "Двор",
                devices = listOf(
                    Device("1", "Двор-заезд", "Заехать", null, null, "a"),
                    Device("2", "Двор-выезд", "Выехать", null, null, "b")
                )
            ),
            Zone(
                id = 2,
                name = "Паркинг",
                devices = listOf(
                    Device("3", "Паркинг въезд", "Заехать", null, null, "c"),
                    Device("4", "Паркинг выезд", "Выехать", null, null, "d")
                )
            )
        )
    )
}
