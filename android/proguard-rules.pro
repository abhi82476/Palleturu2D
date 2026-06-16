# ─── Village Legends 2D – ProGuard Rules ─────────────────────
# Applied to release builds via android/build.gradle

# LibGDX core
-keep class com.badlogic.gdx.** { *; }
-dontwarn com.badlogic.gdx.**
-keepclassmembers class com.badlogic.gdx.** { *; }

# Android backend
-keep class com.badlogic.gdx.backends.android.** { *; }

# Our game classes – keep everything in the villagelegends namespace
-keep class com.villagelegends.** { *; }
-keepclassmembers class com.villagelegends.** { *; }

# Keep data classes for LibGDX Json serialisation
-keepclassmembers class com.villagelegends.data.** {
    <fields>;
    <init>();
}

# Gson / Json serialisable classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.view.View

# Multidex
-keep class androidx.multidex.** { *; }

# Suppress common warnings
-dontwarn sun.misc.**
-dontwarn java.awt.**
-dontwarn javax.security.**
-dontwarn org.lwjgl.**

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
