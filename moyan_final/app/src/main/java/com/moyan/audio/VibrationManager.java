package com.moyan.audio;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.moyan.engine.SettingsManager;
import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.List;

public class VibrationManager {

    private Context context;
    private SettingsManager settings;
    private Vibrator vibrator;

    // 振动强度等级
    public static final int VIB_NONE = 0;
    public static final int VIB_LIGHT = 1;    // 顺子、连对
    public static final int VIB_MEDIUM = 2;   // 飞机、五十K
    public static final int VIB_STRONG = 3;   // 炸弹
    public static final int VIB_EXTREME = 4;  // 王炸

    public VibrationManager(Context ctx, SettingsManager s) {
        this.context = ctx;
        this.settings = s;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    /**
     * 根据出牌类型振动
     */
    public void vibrateForCards(List<Card> cards) {
        if (!settings.isVibrationEnabled()) return;
        if (cards == null || cards.isEmpty()) return;

        int level = identifyVibrationLevel(cards);
        vibrate(level);
    }

    /**
     * 根据牌型识别振动等级
     */
    private int identifyVibrationLevel(List<Card> cards) {
        int size = cards.size();
        if (size == 2) {
            boolean s = false, b = false;
            for (Card c : cards) {
                if (c.getValue() == 16) s = true;
                if (c.getValue() == 17) b = true;
            }
            if (s && b) return VIB_EXTREME; // 王炸：最强
        }

        // 炸弹
        java.util.Map<Integer, Integer> countMap = new java.util.HashMap<>();
        for (Card c : cards) {
            countMap.put(c.getValue(), countMap.getOrDefault(c.getValue(), 0) + 1);
        }
        for (int cnt : countMap.values()) {
            if (cnt == 4) return VIB_STRONG;
        }

        // 飞机（连续三张>=2组）
        if (size >= 6 && countMap.size() >= 2) {
            boolean allTriples = true;
            for (int cnt : countMap.values()) {
                if (cnt != 3) { allTriples = false; break; }
            }
            if (allTriples) return VIB_MEDIUM;
        }

        // 顺子
        if (size >= 5 && countMap.size() == size) {
            return VIB_LIGHT;
        }

        // 连对
        if (size >= 6 && size % 2 == 0) {
            boolean allPairs = true;
            for (int cnt : countMap.values()) {
                if (cnt != 2) { allPairs = false; break; }
            }
            if (allPairs) return VIB_LIGHT;
        }

        return VIB_NONE;
    }

    /**
     * 执行振动
     */
    private void vibrate(int level) {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        long[] pattern;
        int amplitude;

        switch (level) {
            case VIB_LIGHT:
                pattern = new long[]{0, 50};
                amplitude = 50;
                break;
            case VIB_MEDIUM:
                pattern = new long[]{0, 100, 50, 100};
                amplitude = 120;
                break;
            case VIB_STRONG:
                pattern = new long[]{0, 200, 50, 200, 50, 200};
                amplitude = 200;
                break;
            case VIB_EXTREME:
                pattern = new long[]{0, 300, 50, 300, 50, 300, 50, 300};
                amplitude = 255;
                break;
            default:
                return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, amplitude);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    /**
     * 短振动（按钮点击）
     */
    public void vibrateClick() {
        if (!settings.isVibrationEnabled()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, 30));
        } else {
            vibrator.vibrate(20);
        }
    }
}
