# =====================================================================
# Native (JNI) & C++ Interop Rules
# Prevents UnsatisfiedLinkError, NoSuchMethodError, and runtime crashes
# =====================================================================

# Keep all native methods across the entire codebase with complete parameter signatures
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep classes declaring native methods
-keepclassmembers class * {
    native <methods>;
}

# Telegram MTProto & Network Layer (JNI C++ tgnet bindings)
-keep class org.telegram.tgnet.** { *; }
-keepclassmembers class org.telegram.tgnet.** { *; }
-keepinterface org.telegram.tgnet.** { *; }

# Native SQLite Engine (SQLiteDatabase, SQLiteCursor, SQLitePreparedStatement)
-keep class org.telegram.SQLite.** { *; }
-keepclassmembers class org.telegram.SQLite.** { *; }

# Native Dynamic Library Loader
-keep class org.telegram.messenger.NativeLoader { *; }
-keepclassmembers class org.telegram.messenger.NativeLoader { *; }

# Native Intro GL rendering engine
-keep class org.telegram.messenger.Intro { *; }
-keepclassmembers class org.telegram.messenger.Intro { *; }

# Native MRZ passport/ID recognition engine
-keep class org.telegram.messenger.MrzRecognizer { *; }
-keepclassmembers class org.telegram.messenger.MrzRecognizer { *; }

# Native Media & Opus audio recording/processing
-keep class org.telegram.messenger.MediaController {
    native <methods>;
}

# Native VoIP & WebRTC audio/video call engines
-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-keepinterface org.webrtc.** { *; }
-keep class org.telegram.messenger.voip.** { *; }
-keepclassmembers class org.telegram.messenger.voip.** { *; }

# Telegram core media and video subsystems
-keep class org.telegram.messenger.** { *; }
-keep class org.telegram.messenger.camera.** { *; }
-keep class org.telegram.messenger.secretmedia.** { *; }
-keep class org.telegram.messenger.support.** { *; }
-keep class org.telegram.messenger.time.** { *; }
-keep class org.telegram.messenger.video.** { *; }

# ExoPlayer JNI decoders and native audio/video buffers
-keep class com.google.android.exoplayer2.ext.** { *; }
-keepclassmembers class com.google.android.exoplayer2.ext.** { *; }
-keep class com.google.android.exoplayer2.decoder.** { *; }
-keepclassmembers class com.google.android.exoplayer2.decoder.** { *; }
-keep class com.google.android.exoplayer2.extractor.FlacStreamMetadata { *; }
-keep class com.google.android.exoplayer2.metadata.flac.PictureFrame { *; }
-keep class com.google.android.exoplayer2.decoder.SimpleDecoderOutputBuffer { *; }
-keep class com.google.android.exoplayer2.decoder.VideoDecoderOutputBuffer { *; }
-keep class org.telegram.ui.Stories.recorder.FfmpegAudioWaveformLoader { *; }

# Preserving critical reflection, JNI and annotation metadata
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Keep AndroidX and Google Keep annotations
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keep public class com.google.android.gms.* { public *; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keep class androidx.mediarouter.app.MediaRouteButton { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

# https://developers.google.com/ml-kit/known-issues#android_issues
-keep class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni { *; }

# Constant folding for resource integers may mean that a resource passed to this method appears to be unused. Keep the method to prevent this from happening.
-keep class com.google.android.exoplayer2.upstream.RawResourceDataSource {
  public static android.net.Uri buildRawResourceUri(int);
}

# Methods accessed via reflection in DefaultExtractorsFactory
-dontnote com.google.android.exoplayer2.ext.flac.FlacLibrary
-keepclassmembers class com.google.android.exoplayer2.ext.flac.FlacLibrary {

}

# Some members of this class are being accessed from native methods. Keep them unobfuscated.
# (VideoDecoderOutputBuffer is preserved in ExoPlayer JNI section)

-dontnote com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer
-keepclassmembers class com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer {
  <init>(android.os.Handler, com.google.android.exoplayer2.audio.AudioRendererEventListener, com.google.android.exoplayer2.audio.AudioProcessor[]);
}
-dontnote com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer
-keepclassmembers class com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer {
  <init>(android.os.Handler, com.google.android.exoplayer2.audio.AudioRendererEventListener, com.google.android.exoplayer2.audio.AudioProcessor[]);
}
-dontnote com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer
-keepclassmembers class com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer {
  <init>(android.os.Handler, com.google.android.exoplayer2.audio.AudioRendererEventListener, com.google.android.exoplayer2.audio.AudioProcessor[]);
}

# Constructors accessed via reflection in DefaultExtractorsFactory
-dontnote com.google.android.exoplayer2.ext.flac.FlacExtractor
-keepclassmembers class com.google.android.exoplayer2.ext.flac.FlacExtractor {
  <init>();
}

# Constructors accessed via reflection in DefaultDownloaderFactory
-dontnote com.google.android.exoplayer2.source.dash.offline.DashDownloader
-keepclassmembers class com.google.android.exoplayer2.source.dash.offline.DashDownloader {
  <init>(android.net.Uri, java.util.List, com.google.android.exoplayer2.offline.DownloaderConstructorHelper);
}
-dontnote com.google.android.exoplayer2.source.hls.offline.HlsDownloader
-keepclassmembers class com.google.android.exoplayer2.source.hls.offline.HlsDownloader {
  <init>(android.net.Uri, java.util.List, com.google.android.exoplayer2.offline.DownloaderConstructorHelper);
}
-dontnote com.google.android.exoplayer2.source.smoothstreaming.offline.SsDownloader
-keepclassmembers class com.google.android.exoplayer2.source.smoothstreaming.offline.SsDownloader {
  <init>(android.net.Uri, java.util.List, com.google.android.exoplayer2.offline.DownloaderConstructorHelper);
}

# Constructors accessed via reflection in DownloadHelper
-dontnote com.google.android.exoplayer2.source.dash.DashMediaSource$Factory
-keepclasseswithmembers class com.google.android.exoplayer2.source.dash.DashMediaSource$Factory {
  <init>(com.google.android.exoplayer2.upstream.DataSource$Factory);
}
-dontnote com.google.android.exoplayer2.source.hls.HlsMediaSource$Factory
-keepclasseswithmembers class com.google.android.exoplayer2.source.hls.HlsMediaSource$Factory {
  <init>(com.google.android.exoplayer2.upstream.DataSource$Factory);
}
-dontnote com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource$Factory
-keepclasseswithmembers class com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource$Factory {
  <init>(com.google.android.exoplayer2.upstream.DataSource$Factory);
}

# Huawei Services
-keep class com.huawei.hianalytics.**{ *; }
-keep class com.huawei.updatesdk.**{ *; }
-keep class com.huawei.hms.**{ *; }

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**

-keep class io.nano.tex.** {*;}

# JLatexMath: macro/atom classes are loaded reflectively by Class.forName
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**

# Use -keep to explicitly keep any other classes shrinking would remove
-dontoptimize
-dontobfuscate