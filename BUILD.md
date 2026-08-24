# Сборка APK без Android Studio

В проекте нет gradle-wrapper'а, поэтому сборка идёт локально установленным тулчейном.

## Что установлено (один раз, лежит вне проекта)

| Компонент | Версия | Путь |
|---|---|---|
| JDK | Temurin 21.0.12 | `F:\android-build\jdk\jdk-21.0.12.1+1` |
| Gradle | 9.3.1 | `F:\android-build\gradle\gradle-9.3.1` |
| Android SDK | platform 36 / 36.1, build-tools 36.1.0, platform-tools | `F:\android-build\sdk` |

Путь к SDK прописан в `local.properties` (в git не попадает).

## Отладочная сборка

```bash
./build-apk.ps1 :app:assembleDebug
```

Результат: `app/build/outputs/apk/debug/app-debug.apk`.
Подписывается ключом `debug.keystore` в корне проекта (создаётся `keytool`, в git не попадает).

## Релизная сборка

```bash
./build-apk.ps1 :app:assembleRelease
```

Перед запуском нужно задать переменные окружения — см. `keystore-info.txt`:

```bash
$env:KEYSTORE_PATH="<путь>\my-upload-key.jks"; $env:STORE_PASSWORD="..."; $env:KEY_PASSWORD="..."
```

Результат: `app/build/outputs/apk/release/app-release.apk`.

## Тесты

```bash
./build-apk.ps1 :app:testDebugUnitTest
```

* `CadGeometryTest` — геометрия помещения, решатель зафиксированных длин, раскладка плитки.
* `TileMathTest` — расходы материалов, сверка с табличными значениями.
* `ScreenSmokeTest` — CAD-редактор и калькулятор собираются и переключаются (Robolectric).

## Установка на телефон

Скинуть APK на устройство и открыть. В Android 8+ система один раз спросит
разрешение «Установка неизвестных приложений» для того приложения, из которого
открывается файл (проводник, мессенджер, браузер).

Отладочный и релизный APK подписаны разными ключами — перед сменой одного на другой
предыдущую версию нужно удалить.
