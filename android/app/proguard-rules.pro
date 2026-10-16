# Retrofit / OkHttp / kotlinx-serialization
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class tj.osonaloqa.chat.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class tj.osonaloqa.chat.**$$serializer { *; }
