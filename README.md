# BlossomCraft — Native Java App

A native Java client for **BlossomCraft** that reuses the existing website's PHP
API. It ships two front-ends built on one shared core:

- **Desktop (PC):** a JavaFX application (`:desktop`).
- **Android (phone):** a native Android app in Java (`:android`).
- **Shared core (`:core`):** API client, models, services, themes — no UI, no
  platform dependencies. Both apps depend on it.

It mirrors the website's features: authentication (login / register / Google /
token session), the shop, music platform (with player), short-form videos,
Telegram-style messaging (direct chats + groups + channels), the admin panel,
profiles, and the four site themes (dark / light / mirror / gray-mirror).

> The whole project is written in **Java** (not Kotlin), per the requirement.
> Replit cannot preview native apps, so this repository is delivered as
> **buildable source**. Build and run it locally with the steps below.
>
> **Russian quick-start:** see **ИНСТРУКЦИЯ.md** (где вписать адрес сайта, где
> остаются данные БД, как собрать под каждую платформу). iOS details:
> **iOS-GLUON.md**.

---

## Project layout

```
BlossomCraftApp/
├── settings.gradle         # includes :core, :desktop, and :android (if SDK present)
├── build.gradle            # shared versions/repos for the JVM modules
├── gradle.properties       # default API base URL + Gradle/Android flags
├── core/                   # shared, platform-independent module
│   └── src/main/java/com/blossomcraft/core/
│       ├── ApiConfig.java          # base URL resolution + override
│       ├── net/                    # ApiClient, TokenStore, Json, Multipart, exceptions
│       ├── model/                  # User, Product, Track, Video, Message, Group, ...
│       ├── service/                # Auth/Shop/Music/Video/Message/Group/Profile/Admin
│       ├── theme/                  # Theme enum + palettes mirroring the site
│       └── BlossomCraft.java       # facade wiring services + session
├── desktop/                # JavaFX app
│   └── src/main/java/com/blossomcraft/desktop/
│       ├── DesktopApp.java         # JavaFX Application entry point
│       ├── AppContext, Async, ThemeManager, ...
│       └── ui/                     # AuthScreen, MainShell, *Page screens
│   └── src/main/resources/css/app.css   # all four themes
└── android/                # Android app (depends on :core)
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/blossomcraft/android/   # activities, fragments, adapters
        └── res/                             # layouts, menu, drawables, themes
```

---

## Configuring the API base URL

Both apps talk to the **same PHP backend** that serves the website's
`/api/*.php` endpoints. The base URL is **not hard-coded** — set it to your host.

Resolution order in the core (`ApiConfig`):

1. A value set at runtime via `ApiConfig.setBaseUrl(...)` (the in-app Settings
   screen does this).
2. The `blossomcraft.api.base` JVM system property.
3. The `BLOSSOMCRAFT_API_BASE` environment variable.
4. The built-in default placeholder
   (`https://your-blossomcraft-host.example/api`).

Set the default once in `gradle.properties`:

```properties
blossomcraft.api.base=https://your-real-host/api
```

…or override per command (see below). The URL should point at the directory
that contains `auth.php`, `shop.php`, `music.php`, etc. (the website's `api/`
folder).

---

## Prerequisites

- **JDK 17+** (Temurin/Adoptium recommended).
- **Gradle 8.x** — this source ships without a committed Gradle wrapper. Either
  install Gradle locally (`sdk install gradle`, Homebrew, or your package
  manager) and run `gradle …`, or generate a wrapper once with
  `gradle wrapper --gradle-version 8.7` and then use `./gradlew …`.
- **Android only:** Android Studio (Giraffe+) or the Android SDK with
  `ANDROID_HOME` set, plus API level 34 installed.

---

## Build & run — Desktop (JavaFX)

The JavaFX dependencies are pulled automatically by the `org.openjfx.javafxplugin`
Gradle plugin, so no manual JavaFX SDK install is needed.

```bash
cd BlossomCraftApp

# Run directly
gradle :desktop:run -Dblossomcraft.api.base=https://your-real-host/api

# Or build a distributable
gradle :desktop:installDist
# → desktop/build/install/desktop/bin/desktop   (launch script)
```

The token is persisted with Java `Preferences` (the desktop equivalent of the
website's `localStorage` token). On launch the app tries to restore the session;
otherwise it shows the login / register screen. Themes and the API host can be
changed from the in-app **Settings** screen.

## Native installers (Windows / macOS / Linux)

`installDist` (above) produces a runnable layout but expects a JDK on the target
machine. To ship a **self-contained installer with a bundled runtime**, use
`jpackage` (included with JDK 17+; JDK 21 recommended). A non-JavaFX `Launcher`
class is provided so the packaged app starts cleanly.

1. Build the runtime layout:

   ```bash
   gradle :desktop:installDist
   ```

2. Run `jpackage` on the **target OS** (each installer must be built on the OS it
   targets), pointing at the produced `lib/` folder:

   ```bash
   # macOS / Linux
   jpackage \
     --name BlossomCraft \
     --input desktop/build/install/desktop/lib \
     --main-jar desktop.jar \
     --main-class com.blossomcraft.desktop.Launcher \
     --java-options "-Dblossomcraft.api.base=https://your-real-host/api" \
     --type app-image
   ```

   ```bat
   REM Windows (single line)
   jpackage --name BlossomCraft --input desktop\build\install\desktop\lib --main-jar desktop.jar --main-class com.blossomcraft.desktop.Launcher --java-options "-Dblossomcraft.api.base=https://your-real-host/api" --type app-image
   ```

   - Swap `--type` for a real installer: `msi`/`exe` on Windows (needs the free
     [WiX Toolset](https://wixtoolset.org/)), `dmg`/`pkg` on macOS, `deb`/`rpm`
     on Linux.
   - Check the jar name in `desktop/build/install/desktop/lib/` — if your build
     produced `desktop-<version>.jar`, pass that to `--main-jar`.
   - `app-image` yields a ready-to-run folder; the installer types above produce
     a double-clickable installer with the Java runtime bundled in.

## Automated builds (GitHub Actions)

`.github/workflows/build-apps.yml` builds the ready-to-use **Android `.apk`** and
**Windows `.exe`** in the cloud — no local Java/Android toolchain needed. Push the
project to a GitHub repo (so `settings.gradle` and `.github/` are at the repo
root), then open **Actions → Build BlossomCraft apps → Run workflow**. Download the
results from the run's **Artifacts** (`BlossomCraft-Android-APK`,
`BlossomCraft-Windows`). The API base URL is baked in via the `BC_API_BASE` env at
the top of the workflow (currently `https://bc-shop.duckdns.org/api`) — change that
one line if the site address changes. The Windows job emits a portable app-image
zip (contains `BlossomCraft.exe`) and, when WiX is available, an `.msi` installer.

## Build the shared core on its own

The core has no UI/platform dependencies and compiles standalone:

```bash
gradle :core:build
```

---

## Build & run — Android

The `:android` module is only included when an Android SDK is detected
(`local.properties` with `sdk.dir=…`, or `ANDROID_HOME` / `ANDROID_SDK_ROOT`
set). This keeps `:core` and `:desktop` buildable on machines without the
Android toolchain.

**Android Studio (recommended):**

1. Open the `BlossomCraftApp/` folder in Android Studio.
2. Let it sync Gradle (it creates `local.properties` pointing at your SDK).
3. Set your API host: edit `blossomcraft.api.base` in `gradle.properties`, or
   pass `-Pblossomcraft.api.base=…` to a Gradle build.
4. Run the `android` configuration on an emulator or device.

**Command line:**

```bash
cd BlossomCraftApp
# Ensure ANDROID_HOME is set and API 34 is installed
gradle :android:assembleDebug -Pblossomcraft.api.base=https://your-real-host/api
# → android/build/outputs/apk/debug/android-debug.apk
gradle :android:installDebug    # install on a connected device/emulator
```

The token is stored in `SharedPreferences` (key `auth_token`, mirroring the
website). Navigation uses a bottom bar: Shop, Music, Videos, Chats, Profile.
Theme and API host are configurable from the Profile tab.

### Google sign-in (Android)

The email/password and token flows work out of the box. The native Google flow
needs an OAuth client id:

1. Create an OAuth 2.0 Web client id in Google Cloud (same project the website
   uses for `google_auth.php`).
2. Wire `play-services-auth` in `AuthActivity.startGoogleSignIn()` to obtain an
   access token, then call `bc.auth().googleAuth("login", accessToken)` (the
   integration point is marked in the code).

The desktop app accepts a pasted Google access token for the same exchange.

---

## iOS (experimental, via Gluon)

The shared `:core` is plain Java + Gson and the UI is JavaFX, so iOS is reachable
through **Gluon** (GraalVM native-image + Gluon Substrate / Attach). This must be
built on a **Mac with Xcode** and a Gluon GraalVM — it cannot be produced on
Replit or Windows. See **iOS-GLUON.md** for a ready `ios/build.gradle`, the
`settings.gradle` include, and the exact commands. The business logic (auth,
shop, music, videos, messaging, admin) runs as-is; the JavaFX views may need
touch-friendly spacing tweaks, and networking/token storage may need Gluon
Attach equivalents (documented there).

---

## How it maps to the website

| Website                         | Native app                                   |
|---------------------------------|----------------------------------------------|
| `localStorage.auth_token`       | Desktop `Preferences` / Android `SharedPreferences` |
| `Bearer` token on every request | `ApiClient` adds the header automatically    |
| 401 clears token + logs out     | `AuthExpiredException` clears the token       |
| `/api/*.php` endpoints          | one service class per concern in `:core`     |
| dark/light/mirror/gray-mirror   | `Theme` enum + JavaFX CSS / Android theme    |

---

## Notes

- Networking uses the JDK's built-in `java.net.http.HttpClient`; JSON uses Gson.
- On Android, core-library desugaring is enabled so the shared core's modern
  Java APIs work down to `minSdk 24`.
- No secrets are bundled. Point the app at your own BlossomCraft host.
```
