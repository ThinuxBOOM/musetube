package com.musetube.app.webview.injections

/**
 * YouTube Music login/session detector (replaces LoginDetection + LogoutCheck).
 *
 * Returns one of 'in' / 'out' / 'skip':
 *  - 'in': signed-in session visible — also notifies AndBridge.loginDetected().
 *  - 'out': signed-out (ServiceLogin link showing on YTM, no session markers).
 *  - 'skip': uncertain (mid-login on accounts pages, or markers not settled yet).
 *
 * Session markers (DOM-first, cookie second): the account avatar button in
 * ytmusic-nav-bar when signed in, the ServiceLogin link when signed out.
 * SAPISID is js-readable when the cookie jar exposes it; SID/HSID/SSID are
 * httpOnly so the native side (ProfileManager.captureSession) is the
 * authority for those — this script only uses SAPISID opportunistically.
 */
object YtmLoginDetection {
    const val CONTENT = """
            (function(){
                function onLoginPage(){
                    try {
                        var h = location.hostname;
                        return h.indexOf('accounts.google.com') !== -1 ||
                               h.indexOf('accounts.youtube.com') !== -1;
                    } catch(e){ return false; }
                }
                function signedIn(){
                    try {
                        var bar = document.querySelector('ytmusic-nav-bar');
                        if (bar) {
                            if (bar.querySelector('a[href*="ServiceLogin"]')) return false;
                            var avatar = bar.querySelector('#avatar-btn, button[aria-label="Account"], #avatar-btn yt-img-shadow img');
                            if (avatar) return true;
                        }
                        if (document.cookie.indexOf('SAPISID=') !== -1) return true;
                        return null;
                    } catch(e){ return null; }
                }
                if (onLoginPage()) return 'skip';
                var s = signedIn();
                if (s === true) {
                    try { AndBridge.loginDetected(); } catch(e){}
                    return 'in';
                }
                if (s === false) return 'out';
                return 'skip';
            })();
        
    """
}
