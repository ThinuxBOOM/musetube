package com.musetube.app.webview.injections

/*
 * CREDIT: MuseTube - Power Save Mode.
 * GitHub: https://github.com/AldySan
 */
object PowerSave {
    const val CONTENT = """
        (function(){
            if(window.__mtbPsInstalled) return;
            window.__mtbPsInstalled = true;

            var videoObs = null;
            var psStyle = null;
            var origSI = null;

            function isCanvasVideo(v){
                try{
                    // YTM: the main playback element must never be frozen — it is
                    // the audio source. Only ambient/background clips qualify.
                    if(v.closest && v.closest('ytmusic-player')) return false;
                    if(v.muted) return true;
                    if(v.hasAttribute && v.hasAttribute('loop')) return true;
                    if(v.style && v.style.objectFit === 'cover') return true;
                }catch(e){}
                return false;
            }

            function freezeVideo(v){
                if(!isCanvasVideo(v)) return;
                if(v.__mtbFrozen) return;
                v.__mtbFrozen = true;
                try{ v.removeAttribute('autoplay'); }catch(e){}
                try{ v.pause(); }catch(e){}
                try{ v.preload = 'none'; }catch(e){}
                try{
                    Object.defineProperty(v, 'play', {
                        value: function(){ return Promise.resolve(); },
                        configurable: true,
                        writable: true
                    });
                }catch(e){}
            }

            function sweepVideos(){
                try{
                    var vs = document.querySelectorAll('video');
                    for(var i=0;i<vs.length;i++) freezeVideo(vs[i]);
                }catch(e){}
            }

            function unfreezeAll(){
                try{
                    var vs = document.querySelectorAll('video');
                    for(var i=0;i<vs.length;i++){
                        var v = vs[i];
                        if(!v.__mtbFrozen) continue;
                        v.__mtbFrozen = false;
                        try{ delete v.play; }catch(e){}
                        try{ v.preload = 'auto'; }catch(e){}
                        var src = v.currentSrc || v.src || '';
                        if(/^https?:/i.test(src)){
                            try{ v.load(); }catch(e){}
                        }
                        try{
                            var p = v.play();
                            if(p && p.catch) p.catch(function(){});
                        }catch(e){}
                    }
                }catch(e){}
            }

            window.__mtbApplyPowerSave = function(on){
                window.__mtbPowerSavePref = !!on;
                if(on){
                    if(window.__mtbPsActive) return;
                    window.__mtbPsActive = true;

                    sweepVideos();
                    videoObs = new MutationObserver(function(muts){
                        for(var i=0;i<muts.length;i++){
                            var a = muts[i].addedNodes;
                            for(var j=0;j<a.length;j++){
                                var n = a[j];
                                if(n.nodeType !== 1) continue;
                                if(n.tagName === 'VIDEO'){ freezeVideo(n); }
                                else if(n.querySelectorAll){
                                    var vs = n.querySelectorAll('video');
                                    for(var k=0;k<vs.length;k++) freezeVideo(vs[k]);
                                }
                            }
                        }
                    });
                    try{ videoObs.observe(document.body, {childList:true, subtree:true}); }catch(e){}

                    psStyle = document.createElement('style');
                    psStyle.id = 'mtb-power-save';
                    psStyle.textContent = '[data-testid=canvas-container],[data-testid=canvas],[class*=CanvasPlayer],[class*=CanvasVideo]{display:none!important}';
                    function appendStyle(){
                        var t = document.head || document.documentElement;
                        if(t && !document.getElementById('mtb-power-save')) t.appendChild(psStyle);
                    }
                    appendStyle();
                    document.addEventListener('DOMContentLoaded', appendStyle);

                    origSI = window.setInterval.bind(window);
                    window.setInterval = function(fn, delay){
                        if(window.__mtbPsActive){
                            var d = Number(delay) || 0;
                            if(d >= 100 && d < 5000){ delay = Math.max(d*3, 1500); }
                            else if(d >= 5000 && d < 10000){ delay = 10000; }
                        }
                        return origSI(fn, delay);
                    };

                    window.__mtbPsRafMs = 500;
                } else {
                    if(!window.__mtbPsActive) return;
                    window.__mtbPsActive = false;
                    try{ if(videoObs) videoObs.disconnect(); }catch(e){}
                    videoObs = null;
                    try{ if(psStyle) psStyle.remove(); }catch(e){}
                    psStyle = null;
                    if(origSI){ try{ window.setInterval = origSI; }catch(e){} }
                    unfreezeAll();
                    window.__mtbPsRafMs = 0;
                }
            };

            window.__mtbApplyPowerSave(!!window.__mtbPowerSavePref);
        })();
    """
}