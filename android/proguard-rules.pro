# Project ProGuard/R8 rules. R8 full mode is the AGP default.
#
# Uncomment to keep line numbers in release stack traces:
#-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ──────────────────────────────────────────────────
# firebase-license signs a @Serializable payload; the app also uses serialization for sync. Standard
# rules so R8 does not strip generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-if @kotlinx.serialization.Serializable class *
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# ── firebase-license (config-sync gate) ────────────────────────────────────
-keep class io.github.vadymhrnyk.firebaselicense.** { *; }
-dontwarn io.github.vadymhrnyk.firebaselicense.**

# ── GitLive Firebase (dev.gitlive) — Firestore access for the config-sync gate ─
-keep class dev.gitlive.firebase.** { *; }
-dontwarn dev.gitlive.firebase.**

# ── Firebase / Firestore / gRPC transport ──────────────────────────────────
# Most Firebase artifacts bundle consumer rules; these add safety for the Firestore + gRPC stack.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class io.grpc.** { *; }
-dontwarn io.grpc.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-dontwarn com.google.api.**
-dontwarn com.google.cloud.**

# ── BouncyCastle (Ed25519 signature verification) ──────────────────────────
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ── multiplatform-settings (SharedPreferences-backed gate store) ────────────
-dontwarn com.russhwolf.settings.**

# ── Room / WorkManager ─────────────────────────────────────────────────────
# WorkManager instantiates its generated Room WorkDatabase_Impl by reflection; R8 full mode strips
# the no-arg constructor without this, crashing startup with NoSuchMethodException.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.**
