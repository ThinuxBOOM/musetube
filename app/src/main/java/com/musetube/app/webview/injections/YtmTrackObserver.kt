package com.musetube.app.webview.injections

/*
 * YTM track observer + state reader (replaces TrackObserver + AndroidTracker).
 *
 * Two jobs, one poll loop:
 *  1. Identity: parses the 11-char videoId from the player-bar title link
 *     (href "...watch?v=ID") and exposes it globally:
 *       window.mtbTrackId      -> "dQw4w9WgXcQ" or null
 *       window.mtbTrackUri     -> "ytm:video:dQw4w9WgXcQ" or null
 *       window.mtbOnTrackChange(fn) -> subscribe; fn(uri, id) fires immediately
 *                                      if a track is already known. Returns unsubscribe.
 *       'trackchange' CustomEvent (detail = uri) kept for event-style consumers.
 *  2. State: window.addAndAuto() (called by MainLoop, same entry name as before)
 *     installs a MutationObserver on ytmusic-player-bar plus a 1s poll and fills
 *     the PlayerCore globals (track/artist/playing/repmode/isfav/shuffle/
 *     duration/position/cover), then calls updMedia() from MediaUpdater — whose
 *     JSON contract {track,artist,cover,playing,fav,shuffle,repeat,duration,
 *     position} is unchanged, so MediaNotificationService needs no changes.
 *     Units are MILLISECONDS (video.currentTime/duration are seconds).
 *
 * Logged-out DOM was verified by dump (Phase 1); logged-in queue-item/byline
 * internals are best-effort until re-checked with a signed-in session.
 */

object YtmTrackObserver {
    const val CONTENT = """
            window.addAndAuto = function(){
                if(aaint) {
                    if(typeof aaint.disconnect==='function') { try { aaint.disconnect(); } catch(e){} }
                    else { clearInterval(aaint); }
                }
                if(window.__mtbTrackInt) { clearInterval(window.__mtbTrackInt); window.__mtbTrackInt = null; }
                function emitTrack(id){
                    var uri = 'ytm:video:' + id;
                    if(uri === window.__mtbLastUri) return;
                    window.__mtbLastUri = uri;
                    window.mtbTrackId = id;
                    window.mtbTrackUri = uri;
                    try{ AndBridge.dbg('s','[track] ' + uri); }catch(e){}
                    window.dispatchEvent(new CustomEvent('trackchange', { detail: uri }));
                    var listeners = window.__mtbTrackListeners || [];
                    for(var i = 0; i < listeners.length; i++){
                        try{ listeners[i](uri, id); }catch(e){}
                    }
                }
                window.mtbOnTrackChange = window.mtbOnTrackChange || function(fn){
                    if(typeof fn !== 'function') return function(){};
                    window.__mtbTrackListeners = window.__mtbTrackListeners || [];
                    window.__mtbTrackListeners.push(fn);
                    if(window.mtbTrackUri){
                        try{ fn(window.mtbTrackUri, window.mtbTrackId); }catch(e){}
                    }
                    return function(){
                        var l = window.__mtbTrackListeners || [];
                        var i = l.indexOf(fn);
                        if(i >= 0) l.splice(i, 1);
                    };
                };
                function readTrackState(){
                    if(window.__mtbBg) return;
                    var bar = document.querySelector('ytmusic-player-bar');
                    var t = bar && bar.querySelector('yt-formatted-string.title');
                    if(t) track=(t.getAttribute('title') || t.textContent || '').trim(); else track=null;
                    var by = bar && bar.querySelector('.byline-wrapper .subtitle');
                    if(by) artist=(by.textContent || '').trim(); else artist='';
                    var v = document.querySelector('ytmusic-player video');
                    if(v) {
                        playing = !v.paused && !v.ended;
                        var d = v.duration, c = v.currentTime;
                        duration = (typeof d === 'number' && isFinite(d)) ? Math.round(d*1000) : null;
                        position = (typeof c === 'number' && isFinite(c)) ? Math.round(c*1000) : null;
                    } else {
                        var ppb = bar && bar.querySelector('#play-pause-button');
                        var lbl = ppb ? (ppb.getAttribute('aria-label') || ppb.getAttribute('title') || '') : '';
                        playing = (lbl === 'Pause');
                        duration = null; position = null;
                    }
                    window.playing = playing;
                    if(bar) {
                        var a = bar.querySelector('.content-info-wrapper a[href*="watch?v="]');
                        if(a) {
                            var m = /[?&]v=([A-Za-z0-9_-]{11})/.exec(a.getAttribute('href') || '');
                            if(m) emitTrack(m[1]);
                        }
                        var rm = bar.getAttribute('repeat-mode') || 'NONE';
                        if(rm === 'ALL') repmode='true';
                        else if(rm === 'ONE') repmode='mixed';
                        else repmode='false';
                        var sh = null;
                        var btns = bar.querySelectorAll('yt-icon-button, button');
                        for(var i=0;i<btns.length;i++){
                            var al = btns[i].getAttribute('aria-label') || btns[i].getAttribute('title') || '';
                            if(/shuffle/i.test(al)){ sh = btns[i]; break; }
                        }
                        if(sh) {
                            var inner = sh.querySelector('button') || sh;
                            var pressed = inner.getAttribute('aria-pressed');
                            var slab = sh.getAttribute('aria-label') || inner.getAttribute('aria-label') || '';
                            if(pressed === 'true') shuffle='shuffle';
                            else if(/^disable/i.test(slab)) shuffle='shuffle';
                            else shuffle='off';
                        } else shuffle='off';
                        var like = bar.querySelector('ytmusic-like-button-renderer#like-button-renderer');
                        if(like) isfav = (like.getAttribute('like-status') === 'LIKE');
                        else isfav = false;
                        var im = bar.querySelector('.thumbnail-image-wrapper img.image');
                        if(im && im.src) cover=im.src; else cover=null;
                    } else {
                        repmode='false'; shuffle='off'; isfav=false; cover=null;
                    }
                    updMedia();
                }
                try {
                    var npTarget = document.querySelector('ytmusic-player-bar') || document.body;
                    var readTimeout;
                    var obs = new MutationObserver(function(){
                        clearTimeout(readTimeout);
                        readTimeout = setTimeout(readTrackState, 200);
                    });
                    obs.observe(npTarget, { childList: true, subtree: true, attributes: true, characterData: true });
                    aaint = obs;
                    window.__mtbTrackInt = setInterval(readTrackState, 1000);
                    readTrackState();
                } catch(e) {
                    aaint = setInterval(readTrackState, 1000);
                }
            };
        
    """
}
