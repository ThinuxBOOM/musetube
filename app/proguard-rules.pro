# --- JS bridge: WebView calls these by name via reflection ---
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.musetube.app.bridge.MusicBridge { *; }
-keepattributes JavascriptInterface,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- App classes reached from the manifest / WebView (small, keep; cost is negligible) ---
-keep class com.musetube.app.webview.MusicWebViewClient { *; }
-keep class com.musetube.app.webview.MusicWebChromeClient { *; }
-keep class com.musetube.app.webview.injections.** { *; }
-keep class com.musetube.app.webview.helpers.** { *; }
-keep class com.musetube.app.service.MediaNotificationService { *; }
-keep class com.musetube.app.proxy.LocalProxyManager { *; }
-keep class com.musetube.app.ui.SplashActivity { *; }
-keep class com.musetube.app.ui.MainActivity { *; }
-keep class com.musetube.app.ui.OfflineActivity { *; }
-keep class com.musetube.app.service.OfflineMediaService { *; }
-keep class com.musetube.app.service.DownloadService { *; }

# --- BouncyCastle: only the cert-builder APIs LocalProxyManager actually uses ---
-keep class org.bouncycastle.asn1.x500.** { *; }
-keep class org.bouncycastle.asn1.x509.** { *; }
-keep class org.bouncycastle.cert.jcajce.** { *; }
-keep class org.bouncycastle.operator.jcajce.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.annotation.concurrent.GuardedBy

# --- kotlinx.serialization: keep generated serializers for InnerTube models ---
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep class com.musetube.app.innertube.models.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# --- Release: strip verbose/debug/info/warn logs (errors still go to Crashlytics) ---
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** println(...);
}

# NewPipe / Rhino - java.beans.* not available on Android on device
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn javax.script.**
# do not add -keep class org.mozilla.javascript.** - it pulls in classes
# referencing java.beans.* which breaks ART install-time verification
