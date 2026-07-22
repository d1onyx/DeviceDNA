# Project ProGuard/R8 rules. R8 full mode is the AGP default.
#
# Uncomment to keep line numbers in release stack traces:
#-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile

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

-keep class dev.gitlive.firebase.** { *; }
-dontwarn dev.gitlive.firebase.**

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class io.grpc.** { *; }
-dontwarn io.grpc.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-dontwarn com.google.api.**
-dontwarn com.google.cloud.**

-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-dontwarn com.russhwolf.settings.**

# ── Room / WorkManager ─────────────────────────────────────────────────────
# WorkManager instantiates its generated Room WorkDatabase_Impl by reflection; R8 full mode strips
# the no-arg constructor without this, crashing startup with NoSuchMethodException.
# androidx.room is a transitive dependency of work-runtime (not declared directly), so the IDE
# shrinker inspection cannot resolve the class name — the keep rule is correct; suppress the warning.
#noinspection ShrinkerUnresolvedReference
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.**
