# MuseTube — Project Outline

> Status (2026-10-11): initial framework complete — Phases 0–6 committed
> (`1ead3dd` → `34fa9eb` → Phase 6). `assembleDebug` + `testDebugUnitTest`
> green. Deferred post-framework: Firebase enablement, release
> keystore/signing, Play Store, launcher-icon art, gradle-wrapper.jar
> restore. On-device signed-in verification still pending (§10).

## 0. Goal & Locked Decisions

Build a **separate app named MuseTube** that replaces the Spotify WebView
(`open.spotify.com`) with **YouTube Music** (`music.youtube.com`), keeping the
same architecture as MuseMobile:

```
SplashActivity → (OfflineMode? OfflineActivity : CA/proxy gate) → MainActivity
MainActivity: WebView(music.youtube.com) ↔ AndBridge (MusicBridge) ↔ MediaNotificationService
  + LocalProxyManager (MITM, generic) + DownloadManager/OfflineStore + innertube/yt/ stack
```

**v1 scope (locked):** adblocking + background/notification + offline downloads +
Android Auto browse. No Spotify support in this repo. No new native player —
audio stays in WebView, same as MuseMobile.

**Reuse estimate:** ~30% as-is, ~20% mechanical rename/tweak, ~50% rewrite
(every `open.spotify` / `accounts.spotify` / `pathfinder` / `spclient` / `scdn` /
`spotify:` / `data-testid-encore` touchpoint).

**Bootstrap decisions (agreed):**
- Fork method: **clean copy, no history**. Copy `musemobile/main` tree into
  `musetube/`, single squashed `Initial MuseTube fork` commit. No cherry-picks;
  future musemobile fixes ported manually.
- Firebase: no `google-services.json` committed. Phase 0 builds with Firebase
  stubbed; owner creates a `com.musetube.app` Firebase project following the
  guide in Phase 0.
- YTM DOM selectors: assistant dumps `music.youtube.com` via automated browser
  inspection and freezes results into `webview/YtmSelectors.kt`.
- Planning: full plan now (this file), execution starts with Phase 0.

**Source:** `https://github.com/ThinuxBOOM/musemobile` (Free-Spotify-Wrapper,
Kotlin + Compose + WebView, `com.musemobile.app`, minSdk 28, targetSdk 36).
**Target:** `https://github.com/ThinuxBOOM/musetube.git` — currently empty
(`main`, no commits). This file lives at its root.

---

## 1. What MuseMobile Does (Reference Architecture)

MuseMobile wraps `open.spotify.com` in a WebView instead of patching
`com.spotify.music`. Rationale (from `current research.txt`): desktop Spicetify
adblock writes to leaked JS APIs; on Android the same five gates (Premium map,
slot eligibility, creative fetch, ad-break state, UI surface) must be
intercepted — using the web player sidesteps the native/JNI ads SDK entirely.

Modules:
- **Entry:** `ui/SplashActivity.kt`, `ui/MainActivity.kt` (~62k — WebView setup,
  PiP, wake lock, sleep timer, profiles), `ui/OfflineActivity.kt`,
  `MuseMobileApp.kt` (lean `Application`, StrictMode in debug only).
- **WebView:** `webview/SpotifyWebViewClient.kt` (early JS injection in
  `onPageStarted`, player bundle in `onPageFinished`, `shouldInterceptRequest`
  blocks analytics + swaps ad-audio for `assets/silent.mp3`),
  `webview/SpotifyWebChromeClient.kt`, `webview/injections/` (~35 files:
  `MuseMobilePlayer`, `Adblockify`, `CssHack`, `TrackObserver`,
  `PlaybackControls`, `MediaUpdater`, `SearchOverlay`, `DownloadButton`,
  `CollectionDownload`, `ContextMenuDownload`, `AndroidAuto`,
  `LibraryFetcher/Parser`, etc.), `webview/helpers/` (`AdBlocker.kt`,
  `AdIdStore.kt` — 32-ID bounded store, `JsUtils`, `CssInjector`,
  `AmoledTheme`, `LyricsTheme`).
- **Bridge:** `bridge/SpotifyBridge.kt` (311 lines, `@JavascriptInterface`:
  `loginDetected`, `recMediaStatus/Position`, `onMediaItemsLoaded`,
  `onSearchCompleted`, `downloadTrack/Collection`, `skipDownload/cancelDownload`,
  `nFetch` with desktop UA + cookie sync + 2MiB cap, `recAdContentIds`, PiP,
  timer).
- **Proxy (optional MITM):** `proxy/LocalProxyManager.kt` (~42k, local CA,
  `Certificate Required` flow). Normal mode works out of the box.
- **Offline:** `innertube/InnerTube.kt` (Ktor+OkHttp, `search`/`player`, retry,
  proxy, locale) + `innertube/YouTube.kt` (ViMusic-derived + NewPipe fallback) +
  `innertube/models|pages|utils`, `yt/` (`YTPlayerUtils`, `CandidateScorer`,
  `cipher/`, `potoken/`, `sabr/`), `offline/DownloadManager.kt`
  (`Channel<DownloadJob>`, `Single` vs `Collection`, `resolveStream()` via
  `YouTube.search(FILTER_SONG)` + scorer + player resolve, ranged download with
  skip/cancel, save via MediaStore `Music/MuseMobile` + `[trackId]` filename) +
  `offline/OfflineStore.kt` (`offline_meta.json` + `covers/`), plus
  `service/DownloadService.kt`, `service/OfflineMediaService.kt`.
- **Media:** `service/MediaNotificationService.kt` (~36k, `MediaBrowserService`:
  lockscreen/BT/WearOS, play/pause/skip/seek/like/shuffle/repeat, Auto
  browse/search via `AndroidAuto.kt` + pathfinder API).
- **Other:** `searchEngine/GenericSearchEngine.kt` (pure-Kotlin fuzzy search,
  portable as-is), `profile/ProfileManager.kt` (7x Spotify cookie domains,
  `sp_dc` gate, encrypted prefs), `util/UpdateChecker.kt` + `GitHubApi.kt`,
  `net/SharedOkHttp.kt`, `AndroidManifest.xml` (Splash/Main/Offline activities,
  3 foreground services, `MediaButtonReceiver`).

---

## 2. Target Architecture (MuseTube)

Same shape, YouTube Music client:

```
SplashActivity → (OfflineMode? OfflineActivity : CA/proxy gate) → MainActivity
MainActivity
 ├── WebView (music.youtube.com, desktop UA)
 │    ├── early injections: BrowserSpoof/GoogleSpoof/GaBlocker/WorkerNeutralize/
 │    │                        ClassicBridge/FetchOverride/YtmAdblock
 │    └── late bundle: PlayerCore/YtmTrackObserver/PlaybackControls/YtmAuto/
 │                      DownloadButtons/YtmCssHack/...
 ├── MusicBridge (@JavascriptInterface, iface name stays "AndBridge")
 │    └── nFetch (YTM Origin/Referer) → youtubei/v1/* via SAPISIDHASH
 ├── MediaNotificationService (FEmusic_*/VL* ids, same session/notification core)
 ├── YtmAuto (Android Auto over youtubei browse/search)
 ├── DownloadManager/OfflineStore → Music/MuseTube/ + [videoId]
 └── LocalProxyManager (generic; branding only) + innertube/yt/ (100% reuse)
```

Audio stays in the WebView for v1. No native ExoPlayer. No Spotify code remains.

---

## 3. Phase 0 — Fork & Rebrand to MuseTube (do first)

Read: `settings.gradle.kts`, `app/build.gradle.kts:19-33`,
`app/src/main/AndroidManifest.xml:17-26`, `MuseMobileApp.kt`,
`proxy/LocalProxyManager.kt` (search `musemobile`, `proxy_ca`,
`MuseMobile Proxy CA`), `res/values/strings.xml`, `res/values/colors.xml`
(`spotify_green`).

Tasks:
- Copy `musemobile/main` tree into `musetube/` (excluding `.git`), one squashed
  initial commit.
- Mechanical rename (IDE refactor + replaceAll, then grep to verify zero
  leftovers outside comments):

| Old (MuseMobile) | New (MuseTube) |
|---|---|
| namespace/applicationId `com.musemobile.app` | `com.musetube.app` |
| package `com.musemobile.app` (126 files) | `com.musetube.app` |
| `MuseMobileApp` / `SpotifyBridge` / `SpotifyWebViewClient` / `SpotifyWebChromeClient` | `MuseTubeApp` / `MusicBridge` / `MusicWebViewClient` / `MusicWebChromeClient` (keep JS iface name `AndBridge`) |
| `app_name` MuseMobile, `Theme.MuseMobile` | MuseTube, `Theme.MuseTube` |
| `musemobile_prefs` / `musemobile_secure_prefs` | `musetube_prefs` / `musetube_secure_prefs` |
| `proxy_ca.p12`, `CN=MuseMobile Proxy CA`, `musemobile:*` prefs/tags | `musetube_ca.p12`, `CN=MuseTube Proxy CA`, `musetube:*` |
| `CHANNEL_ID` `musemobile_media_playback` | `musetube_media_playback` |
| `Music/MuseMobile/` (MediaStore) + `offline_meta.json` dir | `Music/MuseTube/` |
| `MuseMobile_CA.pem`, `spotify_green`/black | `MuseTube_CA.pem`, `musetube_red`/black |
| `window.__musemobileUseProxy`, `spl-*` ids, `#musemobilePlayerControls` | `window.__musetubeUseProxy`, `mtb-*`, `#musetubePlayerControls` (single replaceAll across `injections/*.kt` + `MainActivity.kt`) |

- New files: `util/Constants.kt` with `DESKTOP_UA` (dedupe copies in
  `MainActivity:437`, `WebViewClient:410`, `ChromeClient:29`, `Bridge:21`,
  `GoogleSpoof:6`), `YTM_HOME=https://music.youtube.com/`,
  `GOOGLE_LOGIN=https://accounts.google.com/`; new empty
  `webview/YtmSelectors.kt` (sole home for all YTM selectors going forward).
- Firebase guide (owner action): console.firebase.google.com → new project →
  register Android app `com.musetube.app` → download `google-services.json` →
  place in `app/` (gitignored, never committed) → enable Analytics/Crashlytics.
  Until then Phase 0 builds with the Firebase plugins stubbed/disabled so
  `assembleDebug` stays green.
- Do NOT touch logic in this phase.

Accept: `grep -ri "musemobile\|open.spotify\|spclient\|pathfinder\|scdn.co\|spotify:" app/src/main --include="*.kt"` = 0 (outside comments);
`./gradlew :app:assembleDebug` green; MuseTube installs side-by-side with
MuseMobile.

---

## 4. Phase 1 — WebView Shell + Google Auth

Read: `ui/MainActivity.kt:464-509,1399-1410`, `ui/SplashActivity.kt`,
`webview/SpotifyWebViewClient.kt:42-100,156-228`,
`webview/SpotifyWebChromeClient.kt:40-106,127-137`,
`profile/ProfileManager.kt:22-30,105-116`,
`injections/{BrowserSpoof,GoogleSpoof,ClassicLoginButton,LoginDetection,LogoutCheck,SettingsFix}.kt`.

Tasks:
- Entry: `loadUrl("https://open.spotify.com/")` → `Constants.YTM_HOME`; logout
  fallback `accounts.spotify.com/login` → `accounts.google.com/`.
- `isSpotifyUrl()` → `isMusicUrl()`: `music.youtube.com` +
  `accounts.google.com` + `accounts.youtube.com` + `youtube.com`; extend
  `isOAuthUrl()` (keep google/facebook/apple, add `youtube.com` /
  `accounts.youtube.com`). `onCreateWindow` child-WebView logic reused, only
  allowlist changes.
- Keep: desktop UA, DomStorage/Database/MediaPlaybackRequiresUserGesture=false,
  BackForwardCache, ProxyConfig addProxyRule/clearProxyOverride,
  `RESOURCE_PROTECTED_MEDIA_ID` grant (YTM needs Widevine),
  GoogleSpoof/BrowserSpoof/GaBlocker/WorkerNeutralize/ClassicBridge injections
  as-is.
- Rewrite auth: delete `ClassicLoginButton` (`?allow_password=1`); replace
  `LogoutCheck` (`#appServerConfig`/atob `isAnonymous`) + `LoginDetection`
  (`button[data-testid=web-player-link]`) with `YtmLoginDetection.kt` —
  logged-in = `SAPISID`/`HSID`/`SSID` cookie present + `ytmusic-app-layout` in
  DOM; else route to Google login.
  `ProfileManager.COOKIE_DOMAINS=[open.spotify...]` →
  `[music.youtube.com, accounts.google.com, accounts.youtube.com]`; session
  check `sp_dc` → `SAPISID`.
- `SettingsFix.kt:24-26` URL check → `isMusicUrl()`.

Accept: cold start → Google sign-in → YTM home desktop-like; kill session →
login screen; `CookieManager.getCookie(music.youtube.com)` non-empty; proxy
on/off both load.

---

## 5. Phase 2 — Tracker + Bridge Contract (Playback Core)

Read: `bridge/SpotifyBridge.kt` (311 lines),
`injections/{TrackObserver,AndroidTracker,PlayerCore,MediaUpdater,PlaybackControls}.kt`,
`service/MediaNotificationService.kt:199-204,393,506-553,594-616`.

Tasks:
- DOM discovery first: on desktop Chrome `music.youtube.com`, dump
  `ytmusic-player-bar.outerHTML`, title/artist (`yt-formatted-string.title` /
  `artist`), `#progress`, `video`, `[ad-showing]`, queue
  `ytmusic-player-queue`, menu `ytmusic-menu-renderer`. Freeze into
  `YtmSelectors.kt`. Note: videoIds 11-char (`[A-Za-z0-9_-]{11}`), not Spotify
  22-char — update all regexes.
- `MusicBridge` (renamed `SpotifyBridge`): keep ALL method names
  (`loginDetected`/`recMediaStatus`/`recMediaPosition`/`onMediaItemsLoaded`/
  `onSearchCompleted`/`downloadTrack`/`downloadCollection`/`skipDownload`/
  `cancelDownload`/`nFetch`/`dbg`/`deferMessage`/`enterPip`/`openTimerDialog`/
  `recAccountName`) so Kotlin callers don't churn. Only change `nFetch:255-257`
  origin guard → `if (music.youtube.com/youtube.com/googlevideo.com)`
  `{ Origin/Referer: https://music.youtube.com }`; keep 2MiB cap,
  `FILTERED_HEADERS`, cookie sync/flush.
- Rewrite `TrackObserver`+`AndroidTracker` → `YtmTrackObserver.kt`: poll
  `ytmusic-player-bar` + `video.currentTime/duration/paused`, emit identical
  JSON `{track,artist,cover,playing,fav,shuffle,repeat,duration}` via
  `MediaUpdater.updMedia()` (reuse). Rewrite `PlaybackControls.kt`
  `actPlayPause/SkipForward/SkipBack/ToggleShuffle/Seek/Repeat/AddToFav/playFromUri`
  to drive YTM DOM. Keep `PlayerCore` `spl*`→`mtb*` globals shell.
- `MediaNotificationService`: keep session/notification/BT/WakeLock/cover-loader;
  change `parentId.startsWith("spotify:")` guard → YTM `FEmusic_*`/`VL*` ids.

Accept: WebView play → notification + lock-screen + BT play/pause/next/prev/seek
with correct metadata; notification pause pauses WebView; position drifts <2s.

---

## 6. Phase 3 — Adblocking

Read: `webview/helpers/AdBlocker.kt:3-59` + `AdIdStore.kt`,
`webview/SpotifyWebViewClient.kt:156-228`,
`injections/{Adblockify,AdStateHook,FetchOverride}.kt`,
`app/src/test/.../AdBlockerTest.kt`.

Tasks:
- `AdBlocker.kt`: keep function shapes, swap lists — delete
  `scdn`/`adstudio`/audio-ads/`adeventtracker`/`pixel`/`vast`/`ad-logic`/
  `canvasset`/`video-ak.spotify`; add `pagead`, `googlevideo.*ctier`,
  `/ptracking`, `doubleclick.net`, `googlesyndication.com`,
  `youtube.com/api/stats/ads`, `manifest.googlevideo.com/*ad`. Keep
  `→assets/silent.mp3` + analytics sinkhole. Add YTM cases to `AdBlockerTest`.
- New `YtmAdblock.kt` (replaces `Adblockify`/`AdStateHook`): trigger
  `ytmusic-player-bar[ad-showing]` OR `playerResponse.adPlacements` OR short
  googlevideo dur; action `video.muted=true; video.currentTime=video.duration`
  + click skip-ad button. `FetchOverride.kt`: drop
  `pathfinder`/`spotify:track:`/`i.scdn.co`, hook
  `youtubei/v1/{player,browse,search}` to strip `adPlacements` (best-effort;
  network layer is backstop).
- Keep UA/`sec-ch-ua*`→Windows/Chrome150 + `X-Requested-With` strip in both
  `MusicWebViewClient` probe and `LocalProxyManager` (generic, no change). Heed
  `AdBlocker.kt:14-18` lesson: never block YTM webpack chunks.

Accept: free-account video/audio ad → muted/skipped <2s; no
ChunkLoadError/white-screen; unit tests green.

---

## 7. Phase 4 — Background, Auto, BT, PiP, Sleep

Read: `service/MediaNotificationService.kt` (roots 386-389
`aa_playlists`/`albums`/`artists`/`podcasts`), `injections/AndroidAuto.kt`,
`MainActivity.kt:686-716` (`spl-timer`), `injections/{VideoPark,PowerSave}.kt`.

Tasks:
- `AndroidAuto.kt` → `YtmAuto.kt`: keep `aa_*` roots +
  `fetchMediaItems`/`searchMediaItems` → `onMediaItemsLoaded`/
  `onSearchCompleted` contract; queries become `POST
  music.youtube.com/youtubei/v1/browse` + `search` via `nFetch` with SAPISIDHASH
  (from SAPISID cookie in JS) instead of `api-partner...pathfinder` +
  `api.spotify.com/v1/shows`.
- Keep BT/`BECOMING_NOISY`/WakeLock/PiP (`enterPip`/`enterPipVideo`)/
  sleep-timer/`KeepScreenOn`/`BtAutoPause` wiring; retarget PowerSave canvas
  selectors to YTM video bg; `VideoPark` reuse.

Accept: screen-off playback; car HU browses Home/Library → plays; BT disconnect
pauses; timer stops audio.

---

## 8. Phase 5 — Offline Downloads

Read: `offline/DownloadManager.kt` + `OfflineStore.kt` (regex
`^(.*) - (.*) \[id\]`, `RELATIVE_PATH LIKE %Music/%`),
`innertube/YouTube.kt` + `InnerTube.kt`, `yt/YTPlayerUtils.kt` +
`CandidateScorer.kt` + `cipher/` + `potoken/`, `injections/{DownloadButton,
CollectionDownload,ContextMenuDownload,DownloadProgress}.kt`,
`service/DownloadService.kt`, `ui/OfflineActivity.kt` +
`screens/OfflineScreen.kt` + `service/OfflineMediaService.kt`.

Tasks:
- `innertube`/`yt`/`SharedOkHttp` reuse 100% (already YouTube). Only change
  `Music/MuseMobile` → `Music/MuseTube` path.
- Rewrite button selectors: `lyrics-button`/`tracklist-row`/`context-menu`/
  `cover-art-image` → `ytmusic-responsive-list-item-renderer` /
  `ytmusic-menu-renderer` / player-bar more-button; keep payload
  `{title,artist,videoId}` → `downloadTrack`/`downloadCollection` →
  `DownloadManager` → MediaStore M4A + covers + manifest + `mtb-dl-progress` UI
  (`DownloadProgress.kt` reuse).
- Reuse `OfflineActivity`/`OfflineScreen`/`OfflineMediaService`/`DownloadService`
  (rebrand only).

Accept: song/playlist tap → `Music/MuseTube/Artist - Title [videoId].m4a` +
cover → airplane-mode playback.

---

## 9. Phase 6 — Proxy, Settings, CSS Polish, Build

Read: `proxy/LocalProxyManager.kt`, `ui/screens/SettingsScreen.kt` +
`components/SettingsDrawer.kt`, `injections/{CssHack,ModalFix,SearchOverlay,
MainLoop,AutoFeatures,MuseMobilePlayer,QueueAutoClose,LibraryAutoClose}.kt`,
`helpers/{AccentTheme,AmoledTheme,LyricsTheme,CssInjector}.kt`,
`res/xml/network_security_config.xml` + `automotive_app_desc.xml`.

Tasks: `LocalProxyManager` reuse (branding only); `ConnectionMode`
normal(default)/proxy + CA export/check flow unchanged; `SettingsScreen` keep
all prefs, update copy; `CssHack` → `YtmCssHack`
(`#Desktop_LeftSidebar_Id`/`global-nav-bar`/`aside[now-playing-bar]` →
`ytmusic-app-layout` mobile tweaks + `#musetubePlayerControls`); rewrite
`SearchOverlay`/`MainLoop`/`AutoFeatures`/`QueueAutoClose`/`MuseMobilePlayer`
selectors; keep `ErrorDialogRestyle`/`FbGdprBypass`; manifest keep
permissions/services, change label/icon only.

Accept: AMOLED/MaterialYou/prefs hot-apply; CA install works;
`:app:assembleRelease` + ProGuard + `./gradlew :app:testDebugUnitTest` green.

Done with deviations: release signing/keystore + Firebase stay stubbed
(post-framework per owner); `assembleDebug` + unit tests green instead of
`assembleRelease`. `SearchOverlay` retired to a no-op (YTM's native search
box serves it) rather than rewritten; `QueueAutoClose`/`LibraryAutoClose`
stubbed (no YTM panels); dead `LibraryFetcher`/`LibraryParser` deleted;
`MuseTubePlayer` overlay rebound to tracker globals + YTM controls (lyrics
button hidden — lyrics still non-goal).

---

## 10. Build Order, Verification, Risks

Order: 0 → 1 (+DOM dump) → 2 → 3 → 4 → 5 → 6 — all committed.
Don't start Auto/offline before tracker JSON stable (done — contract
frozen in Phase 2). Per-phase verify with `assembleDebug` + manual matrix
(free + premium, WiFi/cell, screen-off, BT, Auto DHU, airplane offline, proxy
on/off). Automated side is green (debug APK `com.musetube.app`, 11 unit
tests); the manual matrix needs a signed-in device and is still pending.
Non-goals v1: lyrics sync, podcast parity, Play Store release (sideload like README). Risks: Google login bot-check (mitigate with desktop UA
+ GoogleSpoof), YTM markup drift (mitigate via `YtmSelectors.kt`), ad/SABR arms
race (mitigate via in-tree `yt/potoken/` + `cipher/`).

## 11. Phase Log

| Phase | Commit | What landed |
|---|---|---|
| 0 Fork & rebrand | `1ead3dd` | clean copy, `com.musetube.app`, prefs/CA/colors/JS globals, `Constants.kt`, `YtmSelectors.kt` stub, Firebase stubbed |
| 1 WebView shell + auth | `0747c0f` | YTM_HOME/GOOGLE_LOGIN entry, `isMusicUrl`/`isOAuthUrl`, `YtmLoginDetection`, ProfileManager YTM domains, DOM dump frozen |
| 2 Tracker + bridge | `436618d` | `YtmTrackObserver` (11-char ids, ms units, same JSON contract), YTM `PlaybackControls`, nFetch YTM guard, Auto id guard |
| 3 Adblocking | `996d6db` | YTM `AdBlocker` lists, `YtmAdblock` watchdog, `FetchOverride` adPlacements strip + track mirror, 11 YTM unit tests |
| 4 Background + Auto | `bc65523` | `YtmAuto` (youtubei via nFetch+SAPISIDHASH), VL/MPRE/UC/RD play, PowerSave guard, BT/PiP/sleep kept |
| 5 Offline | `34fa9eb` | YTM download buttons/menus/collection scrape, direct-videoId fast path |
| 6 Polish | _(this commit)_ | YTM CssHack/overlay/MainLoop/AutoFeatures, retired SearchOverlay/Queue/Library stubs, settings+error copy, docs |
