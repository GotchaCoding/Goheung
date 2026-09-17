# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================
# Firebase Firestore / Realtime Database
# ============================================
# Keep all data model classes (Firebase uses reflection for deserialization)
-keep class com.goheung.app.data.model.** { *; }
-keepclassmembers class com.goheung.app.data.model.** {
    public <init>();
    public <init>(...);
}

# Firestore가 제네릭 필드(List<CertificateItem> 등)를 역직렬화하려면 필요.
# 지우면 minify된 release 빌드에서만 조용히 깨진다.
-keepattributes Signature
-keepattributes *Annotation*

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ============================================
# Hilt / Dagger
# ============================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers class * {
    @dagger.hilt.* <fields>;
    @javax.inject.* <fields>;
    @dagger.hilt.* <methods>;
    @javax.inject.* <methods>;
}

# ============================================
# Kotlin
# ============================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
