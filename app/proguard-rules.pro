# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Credential Manager / Google Sign-In
-keep class com.google.android.libraries.identity.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class androidx.credentials.** { *; }

# Keep Kotlin metadata for reflection
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
