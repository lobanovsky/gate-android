package ru.housekpr.gate.models

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Credentials(
    val email: String,
    val password: String
)

@Serializable
data class RegistrationPayload(
    val email: String,
    @SerialName("phoneNumber")
    val phoneNumber: String
)

@Serializable
data class RecoverPasswordPayload(
    val email: String
)

@Serializable
data class UserInfo(
    val id: Int,
    val email: String,
    @SerialName("phoneNumber")
    val phoneNumber: String? = null,
    @SerialName("registrationDate")
    val registrationDate: String? = null,
    @SerialName("isActive")
    val isActive: Boolean? = null
)

@Serializable
data class UserSession(
    val token: String,
    val user: UserInfo
)

@Serializable
data class RegistrationResponse(
    val message: String? = null,
    val password: String,
    val user: UserInfo
)

@Serializable
data class MessageResponse(
    val message: String? = null
)

@Serializable
data class UserDevices(
    val userId: String,
    val zones: List<Zone>
)

@Serializable
data class Zone(
    val id: Int,
    val name: String,
    val devices: List<Device>
)

@Serializable
data class Device(
    val id: String,
    val name: String,
    val label: String,
    val color: String? = null,
    @SerialName("phoneNumber")
    val phoneNumber: String? = null,
    val deviceKey: String
)

enum class GateArea(val title: String) {
    PARKING("Паркинг"),
    COURTYARD("Двор")
}

enum class GateDirection {
    ENTER,
    EXIT
}

data class GateAction(
    val direction: GateDirection,
    val device: Device
)

data class GateSection(
    val area: GateArea,
    val actions: Map<GateDirection, GateAction>
) {
    val title: String = area.title
}

data class GateActionId(
    val area: GateArea,
    val direction: GateDirection
) {
    val phoneNumber: String
        get() = when (area to direction) {
            GateArea.COURTYARD to GateDirection.ENTER -> "+7-903-178-51-52"
            GateArea.COURTYARD to GateDirection.EXIT -> "+7-903-775-86-56"
            GateArea.PARKING to GateDirection.ENTER -> "+7-926-704-96-48"
            GateArea.PARKING to GateDirection.EXIT -> "+7-926-704-97-09"
            else -> ""
        }

    fun dialUri(): Uri = Uri.parse("tel:${normalizePhone(phoneNumber)}")

    private fun normalizePhone(value: String): String {
        val digits = value.filter { it.isDigit() || it == '+' }
        return if (digits.startsWith("+")) digits else "+$digits"
    }
}

@Serializable
data class ApiErrorResponse(
    val error: String? = null,
    val message: String? = null
)

data class AppAlert(
    val title: String,
    val message: String
)

data class BiometricOption(
    val buttonTitle: String,
    val reason: String
)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

sealed class ApiError(message: String? = null) : Exception(message) {
    data object InvalidBaseUrl : ApiError()
    data object InvalidResponse : ApiError()
    data object Unauthorized : ApiError()
    data class ServerError(val details: String) : ApiError(details)
    data class Transport(val details: String) : ApiError(details)
}
