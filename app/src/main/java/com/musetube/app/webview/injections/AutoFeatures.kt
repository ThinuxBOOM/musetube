package com.musetube.app.webview.injections

object AutoFeatures {
    const val CONTENT = """
            window.addAutoFeatures = function(){
                if('pBtn' in window && firstPlay && window.autoPlayMode!=='disabled' && pBtn.getAttribute('aria-label')==='Play') {
                    pBtn.click();
                    firstPlay=false;
                }
                if(afint) clearInterval(afint);
                afint = setInterval(function(){
                    if(window.closeNpPref) closeNowPlay();
                    if(window.autoPlayMode==='permanent' && 'pBtn' in window && !reqPause && !ulFlag && pBtn.getAttribute('aria-label')==='Play') {
                        pBtn.click();
                    }
                    if(window.autoPlayMode==='onetime' && !window.__mtbApDone && !window.__mtbApActive && 'pBtn' in window && !reqPause && pBtn.getAttribute('aria-label')==='Play') {
                        pBtn.click();
                        window.__mtbApActive = true;
                    }
                },5000);
            };
        
    """
}
