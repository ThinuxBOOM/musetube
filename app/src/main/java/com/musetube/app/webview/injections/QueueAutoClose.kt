package com.musetube.app.webview.injections

/*
 * Retired: YTM has no Spotify-style queue side panel to auto-close
 * (the queue lives in ytmusic-player-queue / the fullscreen player).
 * Kept as a no-op so the injection bundle stays stable.
 */

object QueueAutoClose {
    const val CONTENT = """
        (function(){
            if(window.__mtbQueueAutoClose) return;
            window.__mtbQueueAutoClose = true;
        })();
    """
}
