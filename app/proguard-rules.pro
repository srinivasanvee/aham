# Add project specific ProGuard rules here.

# Preserve line numbers for crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# MediaPipe Tasks — GenAI / LlmInference
# R8 would otherwise strip the JNI bridge and task runner internals.
# ---------------------------------------------------------------------------
-keep class com.google.mediapipe.** { *; }
-keep class com.google.mediapipe.tasks.genai.** { *; }
-dontwarn com.google.mediapipe.**

# ---------------------------------------------------------------------------
# LiteRT / TensorFlow Lite (used internally by MediaPipe)
# ---------------------------------------------------------------------------
-keep class org.tensorflow.** { *; }
-keep class com.google.android.odml.** { *; }
-dontwarn org.tensorflow.**

# ---------------------------------------------------------------------------
# Native JNI — keep all methods called from native code
# ---------------------------------------------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------------------------------------------------------------------------
# Media3 / ExoPlayer — keep internal service and session classes
# ---------------------------------------------------------------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**