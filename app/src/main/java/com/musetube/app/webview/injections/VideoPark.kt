package com.musetube.app.webview.injections

/*
 * CREDIT: MuseTube - Background Video Park/Restore
 * GitHub: https://github.com/AldySan
 *
 * Parks canvas video sources when the app goes to background and
 * restores them on resume. Spotify's React state never notices the
 * src was stripped, so without this, returning to the app leaves a
 * sourceless <video> = blank Now Playing canvas.
 *
 * Extracted from WorkerNeutralize so it runs independently of the
 * BlockServiceWorker toggle. Video park/restore is a battery/
 * bandwidth optimization, not an SW feature.
 *
 * Parking: stash src + timecode + playing state, then kill the src.
 * Restore: put it back, seek to stashed time, resume if it was playing.
 * blob: URLs (MediaSource) can't be re-attached after load(), so
 * those are left alone.
 */

object VideoPark {
    const val CONTENT = """
        (function(){
            if(window.__mtbVideoParkInit) return;
            window.__mtbVideoParkInit = true;

            try {
                var parked = [];

                function isCanvasVid(v){
                    try{
                        if(v.muted) return true;
                        if(v.hasAttribute && v.hasAttribute('loop')) return true;
                        if(v.style && v.style.objectFit === 'cover') return true;
                    }catch(e){}
                    return false;
                }

                function parkLive(){
                    var vs = document.querySelectorAll('video');
                    for(var i=0;i<vs.length;i++){
                        var v = vs[i];
                        if(v.__mtbParked) continue;
                        if(!isCanvasVid(v)) continue;
                        var src = v.currentSrc || v.getAttribute('src') || '';
                        /* blob: = MediaSource, can't be re-attached after load().
                           Leave those alone rather than strand them. */
                        if(!src || src.startsWith('blob:')) continue;
                        v.__mtbParked = true;
                        parked.push({el:v, src:src, t:(v.currentTime||0), playing:(!v.paused && !v.ended)});
                        try{ v.pause(); }catch(e){}
                        try{ v.removeAttribute('src'); }catch(e){}
                        try{ v.load(); }catch(e){}
                    }
                    return parked.length;
                }

                window.__mtbParkVideos = function(){
                    parkLive();
                };

                window.__mtbRestoreVideos = function(){
                    if(!parked.length) return;
                    for(var i=0;i<parked.length;i++){
                        var b = parked[i];
                        var v = b.el;
                        if(!v || !v.isConnected) continue;
                        /* Spotify already swapped in a fresh src (track changed
                           while backgrounded) - its own source wins. */
                        if(v.getAttribute('src') || v.currentSrc){ v.__mtbParked = false; continue; }
                        try{
                            v.src = b.src;
                            v.currentTime = b.t || 0;
                            if(b.playing){
                                var p = v.play();
                                if(p) p.catch(function(){});
                            }
                        }catch(e){}
                        v.__mtbParked = false;
                    }
                    parked.length = 0;
                    try{ AndBridge.dbg('s','videos restored from background park'); }catch(e){}
                };

                document.addEventListener('visibilitychange', function(){
                    if(document.visibilityState === 'hidden') window.__mtbParkVideos();
                    else window.__mtbRestoreVideos();
                });
            } catch(e){}
        })();
    """
}