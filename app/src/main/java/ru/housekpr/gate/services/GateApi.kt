package ru.housekpr.gate.services

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.housekpr.gate.models.ApiError
import ru.housekpr.gate.models.ApiErrorResponse
import ru.housekpr.gate.models.Credentials
import ru.housekpr.gate.models.Device
import ru.housekpr.gate.models.MessageResponse
import ru.housekpr.gate.models.RegistrationPayload
import ru.housekpr.gate.models.RegistrationResponse
import ru.housekpr.gate.models.RecoverPasswordPayload
import ru.housekpr.gate.models.UserDevices
import ru.housekpr.gate.models.UserSession
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GateApi {
    suspend fun login(credentials: Credentials): UserSession
    suspend fun register(payload: RegistrationPayload): RegistrationResponse
    suspend fun recoverPassword(email: String): MessageResponse
    suspend fun fetchDevices(token: String): UserDevices
    suspend fun open(device: Device, userId: String, token: String)
}

class OkHttpGateApi(
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) : GateApi {

    override suspend fun login(credentials: Credentials): UserSession {
        return request<Credentials, UserSession>(
            path = "/api/auth/login",
            method = "POST",
            body = credentials,
            token = null,
            privateAuthorized = false
        )
    }

    override suspend fun register(payload: RegistrationPayload): RegistrationResponse {
        return request<RegistrationPayload, RegistrationResponse>(
            path = "/api/auth/register",
            method = "POST",
            body = payload,
            token = null,
            privateAuthorized = false
        )
    }

    override suspend fun recoverPassword(email: String): MessageResponse {
        return request<RecoverPasswordPayload, MessageResponse>(
            path = "/api/auth/recover-password",
            method = "POST",
            body = RecoverPasswordPayload(email),
            token = null,
            privateAuthorized = false
        )
    }

    override suspend fun fetchDevices(token: String): UserDevices {
        return request<Unit, UserDevices>(
            path = "/api/private/devices",
            method = "GET",
            body = null,
            token = token,
            privateAuthorized = true
        )
    }

    override suspend fun open(device: Device, userId: String, token: String) {
        @kotlinx.serialization.Serializable
        data class OpenPayload(val key: String, val userid: String)

        request<OpenPayload, Unit>(
            path = "/api/private/devices/${device.id}/open",
            method = "POST",
            body = OpenPayload(device.deviceKey, userId),
            token = token,
            privateAuthorized = true
        )
    }

    private suspend inline fun <reified RequestBody, reified ResponseBody> request(
        path: String,
        method: String,
        body: RequestBody?,
        token: String?,
        privateAuthorized: Boolean
    ): ResponseBody {
        val url = "${AppConfiguration.backendBaseUrl}${path}"
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val requestBody = if (body != null) {
            val serializer = json.serializersModule.serializer<RequestBody>()
            val payload = json.encodeToString(serializer, body)
            payload.toRequestBody("application/json".toMediaType())
        } else {
            null
        }

        val request = requestBuilder.method(method, requestBody).build()

        try {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> {
                        if (ResponseBody::class == Unit::class) {
                            @Suppress("UNCHECKED_CAST")
                            return Unit as ResponseBody
                        }
                        if (rawBody.isBlank()) {
                            throw ApiError.InvalidResponse
                        }
                        return json.decodeFromString(rawBody)
                    }

                    response.code == 401 || response.code == 403 -> {
                        if (privateAuthorized) throw ApiError.Unauthorized
                        throw ApiError.ServerError(extractErrorMessage(rawBody, response.code))
                    }

                    else -> {
                        throw ApiError.ServerError(extractErrorMessage(rawBody, response.code))
                    }
                }
            }
        } catch (error: ApiError) {
            throw error
        } catch (error: IOException) {
            throw ApiError.Transport(error.localizedMessage ?: "Network error")
        } catch (error: Exception) {
            throw ApiError.Transport(error.localizedMessage ?: "Unknown error")
        }
    }

    fun extractErrorMessage(rawBody: String, statusCode: Int): String {
        if (rawBody.isNotBlank()) {
            runCatching {
                json.decodeFromString<ApiErrorResponse>(rawBody)
            }.getOrNull()?.let { parsed ->
                val message = parsed.error ?: parsed.message
                if (!message.isNullOrBlank()) return message
            }

            val plain = rawBody.trim()
            if (plain.isNotEmpty()) return plain
        }
        return "Ошибка сервера: $statusCode"
    }
}
