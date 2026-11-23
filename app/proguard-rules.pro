# Add project specific ProGuard rules here.
# Moshi
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory { *; }
-keep class **.*Dto* { *; }

# Data models
-keep class com.example.data.model.** { *; }

# Firestore models
-keep class com.example.data.model.Cloud* { *; }
-keep class com.example.data.model.UserProfile { *; }
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Moshi
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
-keep class * extends com.squareup.moshi.JsonAdapter
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep class **.*Dto { *; }
-keep class **.*Dto$* { *; }

# Data Models
-keep class com.example.data.model.** { *; }

# Firestore Models
-keep class com.example.data.firebase.Cloud* { *; }
-keep class com.example.data.firebase.UserProfile { *; }
