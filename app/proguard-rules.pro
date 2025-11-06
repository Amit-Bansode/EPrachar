# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep all receivers and services
-keep class com.chordz.eprachar.*Receiver { *; }
-keep class com.chordz.eprachar.*Service { *; }
-keep class com.chordz.eprachar.EPracharApplication { *; }

# Keep BroadcastReceiver classes
-keep public class * extends android.content.BroadcastReceiver

# Keep Service classes
-keep public class * extends android.app.Service

# Keep OkHttp classes (for network requests)
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep JSON classes
-keep class org.json.** { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile