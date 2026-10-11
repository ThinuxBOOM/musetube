package com.musetube.app.webview.helpers

private val ANALYTICS_DOMAINS = listOf(
    "doubleclick.net",
    "googlesyndication.com",
    "pagead",
    "/ptracking",
    "youtube.com/api/stats/ads",
    "fastly-insights.com",
    "sentry.io",
    "adsrvr.org",
    "tracker.samplicio.us"
    // YTM lesson (same as the old Spotify workbox one): YTM boots from lazy
    // webpack/polymer chunks. Never add googlevideo.com, youtube.com watch/
    // browse/player, or gstatic here — blocking those whitescreens the app.
)

private val AD_AUDIO_DOMAINS = listOf(
    "ctier=",
    "adformat="
)

private val POWER_HOG_HOSTS = listOf<String>(
    // YTM canvas/video-background parking is handled behaviorally by
    // VideoPark/PowerSave (Phase 4/6), not by host blocking.
)

private val VIDEO_EXTENSIONS = listOf(".mp4", ".m4s", ".m3u8", ".webm")

private val AD_CDN_PATTERNS = listOf(
    "pagead",
    "/ptracking",
    "youtube.com/api/stats/ads",
    "doubleclick.net",
    "googlesyndication.com",
    // ad-only paths (never music CDN - do NOT add googlevideo videoplayback
    // or manifest hosts here; music and ads share that CDN).
    "/vast/"
)

fun isAnalyticsDomain(url: String): Boolean =
    ANALYTICS_DOMAINS.any { url.contains(it) }

@Suppress("unused")
fun isAdAudioUrl(url: String): Boolean =
    AD_AUDIO_DOMAINS.any { url.contains(it) }

fun isPowerHogUrl(url: String): Boolean {
    if (POWER_HOG_HOSTS.any { url.contains(it) }) return true
    if (url.contains("/audio/")) return false
    val path = url.substringBefore('?').substringBefore('#')
    return VIDEO_EXTENSIONS.any { path.endsWith(it, ignoreCase = true) }
}

fun matchAdCdn(url: String): String? =
    AD_CDN_PATTERNS.firstOrNull { url.contains(it) }
