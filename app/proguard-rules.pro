# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Firebase
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }

# Cloudinary
-keep class com.cloudinary.** { *; }

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# AndroidX and Support Library
-keep class androidx.appcompat.widget.** { *; }
-keep class androidx.recyclerview.widget.** { *; }

# Models/POJOs (Ensure your data classes are not obfuscated if used with Firestore/Gson)
-keep class com.koshub.psdku.KosItem { *; }
-keep class com.koshub.psdku.models.** { *; }

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
