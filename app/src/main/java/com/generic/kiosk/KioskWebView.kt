package com.generic.kiosk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import android.widget.FrameLayout

@SuppressLint("SetJavaScriptEnabled")
class KioskWebView(context: Context, private val externalAllowed: () -> Boolean) : WebView(context) {
    private var homeHost: String? = null
    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        WebView.setWebContentsDebuggingEnabled(false)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme in listOf("http", "https")) {
                    if (uri.host.isNullOrBlank()) return true
                    return !sameOriginAllowed(uri) && !externalAllowed()
                }
                if (uri.scheme in listOf("mailto", "tel")) return !externalAllowed()
                return true
            }
            private fun sameOriginAllowed(uri: Uri): Boolean = uri.host.equals(homeHost, ignoreCase = true)
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showMessage("Sin conexión", error.description.toString())
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame && response.statusCode >= 400) showMessage("Error HTTP ${response.statusCode}", response.reasonPhrase)
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) { progress?.invoke(true) }
            override fun onPageFinished(view: WebView, url: String?) { progress?.invoke(false) }
        }
        setDownloadListener { _, _, _, _, _ -> /* Downloads remain disabled unless a future UI handles them safely. */ }
        layoutParams = FrameLayout.LayoutParams(-1, -1)
    }
    var progress: ((Boolean) -> Unit)? = null
    fun reloadHome(url: String) { val valid = UrlValidator.validate(url) ?: "https://example.com"; homeHost = Uri.parse(valid).host; loadUrl(valid) }
    fun showMessage(title: String, detail: String) { loadDataWithBaseURL(null, errorHtml(title, detail), "text/html", "UTF-8", null) }
    fun recoverRenderer() { runCatching { destroy() } }
    private fun errorHtml(title: String, detail: String): String {
        fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
        return "<meta name='viewport' content='width=device-width,initial-scale=1'><body style='font-family:sans-serif;padding:2rem'><h2>${escape(title)}</h2><p>${escape(detail.take(240))}</p><p>La página se recargará cuando vuelva la conexión.</p></body>"
    }
}
