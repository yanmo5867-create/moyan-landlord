package com.moyan.engine;

import com.moyan.MoyanApp;
import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {

    private CardEngine cardEngine;
    private List<Card> playerHand;
    private List<Card> leftAIHand;   // AI上家
    private List<Card> rightAIHand;  // AI下家
    private List<Card> bottomCards;  // 底牌
    private List<Card> lastPlayedCards; // 上一手牌
    private int multiplier;          // 当前倍数
    private int baseScore;           // 底分
    private boolean isLandlord;      // 玩家是否地主
    private int gameMode;            // 游戏模式
    private int currentTurn;         // 当前回合 0=玩家, 1=AI上, 2=AI下
    private boolean gameOver;
    private int landlordId;          // 地主ID: 0=玩家, 1=AI上, 2=AI下
    private List<Card> playedCards;  // 全场已出牌（记牌器用）
    private Random random;
    private float luckModifier;      // 运气值影响

    // 倍数事件
    private boolean springTriggered;     // 春天
    private boolean antiSpringTriggered; // 反春天
    private int bombCount;              // 炸弹次数

    public GameEngine(MoyanApp app, int mode) {
        this.gameMode = mode;
        this.cardEngine = new CardEngine();
        this.playerHand = new ArrayList<>();
        this.leftAIHand = new ArrayList<>();
        this.rightAIHand = new ArrayList<>();
        this.bottomCards = new ArrayList<>();
        this.lastPlayedCards = new ArrayList<>();
        this.playedCards = new ArrayList<>();
        this.random = new Random();
        this.multiplier = 1;
        this.baseScore = 1;
        this.gameOver = false;
        this.springTriggered = false;
        this.antiSpringTriggered = false;
        this.bombCount = 0;
        this.luckModifier = app.getSettings().getLuckValue();
    }

    public void startNewGame() {
        List<Card> deck = cardEngine.createDeck();

        // 根据运气值调整洗牌（高运气值让玩家更容易摸到好牌）
        if (luckModifier > 0.5f) {
            // 给玩家偏向性：把大王小王往玩家方向靠
            biasDeck(deck, (luckModifier - 0.5f) * 2);
        }

        cardEngine.shuffle(deck);
        cardEngine.dealCards(deck, playerHand, leftAIHand, rightAIHand, bottomCards);

        // 简化叫地主：随机选地主
        landlordId = random.nextInt(3);
        isLandlord = (landlordId == 0);
        if (isLandlord) {
            playerHand.addAll(bottomCards);
            cardEngine.sortCards(playerHand);
        } else if (landlordId == 1) {
            leftAIHand.addAll(bottomCards);
            cardEngine.sortCards(leftAIHand);
        } else {
            rightAIHand.addAll(bottomCards);
            cardEngine.sortCards(rightAIHand);
        }

        multiplier = baseScore;
        gameOver = false;
        playedCards.clear();
        lastPlayedCards.clear();
        bombCount = 0;
        springTriggered = false;
        antiSpringTriggered = false;
        currentTurn = 0; // 地主先出
        if (landlordId != 0) currentTurn = landlordId;

        // 记录底牌到已出牌
        playedCards.addAll(bottomCards);
    }

    /**
     * 根据运气值调整牌堆顺序（简化实现）
     */
    private void biasDeck(List<Card> deck, float strength) {
        // 把大小王移到牌堆前1/3位置
        int targetPos = deck.size() / 3;
        for (int i = 0; i < deck.size(); i++) {
            Card c = deck.get(i);
            if ((c.getValue() == CardEngine.JOKER_BIG || c.getValue() == CardEngine.JOKER_SMALL)
                && i > targetPos) {
                // 移到前面
                deck.remove(i);
                deck.add(random.nextInt(targetPos), c);
            }
        }
    }

    /**
     * 叫分
     */
    public void bidScore(int score) {
        this.baseScore = Math.max(this.baseScore, score);
        this.multiplier = this.baseScore;
    }

    /**
     * 玩家出牌
     */
    public boolean playCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return false;

        CardType type = cardEngine.identifyCardType(cards);
        if (type == CardType.INVALID) return false;

        // 检查是否能压过上家
        if (!lastPlayedCards.isEmpty()) {
            if (!cardEngine.canBeat(cards, lastPlayedCards)) return false;
        }

        // 从手牌中移除
        removeCards(playerHand, cards);
        playedCards.addAll(cards);
        lastPlayedCards = new ArrayList<>(cards);

        // 倍数计算
        if (type == CardType.BOMB || type == CardType.ROCKET) {
            multiplier *= 2;
            bombCount++;
            if (multiplier > 32) multiplier = 32; // 封顶
        }

        // 检查是否出完
        if (playerHand.isEmpty()) {
            gameOver = true;
        }

        return true;
    }

    /**
     * 过牌
     */
    public void pass() {
        // 连续两人过牌，清空lastPlayed
        // 简化实现
    }

    /**
     * 从手牌中移除打出的牌
     */
    private void removeCards(List<Card> hand, List<Card> played) {
        for (Card pc : played) {
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).getValue() == pc.getValue()
                    && hand.get(i).getSuit().equals(pc.getSuit())) {
                    hand.remove(i);
                    break;
                }
            }
        }
    }

    /**
     * 检查游戏是否结束
     */
    public boolean isGameOver() {
        return gameOver || playerHand.isEmpty() || leftAIHand.isEmpty() || rightAIHand.isEmpty();
    }

    /**
     * 玩家是否获胜
     */
    public boolean hasPlayerWon() {
        return playerHand.isEmpty();
    }

    /**
     * 计算斗币变化
     */
    public int calculateCoinChange(boolean playerWon) {
        int change = 0;
        if (playerWon) {
            change = baseScore * multiplier * (isLandlord ? 2 : 1);
            // 春天奖励
            if (springTriggered) change *= 2;
        } else {
            change = -baseScore * multiplier / (isLandlord ? 1 : 2);
        }
        // 斗币最低0
        MoyanApp app = MoyanApp.getInstance();
        int currentCoins = app.getCoinManager().getCoins();
        if (currentCoins + change < 0) change = -currentCoins;

        return change;
    }

    // ===== Getters =====

    public List<Card> getPlayerHand() { return playerHand; }
    public List<Card> getLeftAIHand() { return leftAIHand; }
    public List<Card> getRightAIHand() { return rightAIHand; }
    public List<Card> getBottomCards() { return bottomCards; }
    public List<Card> getLastPlayedCards() { return lastPlayedCards; }
    public List<Card> getPlayedCards() { return playedCards; }
    public int getMultiplier() { return multiplier; }
    public int getDifficultyLevel() { return MoyanApp.getInstance().getCoinManager().getDifficultyLevel(); }
    public boolean isLandlord() { return isLandlord; }
    public int getLandlordId() { return landlordId; }
    public int getBombCount() { return bombCount; }
    public boolean isSpring() { return springTriggered; }
    public boolean isAntiSpring() { return antiSpringTriggered; }

    public void setLastPlayedCards(List<Card> cards) {
        this.lastPlayedCards = cards != null ? new ArrayList<>(cards) : new ArrayList<>();
    }
}
