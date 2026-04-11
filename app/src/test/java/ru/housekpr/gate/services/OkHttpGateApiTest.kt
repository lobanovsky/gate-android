package ru.housekpr.gate.services

import org.junit.Assert.assertEquals
import org.junit.Test

class OkHttpGateApiTest {
    private val api = OkHttpGateApi()

    @Test
    fun extractErrorMessagePrefersJsonErrorField() {
        val raw = """{"error":"Неверный пароль"}"""

        assertEquals("Неверный пароль", api.extractErrorMessage(raw, 401))
    }

    @Test
    fun extractErrorMessageFallsBackToPlainText() {
        val raw = "Ошибка backend"

        assertEquals("Ошибка backend", api.extractErrorMessage(raw, 500))
    }

    @Test
    fun extractErrorMessageFallsBackToStatusCode() {
        assertEquals("Ошибка сервера: 500", api.extractErrorMessage("", 500))
    }
}
