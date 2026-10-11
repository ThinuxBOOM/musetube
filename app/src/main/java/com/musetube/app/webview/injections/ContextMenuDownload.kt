package com.musetube.app.webview.injections

/*
 * YTM context-menu download (rewritten from the Spotify tippy-menu version).
 *
 * Same architecture, YTM selectors:
 * - The track (videoId/title/artists/album/cover) is harvested from the
 *   ytmusic-responsive-list-item-renderer under the pointer at menu-open
 *   time, via capture-phase listeners on contextmenu and menu-trigger
 *   clicks. A menu opened without a captured row never gets the item, so a
 *   stale track can never leak into the wrong menu (account menus, nested
 *   submenus).
 * - YTM menus render as ytmusic-menu-renderer inside ytmusic-popup-container.
 *   The item is built by cloning the first plain entry at runtime so YTM
 *   styling survives deploys; only the label text is swapped to Download.
 * - Clicking it hands the {trackId,title,artist,album,cover} payload to
 *   AndBridge.downloadTrack, then dismisses the menu with Escape.
 *
 * Row-shape assumptions (title link watch?v=, byline runs, thumbnail) are
 * best-effort until re-checked with a signed-in session.
 */

object ContextMenuDownload {
    const val CONTENT = """
        (function(){
            if(window.__mtbCtxDlInit) return;
            window.__mtbCtxDlInit = true;

            window.__mtbMenuTrack = null;

            function mtbVidFromHref(href){
                var m = (href || '').match(/[?&]v=([A-Za-z0-9_-]{11})/);
                return m ? m[1] : null;
            }

            function mtbGrabRowTrack(row){
                if(!row) return null;
                var id = null, link = null;
                try {
                    var links = row.querySelectorAll('a[href*="watch?v="]');
                    for(var i=0;i<links.length;i++){
                        id = mtbVidFromHref(links[i].getAttribute('href'));
                        if(id){ link = links[i]; break; }
                    }
                } catch(e){}
                if(!id) return null;
                var title = '';
                try {
                    var t = row.querySelector('.title-column yt-formatted-string.title, yt-formatted-string.title');
                    if(t) title = (t.getAttribute('title') || t.textContent || '').trim();
                } catch(e){}
                var artists = [];
                try {
                    var by = row.querySelectorAll('.second-title a, .byline a, [class*="byline"] a');
                    for(var j=0;j<by.length;j++){
                        var nm = (by[j].textContent || '').trim();
                        if(nm && artists.indexOf(nm) === -1) artists.push(nm);
                    }
                    if(!artists.length){
                        var sub = row.querySelector('.second-title, .subtitle, .byline');
                        if(sub){
                            var raw = (sub.textContent || '').split('•')[0].trim();
                            if(raw) artists.push(raw);
                        }
                    }
                } catch(e){}
                var cover = '';
                try {
                    var img = row.querySelector('img');
                    if(img && img.src) cover = img.src;
                } catch(e){}
                return { trackId: id, title: title, artist: artists.join(', '), album: '', cover: cover };
            }

            function mtbActiveMenu(){
                var menus = document.querySelectorAll('ytmusic-menu-renderer');
                for(var i=0;i<menus.length;i++){
                    var el = menus[i];
                    try {
                        var r = el.getBoundingClientRect();
                        if(r.width < 2 || r.height < 2) continue;
                        var cs = getComputedStyle(el);
                        if(cs.visibility === 'hidden' || parseFloat(cs.opacity) < 0.02) continue;
                    } catch(e){ continue; }
                    return el;
                }
                return null;
            }

            function mtbDoMenuDownload(track){
                if(!track || !track.trackId){
                    try{ AndBridge.deferMessage('Track not ready'); }catch(e){}
                    return;
                }
                try{
                    AndBridge.downloadTrack(JSON.stringify({
                        trackId: track.trackId,
                        title: track.title || '',
                        artist: track.artist || '',
                        album: track.album || '',
                        cover: track.cover || ''
                    }));
                }catch(e){}
            }

            function mtbCloseMenu(){
                try{
                    document.dispatchEvent(new KeyboardEvent('keydown', {
                        key: 'Escape', code: 'Escape', keyCode: 27, which: 27,
                        bubbles: true, cancelable: true
                    }));
                }catch(e){}
                setTimeout(function(){
                    try{
                        var m = mtbActiveMenu();
                        if(m){
                            var pop = m.closest('ytmusic-popup-container') || m.parentElement;
                            if(pop && pop.parentElement) pop.remove();
                        }
                    }catch(e){}
                }, 250);
            }

            function mtbInjectItem(menu, track){
                if(menu.querySelector('.mtb-menu-dl')) return true;
                var items = menu.querySelectorAll(
                    'ytmusic-menu-navigation-item-renderer, ytmusic-menu-service-item-renderer, ' +
                    'ytmusic-toggle-menu-service-item-renderer, [role="menuitem"]');
                var src = null;
                for(var i=0;i<items.length;i++){
                    if(items[i].querySelector('.mtb-menu-dl')) continue;
                    src = items[i];
                    break;
                }
                if(!src) return false;
                var clone = src.cloneNode(true);
                clone.classList.add('mtb-menu-dl');
                clone.removeAttribute('id');
                var label = clone.querySelector('yt-formatted-string, [class*="text"], span');
                if(label) label.textContent = 'Download';
                clone.setAttribute('aria-label', 'Download');
                clone.addEventListener('click', function(e){
                    e.preventDefault();
                    e.stopPropagation();
                    mtbDoMenuDownload(track);
                    mtbCloseMenu();
                }, true);
                src.parentElement.insertBefore(clone, src.parentElement.firstChild);
                return true;
            }

            function mtbTryInject(attempt){
                if(attempt > 25) return;
                if(window.__mtbBg) return;
                var t = window.__mtbMenuTrack;
                if(!t || !t.trackId) return;
                var menu = mtbActiveMenu();
                if(!menu){
                    setTimeout(function(){ mtbTryInject(attempt+1); }, 120);
                    return;
                }
                if(menu.querySelector('.mtb-menu-dl')) return;
                if(!mtbInjectItem(menu, t)){
                    setTimeout(function(){ mtbTryInject(attempt+1); }, 120);
                }
            }

            function mtbCaptureFromEvent(e){
                var row = null;
                try{
                    if(e.target && e.target.closest){
                        row = e.target.closest('ytmusic-responsive-list-item-renderer');
                    }
                }catch(err){}
                window.__mtbMenuTrack = mtbGrabRowTrack(row);
            }

            document.addEventListener('contextmenu', mtbCaptureFromEvent, true);

            document.addEventListener('click', function(e){
                var target = e.target;
                if(!target || !target.closest) return;
                var trig = null;
                try{
                    trig = target.closest('yt-icon-button, button[aria-label]');
                }catch(err){}
                if(!trig) return;
                var lbl = '';
                try{ lbl = (trig.getAttribute('aria-label') || ''); }catch(err){}
                if(/more|menu|options/i.test(lbl)) mtbCaptureFromEvent(e);
            }, true);

            var obsPending = false;
            var obs = new MutationObserver(function(muts){
                if(window.__mtbBg) return;
                var hit = false;
                for(var i=0;i<muts.length && !hit;i++){
                    var a = muts[i].addedNodes;
                    for(var j=0;j<a.length;j++){
                        var n = a[j];
                        if(n.nodeType !== 1) continue;
                        if(n.tagName === 'YTMUSIC-MENU-RENDERER' || (n.querySelector && n.querySelector('ytmusic-menu-renderer'))){
                            hit = true;
                            break;
                        }
                    }
                }
                if(hit && !obsPending){
                    obsPending = true;
                    setTimeout(function(){
                        obsPending = false;
                        mtbTryInject(0);
                    }, 60);
                }
            });
            try { obs.observe(document.body, { childList: true, subtree: true }); } catch(e){}

            var mtbLastHover = 0;
            document.addEventListener('mouseover', function(e){
                if(Date.now() - mtbLastHover < 150) return;
                try{
                    if(e.target && e.target.closest && e.target.closest('ytmusic-menu-renderer')){
                        mtbLastHover = Date.now();
                        mtbTryInject(0);
                    }
                }catch(err){}
            }, true);
        })();
    """
}
