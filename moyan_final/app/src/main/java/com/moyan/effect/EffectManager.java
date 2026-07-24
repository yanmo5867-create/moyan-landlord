package com.moyan.effect;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.moyan.engine.SettingsManager;

public class EffectManager {

    private Context context;
    private SettingsManager settings;
    private FrameLayout effectLayer;
    private Handler handler;

    // 画质等级对应的特效开关
    private boolean enableParticles;
    private boolean enableShake;
    private boolean enableLightFlow;

    public EffectManager(Context ctx, SettingsManager s, FrameLayout layer) {
        this.context = ctx;
        this.settings = s;
        this.effectLayer = layer;
        this.handler = new Handler(Looper.getMainLooper());
        updateQuality();
    }

    public void updateQuality() {
        int q = settings.getQualityLevel();
        // 0~6: 极低/低/中低/中/中高/高/极高
        enableParticles = (q >= 2);  // 中低以上开粒子
        enableShake = (q >= 3);     // 中以上开震屏
        enableLightFlow = (q >= 4); // 中高以上开流光
    }

    /**
     * 顺子特效：流光滑动
     */
    public void playStraightEffect() {
        if (!enableLightFlow) return;
        createLightFlowEffect(Color.CYAN);
    }

    /**
     * 连对特效：链条流动
     */
    public void playPairStraightEffect() {
        if (!enableLightFlow) return;
        createLightFlowEffect(Color.GREEN);
    }

    /**
     * 飞机特效：粒子飞行
     */
    public void playAirplaneEffect() {
        if (!enableParticles) return;
        createParticleEffect();
    }

    /**
     * 炸弹特效：火光炸裂 + 震屏
     */
    public void playBombEffect() {
        if (enableShake && effectLayer != null) {
            shakeScreen(effectLayer);
        }
        createExplosionEffect(Color.RED);
    }

    /**
     * 王炸特效：闪电 + 超强震屏
     */
    public void playRocketEffect() {
        if (enableShake && effectLayer != null) {
            shakeScreen(effectLayer);
            handler.postDelayed(() -> shakeScreen(effectLayer), 100);
        }
        createLightningEffect();
    }

    /**
     * 五十K特效：彩带
     */
    public void playFiftyKEffect(int type) {
        if (!enableParticles) return;
        int color = (type == 3) ? Color.BLACK : (type == 2) ? Color.RED : Color.MAGENTA;
        createParticleEffect(color);
    }

    /**
     * 春天特效
     */
    public void playSpringEffect() {
        if (!enableLightFlow) return;
        createLightFlowEffect(Color.YELLOW);
    }

    // ===== 特效实现 =====

    private void createLightFlowEffect(int color) {
        if (effectLayer == null) return;

        TextView flow = new TextView(context);
        flow.setText("✦ ✧ ✦ ✧ ✦");
        flow.setTextColor(color);
        flow.setTextSize(24);
        flow.setShadowLayer(10, 0, 0, color);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = 0;
        lp.topMargin = effectLayer.getHeight() / 3;
        flow.setLayoutParams(lp);

        effectLayer.addView(flow);

        // 从左到右滑动
        TranslateAnimation slide = new TranslateAnimation(-200, effectLayer.getWidth() + 200, 0, 0);
        slide.setDuration(1500);
        slide.setFillAfter(false);

        AlphaAnimation fade = new AlphaAnimation(1f, 0f);
        fade.setDuration(1500);
        fade.setStartOffset(1000);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(slide);
        set.addAnimation(fade);
        set.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) {
                effectLayer.removeView(flow);
            }
        });

        flow.startAnimation(set);
    }

    private void createParticleEffect() {
        if (effectLayer == null) return;
        // 简化粒子：多个小圆点散开
        for (int i = 0; i < 8; i++) {
            TextView dot = new TextView(context);
            dot.setText("●");
            dot.setTextColor(Color.YELLOW);
            dot.setTextSize(16);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            int cx = effectLayer.getWidth() / 2;
            int cy = effectLayer.getHeight() / 2;
            lp.leftMargin = cx;
            lp.topMargin = cy;
            dot.setLayoutParams(lp);
            effectLayer.addView(dot);

            float dx = (float)(Math.cos(i * Math.PI / 4) * 300);
            float dy = (float)(Math.sin(i * Math.PI / 4) * 300);

            TranslateAnimation move = new TranslateAnimation(0, dx, 0, dy);
            move.setDuration(800 + i * 50);
            move.setFillAfter(false);

            AlphaAnimation fade = new AlphaAnimation(1f, 0f);
            fade.setDuration(1000);
            fade.setStartOffset(500);

            AnimationSet set = new AnimationSet(true);
            set.addAnimation(move);
            set.addAnimation(fade);
            set.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}
                @Override public void onAnimationEnd(Animation a) {
                    effectLayer.removeView(dot);
                }
            });
            dot.startAnimation(set);
        }
    }

    private void createParticleEffect(int color) {
        if (effectLayer == null) return;
        for (int i = 0; i < 12; i++) {
            TextView dot = new TextView(context);
            dot.setText("●");
            dot.setTextColor(color);
            dot.setTextSize(14);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = effectLayer.getWidth() / 2;
            lp.topMargin = effectLayer.getHeight() / 2;
            dot.setLayoutParams(lp);
            effectLayer.addView(dot);

            float angle = (float)(i * 30 * Math.PI / 180);
            float dx = (float)(Math.cos(angle) * 250);
            float dy = (float)(Math.sin(angle) * 250);

            TranslateAnimation move = new TranslateAnimation(0, dx, 0, dy);
            move.setDuration(600 + i * 30);
            move.setFillAfter(false);

            AlphaAnimation fade = new AlphaAnimation(1f, 0f);
            fade.setDuration(800);
            fade.setStartOffset(400);

            AnimationSet set = new AnimationSet(true);
            set.addAnimation(move);
            set.addAnimation(fade);
            Animation.AnimationListener listener = new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}
                @Override public void onAnimationEnd(Animation a) {
                    effectLayer.removeView(dot);
                }
            };
            set.setAnimationListener(listener);
            dot.startAnimation(set);
        }
    }

    private void createExplosionEffect(int color) {
        // 炸弹：红色闪光 + 粒子
        createParticleEffect(color);
        if (effectLayer == null) return;

        TextView flash = new TextView(context);
        flash.setBackgroundColor(Color.RED);
        flash.setAlpha(0.6f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        flash.setLayoutParams(lp);
        effectLayer.addView(flash);

        AlphaAnimation flashAnim = new AlphaAnimation(0.6f, 0f);
        flashAnim.setDuration(300);
        flashAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) {
                effectLayer.removeView(flash);
            }
        });
        flash.startAnimation(flashAnim);
    }

    private void createLightningEffect() {
        if (effectLayer == null) return;

        // 白光闪烁
        TextView lightning = new TextView(context);
        lightning.setBackgroundColor(Color.WHITE);
        lightning.setAlpha(0.8f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        lightning.setLayoutParams(lp);
        effectLayer.addView(lightning);

        AlphaAnimation flash = new AlphaAnimation(0.8f, 0f);
        flash.setDuration(150);
        flash.setRepeatCount(3);
        flash.setRepeatMode(Animation.REVERSE);
        flash.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) {
                effectLayer.removeView(lightning);
            }
        });
        lightning.startAnimation(flash);
    }

    private void shakeScreen(View view) {
        TranslateAnimation shake = new TranslateAnimation(-15, 15, -10, 10);
        shake.setDuration(50);
        shake.setRepeatCount(6);
        shake.setRepeatMode(Animation.REVERSE);
        view.startAnimation(shake);
    }
}
