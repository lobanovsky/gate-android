# Gate Android App

Нативное Android-приложение на Kotlin + Jetpack Compose для управления шлагбаумами и воротами ELDES. Проект повторяет поведение iOS-версии из `/Users/evgeny/Projects/ios/gate`: авторизация, регистрация, восстановление пароля, сохранённая сессия, биометрический вход и фиксированный главный экран с зонами `Двор` и `Паркинг`.

## Что реализовано

- вход через `POST /api/auth/login`
- регистрация через `POST /api/auth/register`
- восстановление пароля через `POST /api/auth/recover-password`
- загрузка устройств через `GET /api/private/devices`
- открытие устройства через `POST /api/private/devices/:id/open`
- сохранение сессии и credentials в защищённом local storage
- биометрический вход через `BiometricPrompt`
- резервный звонок на номера для каждой команды
- динамический маппинг backend-устройств в фиксированную схему `2 x 2`

## Структура проекта

```text
app/
├── src/main/java/ru/housekpr/gate/
│   ├── models/      # API и UI модели
│   ├── services/    # API client, storage, mapping, dependencies
│   ├── ui/          # Compose-экраны и тема
│   ├── AppViewModel.kt
│   └── MainActivity.kt
├── src/main/res/    # manifest, theme resources
└── src/test/        # unit tests
```

## Требования

- JDK 17
- Android Studio Hedgehog или новее
- Android SDK с `compileSdk 35`

## Конфигурация backend

Базовый host сейчас задаётся в `app/build.gradle.kts` через:

```kotlin
buildConfigField("String", "BACKEND_HOST", "\"gate-backend.housekpr.ru\"")
```

Приложение собирает URL как `https://<BACKEND_HOST>`.

## Запуск

```bash
./gradlew assembleDebug
./gradlew test
```

После этого откройте проект в Android Studio и запустите конфигурацию `app` на эмуляторе или устройстве.

## Проверка

В проект добавлены unit-тесты для:

- маппинга зон и устройств в фиксированный layout
- обработки `401/403` и logout-flow
- cooldown после открытия ворот
- извлечения server error message из API-ответов

Полная сборка не была прогнана в этой среде автоматически: sandbox блокирует Gradle daemon sockets и сетевой доступ для разрешения зависимостей.

## Release и публикация

Для release-подписи используется локальный файл `keystore.properties`, который не должен попадать в git. Возьмите шаблон из `keystore.properties.example` и заполните его параметрами production keystore.

Основные команды:

```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

Материалы для публикации в RuStore:

- [RuStoreReleaseChecklist.md](/Users/evgeny/Projects/android/gate/docs/RuStoreReleaseChecklist.md)
- [RuStoreMetadataTemplate.md](/Users/evgeny/Projects/android/gate/docs/RuStoreMetadataTemplate.md)
