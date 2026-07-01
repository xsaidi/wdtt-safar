# SafarVPN Android

Android-клиент SafarVPN для WDTT-туннеля через VK TURN relay. Приложение предназначено для подписок, которые выдаются Telegram-ботом SafarVPN.

**Package/applicationId:** `shop.safarkvn.safarvpn`  
**Версия:** 1.2.5

## Пользовательский поток

1. Пользователь открывает Telegram-бота [`@safarvpn_bot`](https://t.me/safarvpn_bot).
2. Покупает или продлевает продукт "Глушилки".
3. Копирует JSON-ссылку подписки.
4. В приложении открывает вкладку "Профили" и добавляет подписку через URL, буфер обмена, QR или файл.
5. Возвращается на вкладку "Туннель" и нажимает "Подключить".

Домены подписок не хардкодятся в приложении. Основной формат подписки:

```text
https://wdtt.safarkvn.shop/wdtt_sub/{token}.json
```

## Что изменено относительно upstream

- Брендинг qWDTT заменен на SafarVPN.
- `applicationId` и Kotlin namespace изменены на `shop.safarkvn.safarvpn`.
- Вкладка "Деплой" скрыта из обычного UI и включается админ-режимом.
- Автообновления через GitHub Releases удалены; APK обновляется через Telegram-бота.
- Основной экран больше не предлагает ручной ввод IP/домена/пароля.
- Расширенные параметры туннеля свернуты по умолчанию.
- Добавлены временные launcher/splash assets SafarVPN.

## Совместимые форматы

- HTTPS JSON-подписки с `subscriptionName` и `profiles[]`.
- `wdtt://` ссылки оригинального WDTT.
- `qwdtt://config?...` ссылки для расширенного импорта.
- Сырой JSON, QR, буфер обмена и файл `.qwdtt`.

## Сборка

Нужны JDK, Android SDK/NDK и Go toolchain для native library.

```bash
export ANDROID_NDK_HOME=/path/to/android-ndk
./scripts/build-native-libs.sh
./gradlew :app:assembleDebug
```

Release-подпись ожидает keystore:

```text
keystore/safarvpn.keystore
```

Пароли передаются через переменные окружения:

```bash
export SAFARVPN_KEYSTORE_PASSWORD=...
export SAFARVPN_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

`keystore/`, `*.keystore`, `*.jks` и `local.properties` исключены из git.

## Лицензия

[GNU GPL v3](LICENSE)

SafarVPN основан на qWDTT от SpaceNeuroX:

- upstream: https://github.com/SpaceNeuroX/proxy-turn-vk-android
- original: https://github.com/amurcanov/proxy-turn-vk-android
