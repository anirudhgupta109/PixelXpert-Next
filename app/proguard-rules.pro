# ProGuard/R8 rules for the PXN module.
#
# This is an Xposed module: LSPosed loads it by name from
# META-INF/xposed/java_init.list, and the mod packs hook other processes'
# internals reflectively. Anything R8 renames or strips can break that
# silently at runtime rather than at build time, so the rules below are
# deliberately conservative and mirror legacy app/proguard.rules.

# ---------------------------------------------------------------------------
# Xposed entry point and mod packs
# ---------------------------------------------------------------------------

# The entry point is loaded reflectively by the framework via
# META-INF/xposed/java_init.list - it must never be renamed or stripped.
-keep class sh.anirudhgupta109.pxn.xposed.XPLauncher { *; }
-keep class * extends sh.anirudhgupta109.pxn.xposed.XposedModPack { *; }

# The generated registry (ModPacks) holds ::class.java references to every
# pack and is read by XPLauncher. Keep the registry and its entry shape.
-keep class sh.anirudhgupta109.pxn.xposed.ModPacks { *; }
-keep class sh.anirudhgupta109.pxn.annotations.ModPackData { *; }

# Whole xposed tree, matching legacy's `-keep class <pkg>.**`. Packs reach
# framework and SystemUI internals by name; renaming our own members that
# legacy call sites expect is not worth the obfuscation.
-keep class sh.anirudhgupta109.pxn.xposed.** { *; }

# ---------------------------------------------------------------------------
# libxposed API - provided at runtime by the framework, never packaged here
# ---------------------------------------------------------------------------
-dontwarn io.github.libxposed.**
-keep class io.github.libxposed.** { *; }

# Legacy Xposed helpers referenced from the packs.

# ---------------------------------------------------------------------------
# AIDL - generated stubs are referenced across process boundaries
# ---------------------------------------------------------------------------
-keep class sh.anirudhgupta109.pxn.IPixelXpertProxy { *; }
-keep class sh.anirudhgupta109.pxn.IPixelXpertProxy$Stub { *; }
-keep class sh.anirudhgupta109.pxn.IRootProviderService { *; }
-keep class sh.anirudhgupta109.pxn.IRootProviderService$Stub { *; }

# ---------------------------------------------------------------------------
# Kotlin runtime - strip intrinsics checks (mirrors legacy)
# ---------------------------------------------------------------------------
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void throwUninitializedPropertyAccessException(...);
}

# ---------------------------------------------------------------------------
# Optional / vendor-provided classes referenced by androidx.window and okio.
# These are supplied by the device (or by a companion artifact) rather than
# packaged here, so R8 can never resolve them. Suppressing the warning is the
# documented remedy; the references sit behind runtime feature checks.
# ---------------------------------------------------------------------------
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
-dontwarn javax.annotation.**

# ---------------------------------------------------------------------------
# Attributes
# ---------------------------------------------------------------------------
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod,SourceFile,LineNumberTable

# ---------------------------------------------------------------------------
# Debug hook audit data models for Gson serialization/deserialization
# ---------------------------------------------------------------------------
-keep class sh.anirudhgupta109.pxn.ui.debug.model.** { *; }
-keepclassmembers class sh.anirudhgupta109.pxn.ui.debug.model.** { *; }
-keep class androidx.core.content.FileProvider { *; }
