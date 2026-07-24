package com.moyan.engine;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREFS = "moyan_settings";
    private SharedPreferences sp;

    // 设置项
    private int fpsLevel;        // 0=30, 1=60, 2=90, 3=120
    private int qualityLevel;    // 0~6
    private boolean cardCounterEnabled;
    private boolean soundEnabled;
    private boolean vibrationEnabled;
    private float luckValue;     // 0~1
    private int gameMode;        // 0=经典, 1=不洗牌, 2=癞子, 3=明牌, 4=快速, 5=五十K, 6=双副牌

    public SettingsManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        fpsLevel = sp.getInt("fpsLevel", 3);
        qualityLevel = sp.getInt("qualityLevel", 4);
        cardCounterEnabled = sp.getBoolean("cardCounterEnabled", true);
        soundEnabled = sp.getBoolean("soundEnabled", true);
        vibrationEnabled = sp.getBoolean("vibrationEnabled", true);
        luckValue = sp.getFloat("luckValue", 0.5f);
        gameMode = sp.getInt("gameMode", 0);
    }

    private void save() {
        sp.edit()
            .putInt("fpsLevel", fpsLevel)
            .putInt("qualityLevel", qualityLevel)
            .putBoolean("cardCounterEnabled", cardCounterEnabled)
            .putBoolean("soundEnabled", soundEnabled)
            .putBoolean("vibrationEnabled", vibrationEnabled)
            .putFloat("luckValue", luckValue)
            .putInt("gameMode", gameMode)
            .apply();
    }

    // Getters & Setters
    public int getFpsLevel() { return fpsLevel; }
    public void setFpsLevel(int v) { fpsLevel = v; save(); }

    public int getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(int v) { qualityLevel = v; save(); }

    public boolean isCardCounterEnabled() { return cardCounterEnabled; }
    public void setCardCounterEnabled(boolean v) { cardCounterEnabled = v; save(); }

    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean v) { soundEnabled = v; save(); }

    public boolean isVibrationEnabled() { return vibrationEnabled; }
    public void setVibrationEnabled(boolean v) { vibrationEnabled = v; save(); }

    public float getLuckValue() { return luckValue; }
    public void setLuckValue(float v) { luckValue = v; save(); }

    public int getGameMode() { return gameMode; }
    public void setGameMode(int v) { gameMode = v; save(); }
}
