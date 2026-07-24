package com.moyan.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.moyan.MoyanApp;
import com.moyan.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView title = findViewById(R.id.tvTitle);
        title.setText("漠视诺言");

        TextView coinText = findViewById(R.id.tvCoin);
        int coins = MoyanApp.getInstance().getCoinManager().getCoins();
        coinText.setText("斗币: " + coins);

        Button btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });

        Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        Button btnStats = findViewById(R.id.btnStats);
        btnStats.setOnClickListener(v -> {
            Toast.makeText(this, "胜率: " + MoyanApp.getInstance().getStats().getWinRate() + "%", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView coinText = findViewById(R.id.tvCoin);
        int coins = MoyanApp.getInstance().getCoinManager().getCoins();
        coinText.setText("斗币: " + coins);
    }
}
