package com.mna.streaming.network

import com.google.gson.GsonBuilder
import com.mna.streaming.data.SessionManager
import com.mna.streaming.security.NativeApiSecurity
import okhttp3.OkHttpClient
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Central HTTP client for the M&A app.
 *
 * Security measures:
 *  1. Certificate pinning — pins (intermediate CA + root CA) come from the
 *     native C layer (XOR-obfuscated). Vercel's leaf cert rotates every
 *     ~90 days; the intermediate/root are stable for years.
 *  2. PersistentCookieJar — manages NextAuth __Host- / __Secure- cookies
 *     and persists the session token across app restarts via DataStore.
 *  3. Custom User-Agent — client identifier decoded from native layer so
 *     the tag string doesn't appear as a plain string in the DEX.
 *  4. Streaming endpoint paths loaded from C, never from Kotlin literals.
 *  5. HTTPS enforced; cleartext disabled in AndroidManifest.
 */
class ApiClient(private val sessionManager: SessionManager) {

    val baseUrl: String = NativeApiSecurity.getBaseUrl() + "/"

    val cookieJar = PersistentCookieJar(sessionManager)

    /** Custom User-Agent decoded from native security layer. */
    private val clientTag: String = NativeApiSecurity.getClientTag()

    private val userAgentInterceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", clientTag)
                .build()
        )
    }

    val okHttpClient: OkHttpClient by lazy {
        val hostname = "sarrows.vercel.app"

        val pinner = CertificatePinner.Builder()
            .add(hostname, NativeApiSecurity.getCertPinIntermediate()) // Google WR1
            .add(hostname, NativeApiSecurity.getCertPinRoot())          // GTS Root R1
            .build()

        OkHttpClient.Builder()
            .certificatePinner(pinner)
            .cookieJar(cookieJar)
            .addInterceptor(userAgentInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            // Redirects disabled — AuthRepository inspects them manually
            // to distinguish NextAuth success from credential failure.
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    private fun buildRetrofit(): Retrofit {
        // serializeNulls() intentionally omitted — sending explicit JSON null for
        // optional fields (e.g. note, comment) causes server schema validation to
        // reject the request with 400.  Absent fields are treated as optional/unset
        // by the API, which is the correct behaviour.
        val gson = GsonBuilder().create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val authApiService: AuthApiService by lazy {
        buildRetrofit().create(AuthApiService::class.java)
    }

    val movieApiService: MovieApiService by lazy {
        buildRetrofit().create(MovieApiService::class.java)
    }
}
