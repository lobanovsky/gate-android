package ru.housekpr.gate.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import ru.housekpr.gate.AppUiState
import ru.housekpr.gate.models.GateArea
import ru.housekpr.gate.models.GateDirection

private enum class AuthScreenMode {
    LOGIN,
    REGISTER,
    RESET_PASSWORD
}

@Composable
fun GateApp(
    state: AppUiState,
    onDismissAlert: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onRecoverPassword: (String, (() -> Unit)?) -> Unit,
    onBiometricLogin: () -> Unit,
    onRefresh: () -> Unit,
    onOpenGate: (GateArea, GateDirection) -> Unit,
    onDial: (GateArea, GateDirection) -> Intent,
    onLogout: () -> Unit,
    buttonTitle: (GateArea, GateDirection) -> String,
    isActionDisabled: (GateArea, GateDirection, Boolean) -> Boolean,
    isActionInProgress: (GateArea, GateDirection) -> Boolean,
    isActionWaiting: (GateArea, GateDirection) -> Boolean,
    onDialFailure: () -> Unit,
    onLinkOpenFailure: () -> Unit
) {
    val context = LocalContext.current

    state.alert?.let { alert ->
        AlertDialog(
            onDismissRequest = onDismissAlert,
            confirmButton = {
                TextButton(onClick = onDismissAlert) {
                    Text("OK")
                }
            },
            title = { Text(alert.title) },
            text = { Text(alert.message) }
        )
    }

    if (state.isAuthenticated) {
        GatesScreen(
            state = state,
            onRefresh = onRefresh,
            onOpenGate = onOpenGate,
            buttonTitle = buttonTitle,
            isActionDisabled = isActionDisabled,
            isActionInProgress = isActionInProgress,
            isActionWaiting = isActionWaiting,
            onDial = { area, direction ->
                runCatching {
                    context.startActivity(onDial(area, direction))
                }.onFailure {
                    if (it is ActivityNotFoundException) {
                        onDialFailure()
                    }
                }
            },
            onOpenStudioLink = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.lobanovsky.ru")
                        )
                    )
                }.onFailure {
                    if (it is ActivityNotFoundException) {
                        onLinkOpenFailure()
                    }
                }
            },
            onLogout = onLogout
        )
    } else {
        LoginScreen(
            state = state,
            onLogin = onLogin,
            onRegister = onRegister,
            onRecoverPassword = onRecoverPassword,
            onBiometricLogin = onBiometricLogin
        )
    }
}

@Composable
private fun LoginScreen(
    state: AppUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onRecoverPassword: (String, (() -> Unit)?) -> Unit,
    onBiometricLogin: () -> Unit
) {
    var mode by rememberSaveable { mutableStateOf(AuthScreenMode.LOGIN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var registrationEmail by rememberSaveable { mutableStateOf("") }
    var registrationPhone by rememberSaveable { mutableStateOf("") }
    var recoveryEmail by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF1F7FF), Color(0xFFE6EFFA))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = when (mode) {
                        AuthScreenMode.LOGIN -> "Шлагбаумы на Роще"
                        AuthScreenMode.REGISTER -> "Регистрация"
                        AuthScreenMode.RESET_PASSWORD -> "Восстановление пароля"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Управление шлагбаумами и воротами с любовью к деталям",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (mode) {
                AuthScreenMode.LOGIN -> {
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )
                    AuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Пароль",
                        keyboardType = KeyboardType.Password
                    )
                    PrimaryActionButton(
                        title = "Войти",
                        loading = state.isBusy,
                        enabled = isValidEmail(email) && password.isNotBlank(),
                        onClick = { onLogin(email, password) }
                    )
                    state.biometricOption?.let { option ->
                        OutlinedButton(
                            onClick = onBiometricLogin,
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                            }
                            Text(option.buttonTitle)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            registrationEmail = email
                            mode = AuthScreenMode.REGISTER
                        }) {
                            Text("Регистрация")
                        }
                        TextButton(onClick = {
                            recoveryEmail = email
                            mode = AuthScreenMode.RESET_PASSWORD
                        }) {
                            Text("Забыли пароль?")
                        }
                    }
                }

                AuthScreenMode.REGISTER -> {
                    AuthTextField(
                        value = registrationEmail,
                        onValueChange = { registrationEmail = it },
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )
                    AuthTextField(
                        value = registrationPhone,
                        onValueChange = { registrationPhone = formatRussianPhone(it) },
                        label = "Номер телефона",
                        keyboardType = KeyboardType.Phone
                    )
                    PrimaryActionButton(
                        title = "Зарегистрироваться",
                        loading = state.isBusy,
                        enabled = isValidEmail(registrationEmail) && isValidRussianPhone(registrationPhone),
                        onClick = {
                            onRegister(registrationEmail, normalizedPhoneNumber(registrationPhone))
                        }
                    )
                    TextButton(onClick = {
                        email = registrationEmail
                        mode = AuthScreenMode.LOGIN
                    }) {
                        Text("Уже есть учётная запись? Войти")
                    }
                }

                AuthScreenMode.RESET_PASSWORD -> {
                    AuthTextField(
                        value = recoveryEmail,
                        onValueChange = { recoveryEmail = it },
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )
                    PrimaryActionButton(
                        title = "Отправить",
                        loading = state.isBusy,
                        enabled = isValidEmail(recoveryEmail),
                        onClick = {
                            onRecoverPassword(recoveryEmail) {
                                email = recoveryEmail
                                mode = AuthScreenMode.LOGIN
                            }
                        }
                    )
                    TextButton(onClick = {
                        email = recoveryEmail
                        mode = AuthScreenMode.LOGIN
                    }) {
                        Text("Вернуться на форму входа")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GatesScreen(
    state: AppUiState,
    onRefresh: () -> Unit,
    onOpenGate: (GateArea, GateDirection) -> Unit,
    buttonTitle: (GateArea, GateDirection) -> String,
    isActionDisabled: (GateArea, GateDirection, Boolean) -> Boolean,
    isActionInProgress: (GateArea, GateDirection) -> Boolean,
    isActionWaiting: (GateArea, GateDirection) -> Boolean,
    onDial: (GateArea, GateDirection) -> Unit,
    onOpenStudioLink: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    TextButton(onClick = onOpenStudioLink) {
                        Text(
                            "Сделано в Бюро Лобановского",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Выйти"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.isBusy) {
                        if (state.isLoadingDevices) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Обновить"
                            )
                        }
                    }
                }
            )
        },
        containerColor = Color(0xFFF2F3F7)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            if (state.isLoadingDevices && state.sections.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(48.dp)
                    ) {
                        state.sections.forEach { section ->
                            GateSectionCard(
                                title = section.title,
                                enterTitle = buttonTitle(section.area, GateDirection.ENTER),
                                exitTitle = buttonTitle(section.area, GateDirection.EXIT),
                                enterEnabled = !isActionDisabled(section.area, GateDirection.ENTER, section.actions[GateDirection.ENTER] != null),
                                exitEnabled = !isActionDisabled(section.area, GateDirection.EXIT, section.actions[GateDirection.EXIT] != null),
                                enterLoading = isActionInProgress(section.area, GateDirection.ENTER),
                                exitLoading = isActionInProgress(section.area, GateDirection.EXIT),
                                enterWaiting = isActionWaiting(section.area, GateDirection.ENTER),
                                exitWaiting = isActionWaiting(section.area, GateDirection.EXIT),
                                onEnterDial = { onDial(section.area, GateDirection.ENTER) },
                                onExitDial = { onDial(section.area, GateDirection.EXIT) },
                                onEnterOpen = { onOpenGate(section.area, GateDirection.ENTER) },
                                onExitOpen = { onOpenGate(section.area, GateDirection.EXIT) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GateSectionCard(
    title: String,
    enterTitle: String,
    exitTitle: String,
    enterEnabled: Boolean,
    exitEnabled: Boolean,
    enterLoading: Boolean,
    exitLoading: Boolean,
    enterWaiting: Boolean,
    exitWaiting: Boolean,
    onEnterDial: () -> Unit,
    onExitDial: () -> Unit,
    onEnterOpen: () -> Unit,
    onExitOpen: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            GateActionRow(
                direction = GateDirection.ENTER,
                title = enterTitle,
                enabled = enterEnabled,
                loading = enterLoading,
                waiting = enterWaiting,
                fill = Color(0xFFA7DB9B),
                onDial = onEnterDial,
                onOpen = onEnterOpen
            )
            GateActionRow(
                direction = GateDirection.EXIT,
                title = exitTitle,
                enabled = exitEnabled,
                loading = exitLoading,
                waiting = exitWaiting,
                fill = Color(0xFFA1D0F2),
                onDial = onExitDial,
                onOpen = onExitOpen
            )
        }
    }
}

@Composable
private fun GateActionRow(
    direction: GateDirection,
    title: String,
    enabled: Boolean,
    loading: Boolean,
    waiting: Boolean,
    fill: Color,
    onDial: () -> Unit,
    onOpen: () -> Unit
) {
    val actionInteractionSource = remember { MutableInteractionSource() }
    val isPressed by actionInteractionSource.collectIsPressedAsState()
    val activeFill = Color(0xFFF5BA69)
    val currentFill = if (waiting || isPressed) activeFill else fill

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Button(
            onClick = onDial,
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = fill)
        ) {
            ThickIcon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Позвонить",
                tint = Color.White,
                size = 32.dp
            )
        }
        Button(
            onClick = onOpen,
            enabled = enabled,
            interactionSource = actionInteractionSource,
            modifier = Modifier
                .weight(1f)
                .height(88.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = currentFill,
                contentColor = Color.White,
                disabledContainerColor = if (waiting) activeFill else fill.copy(alpha = 0.55f),
                disabledContentColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp
                )
                if (waiting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    GateDirectionIcon(direction = direction, fill = fill)
                }
            }
        }
    }
}

@Composable
private fun GateDirectionIcon(
    direction: GateDirection,
    fill: Color
) {
    val rotation = when (direction) {
        GateDirection.ENTER -> 45f
        GateDirection.EXIT -> -45f
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .background(color = Color.White, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        ThickIcon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = fill,
            size = 27.dp,
            modifier = Modifier
                .rotate(rotation)
        )
    }
}

@Composable
private fun ThickIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val offsets = listOf(
        0.dp to 0.dp,
        (-0.7).dp to 0.dp,
        0.7.dp to 0.dp,
        0.dp to (-0.7).dp,
        0.dp to 0.7.dp
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        offsets.forEach { (x, y) ->
            androidx.compose.material3.Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(size)
            )
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (keyboardType == KeyboardType.Password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun PrimaryActionButton(
    title: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(title, modifier = Modifier.padding(vertical = 6.dp))
    }
}

private fun isValidEmail(value: String): Boolean {
    val trimmed = value.trim()
    val regex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    return regex.matches(trimmed)
}

private fun normalizedPhoneNumber(value: String): String {
    var digits = value.filter { it.isDigit() }
    if (digits.startsWith("8")) {
        digits = "7" + digits.drop(1)
    } else if (digits.isNotEmpty() && !digits.startsWith("7")) {
        digits = "7$digits"
    }
    return digits.take(11)
}

private fun formatRussianPhone(value: String): String {
    val normalized = normalizedPhoneNumber(value)
    val local = normalized.drop(1)
    val builder = StringBuilder("+7")
    if (local.isNotEmpty()) builder.append("(").append(local.take(3))
    if (local.length >= 3) builder.append(")")
    if (local.length > 3) builder.append(local.drop(3).take(3))
    if (local.length > 6) builder.append("-").append(local.drop(6).take(2))
    if (local.length > 8) builder.append("-").append(local.drop(8).take(2))
    return builder.toString()
}

private fun isValidRussianPhone(value: String): Boolean {
    return value == formatRussianPhone(value) && normalizedPhoneNumber(value).length == 11
}
