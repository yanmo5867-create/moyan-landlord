package com.moyan.audio;

import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * 振动管理器 - 四级振动强度
 */
public class VibrationManager {

    public static final int VIBE_NONE = 0;
    public static final int VIBE_LIGHT = 1;   // 顺子/连对
    public static final int VIBE_MEDIUM = 2;  // 飞机/五十K
    public static final int VIBE_HEAVY = 3;   // 炸弹
    public static final int VIBE_EXTREME = 4;  // 王炸

    private Vibrator vibrator;
    private boolean enabled = true;

    public VibrationManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public void vibrate(int level) {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return;

        switch (level) {
            case VIBE_LIGHT:
                vibrate(80, 100);
                break;
            case VIBE_MEDIUM:
                vibrate(150, 200);
                break;
            case VIBE_HEAVY:
                vibrate(300, 400);
                break;
            case VIBE_EXTREME:
                // 超强：长震动 + 短间隔
                long[] pattern = {0, 500, 50, 300, 50, 200};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1));
                } else {
                    vibrator.vibrate(pattern, -1);
                }
                break;
            default:
                vibrate(50, 50);
        }
    }

    private void vibrate(long ms, int amplitude) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(ms, amplitude));
        } else {
            vibrator.vibrate(ms);
        }
    }

    /** 根据牌型获取振动等级 */
    public static int getVibeLevelForType(String type) {
        switch (type) {
            case AudioManager.SFX_STRAIGHT:
            case AudioManager.SFX_STRAIGHT_PAIR:
                return VIBE_LIGHT;
            case AudioManager.SFX_PLANE:
            case AudioManager.SFX_FIFTYK:
                return VIBE_MEDIUM;
            case AudioManager.SFX_BOMB:
                return VIBE_HEAVY;
            case AudioManager.SFX_ROCKET:
                return VIBE_EXTREME;
            default:
                return VIBE_NONE;
        }
    }
}
