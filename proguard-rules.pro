# 漠视诺言 - ProGuard 混淆规则

# 保留应用入口
-keep class com.moyan.MoyanApp { *; }
-keep class com.moyan.ui.MainActivity { *; }
-keep class com.moyan.ui.GameActivity { *; }
-keep class com.moyan.ui.SettingsActivity { *; }

# 保留模型类
-keep class com.moyan.model.** { *; }

# 保留引擎
-keep class com.moyan.engine.** { *; }

# TTS
-keep class android.speech.tts.** { *; }

# 不优化枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
