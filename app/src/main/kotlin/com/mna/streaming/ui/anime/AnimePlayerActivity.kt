package com.mna.streaming.ui.anime

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.mna.streaming.MAApplication
import com.mna.streaming.network.models.AnimeStreamInfo
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MATextSecondary
import com.mna.streaming.ui.theme.MATheme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Fullscreen player Activity for anime episodes.
 *
 * Receives an episode ID and performs the two-step stream probe
 * (per API docs Â§4.3) entirely in native code:
 *
 *  1. GET /api/stream/episode/:id  â€” path decoded from C security layer
 *     â€¢ 2xx â†’ ExoPlayer (HLS or direct MP4), with session cookie forwarded
 *     â€¢ 400 â†’ embed type â†’ step 2
 *  2. GET /api/stream/episode/:id/embed â€” path decoded from C security layer
 *     â†’ load the returned URL in a full-screen WebView
 *
 * Also fires:
 *  - POST /api/views (Episode) â€” records a unique view on playback start
 */
@OptIn(UnstableApi::class)
class AnimePlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_EPISODE_ID = "episode_id"
        const val EXTRA_TITLE      = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // True full-screen (hide system bars)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val episodeId = intent.getStringExtra(EXTRA_EPISODE_ID) ?: run { finish(); return }
        val title     = intent.getStringExtra(EXTRA_TITLE) ?: ""

        val animeRepository = MAApplication.animeRepository
        val apiClient       = MAApplication.apiClient

        setContent {
            MATheme {
                var streamInfo by remember { mutableStateOf<AnimeStreamInfo?>(null) }
                var error      by remember { mutableStateOf<String?>(null) }
                var loading    by remember { mutableStateOf(true) }

                // Probe stream + track view concurrently
                LaunchedEffect(episodeId) {
                    try {
                        coroutineScope {
                            val infoDeferred = async { animeRepository.probeEpisodeStream(episodeId) }
                            launch { animeRepository.trackEpisodeView(episodeId) }
                            streamInfo = infoDeferred.await()
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Could not load stream"
                    } finally {
                        loading = false
                    }
                }

                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        loading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MARed)
                                Spacer(Modifier.height(12.dp))
                                Text(title, color = MATextSecondary)
                            }
                        }

                        error != null -> {
                            Text(
                                text     = error ?: "Unknown error",
                                color    = MATextSecondary,
                                modifier = Modifier.padding(24.dp)
                            )
                        }

                        streamInfo is AnimeStreamInfo.Stream -> {
                            val info = streamInfo as AnimeStreamInfo.Stream

                            // Build cookie header from OkHttp's cookie jar
                            val streamHttpUrl = info.streamUrl.toHttpUrlOrNull()
                            val cookieStr = if (streamHttpUrl != null) {
                                apiClient.cookieJar.loadForRequest(streamHttpUrl)
                                    .joinToString("; ") { "${it.name}=${it.value}" }
                            } else ""

                            // ExoPlayer with session cookie forwarded on every request
                            val context = this@AnimePlayerActivity
                            val player = remember {
                                val dsFactory = DefaultHttpDataSource.Factory()
                                    .setDefaultRequestProperties(
                                        if (cookieStr.isNotBlank()) mapOf("Cookie" to cookieStr)
                                        else emptyMap()
                                    )
                                ExoPlayer.Builder(context)
                                    .setMediaSourceFactory(DefaultMediaSourceFactory(dsFactory))
                                    .build()
                                    .apply {
                                        setMediaItem(MediaItem.fromUri(Uri.parse(info.streamUrl)))
                                        prepare()
                                        playWhenReady = true
                                    }
                            }

                            DisposableEffect(Unit) {
                                onDispose {
                                    // Save final progress before destroying
                                    val positionMs = player.currentPosition
                                    val progressSeconds = (positionMs / 1000).toInt()
                                    if (progressSeconds > 0) {
                                        MAApplication.appScope.launch {
                                            animeRepository.saveProgress(episodeId, progressSeconds)
                                        }
                                    }
                                    player.release()
                                }
                            }

                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        this.player = player
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        streamInfo is AnimeStreamInfo.Embed -> {
                            val info = streamInfo as AnimeStreamInfo.Embed
                            EmbedWebView(url = info.embedUrl)

                            // Save progress periodically for embed episodes too
                            var progressSeconds by remember { mutableIntStateOf(0) }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    kotlinx.coroutines.delay(20_000)
                                    progressSeconds += 20
                                    animeRepository.saveProgress(episodeId, progressSeconds)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// â”€â”€ Ad-blocker constants â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Domains whose requests are silently dropped by [shouldInterceptRequest].
 * Covers the ad networks most commonly injected by iframe embed players
 * (popunder networks, redirect trackers, analytics beacons, etc.).
 * Subdomains are matched automatically (e.g. "s.exoclick.com" is blocked
 * because "exoclick.com" is in this set).
 */
private val AD_HOSTS = setOf(
    // â”€â”€ General programmatic â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    "googlesyndication.com", "googleadservices.com", "doubleclick.net",
    "adnxs.com", "appnexus.com", "advertising.com",
    "rubiconproject.com", "pubmatic.com", "openx.net",
    "smartadserver.com", "casalemedia.com", "criteo.com",
    "outbrain.com", "taboola.com", "revcontent.com",
    "mgid.com", "adcash.com", "bidvertiser.com",
    "adform.net", "primis.tech", "vidazoo.com",
    "undertone.com", "sekindo.com", "rhythmone.com",
    // â”€â”€ Streaming / embed-player ad networks â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
    // â”€â”€ Tracking / fingerprinting â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    "scorecardresearch.com", "quantserve.com",
    "chartbeat.com", "hotjar.com",
    "mouseflow.com", "newrelic.com",
    "mixpanel.com", "amplitude.com",
)

/**
 * JavaScript injected into every page once it finishes loading.
 *
 * Defence layers:
 *  1. Neutralise [window.open] â€” the primary popunder mechanism.
 *  2. Capture-phase click listener that cancels any anchor navigation to a
 *     domain other than the player's own origin.  Running in the capture
 *     phase means our handler fires *before* any listener the embed page
 *     registered, so we can call stopImmediatePropagation() first.
 */
private val JS_AD_BLOCK = """
(function() {
    'use strict';

    // 1. Neutralise window.open â€” blocks popunder / new-tab ads entirely.
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
        } catch (err) { /* malformed URL â€” ignore */ }
    }, true /* capture phase */);
})();
""".trimIndent()

// â”€â”€ Embed WebView â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbedWebView(url: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val webViewInstance = this
                // Enable third-party cookies required by iframe player hosts (megaplay, megacloud, etc.)
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webViewInstance, true)
                }

                settings.apply {
                    javaScriptEnabled                = true
                    domStorageEnabled                = true
                    databaseEnabled                  = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess                  = true
                    allowContentAccess               = true
                    mixedContentMode                 = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort                  = true
                    loadWithOverviewMode             = true
                    builtInZoomControls              = false
                    displayZoomControls              = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(true)
                    userAgentString                  = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }

                webChromeClient = object : WebChromeClient() {

                    // â”€â”€ Popup / popunder blocker â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean = false

                    // â”€â”€ Media permission support for iframe video streams â”€â”€
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.grant(request.resources)
                    }

                    // â”€â”€ Fullscreen video support â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    override fun onShowCustomView(
                        view: android.view.View?,
                        callback: CustomViewCallback?
                    ) {
                        val activity = ctx as? ComponentActivity ?: return
                        val win = activity.window
                        WindowCompat.getInsetsController(win, win.decorView).apply {
                            systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            hide(WindowInsetsCompat.Type.systemBars())
                        }
                        (win.decorView as? ViewGroup)?.addView(view)
                    }

                    override fun onHideCustomView() {
                        val activity = ctx as? ComponentActivity ?: return
                        val win = activity.window
                        WindowCompat.getInsetsController(win, win.decorView)
                            .show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                webViewClient = object : WebViewClient() {

                    // â”€â”€ Navigation redirect blocker â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    // Allows iframe sources (isForMainFrame = false) and valid
                    // video hosts while blocking ad networks and deep-link schemes.
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri    = request?.url ?: return false
                        val scheme = uri.scheme ?: return false
                        if (scheme != "http" && scheme != "https") return true
                        val host   = uri.host?.lowercase() ?: return false
                        val isAd   = AD_HOSTS.any { blocked -> host == blocked || host.endsWith(".$blocked") }
                        if (isAd) return true
                        if (request.isForMainFrame == false) return false
                        return false
                    }

                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        url: String?
                    ): Boolean {
                        if (url == null) return false
                        return try {
                            val uri    = Uri.parse(url)
                            val scheme = uri.scheme ?: return false
                            if (scheme != "http" && scheme != "https") return true
                            val host   = uri.host?.lowercase() ?: return false
                            AD_HOSTS.any { blocked -> host == blocked || host.endsWith(".$blocked") }
                        } catch (e: Exception) {
                            false
                        }
                    }

                    // â”€â”€ Resource-level ad blocker â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val host = request?.url?.host?.lowercase() ?: return null
                        val blocked = AD_HOSTS.any { blocked ->
                            host == blocked || host.endsWith(".$blocked")
                        }
                        return if (blocked) {
                            WebResourceResponse(
                                "text/plain", "utf-8",
                                java.io.ByteArrayInputStream(ByteArray(0))
                            )
                        } else null
                    }

                    // â”€â”€ JS injection â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(JS_AD_BLOCK, null)
                    }
                }

                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
