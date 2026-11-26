package com.puzzel.lab.cartrafficjam.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.RoundButton
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.Sunset

/** Shows one of the bundled HTML documents from assets. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(doc: String, onBack: () -> Unit) {
    val (title, file) = when (doc) {
        "terms" -> "Terms of Service" to "terms_of_service.html"
        else -> "Privacy Policy" to "privacy_policy.html"
    }
    Column(Modifier.fillMaxSize().background(Sunset.SkyBottom)) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Sunset.HeaderTop, Sunset.HeaderBottom)))
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            RoundButton(onBack, Modifier.align(Alignment.CenterStart), size = 44.dp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            OutlinedText(title, Modifier.align(Alignment.Center), style = GameText.Title, fontSize = 24.sp, strokeWidth = 6f, outline = Sunset.Outline)
        }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#E94F6A"))
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url
                            if (url.scheme == "file") return false
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                            return true
                        }
                    }
                    loadUrl("file:///android_asset/$file")
                }
            },
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        )
    }
}
