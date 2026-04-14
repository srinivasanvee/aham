# Add project specific ProGuard rules here.

# Preserve line numbers for crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# LiteRT-LM — on-device LLM inference
# ---------------------------------------------------------------------------
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# LiteRT flatbuffer / TFLite internals used by LiteRT-LM
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