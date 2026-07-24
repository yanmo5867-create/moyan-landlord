package com.moyan.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.moyan.MoyanApp;
import com.moyan.audio.AudioManager;
import com.moyan.audio.VibrationManager;
import com.moyan.engine.AIEngine;
import com.moyan.engine.CardCounter;
import com.moyan.engine.CardEngine;
import com.moyan.engine.GameEngine;
import com.moyan.engine.RefreshRateManager;
import com.moyan.model.Card;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends Activity {

    private GameEngine gameEngine;
    private CardEngine cardEngine;
    private AIEngine aiEngine;
    private CardCounter cardCounter;
    private AudioManager audioManager;
    private VibrationManager vibrationManager;
    private RefreshRateManager refreshRateManager;

    private TextView tvMultiplier;
    private TextView tvTimer;
    private TextView tvPlayerHandCount;
    private TextView tvLeftAIHandCount;
    private TextView tvRightAIHandCount;
    private TextView tvMessage;
    private TextView tvCardCounter;
    private LinearLayout handCardArea;
    private Button btnPlay;
    private Button btnPass;
    private CountDownTimer timer;
    private int timeLeft = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // 初始化管理器
        MoyanApp app = MoyanApp.getInstance();
        audioManager = app.getAudioManager();
        vibrationManager = app.getVibrationManager();
        refreshRateManager = app.getRefreshRateManager();
        cardCounter = app.getCardCounter();

        // 设置高刷新率
        refreshRateManager.applyRefreshRate(this);

        // 初始化游戏引擎
        gameEngine = new GameEngine(this, 0); // 0 = 经典模式
        cardEngine = new CardEngine();
        aiEngine = new AIEngine(gameEngine.getDifficultyLevel());

        // 绑定UI
        tvMultiplier = findViewById(R.id.tvMultiplier);
        tvTimer = findViewById(R.id.tvTimer);
        tvPlayerHandCount = findViewById(R.id.tvPlayerHandCount);
        tvLeftAIHandCount = findViewById(R.id.tvLeftAIHandCount);
        tvRightAIHandCount = findViewById(R.id.tvRightAIHandCount);
        tvMessage = findViewById(R.id.tvMessage);
        tvCardCounter = findViewById(R.id.tvCardCounter);
        handCardArea = findViewById(R.id.handCardArea);
        btnPlay = findViewById(R.id.btnPlay);
        btnPass = findViewById(R.id.btnPass);

        // 开始新对局
        startNewGame();

        // 出牌按钮
        btnPlay.setOnClickListener(v -> {
            List<Card> selected = getSelectedCards();
            if (selected.isEmpty()) {
                Toast.makeText(this, "请先选择要出的牌", Toast.LENGTH_SHORT).show();
                return;
            }
            if (gameEngine.playCards(selected)) {
                audioManager.playCardSound(selected);
                vibrationManager.vibrateForCards(selected);
                updateUI();
                if (gameEngine.isGameOver()) {
                    endGame();
                } else {
                    startAITurn();
                }
            } else {
                Toast.makeText(this, "牌型不合法或压不过上家", Toast.LENGTH_SHORT).show();
            }
        });

        // 过牌按钮
        btnPass.setOnClickListener(v -> {
            gameEngine.pass();
            updateUI();
            startAITurn();
        });
    }

    private void startNewGame() {
        gameEngine.startNewGame();
        cardCounter.reset();
        updateUI();
        startBiddingPhase();
    }

    private void startBiddingPhase() {
        tvMessage.setText("叫分阶段 - 请选择叫分");
        // 简化：自动叫分1分
        gameEngine.bidScore(1);
        updateUI();
        tvMessage.setText("对局开始！你是农民，轮到你出牌");
        startPlayerTimer();
    }

    private void startPlayerTimer() {
        timeLeft = 20;
        tvTimer.setText(String.valueOf(timeLeft));
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft--;
                tvTimer.setText(String.valueOf(timeLeft));
                if (timeLeft <= 3) {
                    tvTimer.setTextColor(0xFFFF0000);
                }
            }
            @Override
            public void onFinish() {
                // 超时自动过牌
                gameEngine.pass();
                updateUI();
                startAITurn();
            }
        }.start();
    }

    private void startAITurn() {
        // AI上家出牌
        new android.os.Handler().postDelayed(() -> {
            List<Card> aiCards = aiEngine.decidePlay(gameEngine.getLastPlayedCards());
            if (aiCards != null && !aiCards.isEmpty()) {
                gameEngine.playCards(aiCards);
                audioManager.playCardSound(aiCards);
                tvMessage.setText("AI上家出牌");
            } else {
                tvMessage.setText("AI上家过牌");
            }
            updateUI();

            if (gameEngine.isGameOver()) { endGame(); return; }

            // AI下家出牌
            new android.os.Handler().postDelayed(() -> {
                List<Card> aiCards2 = aiEngine.decidePlay(gameEngine.getLastPlayedCards());
                if (aiCards2 != null && !aiCards2.isEmpty()) {
                    gameEngine.playCards(aiCards2);
                    audioManager.playCardSound(aiCards2);
                    tvMessage.setText("AI下家出牌");
                } else {
                    tvMessage.setText("AI下家过牌");
                }
                updateUI();

                if (gameEngine.isGameOver()) { endGame(); return; }

                // 回到玩家
                tvMessage.setText("轮到你出牌");
                startPlayerTimer();
            }, 3000);
        }, 3000);
    }

    private List<Card> getSelectedCards() {
        // 从UI获取选中的牌（简化实现）
        List<Card> selected = new ArrayList<>();
        // 实际实现中会从CardView获取选中状态
        return selected;
    }

    private void updateUI() {
        tvMultiplier.setText("倍数: " + gameEngine.getMultiplier());
        tvPlayerHandCount.setText("手牌: " + gameEngine.getPlayerHand().size());
        tvLeftAIHandCount.setText("AI上家: " + gameEngine.getLeftAIHand().size());
        tvRightAIHandCount.setText("AI下家: " + gameEngine.getRightAIHand().size());

        // 更新记牌器
        tvCardCounter.setText(cardCounter.getSummary());
    }

    private void endGame() {
        if (timer != null) timer.cancel();
        boolean playerWon = gameEngine.hasPlayerWon();
        int coinsChange = gameEngine.calculateCoinChange(playerWon);
        MoyanApp.getInstance().getCoinManager().addCoins(coinsChange);

        String msg = playerWon ? "你赢了！" : "你输了！";
        msg += " 斗币变化: " + (coinsChange >= 0 ? "+" : "") + coinsChange;
        tvMessage.setText(msg);

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

        // 3秒后回到主界面
        new android.os.Handler().postDelayed(() -> finish(), 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
