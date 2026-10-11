# MuseTube

An Android app that wraps YouTube Music's web player with built-in adblocking — no root, no shady mods, just your Google account on a slick WebView.

**MuseTube**, forked from MuseMobile (clean Kotlin) by **ThinuxBOOM**. Free, open-source. See `project.md` for the full build plan and phase log.

> Status: initial framework complete (Phases 0–6). Debug builds green, unit tests green. On-device verification with a signed-in session is still pending — see "Manual verification" below.

---

## Features (v1)

- blocks video/audio ads & telemetry (behavioral mute + skip, adPlacements stripping, analytics sinkhole)
- media notification: play/pause, skip, seek, like/unlike, shuffle, repeat with custom actions
- **Android Auto**: browse your playlists, albums, artists, and podcasts over the YouTube InnerTube API; search and play from the car dashboard
- **offline downloads**: download songs and play them offline — tracks resolve direct by videoId, audio via the InnerTube/NewPipe stack
- lock screen, Bluetooth, and Wear OS controls
- autoplay modes: off, once at start, or permanent
- mobile-friendly CSS/JS layout tweaks + custom player overlay
- AMOLED dark mode (pure black)
- sleep timer
- update checker (auto & manual)
- multiple account profiles
- picture-in-picture (PiP) support
- wake lock controls & power save mode

---

## Requirements

- Android 8.0+ (API 28+)
- a Google account (free YouTube Music works; Premium works too)
- Android System WebView (comes with your phone)

---

## Quick Start

Install the APK, open it, sign in with Google, done. MuseTube runs in **normal mode** by default — no certificate, no setup, no "Certificate Required" screen.

---

## Proxy MITM Mode (optional)

Flip the mode in **Settings → Connection Mode → "MITM Proxy (Certificate)"**. The app restarts and walks you through the cert install.

### The Certificate Thing

MuseTube generates a local CA cert so fingerprinting sees a desktop browser. It lives on your device, stays on your device.

1. Open MuseTube in proxy mode — you'll see the **"Certificate Required"** screen
2. Tap **"Export .pem"** to save it to your Downloads
3. Go to **Settings > Security > Encryption & Credentials > Install a certificate > CA certificate**
4. Find `MuseTube_CA.pem` in your Downloads and tap it
5. It'll warn you about network monitoring — tap **"Install anyway"**
6. Come back to MuseTube and tap **"Check"**

> **Note:** if you ever clear your device's credential storage (like after a factory reset), you'll have to do this again.

---

## Build It Yourself

```bash
git clone https://github.com/ThinuxBOOM/musetube
cd musetube
```

Requirements: JDK 17, Android SDK with platform `android-37` + build-tools 37, `local.properties` with `sdk.dir` (gitignored — the build reads it from there).

```powershell
# gradle-wrapper.jar is gitignored upstream, so build with a local dist
# (or run `gradle wrapper` once to restore ./gradlew):
gradle :app:assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk` (`applicationId com.musetube.app` — installs side-by-side with MuseMobile).

Unit tests: `gradle :app:testDebugUnitTest` (adblock matching core, 11 cases).

> Build tip: keep the repo outside OneDrive-synced folders if you can — OneDrive file locks on `app/build` intermittently fail incremental builds. A full rebuild after `gradle --stop` always recovers.

### Deferred setup (post-framework)

These are intentionally stubbed until the framework lands:

- **Firebase**: `app/build.gradle.kts` has the google-services/crashlytics plugins and deps commented out. To enable: create a Firebase project, register `com.musetube.app`, drop `google-services.json` into `app/` (gitignored), uncomment the `TODO(Phase0)` blocks in `app/build.gradle.kts` + `yt/Utils.kt`.
- **Release signing**: `app/build.gradle.kts` release type expects `keystore/keystore.properties` (gitignored). Create it + the keystore, then `:app:assembleRelease` + ProGuard.
- **Launcher icon**: still the forked MuseMobile artwork in `app/src/main/res/mipmap-*` + `art/`.

---

## Manual verification (needs a signed-in device)

Per-phase checks in `project.md` §10 that only a human with the app can do:

- cold start → Google sign-in → YTM home; session kill → login screen; proxy on/off both load
- WebView play → notification/lockscreen/BT metadata + transport; pause syncs; position drift <2s
- free-account ad → muted/skipped <2s, no whitescreen
- screen-off playback; car HU browse → play; BT disconnect pauses; timer stops audio
- song/playlist tap → `Music/MuseTube/Artist - Title [videoId].m4a` → airplane-mode playback
- AMOLED/prefs hot-apply; CA install flow

Known best-effort spots (marked in code): signed-in queue/byline/row shapes, the four library browseIds, LM/podcast-corpus ids, avatar-name detection, volume-slider mapping.

---

## Contributing

Contributions are welcome — open issues, throw PRs. See `project.md` for architecture and the phase log.

---

## Credits

**ThinuxBOOM** — all rights reserved. Forked from [MuseMobile](https://github.com/ThinuxBOOM/musemobile).
