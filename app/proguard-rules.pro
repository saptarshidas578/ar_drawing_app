# ==============================================================================
# TraceAR — ProGuard / R8 Rules for Release Builds
# ==============================================================================

# Preserve annotations, signatures, and line numbers for stack traces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# ── 1. Native Methods (JNI) ──────────────────────────────────────────────────
# Keep all classes containing native methods and the methods themselves
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── 2. Google ARCore ─────────────────────────────────────────────────────────
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.core.**

# ── 3. SceneView & Filament ──────────────────────────────────────────────────
-keep class io.github.sceneview.** { *; }
-dontwarn io.github.sceneview.**

-keep class com.google.android.filament.** { *; }
-dontwarn com.google.android.filament.**

-keep class dev.romainguy.kotlin.math.** { *; }
-dontwarn dev.romainguy.kotlin.math.**

# ── 4. OpenCV for Android ───────────────────────────────────────────────────
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# ── 5. Enums & JSON Serialization ────────────────────────────────────────────
# Preserve Enum names used for persistent settings and JSON mapping
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve Project and Export data models serialized to/from JSON
-keep class com.tracear.app.data.** { *; }
-keep class com.tracear.app.ar.** {
    public <fields>;
    public <methods>;
}
-keep class com.tracear.app.export.** { *; }

# ── 6. Jetpack Compose & Accompanist ─────────────────────────────────────────
-dontwarn androidx.compose.**
-dontwarn com.google.accompanist.**
