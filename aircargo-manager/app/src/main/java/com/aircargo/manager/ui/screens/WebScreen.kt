package com.aircargo.manager.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.aircargo.manager.ui.components.SubScreen

/** WebView 제어 핸들 (새로고침·외부 열기용) */
class WebHandle {
    var view: WebView? = null
    fun reload() { view?.reload() }
    fun currentUrl(fallback: String): String = view?.url ?: fallback
}

/**
 * 화면 내장 WebView — url 변경 시 해당 주소 로드.
 * 뒤로가기: 웹 히스토리 우선 처리.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedWeb(url: String, modifier: Modifier = Modifier, handle: WebHandle = remember { WebHandle() }) {
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = canGoBack) { handle.view?.goBack() }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val scheme = request.url.scheme.orEmpty()
                            if (scheme == "http" || scheme == "https") return false
                            return runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                true
                            }.getOrDefault(true)
                        }

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            canGoBack = view.canGoBack()
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            canGoBack = view.canGoBack()
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    handle.view = this
                }
            },
            update = { v ->
                if (loadedUrl != url) {
                    loadedUrl = url
                    v.loadUrl(url)
                }
            },
            onRelease = { it.destroy(); handle.view = null },
        )
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            )
        }
    }
}

/** 외부 웹(인천공항·항공사 화물·Flightradar24) 공용 전체화면 */
@Composable
fun WebScreen(url: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val handle = remember { WebHandle() }

    SubScreen(
        title = title.ifBlank { "WEB" },
        onBack = onBack,
        actions = {
            IconButton(onClick = { handle.reload() }) { Icon(Icons.Filled.Refresh, "새로고침") }
            IconButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(handle.currentUrl(url))))
            }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "브라우저로 열기") }
        },
    ) { pad ->
        EmbeddedWeb(url, Modifier.padding(pad).fillMaxSize(), handle)
    }
}
