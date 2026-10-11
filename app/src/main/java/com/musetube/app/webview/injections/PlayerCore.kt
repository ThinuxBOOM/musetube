package com.musetube.app.webview.injections

object PlayerCore {
    const val CONTENT = """
            var reqPause=false,firstPlay=true,ulFlag=false,ffDone=false,npOpen=false;
            window.__mtbUnlocked=false;
            window.__mtbApActive=false;
            window.__mtbApDone=false;
            var lastState=null,lastPos=null,playing=false;
            var pfint=null,afint=null,cssint=null,aaint=null;
            window.mtbViewH=function(){
                try{if(window.visualViewport&&window.visualViewport.height)return window.visualViewport.height;}catch(e){}
                return document.documentElement.clientHeight||window.innerHeight||0;
            };
            window.mtbPlayerTop=function(){
                var p=document.getElementById('musetubePlayerControls');
                if(p){var r=p.getBoundingClientRect();if(r.height>2)return r.top;}
                var o=document.querySelector('ytmusic-player-bar');
                if(o){var s=getComputedStyle(o);if(s.display!=='none'&&s.visibility!=='hidden'){var r2=o.getBoundingClientRect();if(r2.height>2)return r2.top;}}
                return window.mtbViewH();
            };
            window.__mtbFloaters=[];
            setInterval(function(){
                if(window.__mtbBg) return;
                for(var i=0;i<window.__mtbFloaters.length;i++){
                    try{window.__mtbFloaters[i]();}catch(e){}
                }
            },250);
        
    """
}