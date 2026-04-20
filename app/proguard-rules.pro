# ProGuard rules for ExoPlayer
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Retain generic signatures of TypeToken and its subclasses
-keepattributes Signature

# For Guava
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue
-keepclasseswithmembers class com.google.common.util.concurrent.** {
    <fields>;
    <methods>;
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
