# Keep ProNotes data and serialization models
-keep class com.pronotes.app.data.** { *; }
-keep class com.pronotes.app.editor.PageDoc { *; }
-keep class com.pronotes.app.editor.PageItem { *; }
-keep class com.pronotes.app.editor.PageItem$* { *; }
-keep class com.pronotes.app.editor.StrokePoint { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.pronotes.app.**$$serializer { *; }
-keepclassmembers class com.pronotes.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.pronotes.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Jetpack Ink native bridge & JNI classes
-keep class androidx.ink.** { *; }
-dontwarn androidx.ink.**
-keep class androidx.graphics.** { *; }
-dontwarn androidx.graphics.**

# PdfBox-Android internal font & cmap reflection
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.tom_roush.**
-dontwarn org.bouncycastle.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn java.beans.**
-dontwarn javax.naming.**
-dontwarn javax.jai.**
-dontwarn javax.media.**
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.harmony.**
-dontwarn org.levigo.**
-dontwarn dalvik.system.**
-dontwarn sun.misc.**
-dontwarn java.lang.instrument.**
-dontwarn kotlinx.coroutines.debug.**

# Fix missing JP2 classes error in R8
-dontwarn com.gemalto.jp2.**
-ignorewarnings

# BroadcastReceivers & Services referenced in AndroidManifest
-keep public class com.pronotes.app.UpdatedReceiver
-keep public class com.pronotes.app.InstallResultReceiver
-keep public class com.pronotes.app.UpdateCheckReceiver
-keep public class com.pronotes.app.data.UpdateDownloadService
