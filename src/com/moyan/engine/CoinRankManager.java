package com.moyan.engine;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 斗币 + 段位 + AI难度 管理器
 */
public class CoinRankManager {

    // 段位名称
    public static final String[] RANK_NAMES = {
            "新手", "初学", "入门", "熟手", "进阶",
            "高手", "精英", "大师", "宗师", "斗尊",
            "斗圣", "斗神", "至尊斗帝"
    };

    // 段位对应最低斗币
    private static final int[] RANK_THRESHOLDS = {
            0, 500, 2000, 5000, 10000,
            20000, 40000, 70000, 120000, 200000,
            350000, 600000, 1000000
    };

    // AI难度档位（根据斗币自动切换）
    private static final int[][] COIN_TO_DIFFICULTY = {
            {0, 1},       // 0~999 → 菜鸟
            {1000, 2},    // 1000~4999 → 新手
            {5000, 3},    // 5000~9999 → 入门
            {10000, 4},   // 1万~2万 → 熟手
            {20000, 5},   // 2万~5万 → 进阶
            {50000, 6},   // 5万~10万 → 高手
            {100000, 7},  // 10万~20万 → 精英
            {200000, 8},  // 20万~40万 → 大师
            {400000, 9},  // 40万~70万 → 宗师
            {700000, 10}, // 70万+ → 至尊
    };

    private final SharedPreferences sp;
    private final Context ctx;

    public CoinRankManager(Context ctx) {
        this.ctx = ctx;
        this.sp = ctx.getSharedPreferences("moyan_prefs", Context.MODE_PRIVATE);
    }

    // ========== 斗币 ==========

    public int getCoin() {
        return sp.getInt("coin", 1000);
    }

    public void addCoin(int delta) {
        int cur = getCoin() + delta;
        if (cur < 0) cur = 0;
        sp.edit().putInt("coin", cur).apply();
    }

    /** 根据斗币获取AI难度 1~10 */
    public int getAIDifficulty() {
        int coin = getCoin();
        int diff = 1;
        for (int[] tier : COIN_TO_DIFFICULTY) {
            if (coin >= tier[0]) diff = tier[1];
            else break;
        }
        return diff;
    }

    /** 获取当前段位索引 0~12 */
    public int getRankIndex() {
        int coin = getCoin();
        int rank = 0;
        for (int i = 0; i < RANK_THRESHOLDS.length; i++) {
            if (coin >= RANK_THRESHOLDS[i]) rank = i;
        }
        return rank;
    }

    public String getRankName() {
        return RANK_NAMES[getRankIndex()];
    }

    /** 段位对应的倍数上限加成 */
    public int getRankMultiplierBonus() {
        int rank = getRankIndex();
        // 段位越高，封顶倍数越高
        return 16 + rank * 4; // 新手16倍 → 至尊斗帝64倍
    }

    // ========== 战绩统计 ==========

    public void recordWin(int multiplier, boolean isSpring) {
        addStat("total_games", 1);
        addStat("wins", 1);
        addStat("total_multiplier", multiplier);
        if (isSpring) addStat("springs", 1);
    }

    public void recordLoss() {
        addStat("total_games", 1);
        addStat("losses", 1);
    }

    public int getStat(String key) {
        return sp.getInt(key, 0);
    }

    private void addStat(String key, int delta) {
        sp.edit().putInt(key, getStat(key) + delta).apply();
    }

    public float getWinRate() {
        int total = getStat("total_games");
        if (total == 0) return 0;
        return (float) getStat("wins") / total;
    }

    /** 获取斗币档位名称（用于UI显示） */
    public String getCoinTierName() {
        int coin = getCoin();
        if (coin < 1000) return "新手场";
        if (coin < 5000) return "普通场";
        if (coin < 20000) return "高手场";
        if (coin < 80000) return "大师场";
        return "至尊场";
    }

    /** 根据斗币获取进入该场的最低要求 */
    public static int getMinCoinForTier(int tier) {
        switch (tier) {
            case 0: return 0;
            case 1: return 1000;
            case 2: return 5000;
            case 3: return 20000;
            case 4: return 80000;
            default: return 0;
        }
    }
}
