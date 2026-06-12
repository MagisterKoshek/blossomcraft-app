# BlossomCraft на iOS — через Gluon (экспериментально)

iOS достижим, потому что общий модуль **`:core`** — это обычная Java + Gson, а
интерфейс ПК написан на **JavaFX**. Связка **Gluon** (GraalVM native-image +
Gluon Substrate/Attach) умеет запускать JavaFX-приложения на iOS.

> ⚠️ Собрать iOS-приложение можно **только на компьютере Mac с установленным
> Xcode**. На Replit и на Windows это невозможно — здесь даётся готовая
> конфигурация и команды, которые нужно выполнить на Mac. Это «best-effort»
> путь: бизнес-логика переносится как есть, экраны JavaFX могут потребовать
> косметической адаптации под сенсор.

---

## Что понадобится (на Mac)

- **macOS** + **Xcode** (с установленными Command Line Tools).
- **Gluon GraalVM** (специальная сборка GraalVM от Gluon) — путь к ней
  указывается в переменной окружения `GRAALVM_HOME`.
- **JDK 17+** и **Gradle 8.x** (как и для остального проекта).
- Аккаунт Apple Developer — для запуска на реальном устройстве и публикации.

Подробные требования и установка GraalVM описаны в документации Gluon:
<https://docs.gluonhq.com/> (раздел gluonfx / GraalVM).

---

## Шаг 1. Добавьте модуль `:ios` в сборку

В `settings.gradle` добавьте модуль (например, под флагом, чтобы он не мешал
обычной сборке ПК/Android):

```gradle
// settings.gradle — включать только когда собираем под iOS на Mac
if (System.getenv('BLOSSOMCRAFT_IOS') != null) {
    include ':ios'
}
```

Сборку под iOS затем запускайте так:
`BLOSSOMCRAFT_IOS=1 gradle :ios:nativeRun` (см. шаг 3).

---

## Шаг 2. Создайте `ios/build.gradle`

Создайте папку `ios/` и в ней файл `build.gradle`:

```gradle
plugins {
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.1.0'
    // Плагин Gluon для сборки native-image под iOS/Android.
    id 'com.gluonhq.gluonfx-gradle-plugin' version '1.0.25'
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

javafx {
    version = rootProject.ext.javafxVersion
    modules = ['javafx.controls', 'javafx.graphics', 'javafx.media', 'javafx.web']
}

dependencies {
    // Переиспользуем общий core и готовый JavaFX-интерфейс ПК.
    implementation project(':core')
    implementation project(':desktop')
    // По необходимости — Gluon Attach для нативных возможностей iOS:
    // implementation 'com.gluonhq.attach:storage:4.0.18'
    // implementation 'com.gluonhq.attach:display:4.0.18'
}

application {
    // Можно переиспользовать существующую точку входа JavaFX.
    mainClass = 'com.blossomcraft.desktop.DesktopApp'
}

gluonfx {
    target = 'ios'                 // 'ios' для устройства, 'ios-sim' для симулятора
    mainClassName = 'com.blossomcraft.desktop.DesktopApp'
    // bundleId должен совпадать с App ID в вашем Apple Developer-аккаунте:
    // appIdentifier = 'ru.vashsite.blossomcraft'
    // releaseConfiguration { bundleName = 'BlossomCraft' }
}
```

> Примечание: если переиспользовать `:desktop` целиком окажется тяжело для
> native-image, можно создать в модуле `:ios` отдельную лёгкую JavaFX-точку
> входа, которая использует только `:core` и нужные экраны.

---

## Шаг 3. Команды сборки и запуска (на Mac)

```bash
export GRAALVM_HOME=/путь/к/gluon-graalvm
export BLOSSOMCRAFT_IOS=1

# Запуск на симуляторе iOS:
gradle :ios:nativeRun -Pgluonfx.target=ios-sim

# Сборка под реальное устройство:
gradle :ios:nativeBuild -Pgluonfx.target=ios
gradle :ios:nativeLink  -Pgluonfx.target=ios
gradle :ios:nativePackage -Pgluonfx.target=ios   # создаёт .ipa
```

Адрес API задаётся так же, как везде, например через `gradle.properties`
(`blossomcraft.api.base=...`) или экран «Настройки» внутри приложения.

---

## Известные ограничения / на что обратить внимание

- **Сеть.** `:core` использует `java.net.http.HttpClient`. Под GraalVM
  native-image может потребоваться конфигурация reflection/resource или
  переключение на Gluon Attach. Если возникнут проблемы с сетью на устройстве —
  добавьте соответствующие записи в `reflectionconfig`/`resourceconfig`
  (Gluon генерирует заготовки при сборке).
- **Хранение токена.** На ПК токен хранится через `java.util.prefs.Preferences`.
  На iOS вместо этого используйте Gluon Attach Storage (или файловое хранилище)
  для сохранения токена сессии.
- **Интерфейс.** JavaFX-экраны рассчитаны на мышь; под сенсор может
  понадобиться увеличить отступы/кнопки.
- **Сборка тяжёлая.** native-image под iOS требует времени и ресурсов и
  выполняется только на Mac с Xcode.

Эта конфигурация — стартовая точка для разработчика на Mac. Общий код (`:core`)
гарантированно переносится; интерфейс может потребовать доводки.
