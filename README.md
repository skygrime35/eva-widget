# EVA Widget

A small native Android app whose only purpose is a home-screen widget: it shows quota rings, level and upcoming bookings from a JSON endpoint, and shows more as you make the widget bigger.

> **Notes**
> - This app is entirely vibe coded with an AI assistant. It is provided as is, with no support and no warranty. Anyone is free to use or fork it.
> - It is only useful together with a server that implements the JSON data contract described below. The reference server is a private project and is not available.

## Install

Sideload the APK from the GitHub release page (allow installs from your browser or file manager when Android asks).

## Setup

1. Get the secret link from the server, of the form `https://example.org/widget/<token>`.
2. Open **EVA Widget**, paste the link, tap **Enregistrer**. The link is checked by fetching it.
3. Long-press the home screen, choose **Widgets > EVA Widget**, place it, then resize it.

Tapping the widget opens the server origin (the link without the token path) in the browser. The top-right corner (40 dp) refetches. The widget also refreshes about every 30 minutes; a resize or a failed fetch redraws from the last good response.

## Data contract

The app does `GET <link>/data` (a trailing `/` or `/data` in the pasted link is ignored). `404` means the link is wrong or revoked; `200` returns:

```json
{
  "generatedAt": "2026-11-27T12:00:00.000Z",
  "flag": null,
  "quota": { "peak": {"total": 6, "left": 4}, "offPeak": {"total": 2, "left": 0}, "daysLeft": 12, "endsAt": "2026-12-09" },
  "level": { "level": 37, "progressionPct": 62 },
  "bookings": [ { "startLocal": "2026-12-01T19:20", "game": "After-H Battle Arena", "players": ["Roro", "Kaz"] } ],
  "renewsAt": "2026-12-09"
}
```

- `flag`: `null`, `"reconnect"` (account must be reconnected) or `"stale"` (old data).
- `quota`, `level`, `renewsAt` may be `null`; `progressionPct` may be `null`. `left` = sessions left, `total` = allowance.
- `bookings`: soonest first, at most 8. `startLocal` is venue wall-clock time (`YYYY-MM-DDTHH:mm`); it is formatted from its string parts, never through a time zone.

## How sizing works

On every update the widget reads its size in dp from the launcher and draws one bitmap at that exact size. A pure function (`plan` in `Plan.kt`) decides what fits, in priority order: quota rings, warning marker, level, next booking, following bookings (as many as fit), then players, quota end, renewal. Nothing is drawn cut in half. Layouts: rings only (about 2x1 cells), rings left with text right (wide), rings on top with stacked lines (tall and narrow), everything (large). On Android 12+ a portrait and a landscape bitmap are provided.

## Build and release

Requires JDK 17 and the Android SDK (platform 35).

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew --no-daemon testDebugUnitTest      # unit tests; also writes PNG previews to app/build/outputs/widget-previews/
./gradlew --no-daemon assembleDebug
EVA_WIDGET_SIGNING=/path/to/signing.properties ./gradlew --no-daemon assembleRelease
```

Release signing reads a properties file whose path is given by the Gradle property or environment variable `EVA_WIDGET_SIGNING`, with keys `storeFile`, `storePassword`, `keyAlias`, `keyPassword` (PKCS12 store). Keep it outside the repository. Without it, `assembleRelease` produces an unsigned APK (`app/build/outputs/apk/release/`). R8 minification and resource shrinking are enabled for release.
