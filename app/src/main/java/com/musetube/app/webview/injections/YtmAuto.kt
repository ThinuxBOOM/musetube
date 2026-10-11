package com.musetube.app.webview.injections

/*
 * YTM Android Auto backend (replaces AndroidAuto).
 *
 * Same native contract, YouTube Music transport:
 *  - window.fetchMediaItems(parentId) -> AndBridge.onMediaItemsLoaded(parentId, json)
 *  - window.searchMediaItems(query)   -> AndBridge.onSearchCompleted(query, json)
 *  Items: {id, name, image, artists[], browsable, type?}.
 *
 * Transport: POST music.youtube.com/youtubei/v1/{browse,search} through
 * AndBridge.nFetch (native fetch carries the WebView cookie jar), authed
 * with SAPISIDHASH built from the SAPISID cookie visible to page JS.
 * Request context prefers the live page ytcfg INNERTUBE_CONTEXT (client
 * versions drift); a WEB_REMIX fallback is baked in.
 *
 * Roots: playlists/albums/artists/podcasts -> library browseIds. Drill-down
 * ids (VL playlists, MPRE albums, UC artists, FEmusic browse nodes, raw
 * videoIds) are browsed generically: song shelves become playable tracks,
 * Everything is defensive — any failure reports '[]' like before.
 *
 * Assumptions to verify on-device with a signed-in session: the four library
 * browseIds below, and the liked-songs LM browse shape.
 */

object YtmAuto {
    const val CONTENT = """
            window.mtbSha1 = window.mtbSha1 || function(s){
                function rl(n,c){ return (n<<c)|(n>>>(32-c)); }
                var ml = s.length, words = [], i, j;
                for(i=0;i<ml;i++){ words[i>>2] |= (s.charCodeAt(i)&0xff) << (24-(i%4)*8); }
                words[ml>>2] |= 0x80 << (24-(ml%4)*8);
                words[((ml+8)>>6)*16+15] = ml*8;
                var h0=0x67452301,h1=0xEFCDAB89,h2=0x98BADCFE,h3=0x10325476,h4=0xC3D2E1F0;
                var w = new Array(80), a,b,c,d,e,t;
                for(j=0;j<words.length;j+=16){
                    a=h0;b=h1;c=h2;d=h3;e=h4;
                    for(i=0;i<80;i++){
                        if(i<16) w[i]=words[j+i]|0;
                        else w[i]=rl(w[i-3]^w[i-8]^w[i-14]^w[i-16],1);
                        if(i<20) t=(rl(a,5)+((b&c)|(~b&d))+e+w[i]+0x5A827999)|0;
                        else if(i<40) t=(rl(a,5)+(b^c^d)+e+w[i]+0x6ED9EBA1)|0;
                        else if(i<60) t=(rl(a,5)+((b&c)|(b&d)|(c&d))+e+w[i]+0x8F1BBCDC)|0;
                        else t=(rl(a,5)+(b^c^d)+e+w[i]+0xCA62C1D6)|0;
                        e=d;d=c;c=rl(b,30);b=a;a=t;
                    }
                    h0=(h0+a)|0;h1=(h1+b)|0;h2=(h2+c)|0;h3=(h3+d)|0;h4=(h4+e)|0;
                }
                function hex(n){ var s=''; for(var k=28;k>=0;k-=4) s+='0123456789abcdef'[(n>>k)&15]; return s; }
                return (hex(h0)+hex(h1)+hex(h2)+hex(h3)+hex(h4)).toLowerCase();
            };
            window.mtbSapisid = function(){
                try {
                    var m = /(?:^|;\s*)SAPISID=([^;]+)/.exec(document.cookie || '');
                    return m ? m[1] : null;
                } catch(e){ return null; }
            };
            window.mtbYtContext = function(){
                try {
                    if(window.ytcfg) {
                        if(window.ytcfg.data_ && window.ytcfg.data_.INNERTUBE_CONTEXT) return window.ytcfg.data_.INNERTUBE_CONTEXT;
                        if(typeof window.ytcfg.get === 'function') {
                            var c = window.ytcfg.get('INNERTUBE_CONTEXT');
                            if(c) return c;
                        }
                    }
                } catch(e){}
                return { client: { clientName: 'WEB_REMIX', clientVersion: '1.20260213.01.00', hl: 'en', gl: 'US' } };
            };
            window.mtbYtPost = function(endpoint, payload){
                var url = 'https://music.youtube.com/youtubei/v1/' + endpoint + '?prettyPrint=false';
                var ctx = window.mtbYtContext();
                var body = payload || {};
                body.context = ctx;
                var headers = { 'Content-Type': 'application/json', 'X-Goog-Api-Format-Version': '1', 'X-Origin': 'https://music.youtube.com', 'Referer': 'https://music.youtube.com/' };
                try {
                    if(ctx.client && ctx.client.clientVersion) headers['X-YouTube-Client-Version'] = ctx.client.clientVersion;
                } catch(e){}
                headers['X-YouTube-Client-Name'] = '67';
                try {
                    var sap = window.mtbSapisid();
                    if(sap) {
                        var t = Math.floor(Date.now()/1000);
                        headers['Authorization'] = 'SAPISIDHASH ' + t + '_' + window.mtbSha1(t + ' ' + sap + ' https://music.youtube.com');
                    }
                } catch(e){}
                var raw = AndBridge.nFetch(url, JSON.stringify({ method: 'POST', headers: headers, body: JSON.stringify(body) }));
                var data = JSON.parse(raw);
                if(!data || data.status === 0) throw new Error('network error: ' + (data ? data.body : 'unknown'));
                if(data.status < 200 || data.status >= 300) throw new Error(endpoint + ' HTTP ' + data.status);
                return JSON.parse(data.body || '{}');
            };
            window.mtbThumbs = function(t){
                try {
                    var arr = t && t.thumbnails;
                    if(arr && arr.length) return arr[arr.length - 1].url || null;
                } catch(e){}
                return null;
            };
            window.mtbRunsText = function(runs){
                try {
                    return (runs || []).map(function(r){ return r.text || ''; }).join('');
                } catch(e){ return ''; }
            };
            window.mtbFlexText = function(item, idx){
                try {
                    var cols = item.flexColumns || [];
                    var col = cols[idx] && cols[idx].musicResponsiveListItemFlexColumnRenderer;
                    var runs = col && col.text && col.text.runs;
                    return window.mtbRunsText(runs);
                } catch(e){ return ''; }
            };
            window.mtbArtistsFromFlex = function(item){
                var names = [];
                try {
                    var cols = item.flexColumns || [];
                    var col = cols[1] && cols[1].musicResponsiveListItemFlexColumnRenderer;
                    var runs = (col && col.text && col.text.runs) || [];
                    for(var i=0;i<runs.length;i++){
                        var r = runs[i];
                        if(!r.text || r.text === ' • ') continue;
                        if(/^\d+:\d+$/.test(r.text)) continue;
                        var ep = r.navigationEndpoint;
                        if(ep && ep.browseEndpoint && ep.browseEndpoint.browseId &&
                           ep.browseEndpoint.browseId.indexOf('UC') === 0) {
                            names.push(r.text);
                        }
                    }
                    if(!names.length) {
                        var fallback = window.mtbFlexText(item, 1).split(' • ')[0].trim();
                        if(fallback) names.push(fallback);
                    }
                } catch(e){}
                return names;
            };
            window.mtbEndpointId = function(ep){
                // returns {id, kind} where kind is video|playlist|browse|channel
                if(!ep) return null;
                try {
                    if(ep.watchEndpoint && ep.watchEndpoint.videoId) {
                        if(ep.watchEndpoint.playlistId) return { id: ep.watchEndpoint.playlistId, kind: 'playlist' };
                        return { id: ep.watchEndpoint.videoId, kind: 'video' };
                    }
                    if(ep.browseEndpoint && ep.browseEndpoint.browseId) {
                        var b = ep.browseEndpoint.browseId;
                        var kind = 'browse';
                        if(b.indexOf('UC') === 0) kind = 'channel';
                        else if(b.indexOf('MPRE') === 0) kind = 'album';
                        else if(b.indexOf('VL') === 0 || b === 'LM') kind = 'playlist';
                        return { id: b, kind: kind };
                    }
                } catch(e){}
                return null;
            };
            window.mtbPlayEndpoint = function(item){
                try {
                    var ov = item.overlay && item.overlay.musicItemThumbnailOverlayRenderer &&
                             item.overlay.musicItemThumbnailOverlayRenderer.content;
                    if(ov && ov.musicPlayButtonRenderer && ov.musicPlayButtonRenderer.playNavigationEndpoint)
                        return ov.musicPlayButtonRenderer.playNavigationEndpoint;
                } catch(e){}
                return null;
            };
            window.mtbParseResponsive = function(r){
                if(!r) return null;
                var title = window.mtbFlexText(r, 0) || 'Unknown';
                var artists = window.mtbArtistsFromFlex(r);
                var image = null;
                try { image = window.mtbThumbs(r.thumbnail && r.thumbnail.musicThumbnailRenderer && r.thumbnail.musicThumbnailRenderer.thumbnail); } catch(e){}
                var ep = window.mtbPlayEndpoint(r);
                if(!ep && r.navigationEndpoint) ep = r.navigationEndpoint;
                if(!ep) {
                    try {
                        var cols = r.flexColumns || [];
                        var runs = (cols[0] && cols[0].musicResponsiveListItemFlexColumnRenderer &&
                                    cols[0].musicResponsiveListItemFlexColumnRenderer.text.runs) || [];
                        for(var i=0;i<runs.length;i++){
                            if(runs[i].navigationEndpoint) { ep = runs[i].navigationEndpoint; break; }
                        }
                    } catch(e){}
                }
                var target = window.mtbEndpointId(ep);
                if(!target) return null;
                var browsable = target.kind !== 'video';
                var type = target.kind === 'video' ? 'Song' :
                           target.kind === 'channel' ? 'Artist' :
                           target.kind === 'album' ? 'Album' : 'Playlist';
                return { id: target.id, name: title, image: image, type: type, artists: artists, browsable: browsable };
            };
            window.mtbParseTwoRow = function(r){
                if(!r) return null;
                var title = '', artists = [], image = null;
                try { title = window.mtbRunsText(r.title && r.title.runs) || 'Unknown'; } catch(e){}
                try {
                    var runs = (r.subtitle && r.subtitle.runs) || [];
                    for(var i=0;i<runs.length;i++){
                        var t = runs[i].text;
                        if(!t || t === ' • ') continue;
                        if(/^\d+:\d+$/.test(t)) continue;
                        var ep = runs[i].navigationEndpoint;
                        if(ep && ep.browseEndpoint && ep.browseEndpoint.browseId &&
                           ep.browseEndpoint.browseId.indexOf('UC') === 0) artists.push(t);
                    }
                } catch(e){}
                try { image = window.mtbThumbs(r.thumbnailRenderer && r.thumbnailRenderer.musicThumbnailRenderer && r.thumbnailRenderer.musicThumbnailRenderer.thumbnail); } catch(e){}
                var target = window.mtbEndpointId(r.navigationEndpoint);
                if(!target) return null;
                var browsable = target.kind !== 'video';
                var type = target.kind === 'video' ? 'Song' :
                           target.kind === 'channel' ? 'Artist' :
                           target.kind === 'album' ? 'Album' : 'Playlist';
                return { id: target.id, name: title || 'Unknown', image: image, type: type, artists: artists, browsable: browsable };
            };
            window.mtbShelves = function(resp){
                var out = [];
                try {
                    var tabs = resp.contents && resp.contents.singleColumnBrowseResultsRenderer &&
                               resp.contents.singleColumnBrowseResultsRenderer.tabs;
                    if(resp.contents && resp.contents.tabbedSearchResultsRenderer)
                        tabs = resp.contents.tabbedSearchResultsRenderer.tabs;
                    var tab = (tabs && tabs[0] && tabs[0].tabRenderer) || null;
                    var sections = (tab && tab.content && tab.content.sectionListRenderer &&
                                    tab.content.sectionListRenderer.contents) || [];
                    for(var s=0;s<sections.length;s++){
                        var sec = sections[s];
                        var shelf = sec.musicShelfRenderer || sec.musicPlaylistShelfRenderer || null;
                        if(shelf) {
                            var items = shelf.contents || [];
                            for(var i=0;i<items.length;i++){
                                var it = window.mtbParseResponsive(items[i].musicResponsiveListItemRenderer);
                                if(it) out.push(it);
                            }
                            continue;
                        }
                        var car = sec.musicCarouselShelfRenderer;
                        if(car) {
                            var citems = car.contents || [];
                            for(var j=0;j<citems.length;j++){
                                var c = citems[j];
                                var parsed = c.musicTwoRowItemRenderer ? window.mtbParseTwoRow(c.musicTwoRowItemRenderer)
                                         : (c.musicResponsiveListItemRenderer ? window.mtbParseResponsive(c.musicResponsiveListItemRenderer) : null);
                                if(parsed) out.push(parsed);
                            }
                            continue;
                        }
                        var grid = sec.gridRenderer;
                        if(grid) {
                            var gitems = grid.items || [];
                            for(var k=0;k<gitems.length;k++){
                                var g = window.mtbParseTwoRow(gitems[k].musicTwoRowItemRenderer);
                                if(g) out.push(g);
                            }
                            continue;
                        }
                        var card = sec.musicCardShelfRenderer;
                        if(card) {
                            var ct = window.mtbRunsText(card.title && card.title.runs);
                            var ctg = window.mtbEndpointId(card.title && card.title.runs && card.title.runs[0] && card.title.runs[0].navigationEndpoint);
                            if(ct && ctg) out.push({ id: ctg.id, name: ct, image: null, type: 'Playlist', artists: [], browsable: ctg.kind !== 'video' });
                        }
                    }
                } catch(e){}
                return out;
            };
            var YTM_ROOTS = { playlists: 'FEmusic_library_landing', albums: 'FEmusic_library_corpus_albums', artists: 'FEmusic_library_corpus_artists', podcasts: 'FEmusic_library_corpus_podcasts' };
            async function fetchMediaItems(parentId) {
                try {
                    var browseId = YTM_ROOTS[parentId] || null;
                    if(!browseId) {
                        if(/^[A-Za-z0-9_-]{11}$/.test(parentId)) browseId = null;
                        else browseId = parentId;
                    }
                    var items;
                    if(browseId) {
                        var resp = window.mtbYtPost('browse', { browseId: browseId });
                        items = window.mtbShelves(resp);
                        if(parentId === 'playlists') {
                            items = items.filter(function(it){ return it.browsable; });
                            items.forEach(function(it){ it.isGrid = true; });
                        } else {
                            items.forEach(function(it){ it.isGrid = true; });
                        }
                    } else {
                        items = [];
                    }
                    AndBridge.onMediaItemsLoaded(parentId, JSON.stringify(items));
                } catch (e) {
                    try { AndBridge.dbg('w', 'fetchMediaItems ' + parentId + ': ' + e); } catch(_){}
                    AndBridge.onMediaItemsLoaded(parentId, '[]');
                }
            }
            window.fetchMediaItems = fetchMediaItems;
            async function searchMediaItems(query) {
                try {
                    var resp = window.mtbYtPost('search', { query: query });
                    var all = window.mtbShelves(resp);
                    var seen = {}, results = [];
                    var songs = [], albums = [], playlists = [], artists = [];
                    for(var i=0;i<all.length;i++){
                        var it = all[i];
                        if(!it || !it.id || seen[it.id]) continue;
                        seen[it.id] = 1;
                        if(it.browsable === false) songs.push(it);
                        else if(it.type === 'Album') albums.push(it);
                        else if(it.type === 'Artist') artists.push(it);
                        else playlists.push(it);
                    }
                    songs.slice(0, 4).forEach(function(x){ results.push(x); });
                    albums.slice(0, 3).forEach(function(x){ results.push(x); });
                    playlists.slice(0, 3).forEach(function(x){ results.push(x); });
                    artists.slice(0, 2).forEach(function(x){ results.push(x); });
                    songs.slice(4).forEach(function(x){ results.push(x); });
                    albums.slice(3).forEach(function(x){ results.push(x); });
                    playlists.slice(3).forEach(function(x){ results.push(x); });
                    artists.slice(2).forEach(function(x){ results.push(x); });
                    AndBridge.onSearchCompleted(query, JSON.stringify(results));
                } catch (e) {
                    try { AndBridge.dbg('w', 'searchMediaItems: ' + e); } catch(_){}
                    AndBridge.onSearchCompleted(query, '[]');
                }
            }
            window.searchMediaItems = searchMediaItems;
        
    """
}
