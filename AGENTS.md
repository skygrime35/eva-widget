# EVA Widget - AGENTS.md

Native Android app whose only purpose is a home-screen widget showing quota rings, level and upcoming bookings fetched from a read-only JSON endpoint behind a secret link. The data contract (see README) is owned by a private repo named `eva_calendar` (`packages/core/src/account.ts`, `WidgetData`); change it there first.

## Structure (`app/src/main/java/fr/evacal/widget/`)
- `Model.kt` - data classes and tolerant JSON `Parser`.
- `Format.kt` - `Link` (normalize, origin, data URL) and `FrDate` (French dates from `startLocal` string parts).
- `Plan.kt` - pure `plan(widthDp, heightDp, data)`: decides layout, lines and rectangles. Renderer decides nothing about content.
- `Renderer.kt` - draws a `Plan` into one `Bitmap` with `Canvas`.
- `Store.kt` - SharedPreferences (link, cached JSON) and the HTTP fetch.
- `EvaWidgetProvider.kt` - `AppWidgetProvider`, sizes, `RemoteViews`, click intents.
- `MainActivity.kt` - launcher and widget configuration screen.
- Tests in `app/src/test`: JVM tests for parser, links, dates, plan (including monotonicity); a Robolectric native-graphics test writes previews to `app/build/outputs/widget-previews/`.

## Rules
- Kotlin, single module `app`, applicationId `fr.evacal.widget`, minSdk 26, compileSdk/targetSdk 35.
- No runtime dependencies: platform APIs only (no AndroidX, Compose, Glance, WorkManager, HTTP library). Test-only: JUnit 4, `org.json:json`, Robolectric.
- The widget is one bitmap at the exact pixel size, in an `ImageView`; keep width*height*4 under about 6 MB.
- Text uses density scaling only (not font scale); minimum 11 dp. UI strings are French; code, comments, docs, commits are English.
- `startLocal` is venue wall-clock time: never convert through a time zone.
- Manifest: `INTERNET` only, `allowBackup=false`, no cleartext.

## Commands
```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew --no-daemon testDebugUnitTest assembleDebug
EVA_WIDGET_SIGNING=/path/to/signing.properties ./gradlew --no-daemon assembleRelease
```
Always use `--no-daemon` and a `timeout`; look at the preview PNGs after any change to `Plan.kt` or `Renderer.kt`.

## Guardrails
- No secrets in the repo: no real host, token, IP, keystore or password (use `https://example.org/widget/<token>`). Signing config comes from `EVA_WIDGET_SIGNING`, never committed.
- The link and token are never logged (no `Log` call may contain them) and the configuration window is `FLAG_SECURE`.
- No dependency creep.
- No em or en dashes (U+2014, U+2013) anywhere: short hyphen only.
