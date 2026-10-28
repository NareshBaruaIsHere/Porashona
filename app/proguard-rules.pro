# ==============================================================================
# ProGuard / R8 Configuration for Release Builds (Google Play Store)
# Preserves ML Kit Vision/OCR, Firebase ComponentDiscovery Registrars,
# Retrofit, OkHttp, Room Database, and Kotlinx Serialization in release builds.
# ==============================================================================

# 1. Preserve Essential Reflection, Generic Signature & Stacktrace Attributes
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, AnnotationDefault, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# 2. Strip Verbose/Debug Logging in Release Builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 3. Android Manifest Entry Points
-keep public class com.example.ExamTrackApplication { <init>(); }
-keep public class com.example.MainActivity { <init>(); }
-keep public class com.example.widget.ExamTrackWidgetProvider { <init>(); }
-keep public class com.example.receiver.ExamNotificationReceiver { <init>(); }
-keep public class com.example.receiver.BootReceiver { <init>(); }

# 4. Google ML Kit Text Recognition, Vision Common & ComponentRegistrar Rules
#    CRITICAL: ML Kit initializes via MlKitInitProvider + ComponentDiscovery,
#    which instantiates ComponentRegistrar implementations (TextRegistrar,
#    VisionCommonRegistrar, CommonComponentRegistrar) via reflection
#    (Class.forName(...).getDeclaredConstructor().newInstance()).
-keep interface com.google.firebase.components.ComponentRegistrar { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
    *;
}
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.odml.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_bundled_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.gms.dynamite.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# Keep Google Play Services & AndroidX Keep/Dynamite/JNI Annotations
-keep @interface com.google.android.gms.common.annotation.KeepName
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keep @interface com.google.android.gms.common.util.DynamiteApi
-keep @com.google.android.gms.common.util.DynamiteApi public class * {
    public <fields>;
    public <methods>;
}
-keep class com.google.android.apps.common.proguard.UsedBy*
-keep @com.google.android.apps.common.proguard.UsedBy* class * {
    <init>();
    *;
}
-keepclassmembers class * {
    @com.google.android.apps.common.proguard.UsedBy* *;
}
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}

# 5. Explicitly Keep App OCR, Gemini Vision & Backup Serialization Classes
-keep class com.example.util.OcrScannerHelper { *; }
-keep class com.example.util.GeminiVisionOcrService { *; }
-keep interface com.example.util.GeminiVisionRestApi { *; }
-keep class com.example.util.TimetableScanOutcome { *; }
-keep class com.example.util.AdmitCardScanOutcome { *; }
-keep class com.example.util.ScannedRoutineCandidate { *; }
-keep class com.example.util.ScannedAdmitCardCandidate { *; }
-keep class com.example.util.GeminiPart** { *; }
-keep class com.example.util.GeminiInlineData** { *; }
-keep class com.example.util.GeminiContent** { *; }
-keep class com.example.util.GeminiGenerationConfig** { *; }
-keep class com.example.util.GeminiSchema** { *; }
-keep class com.example.util.GeminiGenerateContentRequest** { *; }
-keep class com.example.util.GeminiCandidate** { *; }
-keep class com.example.util.GeminiGenerateContentResponse** { *; }
-keep class com.example.util.AiTimetableRowDto** { *; }
-keep class com.example.util.AiTimetableExtractionDto** { *; }
-keep class com.example.util.AiAdmitCardExtractionDto** { *; }
-keep class com.example.util.BackupSubjectDto** { *; }
-keep class com.example.util.BackupTopicDto** { *; }
-keep class com.example.util.BackupStudySessionDto** { *; }
-keep class com.example.util.BackupClassRoutineDto** { *; }
-keep class com.example.util.ExamTrackBackupPayload** { *; }

# 6. Room Database & Entity Rules
-keep class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}
-keep class com.example.data.model.** { *; }
-dontwarn androidx.room.paging.**

# 7. Lifecycle ViewModel Constructors
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}

# 8. Kotlinx Serialization Rules
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keep,includedescriptorclasses class <1>$$serializer { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    synthetic <init>(int, **, kotlinx.serialization.internal.SerializationConstructorMarker);
}

# 9. Retrofit, OkHttp, Coroutines & Optional JVM TLS Provider Warnings
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.datastore.**
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.odml.**
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
