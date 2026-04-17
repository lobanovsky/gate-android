package ru.housekpr.gate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ru.housekpr.gate.models.AppThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B5E4E),
    secondary = Color(0xFF58727C),
    tertiary = Color(0xFFC47B2A),
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3E8EF),
    secondaryContainer = Color(0xFFD9E8F0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FD5C2),
    secondary = Color(0xFFB4CBD4),
    tertiary = Color(0xFFF5B56D),
    background = Color(0xFF10161B),
    surface = Color(0xFF172027),
    surfaceVariant = Color(0xFF24323C),
    secondaryContainer = Color(0xFF21303A)
)

@Composable
fun GateTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        content = content
    )
}
