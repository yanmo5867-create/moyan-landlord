package com.moyan.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.moyan.MoyanApp;
import com.moyan.engine.RefreshRateManager;

/**
 * 设置页
 * 帧率选择 / 画质档位 / 音量控制 / 记牌器开关 / 夜间模式 / 开发者选项(运气值)
 */
public class SettingsActivity extends Activity {

    private MoyanApp app;
    private RefreshRateManager refreshMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        app = (MoyanApp) getApplication();
        refreshMgr = new RefreshRateManager(this);

        setupFpsSelection();
        setupQualitySlider();
        setupVolumeControls();
        setupToggles();
        setupDeveloperOptions();
    }

    // ========== 帧率选择 ==========

    private void setupFpsSelection() {
        TextView tvFps = findViewById(R.id.tv_fps_value);
        android.widget.RadioGroup rg = findViewById(R.id.rg_fps);

        int current = app.getPrefs().getInt("fps", 60);
        setFpsRadio(rg, current);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            int fps = 60;
            if (checkedId == R.id.rb_30) fps = 30;
            else if (checkedId == R.id.rb_60) fps = 60;
            else if (checkedId == R.id.rb_90) fps = 90;
            else if (checkedId == R.id.rb_120) fps = 120;
            else if (checkedId == R.id.rb_auto) fps = -1;

            app.getPrefs().edit().putInt("fps", fps).apply();
            if (fps == -1) {
                refreshMgr.setAutoMax();
                tvFps.setText("自动(最高)");
            } else {
                refreshMgr.setFps(fps);
                tvFps.setText(fps + "Hz");
            }
            Toast.makeText(this, "帧率已设为" + tvFps.getText(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setFpsRadio(android.widget.RadioGroup rg, int fps) {
        switch (fps) {
            case 30: rg.check(R.id.rb_30); break;
            case 60: rg.check(R.id.rb_60); break;
            case 90: rg.check(R.id.rb_90); break;
            case 120: rg.check(R.id.rb_120); break;
            default: rg.check(R.id.rb_auto); break;
        }
    }

    // ========== 画质滑块 1~7 ==========

    private void setupQualitySlider() {
        SeekBar sb = findViewById(R.id.sb_quality);
        TextView tv = findViewById(R.id.tv_quality_value);

        String[] names = {"超低", "低", "中低", "中", "中高", "高", "极致"};
        int cur = app.getQuality();
        sb.setMax(6);
        sb.setProgress(cur - 1);
        tv.setText(names[cur - 1]);

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                tv.setText(names[p]);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                int q = s.getProgress() + 1;
                app.getPrefs().edit().putInt("quality", q).apply();
                Toast.makeText(SettingsActivity.this, "画质设为" + names[q-1], Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========== 音量控制 ==========

    private void setupVolumeControls() {
        SeekBar sbBgm = findViewById(R.id.sb_bgm);
        SeekBar sbSfx = findViewById(R.id.sb_sfx);
        SeekBar sbVoice = findViewById(R.id.sb_voice);

        sbBgm.setProgress((int)(app.getPrefs().getFloat("bgm_volume", 0.5f) * 100));
        sbSfx.setProgress((int)(app.getPrefs().getFloat("sfx_volume", 0.8f) * 100));
        sbVoice.setProgress((int)(app.getPrefs().getFloat("voice_volume", 0.8f) * 100));

        sbBgm.setOnSeekBarChangeListener(simpleVolListener("bgm_volume"));
        sbSfx.setOnSeekBarChangeListener(simpleVolListener("sfx_volume"));
        sbVoice.setOnSeekBarChangeListener(simpleVolListener("voice_volume"));
    }

    private SeekBar.OnSeekBarChangeListener simpleVolListener(String key) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {}
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                float v = s.getProgress() / 100f;
                app.getPrefs().edit().putFloat(key, v).apply();
            }
        };
    }

    // ========== 开关 ==========

    private void setupToggles() {
        Switch swCounter = findViewById(R.id.sw_card_counter);
        Switch swNight = findViewById(R.id.sw_night_mode);

        swCounter.setChecked(app.getPrefs().getBoolean("card_counter", false));
        swNight.setChecked(app.getPrefs().getBoolean("night_mode", false));

        swCounter.setOnCheckedChangeListener((b, c) -> {
            app.getPrefs().edit().putBoolean("card_counter", c).apply();
            Toast.makeText(this, c ? "记牌器已开启" : "记牌器已关闭", Toast.LENGTH_SHORT).show();
        });

        swNight.setOnCheckedChangeListener((b, c) -> {
            app.getPrefs().edit().putBoolean("night_mode", c).apply();
            recreate();
        });
    }

    // ========== 开发者选项 ==========

    private void setupDeveloperOptions() {
        SeekBar sbLuck = findViewById(R.id.sb_luck);
        TextView tvLuck = findViewById(R.id.tv_luck_value);

        int luck = app.getPrefs().getInt("luck_value", 50);
        sbLuck.setMax(100);
        sbLuck.setProgress(luck);
        tvLuck.setText(luck + "%");

        sbLuck.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                tvLuck.setText(p + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                app.getPrefs().edit().putInt("luck_value", s.getProgress()).apply();
                Toast.makeText(SettingsActivity.this, "运气值: " + s.getProgress() + "%", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
