package com.musetube.app.webview.injections

/*
 * Retired: YTM ships its own search box in ytmusic-nav-bar, so the custom
 * Spotify search overlay has no YTM equivalent. Kept as a no-op so the
 * injection bundle stays stable; window.playFromUri (PlaybackControls)
 * remains the programmatic play entry point.
 */

object SearchOverlay {
    const val CONTENT = """
            (function(){
                if(window.__mtbSearchRetired) return;
                window.__mtbSearchRetired = true;
            })();
        
    """
}
