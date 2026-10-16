# programmed by parv
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
#-renamesourcefileattribute SourceFile

# PyTorch JNI / Reflection rules
-keep class org.pytorch.** { *; }
-keep class com.facebook.jni.** { *; }

# Ignore missing annotation classes during minification
-dontwarn javax.annotation.**
# Room Database Rules
-keep class com.parv.safetexts.MessageEntity { *; }
-keep class com.parv.safetexts.ChatContextEntity { *; }
-keep class com.parv.safetexts.MessageDao { *; }
-keep class com.parv.safetexts.ChatContextDao { *; }
-keep class com.parv.safetexts.AppDatabase { *; }
-dontwarn androidx.room.**

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
-dontwarn com.google.auto.value.**

