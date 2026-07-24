package com.moyan.effect;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.RotateAnimation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;

/**
 * 特效管理器 - 分级屏幕特效
 * 画质1~7档控制特效强度
 */
public class EffectManager {

    public static final int EFFECT_NONE = 0;
    public static final int EFFECT_LIGHT = 1;     // 顺子/连对
    public static final int EFFECT_MEDIUM = 2;    // 飞机/五十K
    public static final int EFFECT_HEAVY = 3;     // 炸弹
    public static final int EFFECT_EXTREME = 4;   // 王炸

    private Context context;
    private FrameLayout effectLayer;
    private int quality = 4; // 默认中等画质
    private Handler handler = new Handler(Looper.getMainLooper());

    public EffectManager(Context context, FrameLayout effectLayer) {
        this.context = context;
        this.effectLayer = effectLayer;
    }

    public void setQuality(int quality) {
        this.quality = Math.max(1, Math.min(7, quality));
    }

    public int getQuality() { return quality; }

    /** 是否启用特效（画质>=2才开特效） */
    private boolean effectsEnabled() { return quality >= 2; }

    /** 是否启用粒子（画质>=4） */
    private boolean particlesEnabled() { return quality >= 4; }

    /** 是否启用屏幕抖动（画质>=5） */
    private boolean shakeEnabled() { return quality >= 5; }

    // ========== 公共接口 ==========

    public void playStraightEffect() {
        if (!effectsEnabled()) return;
        // 全屏流光滑动
        int duration = quality >= 4 ? 1200 : 600;
        createFlowingLight(duration, Color.parseColor("#4FC3F7"));
    }

    public void playStraightPairEffect() {
        if (!effectsEnabled()) return;
        // 彩色链条
        int duration = quality >= 4 ? 1500 : 800;
        createFlowingLight(duration, Color.parseColor("#FF9800"));
    }

    public void playPlaneEffect() {
        if (!effectsEnabled()) return;
        // 全屏粒子飞行
        if (particlesEnabled()) {
            createParticleBurst(20);
        }
        createFlowingLight(1500, Color.parseColor("#AB47BC"));
    }

    public void playBombEffect() {
        if (!effectsEnabled()) return;
        // 火光炸裂
        if (particlesEnabled()) {
            createParticleBurst(30);
        }
        if (shakeEnabled()) {
            shakeScreen(15, 500);
        }
        flashScreen(Color.parseColor("#FF5722"), 300);
    }

    public void playRocketEffect() {
        // 王炸：全屏闪电 + 强抖动
        flashScreen(Color.parseColor("#FFEB3B"), 500);
        if (particlesEnabled()) {
            createParticleBurst(50);
        }
        if (shakeEnabled()) {
            shakeScreen(25, 800);
        }
    }

    public void playFiftyKEffect() {
        if (!effectsEnabled()) return;
        // 彩带绽放
        if (particlesEnabled()) {
            createParticleBurst(25);
        }
        flashScreen(Color.parseColor("#E91E63"), 200);
    }

    // ========== 特效实现 ==========

    /** 流光效果 */
    private void createFlowingLight(int duration, int color) {
        ImageView light = new ImageView(context);
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.TRANSPARENT, color, Color.TRANSPARENT}
        );
        drawable.setSize(400, 200);
        light.setImageDrawable(drawable);
        light.setAlpha(0.7f);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(400, 200);
        lp.leftMargin = -400;
        lp.topMargin = effectLayer.getHeight() / 2 - 100;
        light.setLayoutParams(lp);
        effectLayer.addView(light);

        TranslateAnimation anim = new TranslateAnimation(
                -400, effectLayer.getWidth() + 400, 0, 0);
        anim.setDuration(duration);
        anim.setInterpolator(new LinearInterpolator());
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) {
                effectLayer.removeView(light);
            }
        });
        light.startAnimation(anim);
    }

    /** 粒子爆发 */
    private void createParticleBurst(int count) {
        int actualCount = (int)(count * (quality / 7.0f * 2)); // 画质越高粒子越多
        if (actualCount < 5) actualCount = 5;

        for (int i = 0; i < actualCount; i++) {
            ImageView particle = new ImageView(context);
            int[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
                    Color.MAGENTA, Color.CYAN, Color.WHITE, Color.parseColor("#FF9800")};
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(colors[i % colors.length]);
            int size = 8 + (int)(Math.random() * 12);
            d.setSize(size, size);
            particle.setImageDrawable(d);

            int cx = effectLayer.getWidth() / 2;
            int cy = effectLayer.getHeight() / 2;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
            lp.leftMargin = cx - size/2;
            lp.topMargin = cy - size/2;
            particle.setLayoutParams(lp);
            effectLayer.addView(particle);

            // 随机方向飞出
            double angle = Math.random() * Math.PI * 2;
            double distance = 200 + Math.random() * 300;
            int dx = (int)(Math.cos(angle) * distance);
            int dy = (int)(Math.sin(angle) * distance);

            TranslateAnimation ta = new TranslateAnimation(0, dx, 0, dy);
            AlphaAnimation aa = new AlphaAnimation(1f, 0f);
            AnimationSet set = new AnimationSet(true);
            set.addAnimation(ta);
            set.addAnimation(aa);
            set.setDuration(800 + (int)(Math.random() * 400));
            set.setInterpolator(new AccelerateDecelerateInterpolator());
            set.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}
                @Override public void onAnimationEnd(Animation a) {
                    effectLayer.removeView(particle);
                }
            });
            particle.startAnimation(set);
        }
    }

    /** 屏幕闪烁 */
    private void flashScreen(int color, int duration) {
        ImageView flash = new ImageView(context);
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        flash.setImageDrawable(d);
        flash.setAlpha(0.5f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        flash.setLayoutParams(lp);
        effectLayer.addView(flash);

        AlphaAnimation fadeOut = new AlphaAnimation(0.6f, 0f);
        fadeOut.setDuration(duration);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) {
                effectLayer.removeView(flash);
            }
        });
        flash.startAnimation(fadeOut);
    }

    /** 屏幕抖动 */
    private void shakeScreen(int intensity, int duration) {
        if (effectLayer.getParent() instanceof View) {
            View root = (View) effectLayer.getParent();
            TranslateAnimation shake = new TranslateAnimation(
                    -intensity, intensity, -intensity/2, intensity/2);
            shake.setDuration(50);
            shake.setRepeatCount(duration / 50);
            shake.setRepeatMode(Animation.REVERSE);
            root.startAnimation(shake);
            handler.postDelayed(() -> root.clearAnimation(), duration + 100);
        }
    }

    /** 卡牌选中上浮动画 */
    public static void animateCardSelect(View cardView, boolean selected) {
        if (selected) {
            cardView.animate()
                    .translationY(-30f)
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(150)
                    .start();
            cardView.setElevation(8f);
        } else {
            cardView.animate()
                    .translationY(0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start();
            cardView.setElevation(2f);
        }
    }
}
