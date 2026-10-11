package com.musetube.app.webview.injections

/*
 * YTM adblock (replaces Adblockify + AdStateHook).
 *
 * YTM serves ads from the SAME googlevideo CDN as music, so there is no
 * Spotify-style "empty the ad host" layer — blocking googlevideo would mute
 * real music. Instead this is purely behavioral:
 *
 *  Trigger (any of):
 *   - ytmusic-player-bar[ad-showing] attribute, or
 *   - the Sponsored badge (.badge-style-type-ad-stark) unhidden in the bar.
 *  Action:
 *   - video.muted = true (flagged via __mtbAdMuted so the watchdog unmutes
 *     when the break ends),
 *   - video.currentTime = duration (burn through the break),
 *   - click any visible skip control (.ytp-skip-ad-button and friends).
 *
 * playerResponse.adPlacements stripping lives in FetchOverride (best-effort;
 * this watchdog is the backstop). No CSS hiding: skip buttons must stay
 * clickable. Runs on a 500ms interval plus a MutationObserver on the bar so
 * breaks are caught in well under 2s.
 */

object YtmAdblock {
    const val CONTENT = """
        (function(){
            if (window.__mtbYtmAdblock) return;
            window.__mtbYtmAdblock = 1;
            function bar(){ try { return document.querySelector('ytmusic-player-bar'); } catch(e){ return null; } }
            function video(){ try { return document.querySelector('ytmusic-player video'); } catch(e){ return null; } }
            function isAd(){
                var b = bar();
                if(!b) return false;
                try { if(b.hasAttribute('ad-showing')) return true; } catch(e){}
                try {
                    var badge = b.querySelector('.badge-style-type-ad-stark');
                    if(badge && !badge.hasAttribute('hidden')) return true;
                } catch(e){}
                return false;
            }
            function clickSkip(){
                var sels = ['.ytp-skip-ad-button', '.ytp-ad-skip-button', 'button[aria-label*="Skip ad" i]', '.ytp-ad-overlay-close-button'];
                for(var i=0;i<sels.length;i++){
                    try {
                        var els = document.querySelectorAll(sels[i]);
                        for(var j=0;j<els.length;j++){
                            try { els[j].click(); } catch(e){}
                        }
                    } catch(e){}
                }
            }
            function handleAd(){
                var v = video();
                if(v) {
                    try { v.muted = true; window.__mtbAdMuted = true; } catch(e){}
                    try {
                        var d = v.duration;
                        if(typeof d === 'number' && isFinite(d) && d > 0) v.currentTime = Math.max(0, d - 0.1);
                    } catch(e){}
                }
                clickSkip();
            }
            function watchdog(){
                try { if(window.__mtbBg) return; } catch(e){}
                if(isAd()){
                    handleAd();
                } else if(window.__mtbAdMuted){
                    var v = video();
                    if(v) { try { v.muted = false; } catch(e){} }
                    window.__mtbAdMuted = false;
                }
            }
            try {
                setInterval(watchdog, 500);
            } catch(e){}
            try {
                var b0 = bar();
                if(b0) {
                    var mo = new MutationObserver(watchdog);
                    mo.observe(b0, { attributes: true, attributeFilter: ['ad-showing'], childList: true, subtree: true });
                }
            } catch(e){}
            try { AndBridge.dbg('s', 'ytm-adblock active'); } catch(e){}
        })();
    """
}
