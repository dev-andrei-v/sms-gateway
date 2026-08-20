# Keep Kotlinx Serialization metadata for our WS protocol DTOs
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class ro.andreidev.sms.middleware.**$$serializer { *; }
-keepclassmembers class ro.andreidev.sms.middleware.** {
    *** Companion;
}
-keepclasseswithmembers class ro.andreidev.sms.middleware.** {
    kotlinx.serialization.KSerializer serializer(...);
}
