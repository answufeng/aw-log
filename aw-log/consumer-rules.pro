# aw-log consumer ProGuard rules

-keep class com.answufeng.log.AwLogger
-keep class com.answufeng.log.AwLogConfig
-keep class com.answufeng.log.AwLogFormatter
-keep class com.answufeng.log.AwLogFormatter$Companion { public *; }
-keep class com.answufeng.log.AwLogInterceptor { public *; }
-keep class com.answufeng.log.AwLogInterceptor$Chain { public *; }
-keep class com.answufeng.log.AwLogInterceptor$LogResult { public *; }
-keep class com.answufeng.log.AwLogInterceptor$LogResult$Accepted { public *; }
-keep class com.answufeng.log.AwLogInterceptor$LogResult$Rejected { public *; }
-keep class com.answufeng.log.AwLogFileManager { public *; }
-keep class com.answufeng.log.AwLogFileManager$Companion { public *; }
-keep class com.answufeng.log.AwDesensitizeInterceptor
-keep class com.answufeng.log.AwDesensitizeInterceptor$*
-keep class com.answufeng.log.AwFormatterDsl
-keep class com.answufeng.log.AwLogListener { public *; }

-keep @interface com.answufeng.log.AwLogDsl

-keepclassmembers class com.answufeng.log.AwLogger {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
    public static *** json(...);
    public static *** init(...);
    public static *** flush(...);
    public static *** reset(...);
}

-dontwarn org.jetbrains.annotations.**
