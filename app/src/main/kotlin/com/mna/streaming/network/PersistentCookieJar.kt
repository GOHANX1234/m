package com.mna.streaming.network

import com.mna.streaming.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * A thread-safe CookieJar that:
 *  - Stores all cookies in memory (keyed by host).
 *  - Persists the NextAuth session-token to DataStore so the session
 *    survives app restarts.
 *  - Properly handles __Host- and __Secure- prefixed cookies (OkHttp
 *    parses and matches these via RFC 6265 rules automatically).
 */
class PersistentCookieJar(
    private val sessionManager: SessionManager
) : CookieJar {

    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Pre-load ──────────────────────────────────────────────────────────────

    /**
     * Called once at startup: if a session token was saved in DataStore,
     * inject it into the in-memory store so OkHttp sends it on first request.
     */
    fun preload(host: String, cookieName: String, cookieValue: String) {
        val cookie = Cookie.Builder()
            .name(cookieName)
            .value(cookieValue)
            .domain(host)
            .path("/")
            .secure()
            .httpOnly()
            .build()
        store.getOrPut(host) { mutableListOf() }.apply {
            removeIf { it.name == cookieName }
            add(cookie)
        }
    }

    // ── CookieJar ─────────────────────────────────────────────────────────────

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        store.getOrPut(host) { mutableListOf() }.apply {
            synchronized(this) {
                cookies.forEach { incoming ->
                    removeIf { it.name == incoming.name }
                    add(incoming)

                    // Persist session token so it survives process death
                    if (incoming.name.contains("session-token")) {
                        scope.launch {
                            sessionManager.saveSessionToken(incoming.value)
                        }
                    }

                    // Clear saved token when server explicitly clears it
                    if (incoming.name.contains("session-token") && incoming.value.isBlank()) {
                        scope.launch { sessionManager.clearSession() }
                    }
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return store[url.host]
            ?.filter { it.matches(url) }
            ?: emptyList()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Remove all in-memory cookies (call on sign-out). */
    fun clear() {
        store.clear()
    }
}
