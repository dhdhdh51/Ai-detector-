<div align="center">

<img src="docs/fitbudget-icon.png" width="120" alt="FitBudget icon" />

# FitBudget

**₹100 a Day. Better Every Day.**

An offline-first Android app for fat loss on an Indian budget — meal planning, rupee budgeting,
water, steps, workouts, weight tracking and reliable local reminders.

</div>

---

## Project overview

FitBudget is a single-module Android application written in Kotlin with Jetpack Compose. It plans an
affordable daily Indian diet inside a rupee budget (₹100/day by default), tracks what you actually
eat and spend, and keeps water, steps, workouts and weight in one place.

There is **no account, no server and no analytics**. Every core feature works with the device
offline; all data lives in a local Room database and a DataStore preferences file.

Every calculated number (BMI, calorie target, deficit, weekly trend) is labelled as an **estimate**.
The app never diagnoses anything and never promises an outcome.

## Features

**Onboarding & profile**
- First-launch onboarding in four steps, pre-filled with the example profile (DOB 22 Nov 2006,
  172 cm, 85 kg, target 75 kg, ₹100/day) — every value editable then and later.
- "Your plan is ready" summary: current weight, target, daily budget, water goal, step goal, plus
  calorie/protein/BMI estimates.
- Editable profile with validation, activity level, food preference and wake/sleep times.

**Home dashboard**
- Time-aware greeting, progress ring (start → target), remaining weight, BMI + category.
- Cards for current/target weight, BMI, today's calories, budget (budget / spent / remaining),
  water, steps, meals ticked off and workout status.
- Daily checklist (8 rows) with a completion percentage, four streak chips, a 7-day step strip,
  and inline quick-add water buttons.
- Actionable banners when notification permission or exact alarms are missing.

**Diet plan**
- Auto-generated daily plan for breakfast / lunch / evening snack / dinner from the bundled food
  database, respecting diet preference, exclusions, calorie target and the rupee budget.
- Per-item: food, serving, quantity, calories, protein, cost, meal time and a completion checkbox.
- Mark item or whole meal complete, change quantity, replace food, add food, delete, add a custom
  meal, change meal times, regenerate or clear the day. Browse previous/next days.

**₹ budget system**
- Today's budget / spent / remaining / over-budget, with spending counted when a meal is ticked off,
  plus manual extra expenses.
- Per-day goal snapshots, so changing your budget never rewrites history.
- Monthly view: total budget, total spent, average per day, days under/over budget, adherence %,
  and a daily spending bar chart with the budget line.

**Trackers**
- Water: customisable target (default ≈35 ml/kg, clamped to 2–4 L), +250/500/750/1000 ml quick
  buttons, undo, per-entry list, 7-day strip, streak.
- Weight: one entry per day with optional note, editable past days, 7/30/90/all-time charts,
  starting/current/target, total change, average weekly change.
- Steps: hardware step-counter support with permission handling, manual entry fallback when no
  sensor exists, custom goal (default 8,000), 7-day strip.
- Workouts: 8 bundled beginner routines across Full Body / Home / Walking / Strength / Mobility,
  with a live session runner (start, pause, resume, complete, skip, per-exercise timer, finish
  early) and saved history.

**Reminders**
- Nine configurable reminders: breakfast, lunch, snack, dinner, water, workout, walking, weight,
  sleep — each with enable/disable, time, repeat days, sound and vibration toggles.
- Water repeats on an interval (default every 2 h) between your wake and sleep times.
- Exact alarms with a graceful inexact fallback; rebuilt after reboot, app update and clock changes.
- Notifications deep-link to the relevant screen and carry inline actions ("Mark eaten", "+250 ml").

**Progress & reports**
- Progress tab: weight trend with target line, spending bars, activity bars, checklist bars,
  streaks, range switch (7 / 30 / 90 / all time).
- Monthly report: average weight, weight change, average calories, average food cost, budget
  adherence, workout count, average steps, water adherence, checklist average, streaks and three
  charts — shareable as a text file or plain text.

**Settings, data and privacy**
- Goals (budget, target weight, calories, water, steps), food preferences and exclusions, reminder
  and notification settings, theme (light / dark / system), Material You toggle, units.
- Export data as CSV, share the monthly report, create a JSON backup, restore from a backup, and
  reset all data behind a confirmation dialog.
- A Privacy screen that explains exactly what is stored and which permissions are used.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.12.01), Material 3, Compose Navigation |
| Architecture | MVVM + repositories, manual DI container (`AppContainer`) |
| Database | Room 2.6.1 (KSP), explicit migrations, exported schema |
| Preferences | DataStore Preferences 1.1.1 |
| Background | WorkManager 2.10.0 + AlarmManager for exact reminder times |
| Charts | Hand-drawn with Compose `Canvas` (no third-party chart library) |
| Build | Gradle 8.11.1 (Kotlin DSL) + AGP 8.7.3, version catalog, KSP |
| SDK | compileSdk / targetSdk 35, minSdk 26, Java 17 toolchain, core library desugaring |
| Tests | JUnit4 unit tests, Room instrumented tests, Compose UI test |

No dependency is added that would break offline use, and no third-party or copyrighted assets are
bundled — the launcher icon, splash logo and notification icon are drawn in-project.

## Folder structure

```
.
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/fitbudget/app/
│       │   │   ├── FitBudgetApp.kt          # Application: channels, startup work
│       │   │   ├── MainActivity.kt          # splash, theme, deep links, permissions
│       │   │   ├── data/
│       │   │   │   ├── database/            # FitBudgetDatabase, Converters
│       │   │   │   │   ├── dao/             # 10 DAOs + aggregate projections
│       │   │   │   │   └── entity/          # 10 entities
│       │   │   │   ├── repository/          # 12 repositories (incl. Stats, Backup)
│       │   │   │   ├── seed/                # Indian food database, workout library
│       │   │   │   ├── sensors/             # StepSensorManager
│       │   │   │   └── settings/            # DataStore settings
│       │   │   ├── di/                      # AppContainer
│       │   │   ├── domain/                  # calculations, validation, plan generator
│       │   │   │   └── model/               # enums / value types
│       │   │   ├── notifications/           # helper, scheduler, receivers
│       │   │   ├── ui/
│       │   │   │   ├── components/          # cards, rings, charts, inputs, dialogs
│       │   │   │   ├── navigation/          # Routes, NavHost, bottom bar
│       │   │   │   ├── screens/             # one package per screen (+ ViewModel)
│       │   │   │   └── theme/               # Color, Type, Theme
│       │   │   ├── util/                    # date/time, formatters, sharing
│       │   │   └── workers/                 # WorkManager workers + scheduler
│       │   └── res/                         # drawable, mipmap, values, values-night, xml
│       ├── test/                            # JVM unit tests
│       └── androidTest/                     # Room + Compose instrumented tests
├── gradle/libs.versions.toml                # version catalog
├── gradle/wrapper/                          # committed Gradle wrapper
├── tools/generate_icons.py                  # regenerates the PNG launcher icons
├── docs/fitbudget-icon.png
├── .github/workflows/android-build.yml
├── .github/workflows/android-release.yml
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── README.md
```

## Local build instructions

Requirements: JDK 17 and the Android SDK (platform 35, build-tools 35.0.0). Android Studio is
**not** required.

```bash
# 1. Point the build at your SDK (skip if ANDROID_HOME / ANDROID_SDK_ROOT is already exported)
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# 2. Build the debug APK  ->  app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# 3. Run the checks
./gradlew lintDebug testDebugUnitTest

# 4. Optional: minified release APK -> app/build/outputs/apk/release/app-release.apk
./gradlew assembleRelease

# 5. Install on a connected device
./gradlew installDebug

# Instrumented tests need a running emulator or device
./gradlew connectedDebugAndroidTest
```

The Gradle wrapper is committed, so `./gradlew` downloads the correct Gradle version on first run.

## GitHub Actions

`.github/workflows/android-build.yml` runs on every push to `main`/`master`/`feat/**`/`fix/**`, on
pull requests, and manually from the Actions tab. It:

1. checks out the repository,
2. sets up JDK 17 (Temurin),
3. sets up the Android SDK (platform 35, build-tools 35.0.0, platform-tools),
4. sets up Gradle with dependency caching,
5. runs `lintDebug`,
6. runs `testDebugUnitTest`,
7. compiles the instrumented tests (`assembleDebugAndroidTest`),
8. builds the debug APK (`assembleDebug`),
9. uploads the APK plus the lint and unit-test reports.

`.github/workflows/android-release.yml` is the optional release pipeline: push a `v*` tag (or run it
manually) to build a minified release APK, upload it, and attach it to a GitHub release. It signs
with your keystore when the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`
secrets are present, and otherwise falls back to debug signing so the workflow never fails for lack
of configuration.

### APK artifact location

| Workflow | Artifact name | File |
|---|---|---|
| Android Build | **`FitBudget-debug.apk`** | `artifacts/FitBudget-debug.apk` (from `app/build/outputs/apk/debug/app-debug.apk`) |
| Android Release | `FitBudget-release.apk` | `artifacts/FitBudget-release.apk` |

Download it from the **Actions → workflow run → Artifacts** section.

## Permissions

| Permission | Why | Required? |
|---|---|---|
| `POST_NOTIFICATIONS` | show meal / water / workout / weight / sleep reminders | Optional — the app works, reminders are simply silent |
| `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` | deliver reminders at the exact chosen minute | Optional — falls back to inexact alarms |
| `RECEIVE_BOOT_COMPLETED` | rebuild the reminder schedule after a reboot | Needed for reminders to survive restarts |
| `ACTIVITY_RECOGNITION` | read the device's step counter | Optional — manual step entry is offered instead |
| `VIBRATE` | reminder vibration | Optional |
| `WAKE_LOCK` | used by WorkManager while the maintenance job runs | Implicit |

`android.permission.INTERNET` is **not** declared, so the app cannot make network requests at all.

Two further permissions appear in the merged manifest because the AndroidX WorkManager library
declares them — `ACCESS_NETWORK_STATE` (so WorkManager can evaluate network constraints, which this
app never sets) and `FOREGROUND_SERVICE` (for WorkManager's expedited-work path). Neither grants
network access and FitBudget starts no foreground service of its own.

## Privacy architecture

- No `INTERNET` permission, no networking code, no analytics or crash-reporting SDK, no ads.
- All health, fitness and spending data is written to a private Room database
  (`/data/data/com.fitbudget.app/databases/fitbudget.db`) and a DataStore preferences file.
- Exports and backups are written to the app's private cache (`cache/exports/`) and only leave the
  device if the user actively picks a target in the Android share sheet (served through a
  `FileProvider`, so no file path is ever exposed).
- OS backup rules are restricted to the database, shared preferences and the DataStore file.
- "Reset all data" clears every table, the preferences file and any generated export files.
- Uninstalling the app removes everything.

## How reminders work

1. Each of the nine `ReminderType`s has exactly one row in the `reminders` table (enabled, hour,
   minute, repeat-day bitmask, interval, sound, vibration).
2. `ReminderScheduler` computes the **next** trigger time for a reminder and arms a single
   `AlarmManager` alarm (`setExactAndAllowWhileIdle`, or `setAndAllowWhileIdle` when the app is not
   allowed to schedule exact alarms). Water reminders generate interval slots between the profile's
   wake and sleep times.
3. When the alarm fires, `ReminderReceiver` posts the notification through `NotificationHelper` and
   immediately re-arms the next occurrence. Nothing relies on inexact `setRepeating`.
4. `BootReceiver` listens for `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`,
   `TIME_SET` and `TIMEZONE_CHANGED`, and enqueues `ReminderSyncWorker` to rebuild every alarm from
   the database.
5. `DailyMaintenanceWorker` (WorkManager, every 6 h) creates the new day's goal snapshot, generates
   the day's meal plan if needed, and re-arms all reminders, so the schedule can never drift.
6. Sound/vibration toggles are honoured by routing each notification to the appropriate channel
   variant (`…`, `…_vibrate`, `…_silent`), because Android does not allow a channel's sound to be
   changed after creation. Variants are created lazily.
7. Tapping a notification opens `MainActivity` with an `EXTRA_ROUTE`, which the nav host consumes to
   deep-link into Diet, Water, Workout, Steps, Weight or Home.

## How the local database works

- **Room** (`FitBudgetDatabase`, version 1) with 10 entities: `profile`, `foods`, `meal_entries`,
  `weight_logs`, `water_logs`, `workout_sessions`, `step_logs`, `reminders`, `expenses`, `day_meta`.
- Every dated row is keyed by `epochDay` (`LocalDate.toEpochDay()`), so "today" always means the
  user's local calendar day and the daily reset is inherent — a new day simply has no rows yet.
- `day_meta` snapshots the budget, water target, step goal and calorie target that applied on a
  given day, so changing a goal later never rewrites history.
- Aggregate SQL projections (`DayMealAggregate`, `MealTypeAggregate`, …) feed `StatsRepository`,
  which builds the `DaySummary` objects used by the dashboard, checklist, streaks and reports. No
  derived value is ever stored twice or fabricated.
- Enums are persisted as stable `name` strings via `Converters`; unknown values decode to a safe
  default instead of crashing.
- Migrations are explicit (`FitBudgetDatabase.MIGRATIONS`) and `fallbackToDestructiveMigration` is
  deliberately **not** used, so an app update can never wipe user data. The schema is exported to
  `app/schemas/` for future migration diffs.
- First launch seeds the bundled Indian food database and the nine default reminders; re-seeding is
  idempotent thanks to a unique index on the food name.

## Known limitations

- Step counting uses the hardware `TYPE_STEP_COUNTER`, which the device accumulates in the
  background; FitBudget reads the delta whenever the app is opened rather than running a permanent
  foreground service. Steps are credited to the day they are observed. Devices without the sensor
  use manual entry.
- Some OEM battery optimisers can still delay alarms; the app detects blocked exact alarms and tells
  the user how to allow them.
- The release workflow signs with the debug key unless you configure keystore secrets, so those APKs
  are for testing rather than Play Store upload.
- Nutrition and cost figures in the bundled database are approximate reference values; edit any food
  to match what you actually eat and pay.

---

**Nutrition estimates are approximate. For medical conditions or special dietary needs, consult a
qualified healthcare professional.**
