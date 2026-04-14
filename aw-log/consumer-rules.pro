# aw-log consumer ProGuard rules

-keep class com.answufeng.log.AwLogger { *; }
-keep class com.answufeng.log.AwLogConfig { *; }
-keep class com.answufeng.log.AwLogFormatter { *; }
-keep class com.answufeng.log.AwLogFormatter$Companion { *; }
-keep class com.answufeng.log.AwLogInterceptor { *; }
-keep class com.answufeng.log.AwLogInterceptor$Chain { *; }
-keep class com.answufeng.log.AwLogInterceptor$LogResult { *; }
-keep class com.answufeng.log.AwLogInterceptor$LogResult$Accepted { *; }
-keep class com.answufeng.log.AwLogInterceptor$LogResult$Rejected { *; }
-keep class com.answufeng.log.AwLogFileManager { *; }
-keep class com.answufeng.log.AwLogFileManager$Companion { *; }
-keep class com.answufeng.log.AwDesensitizeInterceptor { *; }
-keep class com.answufeng.log.AwDesensitizeInterceptor$* { *; }
-keep class com.answufeng.log.AwFormatterDsl { *; }
-keep class com.answufeng.log.AwCrashTree { *; }

-keep @interface com.answufeng.log.AwLogDsl

-keepclassmembers class com.answufeng.log.AwLogger {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
    public static *** json(...);
    public static *** tag(...);
    public static *** init(...);
    public static *** flush(...);
    public static *** reset(...);
}

-dontwarn org.jetbrains.annotations.**
