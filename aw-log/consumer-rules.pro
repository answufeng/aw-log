# aw-log consumer ProGuard rules
# 保留库的公开 API，确保混淆后宿主应用可正常调用

# ===========================================================
# DSL 注解
# ===========================================================
-keep @interface com.answufeng.log.AwLogDsl

# ===========================================================
# 核心类
# ===========================================================
-keep class com.answufeng.log.AwLogger {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
    public static *** json(...);
    public static *** xml(...);
    public static *** init(...);
    public static *** flush(...);
    public static *** reset(...);
    public static *** isInitialized();
    public static *** setMinPriority(...);
    public static *** isLoggable(...);
    public static *** getFileDir();
}
-keep class com.answufeng.log.AwLogConfig { *; }

# ===========================================================
# Formatter
# ===========================================================
-keep class com.answufeng.log.AwLogFormatter { public *; }
-keep class com.answufeng.log.AwLogFormatter$Companion { public *; }
-keep class com.answufeng.log.AwFormatterDsl { *; }

# ===========================================================
# Interceptor
# ===========================================================
-keep class com.answufeng.log.AwLogInterceptor { public *; }
-keep class com.answufeng.log.AwLogInterceptor$Chain { public *; }
-keep class com.answufeng.log.AwLogInterceptor$LogResult { public *; }
-keep class com.answufeng.log.AwLogInterceptor$LogResult$Accepted { public *; }
-keep class com.answufeng.log.AwLogInterceptor$LogResult$Rejected { public *; }

# ===========================================================
# Desensitize
# ===========================================================
-keep class com.answufeng.log.AwDesensitizeInterceptor { public *; }
-keep class com.answufeng.log.AwDesensitizeInterceptor$Companion { public *; }
-keep class com.answufeng.log.AwDesensitizeInterceptor$Strategy {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public <methods>;
}
-keep class com.answufeng.log.AwDesensitizeInterceptor$DesensitizeRule { *; }
-keep class com.answufeng.log.AwDesensitizeInterceptor$DesensitizeBuilder { *; }

# ===========================================================
# Listener
# ===========================================================
-keep class com.answufeng.log.AwLogListener { public *; }

# ===========================================================
# File Manager
# ===========================================================
-keep class com.answufeng.log.AwLogFileManager { public *; }
-keep class com.answufeng.log.AwLogFileManager$Companion { public *; }

# ===========================================================
# Internal Trees（通过 Timber 间接使用）
# ===========================================================
-keep class com.answufeng.log.AwDebugTree { *; }
-keep class com.answufeng.log.AwFileTree { *; }
-keep class com.answufeng.log.AwCrashTree { *; }

# ===========================================================
# Kotlin 元数据
# ===========================================================
-keepattributes *Annotation*, Signature, EnclosingMethod, InnerClasses
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.** { *; }
-keepclassmembers class * {
    @kotlin.Metadata *;
}
