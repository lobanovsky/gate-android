# RuStore Release Checklist

## Перед сборкой
- Проверьте `versionCode` и `versionName` в `app/build.gradle.kts`.
- Создайте production keystore и заполните `keystore.properties` на основе `keystore.properties.example`.
- Убедитесь, что `BACKEND_HOST` указывает на production backend.
- Проверьте название приложения, иконку и основные тексты.

## Техническая проверка
- Выполните `./gradlew assembleRelease`.
- Выполните `./gradlew bundleRelease`.
- Убедитесь, что release-сборка запускается на устройстве или эмуляторе.
- Проверьте login, загрузку устройств, открытие ворот, звонок и переход по ссылке.

## Материалы для карточки
- Название приложения
- Краткое описание
- Полное описание
- Иконка 512x512
- Скриншоты основных экранов
- Контакты разработчика
- Privacy Policy URL

## Публикация в RuStore
- Войдите в RuStore Console через VK ID.
- Создайте карточку приложения.
- Заполните поля из `docs/RuStoreMetadataTemplate.md`.
- Загрузите `bundleRelease` или подписанный release-артефакт.
- Заполните заметки к релизу.
- Отправьте приложение на модерацию.
