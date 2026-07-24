package com.moyan.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.moyan.CoinRankManager;
import com.moyan.MoyanApp;
import com.moyan.R;

/**
 * 漠视诺言 - 主菜单（竖屏）
 * 简约好看：深色背景 + 金色标题 + 模式按钮
 */
public class MainActivity extends Activity {

    private CoinRankManager coinMgr;
    private TextView tvCoin, tvRank;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coinMgr = new CoinRankManager(this);

        tvCoin = findViewById(R.id.tv_coin);
        tvRank = findViewById(R.id.tv_rank);

        // 应用图标 + 名称
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("漠视诺言");

        // 模式按钮
        Button btnClassic = findViewById(R.id.btn_classic);
        Button btnNoShuffle = findViewById(R.id.btn_no_shuffle);
        Button btnLaizi = findViewById(R.id.btn_laizi);
        Button btnMingPai = findViewById(R.id.btn_mingpai);
        Button btnQuick = findViewById(R.id.btn_quick);
        Button btnFiftyK = findViewById(R.id.btn_fiftyk);
        Button btnDoubleDeck = findViewById(R.id.btn_double_deck);

        // 设置按钮
        Button btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        // 各模式入口
        View.OnClickListener startGame = v -> {
            int mode = GameActivity.MODE_CLASSIC;
            if (v == btnNoShuffle) mode = GameActivity.MODE_NO_SHUFFLE;
            else if (v == btnLaizi) mode = GameActivity.MODE_LAIZI;
            else if (v == btnMingPai) mode = GameActivity.MODE_MINGPAI;
            else if (v == btnQuick) mode = GameActivity.MODE_QUICK;
            else if (v == btnFiftyK) mode = GameActivity.MODE_FIFTYK;
            else if (v == btnDoubleDeck) mode = GameActivity.MODE_DOUBLE_DECK;

            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("mode", mode);
            startActivity(intent);
        };

        btnClassic.setOnClickListener(startGame);
        btnNoShuffle.setOnClickListener(startGame);
        btnLaizi.setOnClickListener(startGame);
        btnMingPai.setOnClickListener(startGame);
        btnQuick.setOnClickListener(startGame);
        btnFiftyK.setOnClickListener(startGame);
        btnDoubleDeck.setOnClickListener(startGame);

        // 标题淡入动画
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(1500);
        tvTitle.startAnimation(fadeIn);

        updateInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateInfo();
    }

    private void updateInfo() {
        tvCoin.setText("💰 " + coinMgr.getCoin());
        tvRank.setText("🏆 " + coinMgr.getRankName());
    }

    @Override
    public void onBackPressed() {
        // 双击退出
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
        handler.postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }
    private boolean doubleBackToExitPressedOnce = false;
}
