# aw-log ProGuard Rules
# 此文件用于库自身的 release 构建混淆规则
# Consumer-facing rules（供使用者混淆时使用）位于 consumer-rules.pro

# ===========================================================
# 保留 Kotlin 元数据和注解（精确匹配）
# ===========================================================
-keepattributes *Annotation*, Signature, EnclosingMethod, InnerClasses, Exceptions
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.* { *; }

# ===========================================================
# 保留枚举（供 Strategy 使用）
# ===========================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===========================================================
# 保留 Serializable（如有需要）
# ===========================================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
