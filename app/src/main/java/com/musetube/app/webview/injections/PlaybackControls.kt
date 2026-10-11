package com.musetube.app.webview.injections

/*
 * YTM playback controls (rewritten from the Spotify connect-state version).
 *
 * Same window.* entry points so MediaNotificationService / SearchOverlay /
 * MainLoop callers don't churn: playFromUri(uri, contextUri), actPlayPause,
 * actSkipBack, actSkipForward, actToggleShuffle, actRepeat, actAddToFav,
 * actSeek(posMs). All actions drive the ytmusic-player-bar DOM directly;
 * audio stays in the WebView (v1, no native player).
 *
 * playFromUri accepts an 11-char videoId, a ...watch?v=ID URL, or a context
 * carrying one — anything else is ignored with a dbg line (Spotify URIs from
 * not-yet-rewritten callers land here safely until Phase 4/6 rewrites them).
 */

object PlaybackControls {
    const val CONTENT = """
            window.mtbExtractVideoId = window.mtbExtractVideoId || function(s) {
                if(!s || typeof s !== 'string') return null;
                var m = /[?&]v=([A-Za-z0-9_-]{11})/.exec(s);
                if(m) return m[1];
                if(/^[A-Za-z0-9_-]{11}$/.test(s)) return s;
                return null;
            };
            window.playFromUri = function(uri, contextUri) {
                var id = window.mtbExtractVideoId(uri) || window.mtbExtractVideoId(contextUri);
                if(id) {
                    try{ AndBridge.wakeUp(); }catch(e){}
                    location.href = 'https://music.youtube.com/watch?v=' + id;
                    return;
                }
                // YTM collection ids: playlists (VL*/LM), albums (MPRE*), artists (UC*), radios (RD*).
                var raw = contextUri || uri || '';
                var dest = null;
                try {
                    if(/^VL/i.test(raw) || raw === 'LM') dest = 'https://music.youtube.com/playlist?list=' + raw;
                    else if(/^MPRE/.test(raw)) dest = 'https://music.youtube.com/browse/' + raw;
                    else if(/^UC[A-Za-z0-9_-]{20,}$/.test(raw)) dest = 'https://music.youtube.com/channel/' + raw;
                    else if(/^RD/.test(raw)) dest = 'https://music.youtube.com/watch?v=' + raw;
                } catch(e){}
                if(!dest) { try{ AndBridge.dbg('w','playFromUri: no YTM id in ' + uri); }catch(e){} return; }
                try{ AndBridge.wakeUp(); }catch(e){}
                location.href = dest;
            };
            window.mtbBarBtn = function(pred) {
                var bar = document.querySelector('ytmusic-player-bar');
                if(!bar) return null;
                var btns = bar.querySelectorAll('yt-icon-button, button');
                for(var i=0;i<btns.length;i++){
                    var el = btns[i].querySelector('button') || btns[i];
                    var lbl = (btns[i].getAttribute('aria-label') || btns[i].getAttribute('title') || '') + ' ' +
                              (el.getAttribute('aria-label') || el.getAttribute('title') || '');
                    if(pred(lbl, btns[i])) return el;
                }
                return null;
            };
            window.actPlayPause = function(play) {
                var pb = document.querySelector('ytmusic-player-bar #play-pause-button button') ||
                         document.querySelector('ytmusic-player-bar #play-pause-button');
                if (!pb) return;
                if (play === null || typeof play === 'undefined') {
                    pb.click();
                } else if (play === true) {
                    if (!window.playing) pb.click();
                } else if (play === false) {
                    if (window.playing) pb.click();
                }
            };
            window.actSkipBack = function() {
                var bb = document.querySelector('ytmusic-player-bar .previous-button button') ||
                         document.querySelector('ytmusic-player-bar .previous-button');
                if(bb) { try{ AndBridge.wakeUp(); }catch(e){} bb.click(); }
            };
            window.actSkipForward = function() {
                var fb = document.querySelector('ytmusic-player-bar .next-button button') ||
                         document.querySelector('ytmusic-player-bar .next-button');
                if(fb) { try{ AndBridge.wakeUp(); }catch(e){} fb.click(); }
            };
            window.actToggleShuffle = function() {
                var sb = window.mtbBarBtn(function(lbl){ return /shuffle/i.test(lbl); });
                if(sb) {
                    try{ AndBridge.wakeUp(); }catch(e){}
                    sb.click();
                }
            };
            window.actRepeat = function() {
                var rb = window.mtbBarBtn(function(lbl){ return /^repeat/i.test(lbl.trim()); });
                if(rb) {
                    if(repmode=='false') repmode='true';
                    else if(repmode=='true') repmode='mixed';
                    else repmode='false';
                    updMedia();
                    rb.click();
                }
            };
            window.actAddToFav = function() {
                var fb = document.querySelector('ytmusic-player-bar ytmusic-like-button-renderer#like-button-renderer #button-shape-like') ||
                         document.querySelector('ytmusic-player-bar ytmusic-like-button-renderer#like-button-renderer');
                if(fb) {
                    isfav = !isfav;
                    updMedia();
                    fb.click();
                }
            };
            window.actSeek = function(pos) {
                var v = document.querySelector('ytmusic-player video');
                if(v && isFinite(v.duration)) {
                    v.currentTime = Math.min(Math.max(0, pos/1000), v.duration);
                    position = Math.round(v.currentTime*1000);
                    return;
                }
                var rg = document.querySelector('ytmusic-player-bar tp-yt-paper-slider#progress-bar');
                if(rg) {
                    rg.setAttribute('value', String(pos));
                    rg.dispatchEvent(new Event('change',{bubbles:true}));
                }
            };
        
    """
}
