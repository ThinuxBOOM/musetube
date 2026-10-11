package com.musetube.app.webview

/**
 * Sole home for all YouTube Music DOM selectors going forward.
 *
 * Frozen from the Phase 1 DOM dump (desktop Chrome, logged-out
 * music.youtube.com, 2026-10-11). Verified present in DOM:
 * ytmusic-app, ytmusic-app-layout, ytmusic-player-bar, ytmusic-player,
 * video, ytmusic-player-queue, ytmusic-menu-renderer, ServiceLogin link.
 *
 * NOTE: dump was logged-out (empty player, title/byline blank, queue 0
 * items, video paused t=0/NaN). Logged-in track-shape assumptions
 * (queue-item internals, byline links) are marked UNVERIFIED and must be
 * re-checked with a signed-in session in Phase 2.
 */
object YtmSelectors {
    // Shell
    const val APP = "ytmusic-app"
    const val APP_LAYOUT = "ytmusic-app-layout"
    const val NAV_BAR = "ytmusic-nav-bar"
    const val SIGNIN_LINK = "ytmusic-nav-bar a[href*=\"ServiceLogin\"]"

    // Player bar + controls
    const val PLAYER_BAR = "ytmusic-player-bar"
    const val PLAY_PAUSE = "ytmusic-player-bar #play-pause-button"
    const val PREV = "ytmusic-player-bar .previous-button"
    const val NEXT = "ytmusic-player-bar .next-button"
    /** Repeat state lives on the bar: repeat-mode="NONE|ALL|ONE". */
    const val REPEAT_MODE_ATTR = "repeat-mode"
    const val SHUFFLE_BTN = "ytmusic-player-bar [aria-label*=\"Shuffle\" i]"
    const val LIKE_RENDERER = "ytmusic-player-bar ytmusic-like-button-renderer#like-button-renderer"
    const val LIKE_BTN = "#button-shape-like"
    const val MORE_BTN = "ytmusic-player-bar .toggle-menu-button"

    // Metadata
    const val TITLE = "ytmusic-player-bar yt-formatted-string.title"
    const val BYLINE = "ytmusic-player-bar .byline-wrapper .subtitle"
    const val COVER = "ytmusic-player-bar .thumbnail-image-wrapper img.image"
    const val BIG_COVER = "ytmusic-player #song-image yt-img-shadow#thumbnail img#img"

    // Position
    const val PROGRESS_SLIDER = "ytmusic-player-bar tp-yt-paper-slider#progress-bar"
    const val TIME_INFO = "ytmusic-player-bar .time-info, ytmusic-player-bar #time-info"

    // Playback element
    const val PLAYER = "ytmusic-player#player"
    const val VIDEO = "ytmusic-player video"

    // Ad state
    const val AD_SHOWING_ATTR = "ad-showing"
    const val AD_BADGE = "ytmusic-player-bar .badge-style-type-ad-stark"

    // Queue + menu (UNVERIFIED logged-in shape)
    const val QUEUE = "ytmusic-player-queue"
    const val QUEUE_ITEM = "ytmusic-player-queue-item"
    const val MENU = "ytmusic-menu-renderer"
}
