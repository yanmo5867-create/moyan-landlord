package com.moyan.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.moyan.MoyanApp;
import com.moyan.R;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MoyanApp app = MoyanApp.getInstance();

        // 帧率设置
        SeekBar fpsBar = findViewById(R.id.seekFps);
        TextView fpsLabel = findViewById(R.id.tvFpsLabel);
        fpsBar.setProgress(app.getSettings().getFpsLevel());
        fpsLabel.setText("帧率: " + getFpsText(app.getSettings().getFpsLevel()));
        fpsBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fpsLabel.setText("帧率: " + getFpsText(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                int p = seekBar.getProgress();
                app.getSettings().setFpsLevel(p);
                Toast.makeText(SettingsActivity.this, "已设置为" + getFpsText(p), Toast.LENGTH_SHORT).show();
            }
        });

        // 画质设置
        SeekBar qualityBar = findViewById(R.id.seekQuality);
        TextView qualityLabel = findViewById(R.id.tvQualityLabel);
        qualityBar.setProgress(app.getSettings().getQualityLevel());
        qualityLabel.setText("画质: " + getQualityText(app.getSettings().getQualityLevel()));
        qualityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                qualityLabel.setText("画质: " + getQualityText(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                app.getSettings().setQualityLevel(seekBar.getProgress());
            }
        });

        // 记牌器开关
        Switch cardCounterSwitch = findViewById(R.id.switchCardCounter);
        cardCounterSwitch.setChecked(app.getSettings().isCardCounterEnabled());
        cardCounterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            app.getSettings().setCardCounterEnabled(isChecked);
        });

        // 音效开关
        Switch soundSwitch = findViewById(R.id.switchSound);
        soundSwitch.setChecked(app.getSettings().isSoundEnabled());
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            app.getSettings().setSoundEnabled(isChecked);
        });

        // 振动开关
        Switch vibSwitch = findViewById(R.id.switchVibration);
        vibSwitch.setChecked(app.getSettings().isVibrationEnabled());
        vibSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            app.getSettings().setVibrationEnabled(isChecked);
        });

        // 开发者选项 - 运气值
        SeekBar luckBar = findViewById(R.id.seekLuck);
        TextView luckLabel = findViewById(R.id.tvLuckLabel);
        luckBar.setProgress((int)(app.getSettings().getLuckValue() * 100));
        luckLabel.setText("运气值: " + (int)(app.getSettings().getLuckValue() * 100) + "%");
        luckBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                luckLabel.setText("运气值: " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                app.getSettings().setLuckValue(seekBar.getProgress() / 100.0f);
            }
        });
    }

    private String getFpsText(int level) {
        switch (level) {
            case 0: return "30Hz";
            case 1: return "60Hz";
            case 2: return "90Hz";
            case 3: return "120Hz";
            default: return "自适应";
        }
    }

    private String getQualityText(int level) {
        String[] names = {"极低", "低", "中低", "中", "中高", "高", "极高"};
        if (level >= 0 && level < names.length) return names[level];
        return "中";
    }
}
