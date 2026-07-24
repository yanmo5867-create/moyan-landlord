package com.moyan;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

/**
 * 漠视诺言 - 全局Application
 * 负责初始化全局配置：帧率、画质、斗币、段位、音效音量
 */
public class MoyanApp extends Application {

    public static final String PREFS_NAME = "moyan_prefs";

    // 画质档位 1~7
    public static final int QUALITY_LOWEST = 1;
    public static final int QUALITY_LOW = 2;
    public static final int QUALITY_MEDIUM_LOW = 3;
    public static final int QUALITY_MEDIUM = 4;
    public static final int QUALITY_MEDIUM_HIGH = 5;
    public static final int QUALITY_HIGH = 6;
    public static final int QUALITY_ULTRA = 7;

    // 帧率
    public static final int FPS_30 = 30;
    public static final int FPS_60 = 60;
    public static final int FPS_90 = 90;
    public static final int FPS_120 = 120;

    private static MoyanApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initDefaults();
    }

    private void initDefaults() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!sp.contains("quality")) sp.edit().putInt("quality", QUALITY_MEDIUM).apply();
        if (!sp.contains("fps")) sp.edit().putInt("fps", FPS_60).apply();
        if (!sp.contains("coin")) sp.edit().putInt("coin", 1000).apply();
        if (!sp.contains("rank")) sp.edit().putInt("rank", 0).apply();
        if (!sp.contains("sfx_volume")) sp.edit().putFloat("sfx_volume", 0.8f).apply();
        if (!sp.contains("bgm_volume")) sp.edit().putFloat("bgm_volume", 0.5f).apply();
        if (!sp.contains("voice_volume")) sp.edit().putFloat("voice_volume", 0.8f).apply();
        if (!sp.contains("luck_value")) sp.edit().putInt("luck_value", 50).apply();
        if (!sp.contains("card_counter")) sp.edit().putBoolean("card_counter", false).apply();
        if (!sp.contains("night_mode")) sp.edit().putBoolean("night_mode", false).apply();
    }

    public static MoyanApp getInstance() {
        return instance;
    }

    public SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** 获取当前画质档位 */
    public int getQuality() {
        return getPrefs().getInt("quality", QUALITY_MEDIUM);
    }

    /** 获取当前帧率 */
    public int getFps() {
        return getPrefs().getInt("fps", FPS_60);
    }

    /** 获取斗币 */
    public int getCoin() {
        return getPrefs().getInt("coin", 1000);
    }

    /** 增加/扣除斗币（最低0） */
    public void addCoin(int delta) {
        int cur = getCoin() + delta;
        if (cur < 0) cur = 0;
        getPrefs().edit().putInt("coin", cur).apply();
    }
}
