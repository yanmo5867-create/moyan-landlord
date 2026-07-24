package com.moyan.engine;

import android.app.Activity;
import android.os.Build;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public class RefreshRateManager {

    private int preferredFps;

    public RefreshRateManager(android.content.Context ctx) {
        this.preferredFps = 120; // 默认拉满
    }

    /**
     * 应用到Activity
     */
    public void applyRefreshRate(Activity activity) {
        if (activity == null) return;

        Window window = activity.getWindow();
        if (window == null) return;

        // 优先设置高刷新率
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.preferredDisplayModeId = findBestDisplayMode(activity);
            window.setAttributes(params);
        }

        // 隐藏状态栏
        activity.getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private int findBestDisplayMode(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return 0;

        Display display = activity.getWindowManager().getDefaultDisplay();
        Display.Mode[] modes = display.getSupportedModes();

        int bestModeId = 0;
        float bestRefreshRate = 60f;

        for (Display.Mode mode : modes) {
            float rate = mode.getRefreshRate();
            if (rate >= 120f && rate > bestRefreshRate) {
                bestRefreshRate = rate;
                bestModeId = mode.getModeId();
            }
        }

        // 如果没找到120Hz，找90Hz
        if (bestModeId == 0) {
            for (Display.Mode mode : modes) {
                float rate = mode.getRefreshRate();
                if (rate >= 90f && rate > bestRefreshRate) {
                    bestRefreshRate = rate;
                    bestModeId = mode.getModeId();
                }
            }
        }

        // 兜底60Hz
        if (bestModeId == 0) {
            for (Display.Mode mode : modes) {
                float rate = mode.getRefreshRate();
                if (rate >= 60f && rate > bestRefreshRate) {
                    bestRefreshRate = rate;
                    bestModeId = mode.getModeId();
                }
            }
        }

        return bestModeId;
    }

    /**
     * 设置帧率档位
     */
    public void setFpsLevel(int level) {
        switch (level) {
            case 0: preferredFps = 30; break;
            case 1: preferredFps = 60; break;
            case 2: preferredFps = 90; break;
            case 3: preferredFps = 120; break;
            default: preferredFps = 120; break;
        }
    }

    public int getPreferredFps() { return preferredFps; }
}
