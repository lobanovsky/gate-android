package ru.housekpr.gate.services

import ru.housekpr.gate.BuildConfig

object AppConfiguration {
    val backendBaseUrl: String = "https://${BuildConfig.BACKEND_HOST}"
}
