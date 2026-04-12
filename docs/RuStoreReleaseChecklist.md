# RuStore Release Checklist

## Перед сборкой
- Проверьте `versionCode` и `versionName` в `app/build.gradle.kts`.
- Создайте production keystore и заполните локальный `keystore.properties` на основе `keystore.properties.example`.
- Убедитесь, что `BACKEND_HOST` указывает на production backend.
- Проверьте название приложения, иконку и основные тексты.
- Заполните реальные `Developer Contact` и `Privacy Policy URL` в `docs/RuStoreMetadataTemplate.md`.

## Техническая проверка
- Выполните `./gradlew assembleRelease`.
- Выполните `./gradlew bundleRelease`.
- Убедитесь, что release-сборка запускается на устройстве или эмуляторе.
- Проверьте login, загрузку устройств, открытие ворот, звонок и переход по ссылке.
- Убедитесь, что release подписан production keystore, а не debug-ключом.

## Материалы для карточки
- Название приложения: `Gate of Love`
- Краткое описание
- Полное описание
- Иконка 512x512 для карточки RuStore
- Скриншоты основных экранов
- Контакты разработчика
- Privacy Policy URL
- Release notes первой версии

## Публикация в RuStore
- Войдите в RuStore Console через VK ID.
- Создайте карточку приложения.
- Заполните поля из `docs/RuStoreMetadataTemplate.md`.
- Загрузите `bundleRelease` или подписанный release-артефакт.
- Заполните заметки к релизу.
- Отправьте приложение на модерацию.
