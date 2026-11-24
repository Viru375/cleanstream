# Suppress lint for known false-positive on WebView JavaScript
-keep class com.cleanstream.app.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep WebView JavaScript interface methods from being obfuscated
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
