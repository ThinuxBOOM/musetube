package com.musetube.app.webview.injections

/*
 * YTM player-bar download button (rewritten from the Spotify now-playing-bar
 * version).
 *
 * - Track identity comes from window.__curTrackId (mirrored by FetchOverride
 *   from youtubei player responses) with the Phase 2 tracker globals as
 *   fallback; the payload keeps the {trackId,title,artist,album,cover} shape
 *   so DownloadManager needs no changes (trackId is the 11-char videoId,
 *   which takes the direct-resolve fast path).
 * - The button anchors before the player-bar more-button
 *   (.toggle-menu-button), retried on a 5s interval like before.
 */

object DownloadButton {
    const val CONTENT = """
            window.mtbDoDownload = function(){
                var id = window.__curTrackId || window.mtbTrackId;
                if(!id){
                    AndBridge.deferMessage('Track not ready');
                    return;
                }
                var payload = JSON.stringify({
                    trackId: id,
                    title: window.__curTrackName || window.track || '',
                    artist: window.__curTrackArtist || window.artist || '',
                    album: window.__curTrackAlbum || '',
                    cover: window.__curTrackCover || window.cover || ''
                });
                AndBridge.downloadTrack(payload);
            };
            window.mtbAddDownloadBtn = function(){
                if(typeof window.dlBtn !== 'undefined' && window.dlBtn.isConnected) return;
                var anchorBtn = document.querySelector('ytmusic-player-bar .toggle-menu-button:not(.mtbf)');
                if(!anchorBtn) return;
                anchorBtn.classList.add('mtbf');
                var btn = document.createElement('button');
                btn.className = 'npbtn';
                btn.title = 'Download track';
                btn.setAttribute('aria-label', 'Download track');
                btn.innerHTML = '<svg viewBox="0 0 16 16" width="16" height="16"><path fill="currentColor" d="M8 1a1 1 0 0 1 1 1v6.586l2.293-2.293a1 1 0 1 1 1.414 1.414l-4 4a1 1 0 0 1-1.414 0l-4-4a1 1 0 1 1 1.414-1.414L7 8.586V2a1 1 0 0 1 1-1zM2 13a1 1 0 0 1 1-1h10a1 1 0 1 1 0 2H3a1 1 0 0 1-1-1z"/></svg>';
                btn.onclick = function(){ window.mtbDoDownload(); };
                anchorBtn.before(btn);
                window.dlBtn = btn;
            };
            setInterval(window.mtbAddDownloadBtn, 5000);
        
    """
}
