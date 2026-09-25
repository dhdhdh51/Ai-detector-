<div align="center">

<img src="docs/fitbudget-icon.png" width="120" alt="FitBudget icon" />

# FitBudget

**₹100 a Day. Better Every Day.**

An offline-first fitness, diet and weight-loss tracker built around an affordable Indian diet —
native on **Android** and **iOS**.

</div>

---

## Project overview

FitBudget plans an affordable daily Indian diet inside a rupee budget (₹100/day by default), tracks
what you actually eat and spend, and keeps water, steps, workouts and weight in one place.

Two native apps share one design and one set of rules:

| | Android | iOS |
|---|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 | Swift, SwiftUI, Swift Charts |
| Storage | Room (SQLite) | SwiftData |
| Reminders | AlarmManager + WorkManager | UNUserNotificationCenter |
| Steps | `TYPE_STEP_COUNTER` sensor | CoreMotion `CMPedometer` |
| Min OS | Android 8.0 (API 26) | iOS 17 |
| Source | `/app` | `/ios` |

Both store every dated row against the **same epoch-day key** (days since 1970-01-01 in the user's
own calendar) and use the **same backup format**, so a JSON backup moves between the two apps.

There is **no account, no server and no analytics**. Every feature works with the device offline; all
data lives in a local database. Every calculated number (BMI, calorie target, deficit, weekly trend)
is labelled as an **estimate** — the app never diagnoses anything and never promises an outcome.

## Features

Identical on both platforms unless noted.

**Onboarding & profile** — four-step onboarding pre-filled with the example profile (DOB 22 Nov 2006,
172 cm, 85 kg, target 75 kg, ₹100/day), every value editable then and later; "Your plan is ready"
summary with current weight, target, budget, water goal, step goal and the calorie/protein/BMI
estimates; fully editable profile with validation.

**Home dashboard** — greeting, progress ring (start → target), cards for weight, target, BMI,
calories, budget, water, steps, meals and workout; 8-row daily checklist with a completion
percentage; four streak chips; 7-day step strip; quick-add water; permission banners.

**Diet plan** — auto-generated plan for breakfast / lunch / evening snack / dinner from the bundled
52-food Indian database, respecting diet preference, exclusions, calorie target and the rupee budget.
Per item: food, serving, quantity, calories, protein, cost, meal time, completion checkbox. Mark item
or whole meal done, change quantity, replace food, add food, delete, add a custom meal, change meal
times, regenerate or clear the day, browse previous days.

**₹ budget system** — today's budget / spent / remaining / over-budget, spending counted when a meal
is ticked off plus manual extra expenses; per-day goal snapshots so changing your budget never
rewrites history; monthly totals, average per day, days under/over budget, adherence % and a daily
spending chart with the budget line.

**Trackers** — water (custom target, +250/500/750/1000 ml, undo, 7-day strip, streak); weight (one
entry per day with note, editable past days, 7/30/90/all-time charts, total and weekly change);
steps (sensor with permission handling, manual fallback, custom goal); workouts (8 bundled beginner
routines across Full Body / Home / Walking / Strength / Mobility with a live session runner —
start, pause, resume, complete, skip, per-exercise timer, finish early — and saved history).

**Reminders** — nine configurable reminders (breakfast, lunch, snack, dinner, water, workout,
walking, weight, sleep) with enable/disable, time, repeat days and sound; water repeats on an
interval between your wake and sleep times; notifications deep-link to the right screen and carry
inline actions ("Mark eaten", "+250 ml"). Android additionally exposes a per-reminder vibration
toggle; on iOS vibration follows the system Sounds & Haptics setting.

**Progress & reports** — weight trend with target line, spending bars, activity bars, checklist bars,
streaks, range switch (7 / 30 / 90 / all time); monthly report with average weight, weight change,
average calories, average food cost, budget adherence, workout count, average steps, water
adherence, checklist average and three charts, shareable as a text file or plain text.

**Settings, data & privacy** — goals, food preferences and exclusions, reminder and notification
settings, theme (light / dark / system), units; export CSV, share the monthly report, create a JSON
backup, restore from a backup, and reset all data behind a confirmation; a Privacy screen that
explains exactly what is stored and which permissions are used.

## Tech stack

**Shared approach** — both apps keep all rules (BMI, age, calorie/deficit estimates, validation,
streaks, daily checklist, budget and water maths, the meal-plan generator, the food and workout seed
data) in a dedicated, dependency-free layer that is unit-tested in CI.

**Android** — Kotlin 2.0.21 · Jetpack Compose (BOM 2024.12.01) + Material 3 · Compose Navigation ·
MVVM with repositories and a manual `AppContainer` · Room 2.6.1 via KSP with explicit migrations and
an exported schema · DataStore · WorkManager + AlarmManager · charts hand-drawn with Compose
`Canvas` · Gradle 8.11.1 (Kotlin DSL) + AGP 8.7.3 with a version catalog · compileSdk/targetSdk 35,
minSdk 26, Java 17.

**iOS** — Swift 5.9+ · SwiftUI + Observation (`@Observable`) · SwiftData · Swift Charts ·
UserNotifications · CoreMotion · `FitBudgetCore` Swift package for the shared logic · XcodeGen so the
`.xcodeproj` is generated rather than committed · iOS 17 deployment target.

No dependency is added that would break offline use, and no third-party or copyrighted assets are
bundled — the launcher icon, app icon, splash logo and notification icon are all drawn in-project.

## Folder structure

```
.
├── app/                                  # Android application (Gradle module)
│   ├── build.gradle.kts                  # incl. release signing resolution
│   ├── lint.xml
│   ├── proguard-rules.pro
│   ├── schemas/                          # exported Room schema for migration diffs
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/fitbudget/app/
│       │   │   ├── FitBudgetApp.kt        # Application: channels, startup work
│       │   │   ├── MainActivity.kt        # splash, theme, deep links, permissions
│       │   │   ├── data/                  # database (10 entities, 10 DAOs), repositories,
│       │   │   │                          # seed data, sensors, DataStore settings
│       │   │   ├── di/AppContainer.kt
│       │   │   ├── domain/                # calculations, validation, plan generator
│       │   │   ├── notifications/         # helper, AlarmManager scheduler, receivers
│       │   │   ├── ui/                    # theme, components, navigation, screens + ViewModels
│       │   │   ├── util/
│       │   │   └── workers/               # WorkManager workers + scheduler
│       │   └── res/                       # drawable, mipmap, values, values-night, xml
│       ├── test/                          # JVM unit tests (107)
│       └── androidTest/                   # Room + Compose instrumented tests
│
├── ios/                                  # iOS application
│   ├── Package.swift                     # FitBudgetCore package (Linux-testable)
│   ├── project.yml                       # XcodeGen spec -> FitBudget.xcodeproj
│   ├── Sources/FitBudgetCore/            # shared logic: Domain/, Models/, Seed/, Util/
│   ├── Tests/FitBudgetCoreTests/         # 126 XCTest cases
│   └── FitBudget/
│       ├── App/                          # @main app, Router, RootView + tab bar
│       ├── Persistence/                  # SwiftData models, AppStore (+Stats, +Backup), settings
│       ├── Notifications/                # NotificationService + delegate
│       ├── Sensors/                      # PedometerService
│       ├── DesignSystem/                 # Theme, Components, ChartViews
│       ├── Features/                     # one folder per screen
│       ├── Resources/Assets.xcassets/    # AppIcon, LaunchLogo, colours
│       └── Info.plist
│
├── .github/workflows/
│   ├── android-build.yml                 # lint + tests + debug APK
│   ├── android-release.yml               # signed release APK + AAB
│   ├── ios-build.yml                     # core tests (Linux) + unsigned app build (macOS)
│   └── ios-release.yml                   # signed IPA
├── tools/
│   ├── generate_icons.py                 # regenerates every raster icon from the brand mark
│   └── setup-signing.sh                  # creates/stores signing material for both platforms
├── docs/fitbudget-icon.png
├── gradle/libs.versions.toml
├── gradle/wrapper/                       # committed Gradle wrapper
├── settings.gradle.kts · build.gradle.kts · gradle.properties
└── README.md
```

## Local build instructions

### Android

Requirements: JDK 17 and the Android SDK (platform 35, build-tools 35.0.0). Android Studio optional.

```bash
echo "sdk.dir=/path/to/Android/sdk" > local.properties   # or export ANDROID_HOME

./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug testDebugUnitTest
./gradlew assembleRelease          # minified release APK
./gradlew bundleRelease            # Play AAB
./gradlew installDebug             # install on a connected device
./gradlew connectedDebugAndroidTest   # instrumented tests (needs a device/emulator)
```

The Gradle wrapper is committed, so `./gradlew` fetches the right Gradle version on first run.

### iOS

Requirements: macOS with Xcode 15.4+ (iOS 17 SDK) and [XcodeGen](https://github.com/yonaskolb/XcodeGen).

```bash
brew install xcodegen
cd ios
xcodegen generate                  # creates FitBudget.xcodeproj (git-ignored)
open FitBudget.xcodeproj           # then run on a simulator or device

# Command line equivalents
swift test                         # the shared core's 126 tests (also runs on Linux)
xcodebuild build -project FitBudget.xcodeproj -scheme FitBudget \
  -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO
```

The shared core needs no Apple platform at all:

```bash
cd ios && swift build && swift test    # works on Linux too
```

## Release signing

Both release pipelines sign automatically once the signing material is stored as repository secrets,
and **fall back to a non-distributable build when it is missing** so CI is never red just because
signing has not been configured yet.

Run the helper once per platform, from a machine with the [GitHub CLI](https://cli.github.com)
authenticated (`gh auth login`):

```bash
./tools/setup-signing.sh android    # creates a keystore and uploads the secrets
./tools/setup-signing.sh ios        # uploads your Apple certificate/profile or API key
./tools/setup-signing.sh status     # shows which secrets are configured
```

| Platform | Secrets | Fallback with no secrets |
|---|---|---|
| Android | `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | release build signed with the debug key (testing only) |
| iOS — manual | `IOS_TEAM_ID`, `IOS_CERTIFICATE_P12_BASE64`, `IOS_CERTIFICATE_PASSWORD`, `IOS_PROVISIONING_PROFILE_BASE64`, `IOS_KEYCHAIN_PASSWORD` | unsigned `.xcarchive` |
| iOS — automatic | `IOS_TEAM_ID`, `APPSTORE_API_PRIVATE_KEY`, `APPSTORE_API_KEY_ID`, `APPSTORE_API_ISSUER_ID` (Xcode manages certificates via `-allowProvisioningUpdates`) | unsigned `.xcarchive` |

Android release builds also read a git-ignored `keystore.properties` in the repo root, so
`./gradlew assembleRelease` signs correctly on your own machine. `setup-signing.sh` writes it for
you. Android v1–v4 signing schemes are all enabled.

> **Security.** No private key is ever committed. `.gitignore` blocks `*.jks`, `*.keystore`, `*.p12`,
> `*.mobileprovision`, `AuthKey_*.p8` and `keystore.properties`; the generated keystore lives in the
> git-ignored `signing/` directory. **Back up the Android keystore and its password offline** — losing
> it means you can never ship an update to the same Play listing.

## GitHub Actions

| Workflow | Trigger | What it does | Artifacts |
|---|---|---|---|
| **Android Build** | push / PR / manual | JDK 17 → Android SDK → Gradle cache → `lintDebug` → `testDebugUnitTest` → compile instrumented tests → `assembleDebug` | **`FitBudget-debug.apk`**, lint + unit-test reports |
| **iOS Build** | push / PR / manual | *Linux job:* `swift build` + `swift test` on the shared core. *macOS job:* XcodeGen → `swift test` → unsigned `xcodebuild build` against the iOS SDK | `FitBudget-ios-unsigned.zip` |
| **Android Release** | tag `v*` / manual | unit tests → `assembleRelease` + `bundleRelease` → `apksigner verify --print-certs` | **`FitBudget-release.apk`**, `FitBudget-release.aab` |
| **iOS Release** | tag `v*` / manual | XcodeGen → `swift test` → keychain/profile (or API key) import → `xcodebuild archive` → `-exportArchive` | **`FitBudget-release.ipa`** (or an unsigned archive) |

Artifacts are on **Actions → workflow run → Artifacts**. Tagging `v1.0.0` also attaches the Android
APK/AAB and the iOS IPA to a GitHub release.

## Permissions

### Android

| Permission | Why | Required? |
|---|---|---|
| `POST_NOTIFICATIONS` | meal / water / workout / weight / sleep reminders | Optional — app works, reminders stay silent |
| `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` | deliver reminders at the exact chosen minute | Optional — falls back to inexact alarms |
| `RECEIVE_BOOT_COMPLETED` | rebuild the reminder schedule after a reboot | Needed for reminders to survive restarts |
| `ACTIVITY_RECOGNITION` | read the device step counter | Optional — manual entry offered instead |
| `VIBRATE` | reminder vibration | Optional |
| `WAKE_LOCK` | used by WorkManager while the maintenance job runs | Implicit |

`android.permission.INTERNET` is **not** declared, so the Android app cannot make network requests at
all. `ACCESS_NETWORK_STATE` and `FOREGROUND_SERVICE` appear in the merged manifest only because the
AndroidX WorkManager library declares them; neither grants network access and the app starts no
foreground service of its own.

### iOS

| Key | Why | Required? |
|---|---|---|
| Notifications (`UNAuthorizationOptions`) | the nine reminders | Optional — requested after onboarding |
| `NSMotionUsageDescription` | read the step count iOS already keeps | Optional — manual entry offered instead |

Nothing else is requested: no location, contacts, photos, HealthKit or network entitlement.
`ITSAppUsesNonExemptEncryption` is `false`, so App Store submissions skip the export questionnaire.

## Privacy architecture

- No network code on either platform, no analytics or crash-reporting SDK, no ads, no account.
- Android writes to a private Room database (`/data/data/com.fitbudget.app/databases/fitbudget.db`)
  plus a DataStore file; iOS writes to a private SwiftData store plus `UserDefaults`.
- Exports and backups are written to app-private storage and only leave the device when the user
  picks a target in the system share sheet (Android serves them through a `FileProvider`).
- Android OS backup rules are restricted to the database, shared preferences and the DataStore file.
- "Reset all data" clears every table, the preferences and any generated export files.
- Uninstalling removes everything.

## How reminders work

**Android.** Each of the nine reminder types has one row in the `reminders` table.
`ReminderScheduler` computes the next trigger and arms a single `AlarmManager` alarm
(`setExactAndAllowWhileIdle`, or `setAndAllowWhileIdle` when exact alarms are not permitted); water
reminders generate interval slots between the profile's wake and sleep times. When an alarm fires,
`ReminderReceiver` posts the notification and immediately re-arms the next occurrence — nothing
relies on inexact `setRepeating`. `BootReceiver` listens for `BOOT_COMPLETED`,
`LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET` and `TIMEZONE_CHANGED` and enqueues
`ReminderSyncWorker` to rebuild every alarm from the database, and `DailyMaintenanceWorker` (every
6 h) re-arms everything and prepares the new day. Sound/vibration toggles are honoured by routing
each notification to the matching channel variant, because Android cannot change a channel's sound
after creation.

**iOS.** Each reminder becomes one or more **repeating** `UNCalendarNotificationTrigger`s, which iOS
keeps delivering across restarts and reboots without any background work. When every weekday is
selected a single daily trigger is used; otherwise one trigger per selected weekday. Water adds one
trigger per interval slot inside the wake–sleep window. The whole schedule is rebuilt from the
database on every change and capped below the 64 pending-request limit. Notification categories
provide the "Mark eaten" and "+250 ml" actions, and `userInfo` carries the route used to deep-link.

## How the local database works

Both platforms use the same model, so the two apps agree on what a "day" means:

- Every dated row is keyed by **epoch day** (`LocalDate.toEpochDay()` on Android, the equivalent
  `DayKey` on iOS). The daily reset is therefore inherent — a new day simply has no rows yet.
- A **day-meta** row snapshots the budget, water target, step goal and calorie target that applied on
  a given day, so changing a goal later never rewrites history.
- Summaries (`DaySummary`) are derived on demand from the logged rows and feed the dashboard,
  checklist, streaks and reports. No derived value is stored twice or fabricated.
- Enums persist as stable raw strings; unknown values decode to a safe default instead of crashing.
- **Android:** Room v1 with 10 entities, explicit `MIGRATIONS` and *no* `fallbackToDestructiveMigration`,
  so an app update can never wipe user data; the schema is exported to `app/schemas/`.
- **iOS:** SwiftData with the same 10 records; additive property changes migrate automatically, and
  the container only falls back to an in-memory store if the file is unreadable (never silently on a
  normal update).
- First launch seeds the bundled Indian food database and the nine default reminders; re-seeding is
  idempotent because food identity is the lower-cased name.

## Testing

| Suite | Count | Runs where |
|---|---|---|
| Android unit tests (BMI, age, progress, budget, completion, streaks, water, validation, plan generation, date handling) | 107 | `./gradlew testDebugUnitTest`, every CI build |
| Android instrumented tests (Room round-trips, aggregates, Compose UI) | 11 | `./gradlew connectedDebugAndroidTest` (device needed); compiled in CI |
| iOS shared-core tests (same rules as Android, plus a cross-platform parity test) | 126 | `swift test` on Linux **and** macOS, every CI build |

The parity test pins the shared example profile end to end (85 kg / 172 cm / 18 y → BMI 28.73,
TDEE ≈ 2530 kcal, target 2020 kcal, 120 g protein, 3000 ml water) so the two apps cannot drift apart.

## Known limitations

- **Android steps** use the hardware `TYPE_STEP_COUNTER`, read when the app is opened rather than via
  a permanent foreground service; steps are credited to the day they are observed. **iOS steps** come
  from `CMPedometer`, which keeps its own history, so today's total is always accurate. Devices
  without a sensor use manual entry.
- Aggressive OEM battery optimisers can still delay Android alarms; the app detects blocked exact
  alarms and explains how to allow them.
- Release builds are debug-signed (Android) or unsigned (iOS) until you run `setup-signing.sh`, so
  those artifacts are for testing rather than store submission.
- Instrumented Android tests compile in CI but are not executed (no emulator in the workflow); the
  iOS app is built and type-checked in CI but not launched in a simulator.
- Nutrition and cost figures in the bundled database are approximate reference values — edit any food
  to match what you actually eat and pay.

---

**Nutrition estimates are approximate. For medical conditions or special dietary needs, consult a
qualified healthcare professional.**
