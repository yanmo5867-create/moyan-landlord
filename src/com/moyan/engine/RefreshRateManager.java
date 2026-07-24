package com.moyan.engine;

import android.app.Activity;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.os.Build;

/**
 * 刷新率管理器
 * 支持 30/60/90/120Hz 动态切换
 * 自动检测设备最高物理刷新率
 */
public class RefreshRateManager {

    public static final int FPS_30 = 30;
    public static final int FPS_60 = 60;
    public static final int FPS_90 = 90;
    public static final int FPS_120 = 120;
    public static final int FPS_AUTO = -1; // 自动拉满

    private Activity activity;
    private int currentFps = FPS_60;

    public RefreshRateManager(Activity activity) {
        this.activity = activity;
    }

    /** 设置帧率 */
    public void setFps(int fps) {
        this.currentFps = fps;
        applyRefreshRate();
    }

    /** 自动拉满设备最高刷新率 */
    public void setAutoMax() {
        int max = getMaxRefreshRate();
        setFps(max);
    }

    /** 应用刷新率到窗口 */
    private void applyRefreshRate() {
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;

        WindowManager.LayoutParams params = window.getAttributes();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            Display display = activity.getDisplay();
            if (display != null) {
                // 尝试匹配设备支持的刷新率
                float targetHz = currentFps == FPS_AUTO ? getMaxRefreshRate() : currentFps;
                setPreferredRefreshRate(params, targetHz);
                window.setAttributes(params);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+
            float targetHz = currentFps == FPS_AUTO ? getMaxRefreshRate() : currentFps;
            params.preferredDisplayModeId = findBestDisplayModeId(targetHz);
            window.setAttributes(params);
        }
    }

    private void setPreferredRefreshRate(WindowManager.LayoutParams params, float targetHz) {
        // API 31+ 使用 setFrameRate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.getWindow().getDecorView().getDisplay().setFrameRate(targetHz);
        }
    }

    private int findBestDisplayModeId(float targetHz) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        if (display == null) return 0;

        Display.Mode[] modes = display.getSupportedModes();
        int bestId = 0;
        float bestDiff = Float.MAX_VALUE;

        for (Display.Mode mode : modes) {
            float diff = Math.abs(mode.getRefreshRate() - targetHz);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestId = mode.getModeId();
            }
        }
        return bestId;
    }

    /** 获取设备支持的最高刷新率 */
    public int getMaxRefreshRate() {
        if (activity == null) return 60;
        Display display = activity.getWindowManager().getDefaultDisplay();
        if (display == null) return 60;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode[] modes = display.getSupportedModes();
            float max = 60;
            for (Display.Mode m : modes) {
                if (m.getRefreshRate() > max) max = m.getRefreshRate();
            }
            return (int) max;
        }
        return 60;
    }

    /** 获取当前设置 */
    public int getCurrentFps() { return currentFps; }

    /** 获取所有支持的刷新率选项 */
    public int[] getSupportedFpsOptions() {
        int max = getMaxRefreshRate();
        // 返回 30, 60, 90, 120 中不超过设备最大值的选项
        if (max >= 120) return new int[]{30, 60, 90, 120};
        if (max >= 90) return new int[]{30, 60, 90};
        if (max >= 60) return new int[]{30, 60};
        return new int[]{30};
    }
}
