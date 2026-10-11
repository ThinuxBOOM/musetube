package com.musetube.app.webview.injections

/*
 * YTM fetch override (rewritten from the Spotify connect-state version).
 *
 * Two jobs:
 *  1. Best-effort adPlacements stripping: youtubei/v1/{player,browse,search,
 *     next} responses are cloned and the ad-only keys (adPlacements,
 *     playerAds, adBreakHeartbeatParams) are deleted before the page sees
 *     them. A fresh Response is allocated — live collections are never
 *     mutated. Any failure falls through to the original response, so this
 *     can never break playback. YtmAdblock's watchdog is the backstop.
 *  2. Track mirror: player responses populate the __curTrack* globals
 *     (videoId/title/author/cover) so DownloadButton keeps working with real
 *     data until Phase 5 rewrites the download buttons for YTM.
 *
 * Dropped Spotify-only behavior: spotAuthToken/spotCliToken/spotDevId
 * harvesting, pathfinder op-hash capture, track-playback ffDone sync and the
 * /metadata/4/track canonical_uri harvest (AndroidAuto/LibraryFetcher/
 * SearchOverlay rewrites in later phases own their auth).
 */

object FetchOverride {
    const val CONTENT = """
            (function(){
                if(window.oriFetch) return;
                var orig = window.fetch.bind(window);
                window.oriFetch = orig;
                var AD_KEYS = ['adPlacements', 'playerAds', 'adBreakHeartbeatParams'];
                function stripAds(o, depth){
                    if(!o || typeof o !== 'object' || depth > 8) return false;
                    var changed = false;
                    if(o instanceof Array) {
                        for(var i = 0; i < o.length; i++){
                            if(stripAds(o[i], depth + 1)) changed = true;
                        }
                        return changed;
                    }
                    var keys = null;
                    try { keys = Object.keys(o); } catch(e){ return false; }
                    for(var k = 0; k < keys.length; k++){
                        var key = keys[k];
                        var val;
                        try { val = o[key]; } catch(e){ continue; }
                        var isAdKey = false;
                        for(var a = 0; a < AD_KEYS.length; a++){
                            if(key === AD_KEYS[a]) { isAdKey = true; break; }
                        }
                        if(isAdKey) {
                            try { delete o[key]; changed = true; } catch(e){}
                        } else if(val && typeof val === 'object') {
                            if(stripAds(val, depth + 1)) changed = true;
                        }
                    }
                    return changed;
                }
                function mirrorTrack(j){
                    try {
                        var vd = j && j.videoDetails;
                        if(vd && vd.videoId) {
                            window.__curTrackId = vd.videoId;
                            window.__curTrackName = vd.title || null;
                            window.__curTrackArtist = vd.author || null;
                            window.__curTrackAlbum = null;
                            var th = vd.thumbnail && vd.thumbnail.thumbnails;
                            if(th && th.length) window.__curTrackCover = th[th.length - 1].url;
                        }
                    } catch(e){}
                }
                window.fetch = function(input, init) {
                    var url = typeof input==='string' ? input : (input ? input.url : '');
                    var p = null;
                    try { p = orig(input, init); } catch(e){ return orig.apply(this, arguments); }
                    try {
                        if(url && url.indexOf && /youtubei\/v1\/(player|browse|search|next)/.test(url)) {
                            return Promise.resolve(p).then(function(res){
                                try {
                                    if(!res || typeof res.clone !== 'function') return res;
                                    return res.clone().json().then(function(j){
                                        try {
                                            var changed = stripAds(j, 0);
                                            if(url.indexOf('youtubei/v1/player') !== -1) mirrorTrack(j);
                                            if(changed) {
                                                return new Response(JSON.stringify(j), {
                                                    status: res.status, statusText: res.statusText,
                                                    headers: { 'Content-Type': 'application/json' }
                                                });
                                            }
                                        } catch(e){}
                                        return res;
                                    }).catch(function(){ return res; });
                                } catch(e){ return res; }
                            }).catch(function(){ return p; });
                        }
                    } catch(e){}
                    return p;
                };
            })();
        
    """
}
