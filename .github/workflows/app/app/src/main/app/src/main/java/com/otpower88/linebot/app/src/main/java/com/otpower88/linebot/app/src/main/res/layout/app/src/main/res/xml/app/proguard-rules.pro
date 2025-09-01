# otpower88 LINE搶單機器人混淆規則

# 保留無障礙服務相關類
-keep class com.otpower88.linebot.LineAccessibilityService { *; }
-keep class android.accessibilityservice.** { *; }

# 保留主Activity
-keep class com.otpower88.linebot.MainActivity { *; }

# 保留Android基礎組件
-keep class android.app.** { *; }
-keep class android.widget.** { *; }
-keep class android.view.** { *; }

# 保留反射使用的類
-keepclassmembers class * {
    public <init>(...);
    public <init>();
}

# 保留枚舉
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留序列化相關
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 忽略警告
-dontwarn android.support.**
-dontwarn java.lang.invoke.**
