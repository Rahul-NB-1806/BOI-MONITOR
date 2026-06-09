# ─────────────────────────────────────────────────────────────────────────────
#  BOI Monitor — ProGuard / R8 Rules
# ─────────────────────────────────────────────────────────────────────────────

# ── Firebase Firestore ────────────────────────────────────────────────────────
# Keep all model classes used for Firestore serialization/deserialization
-keep class com.boi.monitor.model.** { *; }
-keepclassmembers class com.boi.monitor.model.** {
    public <init>();
    public <fields>;
}

# Firebase itself
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Gson / JSON ───────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── AndroidX ──────────────────────────────────────────────────────────────────
-keep class androidx.** { *; }
-dontwarn androidx.**

# ── Notification Listener Service ────────────────────────────────────────────
-keep class com.boi.monitor.service.BOINotificationListenerService { *; }
-keep class com.boi.monitor.service.BootReceiver { *; }

# ── Application class ────────────────────────────────────────────────────────
-keep class com.boi.monitor.BOIApplication { *; }

# ── General Android ──────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Application

# ── Firebase Crashlytics ─────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.crashlytics.** { *; }

# Remove verbose logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
