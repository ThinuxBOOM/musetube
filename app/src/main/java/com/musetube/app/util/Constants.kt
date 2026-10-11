package com.musetube.app.util

/**
 * Single home for app-wide constants (Phase 0).
 *
 * DESKTOP_UA was previously copy-pasted in MainActivity, MusicWebViewClient,
 * MusicWebChromeClient, MusicBridge and GoogleSpoof — add new usages here.
 */
object Constants {
    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"

    const val YTM_HOME = "https://music.youtube.com/"
    const val GOOGLE_LOGIN = "https://accounts.google.com/"
}
