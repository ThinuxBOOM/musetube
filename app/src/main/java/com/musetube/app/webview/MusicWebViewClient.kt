package com.musetube.app.webview

import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.musetube.app.webview.helpers.*
import com.musetube.app.webview.injections.*
import com.musetube.app.util.Constants
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MusicWebViewClient(
    private val onLoginRequired: () -> Unit,
    private val onNavStateChanged: ((Boolean) -> Unit)? = null,
    private val onRenderProcessGone: (() -> Unit)? = null,
    private val onWebViewError: ((errorCode: Int, description: String) -> Unit)? = null
) : WebViewClient() {

    private var currentWebView: WebView? = null
    private var prefsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var boundPrefs: android.content.SharedPreferences? = null

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        onNavStateChanged?.invoke(view?.canGoBack() == true)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (view == null || url == null) return

        currentWebView = view
        registerPrefsListener(view)

        if (url.startsWith("https://www.facebook.com/privacy/consent/gdp/")) {
            onPageFinishedClean(view, FbGdprBypass.CONTENT)
            return
        }

        val loggedIn = view.context.getSharedPreferences("musetube_prefs", 0)
            .getBoolean("LoggedIn", false)

        if (!loggedIn) {
            onPageFinishedClean(view, YtmLoginDetection.CONTENT)
            return
        }

        view.postDelayed({
            injectPlayerControl(view)
        }, 500)

        view.evaluateJavascript(YtmLoginDetection.CONTENT) { result ->
            if (result == "\"out\"") {
                view.context.getSharedPreferences("musetube_prefs", 0)
                    .edit().putBoolean("LoggedIn", false).apply()
                view.loadUrl(Constants.GOOGLE_LOGIN)
            }
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val prefs = view?.context?.getSharedPreferences("musetube_prefs", 0)

        val useProxy = prefs?.getString("ConnectionMode", "normal") == "proxy"
        val powerSave = prefs?.getBoolean("PowerSave", false) ?: false
        val blockSW = prefs?.getBoolean("BlockServiceWorker", true) ?: true
        val hideEmptyPlayer = prefs?.getBoolean("HideEmptyPlayer", false) ?: false

        view?.evaluateJavascript("window.__musetubeUseProxy=$useProxy;", null)
        view?.evaluateJavascript("window.__mtbPowerSavePref=$powerSave;", null)
        view?.evaluateJavascript("window.__mtbHideEmpty=$hideEmptyPlayer;", null)
        // FIX: these payloads were injected raw - strip them like every other
        // injection, served from cache.
        if (isGoogleAuthUrl(url)) {
            view?.evaluateJavascript(GoogleSpoof.CONTENT, null)
        } else {
            view?.evaluateJavascript(BrowserSpoof.CONTENT, null)
        }
        view?.evaluateJavascript(FetchOverride.CONTENT, null)
        AdIdStore.clear()
        view?.evaluateJavascript(YtmAdblock.CONTENT, null)
        if (blockSW) view?.evaluateJavascript(WorkerNeutralize.CONTENT, null)
        view?.evaluateJavascript(GaBlocker.CONTENT, null)
        view?.evaluateJavascript("window.__mtbPowerSavePref=$powerSave;", null)
        view?.evaluateJavascript(PowerSave.CONTENT, null)
        view?.evaluateJavascript(SettingsFix.CONTENT, null)
        view?.evaluateJavascript(VideoPark.CONTENT, null)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        Log.w(TAG, "Renderer process gone: crashed=${detail?.didCrash()}")
        view?.let {
            it.stopLoading()
            it.destroy()
        }
        onRenderProcessGone?.invoke()
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame != true) return
        val code = try { error?.errorCode ?: -1 } catch (_: Exception) { -1 }
        val desc = try { error?.description?.toString() ?: "" } catch (_: Exception) { "" }
        onWebViewError?.invoke(code, desc)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame != true) return
        val status = try { errorResponse?.statusCode ?: 0 } catch (_: Exception) { 0 }
        if (status >= 400) {
            onWebViewError?.invoke(status, "HTTP $status")
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()

        if (isAnalyticsDomain(url)) {
            val headers = mapOf("Access-Control-Allow-Origin" to "*")
            return WebResourceResponse("text/plain", "utf-8", 200, "OK", headers,
                ByteArrayInputStream(ByteArray(0)))
        }

        if (AdIdStore.matches(url)) {
            view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
            val silent = view.context.assets?.open("silent.mp3") ?: return null
            return WebResourceResponse("audio/mpeg", null, silent)
        }

        val useProxy = view.context.getSharedPreferences("musetube_prefs", 0)
            .getString("ConnectionMode", "normal") == "proxy"

        if (!useProxy) {
            // Fast path: this probe opens a real HttpURLConnection for EVERY
            // resource (JS/CSS/img/audio) on the WebView thread. Its only
            // observable effects are (a) Set-Cookie sync for Google auth hosts
            // and (b) swapping ad audio for silent.mp3 — both of which only
            // apply to narrow URL sets. Skip the connection for everything else.
            val isGoogle = isGoogleAuthUrl(url)
            if (!isGoogle && !isAdAudioUrl(url)) return null
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = request.method
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    for ((k, v) in request.requestHeaders) {
                        val lk = k.lowercase(Locale.ROOT)
                        if (lk != "x-requested-with" && lk != "sec-gpc" && !lk.startsWith("sec-ch-ua") &&
                            !(isGoogle && lk == "user-agent")
                        ) {
                            conn.setRequestProperty(k, v)
                        }
                    }
                    if (isGoogle) {
                        conn.setRequestProperty("User-Agent", Constants.DESKTOP_UA)
                        val cookie = CookieManager.getInstance().getCookie(url)
                        if (!cookie.isNullOrEmpty()) conn.setRequestProperty("Cookie", cookie)
                    }
                    conn.setRequestProperty("sec-gpc", "1")
                    conn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"")
                    conn.setRequestProperty("sec-ch-ua-mobile", "?0")
                    conn.setRequestProperty("sec-ch-ua", "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Google Chrome\";v=\"150\"")
                    conn.connect()
                    if (isGoogle) {
                        var sawSetCookie = false
                        conn.headerFields.forEach { (key, values) ->
                            if (key != null && key.equals("Set-Cookie", ignoreCase = true)) {
                                sawSetCookie = true
                                values.forEach { CookieManager.getInstance().setCookie(url, it) }
                            }
                        }
                        if (sawSetCookie) CookieManager.getInstance().flush()
                    }
                    val contentType = conn.contentType
                    if (contentType == "audio/mpeg" && isAdAudioUrl(url)) {
                        view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
                        val silent = view.context.assets?.open("silent.mp3") ?: return null
                        return WebResourceResponse("audio/mpeg", null, silent)
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                return null
            }
            return null
        }

        val adMatch = matchAdCdn(url)
        if (adMatch != null) {
            view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
            val silent = view.context.assets?.open("silent.mp3") ?: return null
            return WebResourceResponse("audio/mpeg", null, silent)
        }

        return null
    }

    private fun isGoogleAuthUrl(url: String?): Boolean {
        if (url == null) return false
        val host = runCatching { android.net.Uri.parse(url).host }.getOrNull()
            ?.lowercase() ?: return false
        return host == "google.com" ||
            host.endsWith(".google.com") ||
            host.contains(".google.") ||
            host.endsWith(".youtube.com") ||
            host == "youtube.com"
    }

    private fun injectPlayerControl(view: WebView) {
        val prefs = view.context.getSharedPreferences("musetube_prefs", 0)
        val autoPlayMode = prefs.getString("APlayMode", "disabled") ?: "disabled"
        val closeNowPlay = prefs.getBoolean("CloseNowPlay", true)
        val amoledEnabled = prefs.getBoolean("AmoledTheme", false)
        val customCss = prefs.getString("CustomCss", "") ?: ""
        val playerMode = prefs.getString("PlayerMode", "musetube") ?: "musetube"
        val useProxy = prefs.getString("ConnectionMode", "normal") == "proxy"
        val debugOverlay = prefs.getBoolean("DebugOverlay", false)
        val takeControl = prefs.getBoolean("TakeControl", true)
        val hideEmptyPlayer = prefs.getBoolean("HideEmptyPlayer", false)
        val lyricsStyle = prefs.getString("LyricsStyle", LyricsTheme.DEFAULT_STYLE) ?: LyricsTheme.DEFAULT_STYLE

        val js = buildString {
            append("window.autoPlayMode='$autoPlayMode';\n")
            append("window.closeNpPref=$closeNowPlay;\n")
            append("window.__musetubeUseProxy=$useProxy;\n")
            append("window.__mtbTakeControl=$takeControl;\n")
            append("window.__mtbHideEmpty=$hideEmptyPlayer;\n")
            if (debugOverlay) {
                append(DevLogPrelude.js())
                append("\n")
            }
            append(PlayerCore.CONTENT)
            append(YtmTrackObserver.CONTENT)
            append(ClassicBridge.CONTENT)
            append(MediaUpdater.CONTENT)
            append(PlaybackControls.CONTENT)
            append(YtmAuto.CONTENT)
            append(MainLoop.CONTENT)
            append(AutoFeatures.CONTENT)
            append(SearchOverlay.CONTENT)
            append(DownloadButton.CONTENT)
            append(DownloadProgress.CONTENT)
            append(CollectionDownload.CONTENT)
            append(ContextMenuDownload.CONTENT)
            append("""
                (function(){
                    var recAcc=function(){
                        try{
                            var av=document.querySelector('ytmusic-nav-bar #avatar-btn');
                            var txt='';
                            if(av){
                                txt=(av.getAttribute('aria-label')||'').trim();
                                if(!txt){
                                    var img=av.querySelector('img');
                                    if(img) txt=(img.getAttribute('alt')||'').trim();
                                }
                            }
                            if(txt) AndBridge.recAccountName(txt);
                        }catch(e){}
                    };
                    setTimeout(recAcc,5000);
                    setInterval(recAcc,60000);
                })();
            """.trimIndent())
            append(CssHack.CONTENT)
            append(ModalFix.CONTENT)
            append(ErrorDialogRestyle.CONTENT)
            append(ToastFix.CONTENT)
            append(LyricsSyncFix.CONTENT)
            append(QueueAutoClose.CONTENT)
            append(LibraryAutoClose.CONTENT)
            if (playerMode == "musetube") {
                append(MuseTubePlayer.CONTENT)
            }
        }
        val cleanJs = JsUtils.stripConsoleLogs(js) + "\n" +
                buildAmoledJs(amoledEnabled) + "\n" +
                AccentTheme.buildAccentJs(view.context) + "\n" +
                buildCustomCssJs(customCss) + "\n" +
                LyricsTheme.buildLyricsStyleJs(lyricsStyle)
        if (playerMode == "original") {
            view.evaluateJavascript(cleanJs + "\n(function(){var s=document.createElement('style');s.id='mtb-np-show';s.textContent='ytmusic-player-bar{display:flex!important}';document.head.appendChild(s);})();", null)
        } else {
            view.evaluateJavascript(cleanJs, null)
        }
    }

    private fun registerPrefsListener(view: WebView) {
        val prefs = view.context.getSharedPreferences("musetube_prefs", 0)
        // Listener doesn't depend on the WebView instance (it reads currentWebView),
        // so registering once is enough. Re-register only if the prefs instance
        // actually changed (new context after a renderer-crash rebuild).
        if (boundPrefs === prefs && prefsListener != null) return
        prefsListener?.let { boundPrefs?.unregisterOnSharedPreferenceChangeListener(it) }
        boundPrefs = prefs

        prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            val wv = currentWebView ?: return@OnSharedPreferenceChangeListener
            when (key) {
                "PlayerMode" -> {
                    val raw = prefs.getString("PlayerMode", "musetube") ?: "musetube"
                    switchPlayerMode(wv, raw)
                }
                "PowerSave" -> {
                    val on = prefs.getBoolean("PowerSave", false)
                    wv.evaluateJavascript("if(window.__mtbApplyPowerSave) window.__mtbApplyPowerSave($on);", null)
                }
                "CloseNowPlay" -> {
                    val closeNp = prefs.getBoolean("CloseNowPlay", true)
                    wv.evaluateJavascript("window.closeNpPref=$closeNp;", null)
                }
                "APlayMode" -> {
                    val mode = prefs.getString("APlayMode", "disabled") ?: "disabled"
                    wv.evaluateJavascript("window.autoPlayMode='$mode';", null)
                }
                "AmoledTheme", "CustomCss", "LyricsStyle" -> {
                    val js = buildAmoledJs(prefs.getBoolean("AmoledTheme", false)) + ";\n" +
                            buildCustomCssJs(prefs.getString("CustomCss", "") ?: "")
                    wv.evaluateJavascript(js, null)
                    wv.evaluateJavascript(LyricsTheme.buildLyricsStyleJs(prefs.getString("LyricsStyle", LyricsTheme.DEFAULT_STYLE) ?: LyricsTheme.DEFAULT_STYLE), null)
                }
                "PaletteSeed", "MaterialYou" ->
                    wv.evaluateJavascript(AccentTheme.buildAccentJs(wv.context), null)
                "TakeControl" -> {
                    val on = prefs.getBoolean("TakeControl", true)
                    wv.evaluateJavascript("window.__mtbTakeControl=$on;", null)
                }
                "BlockServiceWorker" -> {
                    if (prefs.getBoolean("BlockServiceWorker", true)) {
                        wv.evaluateJavascript(WorkerNeutralize.CONTENT + ";\n" + SW_UNREGISTER_JS, null)
                    } else {
                        wv.reload()
                    }
                }
                "HideEmptyPlayer" -> {
                    val hideEmpty = prefs.getBoolean("HideEmptyPlayer", false)
                    wv.evaluateJavascript("window.__mtbHideEmpty=$hideEmpty; if(window.mtbApplyEmpty) window.mtbApplyEmpty();", null)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun switchPlayerMode(view: WebView, mode: String) {
        if (mode == "original") {
            val js = """
                (function(){
                    var pl=document.getElementById('musetubePlayerControls');
                    if(pl) pl.style.display='none';
                    var s=document.createElement('style');
                    s.id='mtb-np-show';
                    s.textContent='ytmusic-player-bar{display:flex!important}';
                    document.head.appendChild(s);
                })();
            """.trimIndent()
            view.evaluateJavascript(js, null)
        } else {
            view.evaluateJavascript("if(typeof initMuseTubePlayer!=='function'){" + MuseTubePlayer.CONTENT + "}", null)
            val js = """
                (function(){
                    var s=document.getElementById('mtb-np-show');
                    if(s) s.remove();
                    var npb=document.querySelector('ytmusic-player-bar');
                    if(npb) npb.style.display='none';
                    var pl=document.getElementById('musetubePlayerControls');
                    if(pl){pl.style.display='flex';}
                    else if(typeof initMuseTubePlayer==='function'){initMuseTubePlayer();}
                })();
            """.trimIndent()
            view.evaluateJavascript(js, null)
        }
    }

    private fun onPageFinishedClean(view: WebView, js: String) {
        view.evaluateJavascript(JsUtils.stripConsoleLogs(js), null)
    }

    companion object {
        private const val TAG = "MusicWebViewClient"

        private val SW_UNREGISTER_JS = """
            try {
                if(navigator.serviceWorker){
                    navigator.serviceWorker.getRegistrations().then(function(regs){
                        regs.forEach(function(r){ r.unregister(); });
                    });
                }
            } catch(e){}
        """.trimIndent()
    }
}
