package com.musetube.app.webview.helpers

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Functional tests for the adblock matching core (pure JVM logic, no WebView).
 *
 * These lock in the behaviors the native + JS layers depend on:
 * - analytics/ad endpoints are stubbed, music/player traffic is not
 * - ad markers in googlevideo URLs are probed, music streams are never
 *   silent-swapped (music and ads share the googlevideo CDN)
 * - harvested ad content IDs match exactly and stay bounded
 */
class AdBlockerTest {

    @Before
    @After
    fun resetStore() {
        AdIdStore.clear()
    }

    // --- Layer B1: analytics stubbing ------------------------------------

    @Test
    fun analyticsDomains_areBlocked() {
        assertTrue(isAnalyticsDomain("https://googleads.g.doubleclick.net/pagead/id"))
        assertTrue(isAnalyticsDomain("https://pagead2.googlesyndication.com/getconfig"))
        assertTrue(isAnalyticsDomain("https://sentry.io/api/123/envelope/"))
        assertTrue(isAnalyticsDomain("https://www.youtube.com/api/stats/ads?ad_v=abc"))
        assertTrue(isAnalyticsDomain("https://www.youtube.com/ptracking?video_id=abc"))
        assertTrue(isAnalyticsDomain("https://tracker.samplicio.us/track"))
    }

    @Test
    fun playerAndMusicTraffic_areNotAnalytics() {
        assertFalse(isAnalyticsDomain("https://music.youtube.com/"))
        assertFalse(isAnalyticsDomain("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertFalse(isAnalyticsDomain("https://rr1---sn-abc.googlevideo.com/videoplayback?itag=140"))
        assertFalse(isAnalyticsDomain("https://manifest.googlevideo.com/api/manifest/hls_variant/itag/140"))
    }

    @Test
    fun ytmChunks_areNotBlocked_regressionChunkLoadError() {
        // Same lesson as the old Spotify workbox regression: blocking the
        // player's lazy chunks whitescreens the app. Must stay allowed.
        assertFalse(isAnalyticsDomain("https://music.youtube.com/_polymer_/chunk.js"))
        assertFalse(isAnalyticsDomain("https://www.youtube.com/s/player/abcd/player-plasma-ias-phone-en_GB.vflset/base.js"))
    }

    // --- Layer B2: ad-marker matching (non-proxy probe gate) --------------

    @Test
    fun adAudioHosts_areDetected() {
        assertTrue(isAdAudioUrl("https://rr1---sn-abc.googlevideo.com/videoplayback?itag=140&ctier=AFQ"))
        assertTrue(isAdAudioUrl("https://rr1---sn-abc.googlevideo.com/videoplayback?itag=18&adformat=1_1"))
    }

    // --- Layer B3: static ad-CDN matching (proxy mode) --------------------

    @Test
    fun adCdnPatterns_areDetected() {
        assertNotNull(matchAdCdn("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertNotNull(matchAdCdn("https://www.youtube.com/ptracking?video_id=abc"))
        assertNotNull(matchAdCdn("https://www.youtube.com/api/stats/ads?ad_v=abc"))
        assertNotNull(matchAdCdn("https://googleads.g.doubleclick.net/pagead/id"))
        assertNotNull(matchAdCdn("https://example.com/vast/vast.xml"))
    }

    @Test
    fun musicCdn_isNeverAdCdn() {
        // Silencing these would mute real music. Locked in per the
        // "never music CDN" rule in AdBlocker.kt: music and ads share
        // googlevideo, so only ad-only markers may match.
        assertNull(matchAdCdn("https://rr1---sn-abc.googlevideo.com/videoplayback?itag=140"))
        assertNull(matchAdCdn("https://manifest.googlevideo.com/api/manifest/hls_variant/itag/140"))
        assertNull(matchAdCdn("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"))
        assertNull(matchAdCdn("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertNull(matchAdCdn("https://music.youtube.com/youtubei/v1/player?prettyPrint=false"))
    }

    // --- Layer B4: harvested ad content IDs -------------------------------

    @Test
    fun harvestedIds_matchExactMediaRequests() {
        assertTrue(AdIdStore.addAll(listOf("abc12345", "zzz_--12")))
        assertTrue(AdIdStore.matches("https://rr1---sn-abc.googlevideo.com/videoplayback?id=abc12345&x=1"))
        assertTrue(AdIdStore.matches("https://rr1---sn-abc.googlevideo.com/videoplayback?id=zzz_--12"))
        assertFalse(AdIdStore.matches("https://rr1---sn-abc.googlevideo.com/videoplayback?id=music999"))
    }

    @Test
    fun invalidIds_areRejected() {
        assertFalse(AdIdStore.addAll(listOf("short", "has space12", "bad!chars1", "")))
        assertFalse(AdIdStore.matches("https://rr1---sn-abc.googlevideo.com/videoplayback?id=short"))
    }

    @Test
    fun clear_resetsMatching() {
        AdIdStore.addAll(listOf("abc12345"))
        assertTrue(AdIdStore.matches("https://x/abc12345"))
        AdIdStore.clear()
        assertFalse(AdIdStore.matches("https://x/abc12345"))
    }

    @Test
    fun store_isBounded_leastRecentlySeenEvicted() {
        val ids = (0 until 40).map { "fileid_%02d".format(it) }
        AdIdStore.addAll(ids)
        // First 8 evicted, last 32 kept.
        assertFalse(AdIdStore.matches("https://x/fileid_00"))
        assertFalse(AdIdStore.matches("https://x/fileid_07"))
        assertTrue(AdIdStore.matches("https://x/fileid_08"))
        assertTrue(AdIdStore.matches("https://x/fileid_39"))
    }

    // --- Power-hog detection (video/canvas parking) -----------------------

    @Test
    fun powerHogUrls_areDetected_musicAudioIsNot() {
        assertTrue(isPowerHogUrl("https://example.com/video.mp4"))
        assertFalse(isPowerHogUrl("https://rr1---sn-abc.googlevideo.com/videoplayback?itag=140/audio/x"))
    }
}
