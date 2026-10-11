package com.musetube.app.webview.injections

/*
 * YTM collection download (rewritten from the Spotify tracklist-row version).
 *
 * Same pipeline, YTM selectors:
 * - Pages: /playlist?list= (incl. list=LM liked), /browse/MPRE albums,
 *   /channel/UC artists. Rows are ytmusic-responsive-list-item-renderer;
 *   the videoId comes from the row's watch?v= link.
 * - Virtualized shelves lazy-load on scroll: loadAll() scrolls the page
 *   until the row count stabilizes (same stable-count pattern as before).
 * - Native Download buttons can't be reliably hijacked on YTM (no stable
 *   download affordance on free), so this adds its own fixed-position
 *   download-all button on collection pages, plus the skip/cancel cluster
 *   (same mtb-dl-skip-btn / mtb-dl-cancel-btn ids DownloadProgress drives).
 * - Payload keeps the {type,name,cover,tracks[]} shape; each track keeps
 *   {trackId,title,artist,album,cover} with trackId = videoId.
 *
 * Row-shape assumptions are best-effort until re-checked signed-in.
 */

object CollectionDownload {
    const val CONTENT = """
        (function(){
            if (window.__mtbColDlInit) return;
            window.__mtbColDlInit = true;

            var SVG_DL = '<svg viewBox="0 0 16 16" width="24" height="24"><path fill="currentColor" d="M8 1a1 1 0 0 1 1 1v6.586l2.293-2.293a1 1 0 1 1 1.414 1.414l-4 4a1 1 0 0 1-1.414 0l-4-4a1 1 0 1 1 1.414-1.414L7 8.586V2a1 1 0 0 1 1-1zM2 13a1 1 0 0 1 1-1h10a1 1 0 1 1 0 2H3a1 1 0 0 1-1-1z"/></svg>';
            var SVG_SKIP = '<svg viewBox="0 0 16 16" width="24" height="24"><path fill="currentColor" d="M3.3 1a.7.7 0 0 1 .7.7v5.15l9.95-5.744a.7.7 0 0 1 1.05.606v12.575a.7.7 0 0 1-1.05.607L4 9.149V14.3a.7.7 0 0 1-.7.7H1.7a.7.7 0 0 1-.7-.7V1.7a.7.7 0 0 1 .7-.7h1.6z"/></svg>';
            var SVG_X = '<svg viewBox="0 0 16 16" width="24" height="24"><path fill="currentColor" d="M2.47 2.47a.75.75 0 0 1 1.06 0L8 6.94l4.47-4.47a.75.75 0 1 1 1.06 1.06L9.06 8l4.47 4.47a.75.75 0 1 1-1.06 1.06L8 9.06l-4.47 4.47a.75.75 0 0 1-1.06-1.06L6.94 8 2.47 3.53a.75.75 0 0 1 0-1.06z"/></svg>';

            var ROW_SEL = 'ytmusic-responsive-list-item-renderer';
            var VID_RE = /[?&]v=([A-Za-z0-9_-]{11})/;

            function pageType(){
                var href = location.href;
                var path = location.pathname;
                if(href.indexOf('list=LM') !== -1) return 'liked';
                if(path.indexOf('/playlist') !== -1 || href.indexOf('list=') !== -1) return 'playlist';
                if(path.indexOf('/browse/MPRE') !== -1 || path.indexOf('MPRE') !== -1) return 'album';
                if(path.indexOf('/channel/') !== -1) return 'artist';
                return null;
            }

            function findScroller(){
                var el = document.scrollingElement || document.documentElement;
                if(el && el.scrollHeight > el.clientHeight + 100) return el;
                var cands = document.querySelectorAll('ytmusic-playlist-shelf-renderer, ytmusic-shelf-renderer, #contents');
                for(var i=0;i<cands.length;i++){
                    var c = cands[i];
                    if(c.scrollHeight > c.clientHeight + 100) return c;
                }
                return el;
            }

            function findCover(){
                var c = '';
                try {
                    var og = document.querySelector('meta[property="og:image"]');
                    if(og) c = og.getAttribute('content') || '';
                } catch(e){}
                if(!c){
                    try {
                        var h = document.querySelector('ytmusic-detail-header-renderer img, ytmusic-responsive-header-renderer img');
                        if(h && h.src) c = h.src;
                    } catch(e){}
                }
                return c;
            }

            function headerName(fallback){
                var n = '';
                try {
                    var h = document.querySelector('ytmusic-detail-header-renderer .title, ytmusic-responsive-header-renderer .title, h2.title');
                    if(h) n = (h.textContent || '').trim();
                } catch(e){}
                return n || fallback;
            }

            function loadAll(cb){
                var sc = findScroller();
                var top = 0;
                try { top = sc.scrollTop; } catch(e){}
                var last = -1, stable = 0, iter = 0;
                var iv = setInterval(function(){
                    iter++;
                    try {
                        if(sc.scrollTo) sc.scrollTo(0, sc.scrollHeight);
                        else sc.scrollTop = sc.scrollHeight;
                    } catch(e){}
                    try { window.dispatchEvent(new Event('scroll')); } catch(e){}
                    var c = document.querySelectorAll(ROW_SEL).length;
                    if(c === last) stable++; else stable = 0;
                    last = c;
                    if(stable >= 5 || iter >= 90){
                        clearInterval(iv);
                        try {
                            if(sc.scrollTo) sc.scrollTo(0, top);
                            else sc.scrollTop = top;
                        } catch(e){}
                        setTimeout(function(){ cb(); }, 300);
                    }
                }, 250);
            }

            function scrapeRow(row, albumFallback){
                var id = null;
                try {
                    var links = row.querySelectorAll('a[href*="watch?v="]');
                    for(var i=0;i<links.length;i++){
                        var m = VID_RE.exec(links[i].getAttribute('href') || '');
                        if(m){ id = m[1]; break; }
                    }
                } catch(e){}
                if(!id) return null;
                var title = '';
                try {
                    var t = row.querySelector('.title-column yt-formatted-string.title, yt-formatted-string.title');
                    if(t) title = (t.getAttribute('title') || t.textContent || '').trim();
                } catch(e){}
                var arts = [];
                try {
                    var by = row.querySelectorAll('.second-title a, .byline a, [class*="byline"] a');
                    for(var j=0;j<by.length;j++){
                        var nm = (by[j].textContent || '').trim();
                        if(nm && arts.indexOf(nm) === -1) arts.push(nm);
                    }
                    if(!arts.length){
                        var sub = row.querySelector('.second-title, .subtitle, .byline');
                        if(sub){
                            var raw = (sub.textContent || '').split('•')[0].trim();
                            if(raw) arts.push(raw);
                        }
                    }
                } catch(e){}
                var cover = '';
                try {
                    var img = row.querySelector('img');
                    if(img && img.src) cover = img.src;
                } catch(e){}
                return { trackId: id, title: title, artist: arts.join(', '), album: albumFallback || '', cover: cover };
            }

            function scrape(albumFallback){
                var rows = document.querySelectorAll(ROW_SEL);
                var seen = {}, out = [];
                for(var i=0;i<rows.length;i++){
                    var t = scrapeRow(rows[i], albumFallback);
                    if(!t || seen[t.trackId]) continue;
                    seen[t.trackId] = true;
                    out.push(t);
                }
                return out;
            }

            function busy(){
                return !!window.__mtbColBusy;
            }

            function onNativeDownload(){
                if(busy()) return;
                window.__mtbColBusy = true;
                var type = pageType();
                if(!type){ window.__mtbColBusy = false; return; }
                var name = headerName(type === 'liked' ? 'Liked Songs' : 'Collection');
                var albumFallback = (type === 'album') ? name : '';
                try {
                    if(typeof window.mtbDownloadProgress === 'function'){
                        window.mtbDownloadProgress(0, 'Loading tracklist...');
                    }
                } catch(e){}
                loadAll(function(){
                    var tracks = scrape(albumFallback);
                    window.__mtbColBusy = false;
                    if(!tracks.length){
                        try { AndBridge.deferMessage('No downloadable tracks found'); } catch(e){}
                        return;
                    }
                    var payload = { type: type, name: name, cover: findCover(), tracks: tracks };
                    try {
                        AndBridge.downloadCollection(JSON.stringify(payload));
                    } catch(e2) {
                        try { AndBridge.deferMessage('Download failed'); } catch(e3){}
                    }
                });
            }

            function fabStyle(){
                return 'display:inline-flex;align-items:center;justify-content:center;width:48px;height:48px;' +
                    'background:rgba(24,24,24,.92);border:1px solid rgba(255,255,255,.14);border-radius:50%;' +
                    'color:#fff;cursor:pointer;margin-left:8px;flex-shrink:0;';
            }

            function ensureButtons(){
                var type = pageType();
                var dl = document.getElementById('mtb-dl-all-btn');
                if(type && !dl){
                    var host = document.querySelector('ytmusic-detail-header-renderer, ytmusic-responsive-header-renderer, ytmusic-player-bar');
                    if(host){
                        var b = document.createElement('button');
                        b.id = 'mtb-dl-all-btn';
                        b.type = 'button';
                        b.title = 'Download all';
                        b.setAttribute('aria-label', 'Download all tracks');
                        b.style.cssText = fabStyle();
                        b.innerHTML = SVG_DL;
                        b.addEventListener('click', function(e){ e.stopPropagation(); onNativeDownload(); });
                        try { host.appendChild(b); } catch(e){}
                    }
                } else if(!type && dl){
                    try { dl.remove(); } catch(e){}
                }
                var ctl = document.getElementById('mtb-dl-ctl');
                if(!ctl){
                    var c = document.createElement('div');
                    c.id = 'mtb-dl-ctl';
                    c.style.cssText = 'position:fixed;bottom:92px;right:12px;z-index:2147483647;display:none;';
                    c.innerHTML = '<button id="mtb-dl-skip-btn" title="Skip current download" aria-label="Skip current download" style="' + fabStyle() + 'margin-left:0;margin-bottom:8px;">' + SVG_SKIP + '</button>' +
                        '<button id="mtb-dl-cancel-btn" title="Cancel download" aria-label="Cancel download" style="' + fabStyle() + 'margin-left:0;">' + SVG_X + '</button>';
                    try { document.body.appendChild(c); } catch(e){}
                    var sk = document.getElementById('mtb-dl-skip-btn');
                    if(sk) sk.addEventListener('click', function(){ try{ AndBridge.skipDownload(); }catch(e){} });
                    var ca = document.getElementById('mtb-dl-cancel-btn');
                    if(ca) ca.addEventListener('click', function(){ try{ AndBridge.cancelDownload(); }catch(e){} });
                }
            }

            function syncButtons(){
                var act = !!window.__mtbDlActive;
                var ctl = document.getElementById('mtb-dl-ctl');
                if(ctl) ctl.style.display = act ? 'block' : 'none';
            }

            function tick(){
                if(window.__mtbBg) return;
                try { ensureButtons(); } catch(e){}
                try { syncButtons(); } catch(e){}
            }
            setInterval(tick, 2000);
            tick();
        })();
    """
}
