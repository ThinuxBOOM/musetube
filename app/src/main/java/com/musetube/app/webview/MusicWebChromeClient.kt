package com.musetube.app.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.musetube.app.util.Constants
import com.musetube.app.webview.injections.BrowserSpoof
import com.musetube.app.webview.injections.FbGdprBypass
import com.musetube.app.webview.injections.GoogleSpoof
import androidx.core.net.toUri

class MusicWebChromeClient(
    private val onProgressChanged: ((Int) -> Unit)? = null,
    private val onShowCustomView: ((View?, CustomViewCallback?) -> Unit)? = null,
    private val onHideCustomView: (() -> Unit)? = null,
    private val onFileChooser: ((ValueCallback<Array<Uri>>, Array<String>) -> Boolean)? = null
) : WebChromeClient() {

    private var childWebView: WebView? = null

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        onShowCustomView?.invoke(view, callback)
    }

    override fun onHideCustomView() {
        onHideCustomView?.invoke()
    }

    private fun isMusicUrl(url: String): Boolean {
        return url.startsWith("https://music.youtube.com/") ||
                url.startsWith("https://accounts.google.com/") ||
                url.startsWith("https://accounts.youtube.com/") ||
                url.startsWith("https://youtube.com/") ||
                url.startsWith("https://www.youtube.com/") ||
                url.startsWith("https://m.youtube.com/")
    }

    private fun isOAuthUrl(url: String): Boolean {
        val host = runCatching { url.toUri().host?.lowercase() }.getOrNull() ?: return false
        return host == "google.com" ||
                host.endsWith(".google.com") ||
                host.indexOf(".google.") != -1 ||
                host == "youtube.com" ||
                host.endsWith(".youtube.com") ||
                host == "accounts.youtube.com" ||
                host == "facebook.com" ||
                host.endsWith(".facebook.com") ||
                host == "appleid.apple.com" ||
                host.endsWith(".apple.com")
    }

    private fun isGoogleUrl(url: String): Boolean {
        val host = runCatching { url.toUri().host?.lowercase() }.getOrNull() ?: return false
        return host == "google.com" || host.endsWith(".google.com") || host.indexOf(".google.") != -1
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?
    ): Boolean {
        childWebView?.let {
            try { it.destroy() } catch (_: Exception) {}
        }
        val context = view?.context ?: return false
        val newWebView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = Constants.DESKTOP_UA
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (url != null) {
                        if (isGoogleUrl(url)) {
                            view?.evaluateJavascript(GoogleSpoof.CONTENT, null)
                        } else if (!isMusicUrl(url)) {
                            view?.evaluateJavascript(BrowserSpoof.CONTENT, null)
                        }
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url != null && url.startsWith("https://www.facebook.com/privacy/consent/gdp/")) {
                        view?.evaluateJavascript(FbGdprBypass.CONTENT, null)
                    }
                }

                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    val targetUrl = url ?: return false
                    val allowed = isMusicUrl(targetUrl) || isOAuthUrl(targetUrl)
                    if (!allowed) {
                        try { view?.destroy() } catch (_: Exception) {}
                        return true
                    }
                    view?.loadUrl(targetUrl)
                    return true
                }
            }
        }
        childWebView = newWebView
        val transport = resultMsg?.obj as? WebView.WebViewTransport
        transport?.webView = newWebView
        resultMsg?.sendToTarget()
        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback == null) return false
        val acceptTypes = fileChooserParams?.acceptTypes ?: emptyArray()
        return onFileChooser?.invoke(filePathCallback, acceptTypes) ?: false
    }

    @Suppress("DEPRECATION")
    override fun onPermissionRequest(permissionRequest: PermissionRequest?) {
        permissionRequest ?: return
        Handler(Looper.getMainLooper()).post {
            val resources = permissionRequest.resources
            if (resources.contains(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                permissionRequest.grant(resources)
            } else {
                permissionRequest.deny()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onConsoleMessage(message: String?, lineNumber: Int, sourceId: String?) {
        android.util.Log.d("MuseTubeJS", "$message [$sourceId:$lineNumber]")
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged?.invoke(newProgress)
    }

    fun cleanup() {
        childWebView?.let {
            try { it.destroy() } catch (_: Exception) {}
        }
        childWebView = null
    }
}