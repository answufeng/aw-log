# aw-log consumer ProGuard rules

-keep class com.answufeng.log.** { *; }

-dontwarn org.jetbrains.annotations.**

-keepclassmembers class * {
    public void onLog*(...);
}
