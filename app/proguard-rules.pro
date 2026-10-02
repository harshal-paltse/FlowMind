# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep,allowobfuscation,allowshrinking @interface dagger.hilt.**

# Room Database / SQLCipher
-keep class androidx.room.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Firebase Auth & App Check
-keep class com.google.firebase.** { *; }

# TensorFlow Lite & ML Kit
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }

# Models
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}
-keep class com.example.flowmind.domain.models.** { *; }
