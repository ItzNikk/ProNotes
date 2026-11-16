# Preserve source file & line numbers for readable crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
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

# Serialized data models & ItemDoc hierarchy
-keep class com.pronotes.app.data.** { *; }
-keep class com.pronotes.app.editor.ItemDoc* { *; }
-keep class com.pronotes.app.editor.BrushDoc { *; }
-keep class com.pronotes.app.editor.PageDoc { *; }
-keep class com.pronotes.app.editor.NoteDoc { *; }
-keep class com.pronotes.app.editor.PagesManifest { *; }

# Jetpack Ink JNI & Stroke/Brush classes
-keep class androidx.ink.** { *; }
-keepclassmembers class androidx.ink.** {
    native <methods>;
    <init>(...);
}
-keep class androidx.input.motionprediction.** { *; }

# PdfBox Android & BouncyCastle
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
# PdfBox references this optional codec for JPX images, but it is not bundled.
# Suppress R8 missing-class errors; JPX image decoding requires the codec at runtime.
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn org.apache.commons.logging.**

# Coil 3 image loader
-keep class coil3.** { *; }
-dontwarn coil3.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**

# Baseline Profile installer
-keep class androidx.profileinstaller.** { *; }
