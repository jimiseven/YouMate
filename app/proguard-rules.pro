# ProGuard / R8 Rules for YouMate

# NewPipeExtractor and Mozilla Rhino JS Engine
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
-dontwarn org.mozilla.javascript.**
-dontwarn java.beans.**
-dontwarn org.schabi.newpipe.extractor.**
-keep class org.schabi.newpipe.extractor.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Kotlin Coroutines & Reflection
-keep class kotlinx.coroutines.** { *; }
