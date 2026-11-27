package com.ngstudio.trafficjam.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.components.RoundButton
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.Sunset

private const val SITE_HOST = "game-trafficjamcarescape.web.app"
private const val LIVE_PRIVACY = "https://$SITE_HOST/privacy_policy.html"
private const val LIVE_TERMS = "https://$SITE_HOST/terms_of_condition.html"

private fun isTerms(url: String) = url.contains("terms") || url.contains("condition")
private fun titleFor(url: String) = if (isTerms(url)) "Terms of Service" else "Privacy Policy"

/** The bundled copy to fall back to when the matching live page cannot load. */
private fun localFor(url: String) =
    "file:///android_asset/" + if (isTerms(url)) "terms_of_service.html" else "privacy_policy.html"

/**
 * Shows the Privacy Policy / Terms. It loads the live web page first and quietly falls back to the
 * HTML bundled in assets if the live page fails (offline, server down, etc.). The two docs
 * cross-link to each other.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(doc: String, onBack: () -> Unit) {
    val startUrl = if (doc == "terms") LIVE_TERMS else LIVE_PRIVACY
    var title by remember { mutableStateOf(titleFor(startUrl)) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun back() {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }
    BackHandler { back() }

    Column(Modifier.fillMaxSize().background(Sunset.SkyBottom)) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Sunset.HeaderTop, Sunset.HeaderBottom)))
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            RoundButton(onClick = { back() }, modifier = Modifier.align(Alignment.CenterStart), size = 44.dp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            OutlinedText(title, Modifier.align(Alignment.Center), style = GameText.Title, fontSize = 24.sp, strokeWidth = 6f, outline = Sunset.Outline)
        }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    setBackgroundColor(android.graphics.Color.parseColor("#E94F6A"))
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            if (url != null) title = titleFor(url)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url
                            // Our own pages (live site or bundled file) stay in the WebView.
                            if (url.scheme == "file" || url.host == SITE_HOST) return false
                            // Anything else (e.g. Google's policy page) opens in the browser.
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                            return true
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            fallBackIfLive(view, request)
                        }

                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                            fallBackIfLive(view, request)
                        }

                        /** When a live page fails to load, swap in the matching bundled copy. */
                        private fun fallBackIfLive(view: WebView, request: WebResourceRequest) {
                            if (!request.isForMainFrame) return
                            val url = request.url
                            if (url.scheme == "http" || url.scheme == "https") {
                                view.loadUrl(localFor(url.toString()))
                            }
                        }
                    }
                    loadUrl(startUrl)
                }
            },
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        )
    }
}
