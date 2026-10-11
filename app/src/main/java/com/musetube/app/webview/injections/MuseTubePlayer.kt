package com.musetube.app.webview.injections
/*
 * CREDIT: MuseTube - Custom Player.
 *
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⡀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣾⠙⠻⢶⣄⡀⠀⠀⠀⢀⣤⠶⠛⠛⡇⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⣇⠀⠀⣙⣿⣦⣤⣴⣿⣁⠀⠀⣸⠇⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠙⣡⣾⣿⣿⣿⣿⣿⣿⣿⣷⣌⠋⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣴⣿⣷⣄⡈⢻⣿⡟⢁⣠⣾⣿⣦⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⣿⣿⣿⣿⠘⣿⠃⣿⣿⣿⣿⡏⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⠀⠈⠛⣰⠿⣆⠛⠁⠀⡀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣼⣿⣦⠀⠘⠛⠋⠀⣴⣿⠁⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⣤⣶⣾⣿⣿⣿⣿⡇⠀⠀⠀⢸⣿⣏⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⣠⣶⣿⣿⣿⣿⣿⣿⣿⣿⠿⠿⠀⠀⠀⠾⢿⣿⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⣠⣿⣿⣿⣿⣿⣿⡿⠟⠋⣁⣠⣤⣤⡶⠶⠶⣤⣄⠈⠀⠀⠀⠀⠀⠀
⠀⠀⠀⢰⣿⣿⣮⣉⣉⣉⣤⣴⣶⣿⣿⣋⡥⠄⠀⠀⠀⠀⠉⢻⣄⠀⠀⠀⠀⠀
⠀⠀⠀⠸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣟⣋⣁⣤⣀⣀⣤⣤⣤⣤⣄⣿⡄⠀⠀⠀⠀
⠀⠀⠀⠀⠙⠿⣿⣿⣿⣿⣿⣿⣿⡿⠿⠛⠋⠉⠁⠀⠀⠀⠀⠈⠛⠃⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠉⠉⠉⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
 */


object MuseTubePlayer {
    const val CONTENT = """
            window.initMuseTubePlayer=function(){
                if(document.getElementById('musetubePlayerControls')) return;
                var npb=document.querySelector('ytmusic-player-bar');
                if(!npb) return;
                npb.style.display='none';

                function mtbFindShuffle(){
                    var bar=document.querySelector('ytmusic-player-bar');
                    if(!bar) return null;
                    var all=bar.querySelectorAll('yt-icon-button, button');
                    for(var i=0;i<all.length;i++){
                        var el=all[i].querySelector('button')||all[i];
                        var al=(all[i].getAttribute('aria-label')||'')+' '+(el.getAttribute('aria-label')||'');
                        if(/shuffle/i.test(al)) return el;
                    }
                    return null;
                }
                function mtbFindRepeat(){
                    var bar=document.querySelector('ytmusic-player-bar');
                    if(!bar) return null;
                    var all=bar.querySelectorAll('yt-icon-button, button');
                    for(var i=0;i<all.length;i++){
                        var el=all[i].querySelector('button')||all[i];
                        var al=(all[i].getAttribute('aria-label')||'')+' '+(el.getAttribute('aria-label')||'');
                        if(/^repeat/i.test(al.trim())) return el;
                    }
                    return null;
                }
                function mtbShuffleState(){
                    if(typeof shuffle!=='undefined') return shuffle;
                    var b=mtbFindShuffle();
                    if(!b) return 'off';
                    if(b.getAttribute('aria-pressed')==='true') return 'shuffle';
                    var al=b.getAttribute('aria-label')||'';
                    return /^disable/i.test(al)?'shuffle':'off';
                }

                var pl=document.createElement('div');
                pl.id='musetubePlayerControls';
                pl.innerHTML=''
                    +'<div class="mtb-top">'
                    +'<div class="mtb-cover"><img id="mtb-cover-img" src="" alt=""></div>'
                    +'<div class="mtb-info"><div class="mtb-track" id="mtb-track">No track</div>'
                    +'<div class="mtb-artist" id="mtb-artist">\u2014</div></div>'
                    +'<div class="mtb-mini-transport">'
                    +'<button class="mtb-btn" id="mtb-prev-mini" aria-label="Previous"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M3.3 1a.7.7 0 0 1 .7.7v5.15l9.95-5.744a.7.7 0 0 1 1.05.606v12.575a.7.7 0 0 1-1.05.607L4 9.149V14.3a.7.7 0 0 1-.7.7H1.7a.7.7 0 0 1-.7-.7V1.7a.7.7 0 0 1 .7-.7z"/></svg></button>'
                    +'<button class="mtb-btn mtb-play" id="mtb-play-mini" aria-label="Play"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M3 1.713a.7.7 0 0 1 1.05-.607l10.89 6.288a.7.7 0 0 1 0 1.212L4.05 14.894A.7.7 0 0 1 3 14.288z"/></svg></button>'
                    +'<button class="mtb-btn" id="mtb-next-mini" aria-label="Next"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M12.7 1a.7.7 0 0 0-.7.7v5.15L2.05 1.107A.7.7 0 0 0 1 1.712v12.575a.7.7 0 0 0 1.05.607L12 9.149V14.3a.7.7 0 0 0 .7.7h1.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7z"/></svg></button>'
                    +'</div>'
                    +'</div>'
                    +'<div class="mtb-row2">'
                    +'<div class="mtb-actions-left">'
                    +'<button class="mtb-btn mtb-btn-sm" id="mtb-timer" aria-label="Timer"><svg viewBox="0 0 20 20"><path fill="currentColor" d="M16.32 7.1A8 8 0 1 1 9 4.06V2h2v2.06c1.46.18 2.8.76 3.9 1.62l1.46-1.46l1.42 1.42l-1.46 1.45zM10 18a6 6 0 1 0 0-12a6 6 0 0 0 0 12zM7 0h6v2H7V0zm5.12 8.46l1.42 1.42L10 13.4L8.59 12l3.53-3.54z"/></svg></button>'
                    +'<button class="mtb-btn mtb-btn-sm" id="mtb-pip" aria-label="Picture in Picture"><svg viewBox="0 0 24 24"><path fill="currentColor" d="M19 11h-8v6h8v-6zm4 8V4.98C23 3.88 22.1 3 21 3H3c-1.1 0-2 .88-2 1.98V19c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2zm-2 .02H3V4.97h18v14.05z"/></svg></button>'
                    +'<button class="mtb-btn mtb-btn-sm" id="mtb-nptoggle" aria-label="Now Playing"><svg viewBox="0 0 16 17"><rect x="1" y="0.75" width="14" height="15.5" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M 6 5 L 6 5.9160156 L 9.6933594 8.5 L 6 11.080078 L 6 12 L 11 8.5 L 6 5 z" stroke="currentColor" stroke-width="1.2"/></svg></button>'
                    +'<button class="mtb-btn mtb-btn-sm" id="mtb-lyrics" aria-label="Lyrics"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M13.426 2.574a2.831 2.831 0 0 0-4.797 1.55l3.247 3.247a2.831 2.831 0 0 0 1.55-4.797M10.5 8.118l-2.619-2.62L4.74 9.075 2.065 12.12a1.287 1.287 0 0 0 1.816 1.816l3.06-2.688 3.56-3.129zM7.12 4.094a4.331 4.331 0 1 1 4.786 4.786l-3.974 3.493-3.06 2.689a2.787 2.787 0 0 1-3.933-3.933l2.676-3.045z"/></svg></button>'
                    +'<button class="mtb-btn mtb-btn-sm" id="mtb-queue" aria-label="Queue"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M15 15H1v-1.5h14zm0-4.5H1V9h14zm-14-7A2.5 2.5 0 0 1 3.5 1h9a2.5 2.5 0 0 1 0 5h-9A2.5 2.5 0 0 1 1 3.5m2.5-1a1 1 0 0 0 0 2h9a1 1 0 1 0 0-2z"/></svg></button>'
                    +'<button class="mtb-btn mtb-btn-sm" id="mtb-download" aria-label="Download"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M8 1a1 1 0 0 1 1 1v6.586l2.293-2.293a1 1 0 1 1 1.414 1.414l-4 4a1 1 0 0 1-1.414 0l-4-4a1 1 0 1 1 1.414-1.414L7 8.586V2a1 1 0 0 1 1-1zM2 13a1 1 0 0 1 1-1h10a1 1 0 1 1 0 2H3a1 1 0 0 1-1-1z"/></svg></button>'
                    +'<button class="mtb-btn mtb-btn-sm" id="mtb-dl-cancel" aria-label="Cancel download" style="display:none;"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M2.47 2.47a.75.75 0 0 1 1.06 0L8 6.94l4.47-4.47a.75.75 0 1 1 1.06 1.06L9.06 8l4.47 4.47a.75.75 0 1 1-1.06 1.06L8 9.06l-4.47 4.47a.75.75 0 0 1-1.06-1.06L6.94 8 2.47 3.53a.75.75 0 0 1 0-1.06z"/></svg></button>'
                    +'<div class="mtb-vol-wrap" id="mtb-vol">'
                    +'<button class="mtb-btn mtb-btn-sm mtb-vol-btn" id="mtb-vol-btn" aria-label="Volume"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M9.741.85a.75.75 0 0 1 .375.65v13a.75.75 0 0 1-1.125.65l-6.925-4a3.64 3.64 0 0 1-1.33-4.967 3.64 3.64 0 0 1 1.33-1.332l6.925-4a.75.75 0 0 1 .75 0zm-6.924 5.3a2.14 2.14 0 0 0 0 3.7l5.8 3.35V2.8zm8.683 4.29V5.56a2.75 2.75 0 0 1 0 4.88"/><path fill="currentColor" d="M11.5 13.614a5.752 5.752 0 0 0 0-11.228v1.55a4.252 4.252 0 0 1 0 8.127z"/></svg></button>'
                    +'<div class="mtb-vol-bar" id="mtb-vol-bar"><div class="mtb-vol-track"></div><div class="mtb-vol-fill" id="mtb-vol-fill"></div><div class="mtb-vol-handle" id="mtb-vol-handle"></div></div>'
                    +'</div>'
                    +'</div>'
                    +'<button class="mtb-btn mtb-btn-sm mtb-liked-btn" id="mtb-liked" aria-label="Like"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M15.724 4.22A4.313 4.313 0 0 0 12.192.814a4.269 4.269 0 0 0-3.622 1.13.837.837 0 0 1-1.14 0 4.272 4.272 0 0 0-6.38 5.69l5.4 6.06a1.09 1.09 0 0 0 1.504.06l5.397-5.892a4.32 4.32 0 0 0 1.253-3.436z"/></svg></button>'
                    +'</div>'
                    +'<div class="mtb-bottom">'
                    +'<span class="mtb-time" id="mtb-pos">0:00</span>'
                    +'<div class="mtb-bar-wrap"><div class="mtb-bar" id="mtb-bar"><div class="mtb-fill" id="mtb-fill"></div><div class="mtb-handle" id="mtb-handle"></div></div></div>'
                    +'<span class="mtb-time" id="mtb-dur">0:00</span>'
                    +'</div>'
                    +'<div class="mtb-edgebar" id="mtb-edgebar"><div class="mtb-fill" id="mtb-fill-edge"></div></div>'
                    +'<div class="mtb-transport">'
                    +'<button class="mtb-btn mtb-btn-sm" id="mtb-shuffle" aria-label="Shuffle"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M13.151.922a.75.75 0 1 0-1.06 1.06L13.109 3H11.16a3.75 3.75 0 0 0-2.873 1.34l-6.173 7.356A2.25 2.25 0 0 1 .39 12.5H0V14h.391a3.75 3.75 0 0 0 2.873-1.34l6.173-7.356a2.25 2.25 0 0 1 1.724-.804h1.947l-1.017 1.018a.75.75 0 0 0 1.06 1.06L15.98 3.75zM.391 3.5H0V2h.391c1.109 0 2.16.49 2.873 1.34L4.89 5.277l-.979 1.167-1.796-2.14A2.25 2.25 0 0 0 .39 3.5zm7.758 6.22l.979-1.167 1.35 1.605a2.25 2.25 0 0 0 1.724.804h1.947l-1.017-1.018a.75.75 0 1 1 1.06-1.06l2.829 2.828-2.829 2.828a.75.75 0 1 1-1.06-1.06L13.109 13H11.16a3.75 3.75 0 0 1-2.873-1.34l-1.138-1.94z"/></svg></button>'
                    +'<button class="mtb-btn" id="mtb-prev" aria-label="Previous"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M3.3 1a.7.7 0 0 1 .7.7v5.15l9.95-5.744a.7.7 0 0 1 1.05.606v12.575a.7.7 0 0 1-1.05.607L4 9.149V14.3a.7.7 0 0 1-.7.7H1.7a.7.7 0 0 1-.7-.7V1.7a.7.7 0 0 1 .7-.7z"/></svg></button>'
                    +'<button class="mtb-btn mtb-play" id="mtb-play" aria-label="Play"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M3 1.713a.7.7 0 0 1 1.05-.607l10.89 6.288a.7.7 0 0 1 0 1.212L4.05 14.894A.7.7 0 0 1 3 14.288z"/></svg></button>'
                    +'<button class="mtb-btn" id="mtb-next" aria-label="Next"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M12.7 1a.7.7 0 0 0-.7.7v5.15L2.05 1.107A.7.7 0 0 0 1 1.712v12.575a.7.7 0 0 0 1.05.607L12 9.149V14.3a.7.7 0 0 0 .7.7h1.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7z"/></svg></button>'
                    +'<button class="mtb-btn mtb-btn-sm" id="mtb-repeat" aria-label="Repeat"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M0 4.75A3.75 3.75 0 0 1 3.75 1h8.5A3.75 3.75 0 0 1 16 4.75v5a3.75 3.75 0 0 1-3.75 3.75H9.81l1.018 1.018a.75.75 0 1 1-1.06 1.06L6.939 12.75l2.829-2.828a.75.75 0 1 1 1.06 1.06L9.811 12h2.439a2.25 2.25 0 0 0 2.25-2.25v-5a2.25 2.25 0 0 0-2.25-2.25h-8.5A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75z"/></svg></button>'
                    +'</div>';

                document.body.appendChild(pl);
                if(window.__mtbHideEmpty) pl.classList.add('mtb-empty');
                    document.body.appendChild(pl);
                    
                    window.mtbApplyEmpty=function(){
                        var t=document.getElementById('mtb-track');
                        var empty=!t||!t.textContent||t.textContent==='No track';
                        if(window.__mtbHideEmpty&&empty) pl.classList.add('mtb-empty');
                        else pl.classList.remove('mtb-empty');
                    };

                if(!document.getElementById('mtb-vol-css')){
                    var sst=document.createElement('style');sst.id='mtb-vol-css';
                    sst.textContent='#musetubePlayerControls .mtb-vol-wrap{display:flex;align-items:center;gap:2px;margin-right:2px}#musetubePlayerControls .mtb-vol-btn{flex-shrink:0}#musetubePlayerControls .mtb-vol-bar{position:relative;width:70px;height:38px;display:flex;align-items:center;cursor:pointer;flex-shrink:0;margin:0 2px}#musetubePlayerControls .mtb-vol-track{position:absolute;left:0;right:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:rgba(255,255,255,.14)}#musetubePlayerControls .mtb-vol-fill{position:absolute;left:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:var(--mtb-accent,#ff0000);width:0%}#musetubePlayerControls .mtb-vol-handle{position:absolute;top:50%;left:0%;width:12px;height:12px;transform:translate(-50%,-50%);border-radius:50%;background:#fff;opacity:0;transition:opacity .15s;box-shadow:0 1px 4px rgba(0,0,0,.5);pointer-events:none}#musetubePlayerControls .mtb-vol-bar:hover .mtb-vol-handle,#musetubePlayerControls .mtb-vol-bar:active .mtb-vol-handle{opacity:1}#musetubePlayerControls.mtb-empty{opacity:0!important;pointer-events:none!important;transform:translateY(24px)!important}';
                    var t=document.head||document.documentElement;if(t)t.appendChild(sst);
                }

                if(!document.getElementById('mtb-vol-css')){
                    var sst=document.createElement('style');sst.id='mtb-vol-css';
                    sst.textContent='#musetubePlayerControls .mtb-vol-wrap{display:flex;align-items:center;gap:2px;margin-right:2px}#musetubePlayerControls .mtb-vol-btn{flex-shrink:0}#musetubePlayerControls .mtb-vol-bar{position:relative;width:70px;height:38px;display:flex;align-items:center;cursor:pointer;flex-shrink:0;margin:0 2px}#musetubePlayerControls .mtb-vol-track{position:absolute;left:0;right:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:rgba(255,255,255,.14)}#musetubePlayerControls .mtb-vol-fill{position:absolute;left:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:#ff0000;width:0%}#musetubePlayerControls .mtb-vol-handle{position:absolute;top:50%;left:0%;width:12px;height:12px;transform:translate(-50%,-50%);border-radius:50%;background:#fff;opacity:0;transition:opacity .15s;box-shadow:0 1px 4px rgba(0,0,0,.5);pointer-events:none}#musetubePlayerControls .mtb-vol-bar:hover .mtb-vol-handle,#musetubePlayerControls .mtb-vol-bar:active .mtb-vol-handle{opacity:1}';
                    var t=document.head||document.documentElement;if(t)t.appendChild(sst);
                }

                document.getElementById('mtb-prev').onclick=function(){actSkipBack()};
                document.getElementById('mtb-next').onclick=function(){actSkipForward()};
                document.getElementById('mtb-play').onclick=function(){actPlayPause()};
                document.getElementById('mtb-prev-mini').onclick=function(){actSkipBack()};
                document.getElementById('mtb-next-mini').onclick=function(){actSkipForward()};
                document.getElementById('mtb-play-mini').onclick=function(){actPlayPause()};
                document.getElementById('mtb-shuffle').onclick=function(){actToggleShuffle()};
                document.getElementById('mtb-repeat').onclick=function(){actRepeat()};
                document.getElementById('mtb-lyrics').style.display='none';
                document.getElementById('mtb-queue').onclick=function(){clickNP()};
                document.getElementById('mtb-vol-btn').onclick=function(){
                    var v=document.querySelector('ytmusic-player video');
                    if(v){ try{ v.muted=!v.muted; }catch(e){} }
                };
                (function(){
                    var vBar=document.getElementById('mtb-vol-bar');
                    function mtbSetVolPct(pct){
                        var v=document.querySelector('ytmusic-player video');
                        if(!v) return;
                        pct=Math.max(0,Math.min(1,pct));
                        try{ v.volume=pct; v.muted=(pct<=0); }catch(e){}
                    }
                    function volTo(e){if(!vBar)return;var r=vBar.getBoundingClientRect();var pct=Math.max(0,Math.min(1,(e.clientX-r.left)/r.width));mtbSetVolPct(pct);}
                    var vDrag=false;
                    vBar.addEventListener('mousedown',function(e){vDrag=true;volTo(e);});
                    vBar.addEventListener('touchstart',function(e){vDrag=true;volTo(e.touches[0]);},{passive:true});
                    document.addEventListener('mousemove',function(e){if(vDrag)volTo(e);});
                    document.addEventListener('touchmove',function(e){if(vDrag)volTo(e.touches[0]);},{passive:true});
                    document.addEventListener('mouseup',function(){vDrag=false;});
                    document.addEventListener('touchend',function(){vDrag=false;});
                    document.addEventListener('touchcancel',function(){vDrag=false;});
                })();
                document.getElementById('mtb-nptoggle').onclick=function(){clickNP()};
                document.getElementById('mtb-timer').onclick=function(){AndBridge.openTimerDialog()};
                document.getElementById('mtb-pip').onclick=function(){
                    var pv=document.querySelector('ytmusic-player video');
                    if(pv){
                        var w=pv.videoWidth||0,h=pv.videoHeight||0;
                        try{pv.requestFullscreen();}catch(e){}
                        AndBridge.enterPipVideo(w,h);
                    } else {
                        AndBridge.enterPip();
                    }
                };
                document.getElementById('mtb-liked').onclick=function(){actAddToFav()};
                document.getElementById('mtb-download').onclick=function(){window.mtbDoDownload()};
                document.getElementById('mtb-dl-cancel').onclick=function(){ try{ AndBridge.cancelDownload(); }catch(e){} };

                var splTrack=document.getElementById('mtb-track');
                var splArtist=document.getElementById('mtb-artist');
                splTrack.style.cursor='pointer';
                splArtist.style.cursor='pointer';
                splTrack.onclick=function(){
                    if(pl.classList.contains('mtb-mini'))return;
                    if(typeof clickNP==='function') clickNP();
                };
                splArtist.onclick=function(){
                    if(pl.classList.contains('mtb-mini'))return;
                    try {
                        var al=document.querySelector('ytmusic-player-bar .byline-wrapper a[href]');
                        if(al){ al.click(); return; }
                    } catch(e){}
                    if(typeof clickNP==='function') clickNP();
                };

                var barEl=document.getElementById('mtb-bar');
                var edgeBarEl=document.getElementById('mtb-edgebar');
                var dragging=false,dragEl=barEl;
                function seekTo(el,e){var r=el.getBoundingClientRect();var pct=Math.max(0,Math.min(1,(e.clientX-r.left)/r.width));var mx=(typeof duration==='number'&&duration>0)?duration:0;actSeek(Math.round(pct*mx))}
                function bindSeek(el){el.addEventListener('mousedown',function(e){dragEl=el;dragging=true;seekTo(el,e)});el.addEventListener('touchstart',function(e){dragEl=el;dragging=true;seekTo(el,e.touches[0])},{passive:true});}
                bindSeek(barEl);bindSeek(edgeBarEl);
                document.addEventListener('mousemove',function(e){if(dragging)seekTo(dragEl,e)});
                document.addEventListener('touchmove',function(e){if(dragging)seekTo(dragEl,e.touches[0])},{passive:true});
                document.addEventListener('mouseup',function(){dragging=false});
                document.addEventListener('touchend',function(){dragging=false});

                var splMini=false;
                var splDrag=null,splSuppressClick=false,splLastDragEnd=0;
                function splSetMini(m){
                    splMini=!!m;
                    window.mtbMiniPref=splMini;
                    pl.classList.toggle('mtb-mini',splMini);
                }
                function splDragStart(x,y){
                    splDrag={sx:x,sy:y,moving:false,dy:0,mini:splMini};
                    pl.style.transition='none';
                }
                function splDragMove(x,y){
                    if(!splDrag)return;
                    var dy=y-splDrag.sy,dx=x-splDrag.sx;
                    if(!splDrag.moving){
                        if(Math.abs(dy)<10||Math.abs(dy)<Math.abs(dx))return;
                        splDrag.moving=true;
                    }
                    splDrag.dy=splDrag.mini?Math.min(0,dy):Math.max(0,dy);
                    pl.style.transform='translateY('+splDrag.dy+'px)';
                    pl.style.opacity=String(Math.max(.7,1-Math.abs(splDrag.dy)/500));
                }
                function splDragEnd(){
                    if(!splDrag)return;
                    var d=splDrag;
                    splDrag=null;
                    pl.style.transition='';
                    pl.style.transform='';
                    pl.style.opacity='';
                    if(d.moving){
                        splSuppressClick=true;
                        splLastDragEnd=Date.now();
                        setTimeout(function(){splSuppressClick=false;},100);
                        if(d.mini){if(d.dy<-70)splSetMini(false);}
                        else{if(d.dy>70)splSetMini(true);}
                    }
                }
                pl.addEventListener('touchstart',function(e){if(e.target.closest('#mtb-bar')||e.target.closest('#mtb-edgebar')||e.target.closest('.mtb-vol-bar'))return;var t=e.touches[0];splDragStart(t.clientX,t.clientY);},{passive:true});
                pl.addEventListener('touchmove',function(e){if(splDrag&&splDrag.moving)e.preventDefault();if(!splDrag)return;var t=e.touches[0];splDragMove(t.clientX,t.clientY);},{passive:false});
                pl.addEventListener('touchend',function(e){if(splDrag&&splDrag.moving)e.preventDefault();splDragEnd();});
                pl.addEventListener('touchcancel',function(){splDragEnd();});
                pl.addEventListener('mousedown',function(e){if(e.button!==0)return;if(e.target.closest('#mtb-bar')||e.target.closest('#mtb-edgebar')||e.target.closest('.mtb-vol-bar')||e.target.closest('button'))return;splDragStart(e.clientX,e.clientY);});
                document.addEventListener('mousemove',function(e){splDragMove(e.clientX,e.clientY);});
                document.addEventListener('mouseup',function(){splDragEnd();});
                pl.addEventListener('click',function(e){if(splSuppressClick)return;if(Date.now()-splLastDragEnd<400)return;if(splMini&&!e.target.closest('button')&&!e.target.closest('#mtb-bar')&&!e.target.closest('#mtb-edgebar'))splSetMini(false);});                    window.mtbUpdate=function(){
                        var ci=document.getElementById('mtb-cover-img');
                        var tk=document.getElementById('mtb-track');
                        var ar=document.getElementById('mtb-artist');
                        var fl=document.getElementById('mtb-fill');
                        var fe=document.getElementById('mtb-fill-edge');
                        var hd=document.getElementById('mtb-handle');
                        var ps=document.getElementById('mtb-pos');
                        var ds=document.getElementById('mtb-dur');
                        var pp=document.getElementById('mtb-play');
                        var ppm=document.getElementById('mtb-play-mini');
                        var sh=document.getElementById('mtb-shuffle');
                        var rp=document.getElementById('mtb-repeat');
                        var vl=document.getElementById('mtb-vol');
                        var lk=document.getElementById('mtb-liked');
                        var ly=document.getElementById('mtb-lyrics');
                        var tm=document.getElementById('mtb-timer');

                        if(ci&&typeof cover!=='undefined'&&cover&&ci.src!==cover) ci.src=cover;
                        var ttxt=(typeof track!=='undefined'&&track)?track:'No track';
                        if(tk&&tk.textContent!==ttxt) tk.textContent=ttxt;
                        var atxt=(typeof artist!=='undefined'&&artist)?artist:'—';
                        if(ar&&ar.textContent!==atxt) ar.textContent=atxt;

                        if(pp||ppm){
                            var isPlaying=!!window.playing;
                            var ph=isPlaying
                                ?'<svg viewBox="0 0 16 16"><path fill="currentColor" d="M2.7 1a.7.7 0 0 0-.7.7v12.6a.7.7 0 0 0 .7.7h2.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7zm8 0a.7.7 0 0 0-.7.7v12.6a.7.7 0 0 0 .7.7h2.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7z"/></svg>'
                                :'<svg viewBox="0 0 16 16"><path fill="currentColor" d="M3 1.713a.7.7 0 0 1 1.05-.607l10.89 6.288a.7.7 0 0 1 0 1.212L4.05 14.894A.7.7 0 0 1 3 14.288z"/></svg>';
                            if(pp)pp.innerHTML=ph;
                            if(ppm)ppm.innerHTML=ph;
                        }
                        if(sh){
                            var sst=(typeof shuffle!=='undefined')?shuffle:'off';
                            sh.classList.toggle('mtb-active',sst==='shuffle'||sst==='smart');
                            sh.classList.toggle('mtb-disabled',sst==='disabled');
                            var isSmart=sst==='smart';
                            var hasSparkle=!!sh.querySelector('.mtb-sparkle');
                            if(isSmart&&!hasSparkle){
                                sh.innerHTML='<svg class="mtb-sparkle" viewBox="0 0 16 16"><path fill="currentColor" d="M4.502 0a.637.637 0 0 1 .634.58 4.84 4.84 0 0 0 .81 2.184c.515.739 1.297 1.356 2.487 1.486a.637.637 0 0 1 0 1.267c-1.19.13-1.972.747-2.487 1.487a4.8 4.8 0 0 0-.81 2.185.637.637 0 0 1-1.268 0 4.8 4.8 0 0 0-.81-2.185C2.543 6.265 1.76 5.648.57 5.518a.637.637 0 0 1 0-1.268c1.19-.13 1.972-.747 2.487-1.486a4.84 4.84 0 0 0 .81-2.185A.637.637 0 0 1 4.502 0m4.765 11.878c.056.065.126.15.198.236l.33.397.013.015A3 3 0 0 0 12.1 13.59h1.009l-.444.443a.75.75 0 0 0 1.061 1.06l2.254-2.253-2.254-2.254a.75.75 0 0 0-1.06 1.06l.443.444H12.1a1.5 1.5 0 0 1-1.146-.533l-.004-.005-.333-.4-.288-.343-.031-.035-.02-.021-.037-.037-.974 1.16Z"/><path fill="currentColor" d="M12.69 4.196a.75.75 0 0 1 1.06 0l2.254 2.254-2.254 2.254a.75.75 0 0 1-1.06-1.06l.443-.444h-1.008a1.5 1.5 0 0 0-1.15.536l-4.63 5.517c-.344.411-.982 1.021-1.822 1.021v-1.5c.122 0 .371-.124.674-.485l4.63-5.517A3 3 0 0 1 12.125 5.7h1.008l-.443-.443a.75.75 0 0 1 0-1.061"/></svg>';
                            } else if(!isSmart&&hasSparkle){
                                sh.innerHTML='<svg viewBox="0 0 16 16"><path fill="currentColor" d="M13.151.922a.75.75 0 1 0-1.06 1.06L13.109 3H11.16a3.75 3.75 0 0 0-2.873 1.34l-6.173 7.356A2.25 2.25 0 0 1 .39 12.5H0V14h.391a3.75 3.75 0 0 0 2.873-1.34l6.173-7.356a2.25 2.25 0 0 1 1.724-.804h1.947l-1.017 1.018a.75.75 0 0 0 1.06 1.06L15.98 3.75zM.391 3.5H0V2h.391c1.109 0 2.16.49 2.873 1.34L4.89 5.277l-.979 1.167-1.796-2.14A2.25 2.25 0 0 0 .39 3.5zm7.758 6.22l.979-1.167 1.35 1.605a2.25 2.25 0 0 0 1.724.804h1.947l-1.017-1.018a.75.75 0 1 1 1.06-1.06l2.829 2.828-2.829 2.828a.75.75 0 1 1-1.06-1.06L13.109 13H11.16a3.75 3.75 0 0 1-2.873-1.34l-1.138-1.94z"/></svg>';
                            }
                        }
                        if(rp){
                            var rc=(typeof repmode!=='undefined')?repmode:'false';
                            var rDisabled=false;
                            rp.classList.toggle('mtb-active',rc==='true'||rc==='mixed');
                            rp.classList.toggle('mtb-disabled',rDisabled);
                            rp.classList.toggle('mtb-repeat-track',rc==='mixed');
                            if(rc==='mixed'&&!rp.getAttribute('data-rt')){
                                rp.setAttribute('data-rt','1');
                                rp.innerHTML='<svg viewBox="0 0 16 16"><path fill="currentColor" d="M0 4.75A3.75 3.75 0 0 1 3.75 1h.75v1.5h-.75A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75zM12.25 2.5a2.25 2.25 0 0 1 2.25 2.25v5A2.25 2.25 0 0 1 12.25 12H9.81l1.018-1.018a.75.75 0 0 0-1.06-1.06L6.939 12.75l2.829 2.828a.75.75 0 1 0 1.06-1.06L9.811 13.5h2.439A3.75 3.75 0 0 0 16 9.75v-5A3.75 3.75 0 0 0 12.25 1h-.75v1.5z"/><path fill="currentColor" d="m8 1.85.77.694H6.095V1.488q1.046-.077 1.507-.385.474-.308.583-.913h1.32V8H8z"/><path fill="currentColor" d="M8.77 2.544 8 1.85v.693z"/></svg>';
                            } else if(rc!=='mixed'&&rp.getAttribute('data-rt')){
                                rp.removeAttribute('data-rt');
                                rp.innerHTML='<svg viewBox="0 0 16 16"><path fill="currentColor" d="M0 4.75A3.75 3.75 0 0 1 3.75 1h8.5A3.75 3.75 0 0 1 16 4.75v5a3.75 3.75 0 0 1-3.75 3.75H9.81l1.018 1.018a.75.75 0 1 1-1.06 1.06L6.939 12.75l2.829-2.828a.75.75 0 1 1 1.06 1.06L9.811 12h2.439a2.25 2.25 0 0 0 2.25-2.25v-5a2.25 2.25 0 0 0-2.25-2.25h-8.5A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75z"/></svg>';
                            }
                        }
                        if(lk){
                            var liked=(typeof isfav!=='undefined')&&isfav===true;
                            lk.classList.toggle('mtb-active',liked===true);
                        }
                        if(vl){
                            var vbb=document.getElementById('mtb-vol-btn');
                            var vf=document.getElementById('mtb-vol-fill');
                            var vh=document.getElementById('mtb-vol-handle');
                            var vrb=null;
                            var vv=document.querySelector('ytmusic-player video');
                            var vpct=0;
                            if(vv){vpct=vv.muted?0:(typeof vv.volume==='number'?vv.volume:0);}
                            var muted=!vv||vv.muted||vpct<=0;
                            vl.classList.toggle('mtb-active',muted===true);
                            var hasX=vbb&&!!vbb.querySelector('.mtb-mute-x');
                            if(muted&&!hasX){
                                vbb.innerHTML='<svg viewBox="0 0 16 16"><path class="mtb-mute-x" fill="currentColor" d="M13.86 5.47a.75.75 0 0 0-1.061 0l-1.47 1.47-1.47-1.47A.75.75 0 0 0 8.8 6.53L10.269 8l-1.47 1.47a.75.75 0 1 0 1.06 1.06l1.47-1.47 1.47 1.47a.75.75 0 0 0 1.06-1.06L12.39 8l1.47-1.47a.75.75 0 0 0 0-1.06"/><path fill="currentColor" d="M10.116 1.5A.75.75 0 0 0 8.991.85l-6.925 4a3.64 3.64 0 0 0-1.33 4.967 3.64 3.64 0 0 0 1.33 1.332l6.925 4a.75.75 0 0 0 1.125-.649v-1.906a4.7 4.7 0 0 1-1.5-.694v1.3L2.817 9.852a2.14 2.14 0 0 1-.781-2.92c.187-.324.456-.594.78-.782l5.8-3.35v1.3c.45-.313.956-.55 1.5-.694z"/></svg>';
                            } else if(!muted&&hasX){
                                vbb.innerHTML='<svg viewBox="0 0 16 16"><path fill="currentColor" d="M9.741.85a.75.75 0 0 1 .375.65v13a.75.75 0 0 1-1.125.65l-6.925-4a3.64 3.64 0 0 1-1.33-4.967 3.64 3.64 0 0 1 1.33-1.332l6.925-4a.75.75 0 0 1 .75 0zm-6.924 5.3a2.14 2.14 0 0 0 0 3.7l5.8 3.35V2.8zm8.683 4.29V5.56a2.75 2.75 0 0 1 0 4.88"/><path fill="currentColor" d="M11.5 13.614a5.752 5.752 0 0 0 0-11.228v1.55a4.252 4.252 0 0 1 0 8.127z"/></svg>';
                            }
                            if(vf) vf.style.width=(Math.max(0,Math.min(1,vpct))*100)+'%';
                            if(vh) vh.style.left=(Math.max(0,Math.min(1,vpct))*100)+'%';
                        }
                        if(ly) ly.style.display='none';
                        if(tm) tm.classList.toggle('mtb-active',typeof sleepTimerActive!=='undefined'&&sleepTimerActive&&sleepTimerActive.value);
                        var dcb=document.getElementById('mtb-dl-cancel');
                        if(dcb) dcb.style.display = window.__mtbDlActive ? '' : 'none';

                        var durMs=(typeof duration==='number'&&duration>0)?duration:0;
                        var posMs=(typeof position==='number'&&position>0)?position:0;
                        var pct=durMs>0?Math.max(0,Math.min(1,posMs/durMs)):0;
                        if(fl) fl.style.transform='scaleX('+pct+')';
                        if(fe) fe.style.transform='scaleX('+pct+')';
                        if(hd) hd.style.left=(pct*100)+'%';
                        if(ps) ps.textContent=formatTime(posMs);
                        if(ds) ds.textContent=formatTime(durMs);
                        mtbApplyEmpty();
                    };
                    function formatTime(ms){
                        var t=Math.floor(ms/1000);
                        return Math.floor(t/60)+':'+(t%60<10?'0':'')+t%60;
                    }

                    var rafLastTime=0;
                    function rafUpdate(timestamp){
                        if(timestamp-rafLastTime>100){ mtbUpdate(); rafLastTime=timestamp; }
                        requestAnimationFrame(rafUpdate);
                    }
                    if(window.mtbMiniPref) splSetMini(true);
                    requestAnimationFrame(rafUpdate);
            };
            if(document.readyState==='complete') initMuseTubePlayer();
            else window.addEventListener('load',initMuseTubePlayer);
            setInterval(function(){
                if(window.__mtbBg) return;
                if(!document.getElementById('musetubePlayerControls')) initMuseTubePlayer();
            },3000);
        
    """
}
