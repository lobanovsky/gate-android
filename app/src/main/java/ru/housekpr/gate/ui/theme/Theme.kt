package ru.housekpr.gate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B5E4E),
    secondary = Color(0xFF58727C),
    tertiary = Color(0xFFC47B2A),
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FD5C2),
    secondary = Color(0xFFB4CBD4),
    tertiary = Color(0xFFF5B56D)
)

@Composable
fun GateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
