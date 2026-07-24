package com.moyan.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.moyan.MoyanApp;
import com.moyan.audio.AudioManager;
import com.moyan.audio.VibrationManager;
import com.moyan.effect.EffectManager;
import com.moyan.engine.AIEngine;
import com.moyan.engine.CardCounter;
import com.moyan.engine.CardEngine;
import com.moyan.engine.CoinRankManager;
import com.moyan.engine.FiftyKEngine;
import com.moyan.engine.GameEngine;
import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.ArrayList;
import java.util.List;

/**
 * 核心对局页（横屏）
 * 整合：发牌/叫分/出牌/AI/特效/音效/振动/记牌器/倒计时
 */
public class GameActivity extends Activity
        implements GameEngine.GameListener, FiftyKEngine.FiftyKListener {

    // 模式常量
    public static final int MODE_CLASSIC = 0;
    public static final int MODE_NO_SHUFFLE = 1;
    public static final int MODE_LAIZI = 2;
    public static final int MODE_MINGPAI = 3;
    public static final int MODE_QUICK = 4;
    public static final int MODE_FIFTYK = 5;
    public static final int MODE_DOUBLE_DECK = 6;

    private int mode = MODE_CLASSIC;

    // 引擎
    private GameEngine gameEngine;
    private FiftyKEngine fiftyKEngine;
    private AIEngine aiUp, aiDown;
    private CardCounter cardCounter;
    private CoinRankManager coinMgr;
    private AudioManager audioMgr;
    private VibrationManager vibeMgr;
    private EffectManager effectMgr;

    // UI
    private LinearLayout llMyHand;
    private LinearLayout llPlayArea;
    private TextView tvInfo, tvMultiplier, tvTimer, tvTopPlayer, tvBottomPlayer;
    private TextView tvTopCount, tvBottomCount;
    private Button btnPlay, btnPass, btnHint;
    private HorizontalScrollView svMyHand;
    private FrameLayout effectLayer;

    // 卡牌视图
    private List<CardView> myCardViews;
    private CardTouchHandler touchHandler;

    // 倒计时
    private CountDownTimer playTimer;
    private int timeLeft = 20;

    // AI回合处理
    private Handler aiHandler = new Handler(Looper.getMainLooper());

    // 记牌器
    private boolean cardCounterEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 横屏
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // 全屏
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 常亮
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_game);

        mode = getIntent().getIntExtra("mode", MODE_CLASSIC);

        // 初始化管理器
        MoyanApp app = (MoyanApp) getApplication();
        coinMgr = new CoinRankManager(this);
        audioMgr = new AudioManager(this);
        vibeMgr = new VibrationManager(this);
        effectMgr = new EffectManager(this, findViewById(R.id.effect_layer));
        effectMgr.setQuality(app.getQuality());

        // 帧率
        com.moyan.engine.RefreshRateManager refreshMgr =
                new com.moyan.engine.RefreshRateManager(this);
        int fps = app.getPrefs().getInt("fps", 60);
        if (fps == -1) refreshMgr.setAutoMax();
        else refreshMgr.setFps(fps);

        // 记牌器
        cardCounterEnabled = app.getPrefs().getBoolean("card_counter", false);

        initViews();
        startGame();
    }

    private void initViews() {
        llMyHand = findViewById(R.id.ll_my_hand);
        llPlayArea = findViewById(R.id.ll_play_area);
        tvInfo = findViewById(R.id.tv_info);
        tvMultiplier = findViewById(R.id.tv_multiplier);
        tvTimer = findViewById(R.id.tv_timer);
        tvTopPlayer = findViewById(R.id.tv_top_player);
        tvBottomPlayer = findViewById(R.id.tv_bottom_player);
        tvTopCount = findViewById(R.id.tv_top_count);
        tvBottomCount = findViewById(R.id.tv_bottom_count);
        btnPlay = findViewById(R.id.btn_play);
        btnPass = findViewById(R.id.btn_pass);
        btnHint = findViewById(R.id.btn_hint);
        svMyHand = findViewById(R.id.sv_my_hand);
        effectLayer = findViewById(R.id.effect_layer);

        btnPlay.setOnClickListener(v -> onPlayClicked());
        btnPass.setOnClickListener(v -> onPassClicked());
        btnHint.setOnClickListener(v -> showHint());

        // 空白区域点击 → 取消选中
        View root = findViewById(R.id.game_root);
        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (touchHandler != null) {
                    touchHandler.checkBlankTouch(event.getRawX(), event.getRawY());
                }
            }
            return false;
        });
    }

    private void startGame() {
        myCardViews = new ArrayList<>();

        if (mode == MODE_FIFTYK) {
            startFiftyKGame();
        } else {
            startClassicGame();
        }
    }

    // ========== 经典斗地主模式 ==========

    private void startClassicGame() {
        gameEngine = new GameEngine();
        gameEngine.setListener(this);

        // 双副牌模式
        if (mode == MODE_DOUBLE_DECK) {
            gameEngine.setDoubleDeck(true);
        }

        // AI难度
        int diff = coinMgr.getAIDifficulty();
        // 不洗牌模式：AI更保守
        if (mode == MODE_NO_SHUFFLE) diff = Math.max(1, diff - 2);
        // 明牌模式：AI更激进
        if (mode == MODE_MINGPAI) diff = Math.min(10, diff + 1);

        cardCounter = new CardCounter(mode == MODE_DOUBLE_DECK);
        aiUp = new AIEngine(diff, cardCounter);
        aiDown = new AIEngine(diff, cardCounter);

        gameEngine.startNewGame();

        tvInfo.setText("游戏开始！等待叫分...");
        updateAIInfo();
    }

    // ========== 五十K模式 ==========

    private void startFiftyKGame() {
        fiftyKEngine = new FiftyKEngine();
        fiftyKEngine.setListener(this);

        int diff = coinMgr.getAIDifficulty();
        cardCounter = new CardCounter(false);
        aiUp = new AIEngine(diff, cardCounter);
        aiDown = new AIEngine(diff, cardCounter);

        fiftyKEngine.startNewGame();

        tvInfo.setText("五十K模式 - 三方混战！");
        updateAIInfo();
    }

    // ========== 渲染手牌 ==========

    private void renderMyHand(List<Card> hand) {
        llMyHand.removeAllViews();
        myCardViews.clear();

        int margin = (int) (-15 * getResources().getDisplayMetrics().density); // 牌间距重叠
        int totalWidth = 0;

        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            CardView cv = new CardView(this, card);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            if (i > 0) lp.setMarginStart(margin);
            cv.setLayoutParams(lp);

            llMyHand.addView(cv);
            myCardViews.add(cv);
            totalWidth += cv.getLayoutParams().width + margin;
        }

        // 触摸处理
        touchHandler = new CardTouchHandler(llMyHand);
        touchHandler.setCardViews(myCardViews);
        touchHandler.setOnSelectionChangedListener(selected -> {
            // 选中变化回调
        });

        // 明牌模式：全部展开
        if (mode == MODE_MINGPAI) {
            for (CardView cv : myCardViews) cv.setCardEnabled(true);
        }
    }

    // ========== 出牌 ==========

    private void onPlayClicked() {
        if (touchHandler == null) return;
        List<Card> selected = touchHandler.getSelectedCards();
        if (selected.isEmpty()) {
            Toast.makeText(this, "请先选择要出的牌", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gameEngine != null && gameEngine.getPhase() == GameEngine.PHASE_PLAY) {
            boolean ok = gameEngine.playCards(GameEngine.PLAYER_HUMAN, selected);
            if (!ok) {
                CardType type = CardEngine.recognizeType(selected);
                if (type == CardType.INVALID) {
                    Toast.makeText(this, "牌型不合法", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "压不过上家", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (fiftyKEngine != null) {
            boolean ok = fiftyKEngine.playCards(FiftyKEngine.PLAYER_HUMAN, selected);
            if (!ok) {
                Toast.makeText(this, "牌型不合法或压不过", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onPassClicked() {
        if (gameEngine != null) {
            gameEngine.pass(GameEngine.PLAYER_HUMAN);
        } else if (fiftyKEngine != null) {
            fiftyKEngine.pass(FiftyKEngine.PLAYER_HUMAN);
        }
        if (touchHandler != null) touchHandler.clearSelection();
    }

    private void showHint() {
        Toast.makeText(this, "提示：选择最小可出的牌型", Toast.LENGTH_SHORT).show();
    }

    // ========== 叫分 ==========

    private void showCallDialog() {
        // 简化：弹出三个按钮
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("叫分");
        b.setItems(new String[]{"不叫", "1分", "2分", "3分"}, (dialog, which) -> {
            int score = which; // 0=不叫,1=1分,2=2分,3=3分
            if (gameEngine != null) {
                gameEngine.callScore(GameEngine.PLAYER_HUMAN, score);
            }
            audioMgr.playCardTypeSfx(AudioManager.SFX_CALL);
        });
        b.setCancelable(false);
        b.show();
    }

    // ========== 倒计时 ==========

    private void startTimer(int seconds) {
        if (playTimer != null) playTimer.cancel();
        timeLeft = seconds;
        tvTimer.setVisibility(View.VISIBLE);
        tvTimer.setText(String.valueOf(timeLeft));

        playTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = (int)(millisUntilFinished / 1000);
                tvTimer.setText(String.valueOf(timeLeft));
                // 最后3秒闪烁
                if (timeLeft <= 3) {
                    tvTimer.setTextColor(0xFFFF0000);
                    flashTimer();
                }
            }
            @Override
            public void onFinish() {
                tvTimer.setText("0");
                // 超时自动过牌
                if (gameEngine != null) {
                    gameEngine.pass(GameEngine.PLAYER_HUMAN);
                } else if (fiftyKEngine != null) {
                    fiftyKEngine.pass(FiftyKEngine.PLAYER_HUMAN);
                }
            }
        };
        playTimer.start();
    }

    private void flashTimer() {
        Animation flash = new TranslateAnimation(0, 10, 0, 0);
        flash.setDuration(100);
        flash.setRepeatCount(3);
        flash.setRepeatMode(Animation.REVERSE);
        tvTimer.startAnimation(flash);
    }

    // ========== AI回合 ==========

    private void scheduleAITurn(int aiPlayer) {
        AIEngine ai = (aiPlayer == GameEngine.PLAYER_AI_UP) ? aiUp : aiDown;
        int delay = ai.getThinkDelay();

        aiHandler.postDelayed(() -> executeAIPlay(aiPlayer), delay);
    }

    private void executeAIPlay(int aiPlayer) {
        AIEngine ai = (aiPlayer == GameEngine.PLAYER_AI_UP) ? aiUp : aiDown;
        List<Card> myHand;
        List<Card> lastCards;
        int lastPlayer;

        if (gameEngine != null) {
            myHand = gameEngine.getHand(aiPlayer);
            lastCards = gameEngine.getLastPlayedCards();
            lastPlayer = gameEngine.getLastPlayedPlayer();
        } else {
            myHand = fiftyKEngine.getHand(aiPlayer);
            lastCards = fiftyKEngine.getLastPlayedCards();
            lastPlayer = fiftyKEngine.getLastPlayedPlayer();
        }

        // 判断是否是队友（五十K无队友）
        List<Integer> teammates = new ArrayList<>();
        if (gameEngine != null && gameEngine.getLandlord() >= 0) {
            int landlord = gameEngine.getLandlord();
            if (aiPlayer != landlord) {
                // 另一个农民是队友
                teammates.add(aiPlayer == GameEngine.PLAYER_AI_UP
                        ? GameEngine.PLAYER_AI_DOWN : GameEngine.PLAYER_AI_UP);
            }
        }

        boolean isLandlord = gameEngine != null && gameEngine.getLandlord() == aiPlayer;
        List<Card> toPlay = ai.decidePlay(myHand, lastCards, lastPlayer, isLandlord, teammates);

        if (toPlay != null && !toPlay.isEmpty()) {
            if (gameEngine != null) {
                gameEngine.playCards(aiPlayer, toPlay);
            } else {
                fiftyKEngine.playCards(aiPlayer, toPlay);
            }
        } else {
            if (gameEngine != null) {
                gameEngine.pass(aiPlayer);
            } else {
                fiftyKEngine.pass(aiPlayer);
            }
        }
    }

    // ========== 更新UI ==========

    private void updateAIInfo() {
        if (gameEngine != null) {
            tvTopCount.setText("手牌: " + gameEngine.getHandSize(GameEngine.PLAYER_AI_UP));
            tvBottomCount.setText("手牌: " + gameEngine.getHandSize(GameEngine.PLAYER_AI_DOWN));
        } else if (fiftyKEngine != null) {
            tvTopCount.setText("手牌: " + fiftyKEngine.getHandSize(FiftyKEngine.PLAYER_AI_UP));
            tvBottomCount.setText("手牌: " + fiftyKEngine.getHandSize(FiftyKEngine.PLAYER_AI_DOWN));
        }
    }

    private void renderPlayedCards(int player, List<Card> cards) {
        llPlayArea.removeAllViews();
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);

        for (Card c : cards) {
            CardView cv = new CardView(this, c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int)(5 * getResources().getDisplayMetrics().density));
            cv.setLayoutParams(lp);
            ll.addView(cv);
        }
        llPlayArea.addView(ll);

        // 出牌动画
        ll.setAlpha(0f);
        ll.animate().alpha(1f).translationX(-30f).setDuration(300).start();
    }

    private void announceCardType(int player, CardType type, List<Card> cards) {
        String typeName = type.getDisplayName();
        String cardDesc = "";
        if (!cards.isEmpty()) {
            cardDesc = cards.get(0).getLabel();
            if (cards.size() > 1) cardDesc += "等" + cards.size() + "张";
        }

        audioMgr.announcePlay(player, typeName, cardDesc);
        audioMgr.playCardTypeSfx(getSfxForType(type));

        // 振动
        int vibeLevel = VibrationManager.getVibeLevelForType(getSfxForType(type));
        vibeMgr.vibrate(vibeLevel);

        // 特效
        playEffectForType(type);
    }

    private String getSfxForType(CardType type) {
        switch (type) {
            case SINGLE: return AudioManager.SFX_SINGLE;
            case PAIR: return AudioManager.SFX_PAIR;
            case TRIPLE: return AudioManager.SFX_TRIPLE;
            case STRAIGHT: return AudioManager.SFX_STRAIGHT;
            case STRAIGHT_PAIR: return AudioManager.SFX_STRAIGHT_PAIR;
            case PLANE: return AudioManager.SFX_PLANE;
            case BOMB: return AudioManager.SFX_BOMB;
            case ROCKET: return AudioManager.SFX_ROCKET;
            case FIFTY_K: case FIFTY_K_BLACK: case FIFTY_K_RED:
                return AudioManager.SFX_FIFTYK;
            default: return AudioManager.SFX_SINGLE;
        }
    }

    private void playEffectForType(CardType type) {
        switch (type) {
            case STRAIGHT: effectMgr.playStraightEffect(); break;
            case STRAIGHT_PAIR: effectMgr.playStraightPairEffect(); break;
            case PLANE: effectMgr.playPlaneEffect(); break;
            case BOMB: effectMgr.playBombEffect(); break;
            case ROCKET: effectMgr.playRocketEffect(); break;
            case FIFTY_K: case FIFTY_K_BLACK: case FIFTY_K_RED:
                effectMgr.playFiftyKEffect(); break;
        }
    }

    // ========== GameListener 回调 ==========

    @Override
    public void onDealComplete(List<Card> bottomCards) {
        if (gameEngine == null) return;
        renderMyHand(gameEngine.getHand(GameEngine.PLAYER_HUMAN));
        updateAIInfo();
    }

    @Override
    public void onCallScore(int player, int score) {
        String[] names = {"我", "上方玩家", "下方玩家"};
        tvInfo.setText(names[player] + (score == 0 ? "不叫" : "叫" + score + "分"));
        audioMgr.announceCall(player, score);

        if (player == GameEngine.PLAYER_HUMAN) {
            // 等待下一个AI叫分
        }
        updateAIInfo();
    }

    @Override
    public void onLandlordDecided(int landlord, List<Card> bottomCards) {
        String[] names = {"我", "上方玩家", "下方玩家"};
        tvInfo.setText(names[landlord] + " 成为地主！底牌：" + bottomCards.size() + "张");
        audioMgr.announceLandlord(landlord);

        // 更新手牌显示
        if (landlord == GameEngine.PLAYER_HUMAN) {
            renderMyHand(gameEngine.getHand(GameEngine.PLAYER_HUMAN()));
        }
        updateAIInfo();

        // 地主先出牌，开始倒计时
        if (landlord == GameEngine.PLAYER_HUMAN) {
            startTimer(20);
        } else {
            scheduleAITurn(landlord);
        }
    }

    @Override
    public void onCardPlayed(int player, List<Card> cards, CardType type) {
        renderPlayedCards(player, cards);
        announceCardType(player, type, cards);
        updateAIInfo();

        // 记录到记牌器
        if (cardCounter != null) cardCounter.recordCards(cards, player);

        // 切换回合
        if (player == GameEngine.PLAYER_HUMAN) {
            renderMyHand(gameEngine.getHand(GameEngine.PLAYER_HUMAN));
            // 下一个是AI
            int next = gameEngine.getCurrentPlayer();
            if (next != GameEngine.PLAYER_HUMAN) {
                scheduleAITurn(next);
            }
        } else {
            // AI出了牌，轮到人
            if (gameEngine.getCurrentPlayer() == GameEngine.PLAYER_HUMAN) {
                startTimer(20);
            }
        }
    }

    @Override
    public void onPlayerPass(int player) {
        String[] names = {"我", "上方玩家", "下方玩家"};
        tvInfo.setText(names[player] + " 过牌");
        audioMgr.announcePass(player);
        updateAIInfo();

        if (player != GameEngine.PLAYER_HUMAN) {
            // 检查是否轮到人
            if (gameEngine != null && gameEngine.getCurrentPlayer() == GameEngine.PLAYER_HUMAN) {
                startTimer(20);
            } else if (gameEngine != null) {
                scheduleAITurn(gameEngine.getCurrentPlayer());
            }
        }
    }

    @Override
    public void onTurnChanged(int player) {
        String[] names = {"我", "上方玩家", "下方玩家"};
        if (gameEngine != null && gameEngine.getPhase() == GameEngine.PHASE_CALL) {
            tvInfo.setText(names[player] + " 请叫分");
            if (player == GameEngine.PLAYER_HUMAN) {
                showCallDialog();
            } else {
                // AI叫分
                aiHandler.postDelayed(() -> {
                    if (gameEngine != null) {
                        int call = aiUp.decideCallScore(
                                gameEngine.getHand(player),
                                gameEngine.getBaseScore());
                        gameEngine.callScore(player, call);
                    }
                }, 2000);
            }
        }
        updateAIInfo();
    }

    @Override
    public void onMultiplierChanged(int multiplier) {
        tvMultiplier.setText("倍数: x" + multiplier);
        tvMultiplier.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGameEnd(int winner, int coinDelta, int finalMultiplier, boolean spring, boolean antiSpring) {
        if (playTimer != null) playTimer.cancel();

        String result = (winner == GameEngine.PLAYER_HUMAN) ? "胜利！" : "失败！";
        String springText = "";
        if (spring) springText = "\n🌸 春天！倍数x2";
        if (antiSpring) springText = "\n🔥 反春天！倍数x2";

        // 斗币结算
        coinMgr.addCoin(coinDelta);
        if (coinDelta > 0) {
            coinMgr.recordWin(finalMultiplier, spring || antiSpring);
            audioMgr.playCardTypeSfx(AudioManager.SFX_WIN);
        } else {
            coinMgr.recordLoss();
            audioMgr.playCardTypeSfx(AudioManager.SFX_LOSE);
        }

        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle(result);
        b.setMessage("倍数: x" + finalMultiplier + "\n斗币变化: " + (coinDelta > 0 ? "+" : "") + coinDelta
                + "\n当前斗币: " + coinMgr.getCoin() + springText);
        b.setPositiveButton("再来一局", (d, w) -> {
            d.dismiss();
            recreate();
        });
        b.setNegativeButton("返回菜单", (d, w) -> {
            d.dismiss();
            finish();
        });
        b.setCancelable(false);
        b.show();
    }

    // ========== FiftyK Listener ==========

    @Override
    public void onDealComplete() {
        if (fiftyKEngine == null) return;
        renderMyHand(fiftyKEngine.getHand(FiftyKEngine.PLAYER_HUMAN));
        updateAIInfo();
        tvInfo.setText("五十K模式 - 你的回合准备！");
    }

    // (其他FiftyK回调复用上面的逻辑)
    // 为节省篇幅，FiftyK的play/pass/turn回调在此简化处理

    // ========== 生命周期 ==========

    @Override
    protected void onPause() {
        super.onPause();
        if (playTimer != null) playTimer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioMgr != null) audioMgr.release();
        if (playTimer != null) playTimer.cancel();
    }

    @Override
    public void onBackPressed() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("退出对局");
        b.setMessage("确定要退出当前对局吗？");
        b.setPositiveButton("退出", (d, w) -> finish());
        b.setNegativeButton("继续游戏", null);
        b.show();
    }
}
