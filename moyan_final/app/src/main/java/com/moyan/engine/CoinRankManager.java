package com.moyan.engine;

import android.content.Context;
import android.content.SharedPreferences;

public class CoinRankManager {
    private static final String PREFS = "moyan_coin";
    private SharedPreferences sp;
    private int coins;

    // 段位名称
    public static final String[] RANKS = {
        "新手", "初学", "入门", "熟手", "进阶",
        "高手", "精英", "大师", "宗师", "斗尊",
        "斗圣", "斗神", "至尊斗帝"
    };

    // AI难度阈值（对应斗币数量）
    public static final int[] AI_THRESHOLDS = {
        0, 1000, 5000, 20000, 80000
    };

    // AI难度名称
    public static final String[] AI_NAMES = {
        "新手场", "普通场", "高手场", "大师场", "至尊场"
    };

    public CoinRankManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        coins = sp.getInt("coins", 1000);
    }

    public int getCoins() { return coins; }

    public void addCoins(int amount) {
        coins += amount;
        if (coins < 0) coins = 0;
        sp.edit().putInt("coins", coins).apply();
    }

    public int getDifficultyLevel() {
        for (int i = AI_THRESHOLDS.length - 1; i >= 0; i--) {
            if (coins >= AI_THRESHOLDS[i]) return i;
        }
        return 0;
    }

    public String getDifficultyName() {
        return AI_NAMES[getDifficultyLevel()];
    }

    public String getRankName() {
        int idx = Math.min(getDifficultyLevel() * 2 + (coins > 500 ? 1 : 0), RANKS.length - 1);
        return RANKS[idx];
    }

    public int getMaxMultiplier() {
        return 32; // 默认32倍封顶
    }
}
