package com.mna.streaming.ui.player

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mna.streaming.MAApplication
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MATextSecondary
import com.mna.streaming.ui.theme.MATheme
import java.io.ByteArrayInputStream
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Fullscreen player Activity.
 *
 * Receives a movie ID, fetches the embed URL from the API (via a path decoded
 * from the native C security layer — never a plain string in Kotlin code),
 * and loads it inside a WebView with JS and autoplay enabled.
 *
 * Also fires:
 *  - POST /api/views — records a unique view on playback start
 */
class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MOVIE_ID = "movie_id"
        const val EXTRA_TITLE    = "title"
        // Legacy constant kept for any call-sites that still pass a direct stream URL;
        // ignored by this Activity — all streaming now goes through the embed endpoint.
        const val EXTRA_STREAM_URL = "stream_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val movieId = intent.getStringExtra(EXTRA_MOVIE_ID) ?: run { finish(); return }
        val title   = intent.getStringExtra(EXTRA_TITLE) ?: ""

        val movieRepository = MAApplication.movieRepository

        setContent {
            MATheme {
                var embedUrl by remember { mutableStateOf<String?>(null) }
                var error    by remember { mutableStateOf<String?>(null) }
                var loading  by remember { mutableStateOf(true) }

                // Fetch embed URL + track view concurrently on launch
                LaunchedEffect(movieId) {
                    try {
                        coroutineScope {
                            val embedDeferred = async { movieRepository.getEmbedUrl(movieId) }
                            launch { movieRepository.trackView(movieId) }  // fire-and-forget
                            embedUrl = embedDeferred.await()
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Could not load stream"
                    } finally {
                        loading = false
                    }
                }

                PlayerScreen(
                    embedUrl = embedUrl,
                    title    = title,
                    isLoading = loading,
                    error    = error
                )
            }
        }
    }
}

// ── Ad-blocker constants ──────────────────────────────────────────────────────

/**
 * Domains whose requests are silently dropped by [shouldInterceptRequest].
 * Covers the ad networks most commonly injected by iframe embed players
 * (popunder networks, redirect trackers, analytics beacons, etc.).
 * Subdomains are matched automatically (e.g. "s.exoclick.com" is blocked
 * because "exoclick.com" is in this set).
 */
private val AD_HOSTS = setOf(
    // ── General programmatic ──────────────────────────────────────────────
    "googlesyndication.com", "googleadservices.com", "doubleclick.net",
    "adnxs.com", "appnexus.com", "advertising.com",
    "rubiconproject.com", "pubmatic.com", "openx.net",
    "smartadserver.com", "casalemedia.com", "criteo.com",
    "outbrain.com", "taboola.com", "revcontent.com",
    "mgid.com", "adcash.com", "bidvertiser.com",
    "adform.net", "primis.tech", "vidazoo.com",
    "undertone.com", "sekindo.com", "rhythmone.com",
    // ── Streaming / embed-player ad networks ─────────────────────────────
    "exoclick.com", "trafficjunky.com", "juicyads.com",
    "hilltopads.net", "hilltopads.com",
    "adsterra.com", "adsterraserver.com", "adsterraaudio.com",
    "propellerads.com", "propellermedia.net",
    "popads.net", "popcash.net",
    "clickadu.com", "clickagy.com",
    "yllix.com", "coinzilla.com",
    "plugrush.com", "richaudience.com",
    "jetpackdigital.com", "adtelligent.com", "adtelligent.net",
    "adskeeper.co.uk", "adskeeper.com",
    "adspyglass.com", "monetizer101.com",
    "fuckingfast.co", "go2jump.org",
    "clkmon.com", "clkrev.com",
    "trackedlink.net", "ptrk.io",
    "getpopads.com", "popmyads.com",
    "adf.ly", "adfoc.us",
    // ── Tracking / fingerprinting ─────────────────────────────────────────
    "scorecardresearch.com", "quantserve.com",
    "chartbeat.com", "hotjar.com",
    "mouseflow.com", "newrelic.com",
    "mixpanel.com", "amplitude.com",
)

/**
 * JavaScript injected into every page once it finishes loading.
 *
 * Defence layers:
 *  1. Neutralise [window.open] — the primary popunder mechanism.
 *  2. Capture-phase click listener that cancels any anchor navigation to a
 *     domain other than the player's own origin.  Running in the capture
 *     phase means our handler fires *before* any listener the embed page
 *     registered, so we can call stopImmediatePropagation() first.
 */
private val JS_AD_BLOCK = """
(function() {
    'use strict';

    // 1. Neutralise window.open — blocks popunder / new-tab ads entirely.
    window.open = function() { return null; };

    // 2. Block off-origin anchor navigations triggered by click overlays.
    document.addEventListener('click', function(e) {
        var el = e.target;
        // Walk the DOM upward to find the nearest <a> ancestor.
        while (el && el.nodeName !== 'A') { el = el.parentElement; }
        if (!el || !el.href) return;
        try {
            if (el.href.startsWith('javascript:')) return;
            var linkHost = new URL(el.href).hostname;
            var pageHost = window.location.hostname;
            // Allow same origin and subdomains; block everything else.
            if (linkHost !== pageHost && !linkHost.endsWith('.' + pageHost)) {
                e.preventDefault();
                e.stopImmediatePropagation();
            }
        } catch (err) { /* malformed URL — ignore */ }
    }, true /* capture phase */);
})();
""".trimIndent()

// ── Player screen ─────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    embedUrl: String?,
    title: String,
    isLoading: Boolean,
    error: String?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MARed)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text  = "Loading $title…",
                        color = MATextSecondary
                    )
                }
            }

            error != null -> {
                Text(
                    text     = "Error: $error",
                    color    = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }

            embedUrl != null -> {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled             = true
                                domStorageEnabled             = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess               = false
                                // Allow mixed content for third-party embed players
                                mixedContentMode              = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                useWideViewPort               = true
                                loadWithOverviewMode          = true
                                builtInZoomControls           = false
                                displayZoomControls           = false
                                // Required for WebChromeClient.onCreateWindow to fire.
                                // Without this flag, window.open() calls are never routed
                                // through onCreateWindow — it silently does nothing instead.
                                // We override onCreateWindow to return false, so all popup
                                // windows are discarded at the WebView level.
                                setSupportMultipleWindows(true)
                            }

                            // Derive the embed player's own host so we can allow its
                            // internal navigation while blocking everything else.
                            val embedHost = Uri.parse(embedUrl).host ?: ""

                            webChromeClient = object : WebChromeClient() {

                                // ── Popup / popunder blocker ──────────────────
                                // window.open() calls land here. Returning false
                                // tells the WebView to discard the new window
                                // request, killing popup and popunder ads entirely.
                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: android.os.Message?
                                ): Boolean = false

                                // ── Fullscreen video support ──────────────────
                                override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                    val activity = ctx as? ComponentActivity ?: return
                                    val window = activity.window
                                    WindowCompat.getInsetsController(window, window.decorView).apply {
                                        systemBarsBehavior =
                                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                        hide(WindowInsetsCompat.Type.systemBars())
                                    }
                                    (window.decorView as? ViewGroup)?.addView(view)
                                }

                                override fun onHideCustomView() {
                                    val activity = ctx as? ComponentActivity ?: return
                                    val window = activity.window
                                    WindowCompat.getInsetsController(window, window.decorView)
                                        .show(WindowInsetsCompat.Type.systemBars())
                                }
                            }

                            webViewClient = object : WebViewClient() {

                                // ── Navigation redirect blocker ───────────────
                                // Two overloads are needed:
                                //  • shouldOverrideUrlLoading(request) — API 24+
                                //    handles http/https navigations.
                                //  • shouldOverrideUrlLoading(url: String) — the
                                //    deprecated String overload that Android still
                                //    calls for non-http schemes like intent://.
                                //    Without it, the WebView tries to load the
                                //    intent:// URL, Android can't handle it inside
                                //    a WebView, and shows "Web page not available".
                                //
                                // Both block everything except the embed player's
                                // own host.  Non-http schemes are always blocked.

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri    = request?.url ?: return true
                                    val scheme = uri.scheme ?: return true
                                    // Block any non-web scheme (intent://, market://,
                                    // tel:, javascript:, etc.) immediately.
                                    if (scheme != "http" && scheme != "https") return true
                                    val host    = uri.host ?: return true
                                    val allowed = host == embedHost ||
                                        host.endsWith(".$embedHost")
                                    return !allowed  // true = block, false = allow
                                }

                                @Suppress("OVERRIDE_DEPRECATION")
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    url: String?
                                ): Boolean {
                                    if (url == null) return true
                                    return try {
                                        val uri    = Uri.parse(url)
                                        val scheme = uri.scheme ?: return true
                                        if (scheme != "http" && scheme != "https") return true
                                        val host    = uri.host ?: return true
                                        val allowed = host == embedHost ||
                                            host.endsWith(".$embedHost")
                                        !allowed
                                    } catch (e: Exception) {
                                        true  // block anything we can't parse
                                    }
                                }

                                // ── Resource-level ad blocker ─────────────────
                                // Intercepts every sub-resource the page loads
                                // (scripts, images, XHR). Requests whose host
                                // matches AD_HOSTS get an instant empty response
                                // so the ad script never downloads or executes.
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val host = request?.url?.host ?: return null
                                    val blocked = AD_HOSTS.any { blocked ->
                                        host == blocked || host.endsWith(".$blocked")
                                    }
                                    return if (blocked) {
                                        // Return an empty body — no redirect, no error,
                                        // the page just gets nothing for that request.
                                        WebResourceResponse(
                                            "text/plain", "utf-8",
                                            ByteArrayInputStream(ByteArray(0))
                                        )
                                    } else null
                                }

                                // ── JS injection ──────────────────────────────
                                // Injected after each page load. Overrides
                                // window.open and cancels off-origin anchor
                                // clicks in the capture phase so they fire before
                                // the player's own click listeners.
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(JS_AD_BLOCK, null)
                                }
                            }

                            loadUrl(embedUrl)
                        }
                    },
                    update = { webView ->
                        // No-op — URL is set once in factory
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
