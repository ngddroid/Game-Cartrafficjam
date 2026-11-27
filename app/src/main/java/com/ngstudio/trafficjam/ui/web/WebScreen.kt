package com.ngstudio.trafficjam.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
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

private fun titleFor(file: String) = if (file.contains("terms")) "Terms of Service" else "Privacy Policy"

/** Shows one of the bundled HTML documents from assets; the two docs cross-link to each other. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(doc: String, onBack: () -> Unit) {
    val startFile = if (doc == "terms") "terms_of_service.html" else "privacy_policy.html"
    var title by remember { mutableStateOf(titleFor(startFile)) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Back goes to the previous document first (after following a cross-link), then leaves the screen.
    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    Column(Modifier.fillMaxSize().background(Sunset.SkyBottom)) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Sunset.HeaderTop, Sunset.HeaderBottom)))
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            RoundButton(
                onClick = {
                    val wv = webView
                    if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
                },
                modifier = Modifier.align(Alignment.CenterStart),
                size = 44.dp,
            ) {
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
                            // Keep the toolbar title in step with the document being shown.
                            if (url != null) title = titleFor(url)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url
                            // Local docs (including the cross-link) load in-place; anything else opens outside.
                            if (url.scheme == "file") return false
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                            return true
                        }
                    }
                    loadUrl("file:///android_asset/$startFile")
                }
            },
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        )
    }
}
