# ===================================================================
# SynapMusic ProGuard / R8 Keep Rules
# ===================================================================

# --- Keep Attributes for Reflection & Serialization ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault

# --- Keep Kotlin & Coroutines ---
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    *** completion;
}
-keepclassmembers class * {
    @kotlin.jvm.JvmField *;
}

# --- Keep kotlinx.serialization Classes & Serializers ---
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    *** $serializer;
}
-keep class com.synap.pulse.models.** { *; }
-keep class com.synap.pulse.requests.** { *; }
-keep class com.synap.lyrics.** { *; }
-keep class com.synap.music.models.** { *; }
-keepclassmembers class com.synap.music.models.** { *; }

# --- Keep Enums for SharedPreferences, Reflection & Room DB ---
-keep class com.synap.music.enums.** { *; }
-keepclassmembers enum com.synap.music.enums.** { *; }
-keepclassmembers class * extends java.lang.Enum { *; }

# --- Keep Room Database & DAOs ---
-keep class * extends androidx.room.RoomDatabase
-keep class com.synap.music.Database** { *; }
-keep class com.synap.music.Database_Impl { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.* *;
}
-dontwarn androidx.room.paging.**

# --- Keep YouTubeDL / Python / FFmpeg JNI Libraries ---
-keep class com.yausername.** { *; }
-keepclassmembers class com.yausername.** { *; }
-keep class io.github.junkfood02.** { *; }
-keepclassmembers class io.github.junkfood02.** { *; }
-keep class com.yausername.youtubedl_android.** { *; }
-keepclassmembers class com.yausername.youtubedl_android.** { *; }
-keep class io.github.junkfood02.youtubedl_android.** { *; }
-keepclassmembers class io.github.junkfood02.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keepclassmembers class com.yausername.ffmpeg.** { *; }
-keep class com.synap.music.utils.MediaFetchUtils** { *; }
-keepclassmembers class com.synap.music.utils.MediaFetchUtils** { *; }
-keep class com.synap.music.service.CompressionManager** { *; }
-keepclassmembers class com.synap.music.service.CompressionManager** { *; }
-dontwarn com.yausername.**
-dontwarn io.github.junkfood02.**

# --- Keep Zstd JNI ---
-keep class com.github.luben.zstd.** { *; }
-keepclassmembers class com.github.luben.zstd.** { *; }

# --- Keep Ktor Client & Serialization ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# --- Keep Media3 & ExoPlayer ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- Keep Coil Image Loader ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- Keep WorkManager ---
-keep class androidx.work.** { *; }

# --- Keep Compose Persist / Routing / Reordering ---
-keep class com.synap.compose.persist.** { *; }
-keep class com.synap.compose.routing.** { *; }
-keep class com.synap.compose.reordering.** { *; }

# --- Keep Desugaring ---
-dontwarn com.android.tools.r8.**

# --- Suppress Warnings for External Libraries ---
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
-dontwarn com.synap.compose.persist.**
-dontwarn com.synap.compose.reordering.**
-dontwarn com.synap.compose.routing.**
