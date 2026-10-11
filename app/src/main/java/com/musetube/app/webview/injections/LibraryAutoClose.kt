package com.musetube.app.webview.injections

/*
 * Retired: YTM has no Spotify-style library side panel to auto-close.
 * Kept as a no-op so the injection bundle stays stable.
 */

object LibraryAutoClose {
    const val CONTENT = """
        (function () {
            if (window.__mtbLibraryAutoClose) return;
            window.__mtbLibraryAutoClose = true;
        })();
    """
}
