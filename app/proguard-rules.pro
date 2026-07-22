# Add project specific ProGuard rules here.

# Keep M&A model classes (Gson serialisation targets)
-keep class com.mna.streaming.data.model.** { *; }
-keep class com.mna.streaming.network.models.** { *; }

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

# Native security layer — never obfuscate the JNI bridge
-keep class com.mna.streaming.security.NativeApiSecurity { *; }

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
