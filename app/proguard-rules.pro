# Add project specific ProGuard rules here.

# Keep M&A model classes (Gson serialisation targets)
-keep class com.mna.streaming.data.model.** { *; }
-keep class com.mna.streaming.network.models.** { *; }
# Keep repository-local response wrappers used by Gson via reflection
-keep class com.mna.streaming.data.repository.** { *; }
# Preserve generic signatures so Gson can resolve List<T> field types at runtime
-keepattributes Signature

# OkHttp + Okio
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**

# Gson — preserve serialised field names; suppress generic-signature warnings
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Prevent stripping data-class fields used by Gson reflection
-keepclassmembers class com.mna.streaming.network.models.** {
    <fields>;
}
-keepclassmembers class com.mna.streaming.data.model.** {
    <fields>;
}

# WebView JavaScript interface (not used directly, but keep as a precaution)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Native security layer — never obfuscate the JNI bridges.
# Both classes are loaded by System.loadLibrary() at runtime; ProGuard must
# preserve their names and all method signatures exactly as declared.
-keep class com.mna.streaming.security.NativeApiSecurity { *; }
-keep class com.mna.streaming.security.IntegrityGuard { *; }

# Preserve the tamper-screen composable so it is never stripped.
-keep class com.mna.streaming.ui.security.TamperDetectedScreen { *; }

# Media3 / ExoPlayer (kept for future direct-stream fallback)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**
