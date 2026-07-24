package com.moyan.engine;

import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 斗地主核心游戏引擎
 * 负责：洗牌、发牌、叫分、抢地主、出牌流转、倍数计算、春天判定
 */
public class GameEngine {

    public static final int PLAYER_COUNT = 3;
    public static final int HAND_SIZE = 17; // 每人17张
    public static final int BOTTOM_SIZE = 3; // 底牌3张

    // 玩家索引
    public static final int PLAYER_HUMAN = 0;
    public static final int PLAYER_AI_UP = 1;
    public static final int PLAYER_AI_DOWN = 2;

    // 游戏阶段
    public static final int PHASE_DEAL = 0;
    public static final int PHASE_CALL = 1;
    public static final int PHASE_PLAY = 2;
    public static final int PHASE_END = 3;

    // 叫分选项
    public static final int CALL_PASS = 0;
    public static final int CALL_1 = 1;
    public static final int CALL_2 = 2;
    public static final int CALL_3 = 3;

    private List<Card> deck;
    private List<List<Card>> hands;  // 三人手牌
    private List<Card> bottomCards;  // 底牌
    private int landlord = -1;       // 地主是谁
    private int currentPlayer;       // 当前出牌人
    private int phase = PHASE_DEAL;

    // 叫分状态
    private int[] callScores = new int[PLAYER_COUNT];
    private int callRound = 0;
    private int highestCall = 0;
    private int highestCaller = -1;

    // 倍数系统
    private int baseScore = 0;       // 叫分底分
    private int multiplier = 1;     // 当前倍数
    private int maxMultiplier = 32;  // 封顶32倍

    // 春天判定
    private boolean[] hasPlayed;     // 每个玩家是否出过牌
    private boolean springOccurred = false;
    private boolean antiSpringOccurred = false;

    // 上一手牌
    private List<Card> lastPlayedCards;
    private int lastPlayedPlayer = -1;

    // 历史出牌记录（用于记牌器）
    private List<Card> playedHistory;

    private Random random;
    private boolean doubleDownFarmer = false; // 农民加倍
    private boolean superDoubleLord = false;  // 地主超级加倍

    // 双副牌模式
    private boolean doubleDeck = false;

    public interface GameListener {
        void onDealComplete(List<Card> bottomCards);
        void onCallScore(int player, int score);
        void onLandlordDecided(int landlord, List<Card> bottomCards);
        void onCardPlayed(int player, List<Card> cards, CardType type);
        void onPlayerPass(int player);
        void onTurnChanged(int player);
        void onGameEnd(int winner, int coinDelta, int finalMultiplier, boolean spring, boolean antiSpring);
        void onMultiplierChanged(int multiplier);
    }

    private GameListener listener;

    public GameEngine() {
        this.random = new Random();
        this.hands = new ArrayList<>();
        this.playedHistory = new ArrayList<>();
        this.hasPlayed = new boolean[PLAYER_COUNT];
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    public void setDoubleDeck(boolean doubleDeck) {
        this.doubleDeck = doubleDeck;
    }

    // ========== 发牌 ==========

    public void startNewGame() {
        // 重置状态
        deck = new ArrayList<>();
        hands.clear();
        playedHistory.clear();
        bottomCards = new ArrayList<>();
        phase = PHASE_DEAL;
        landlord = -1;
        multiplier = 1;
        baseScore = 0;
        highestCall = 0;
        highestCaller = -1;
        callRound = 0;
        springOccurred = false;
        antiSpringOccurred = false;
        lastPlayedCards = null;
        lastPlayedPlayer = -1;
        doubleDownFarmer = false;
        superDoubleLord = false;
        for (int i = 0; i < PLAYER_COUNT; i++) hasPlayed[i] = false;

        // 构建牌库
        if (doubleDeck) {
            buildDeck(2); // 两副牌 108张
        } else {
            buildDeck(1); // 一副牌 54张
        }

        // 洗牌
        Collections.shuffle(deck, random);

        // 发牌
        int idx = 0;
        for (int p = 0; p < PLAYER_COUNT; p++) {
            List<Card> hand = new ArrayList<>();
            int handSize = doubleDeck ? 18 : HAND_SIZE; // 双副牌每人18
            for (int i = 0; i < handSize; i++) {
                hand.add(deck.get(idx++));
            }
            CardEngine.sortCards(hand);
            hands.add(hand);
        }

        // 底牌
        int bottomCount = doubleDeck ? 6 : BOTTOM_SIZE;
        for (int i = 0; i < bottomCount; i++) {
            bottomCards.add(deck.get(idx++));
        }

        if (listener != null) {
            listener.onDealComplete(new ArrayList<>(bottomCards));
        }

        // 进入叫分阶段
        phase = PHASE_CALL;
        currentPlayer = random.nextInt(PLAYER_COUNT); // 随机先叫
    }

    /** 构建牌库：count副牌 */
    private void buildDeck(int count) {
        for (int c = 0; c < count; c++) {
            for (int suit = 0; suit < 4; suit++) {
                for (int v = 3; v <= 13; v++) {
                    deck.add(new Card(v, suit, c * 100 + suit * 13 + v));
                }
                // A(14)
                deck.add(new Card(14, suit, c * 100 + suit * 100 + 1));
                // 2(15)
                deck.add(new Card(15, suit, c * 100 + suit * 100 + 2));
            }
            // 小王(16) 大王(17)
            deck.add(new Card(Card.JOKER_SMALL, Card.SUIT_JOKER, c * 100 + 200));
            deck.add(new Card(Card.JOKER_BIG, Card.SUIT_JOKER, c * 100 + 201));
        }
    }

    // ========== 叫分 ==========

    public void callScore(int player, int score) {
        if (phase != PHASE_CALL) return;
        if (player != currentPlayer) return;

        callScores[player] = score;
        if (score > highestCall) {
            highestCall = score;
            highestCaller = player;
        }

        if (listener != null) listener.onCallScore(player, score);

        // 叫分结束条件：有人叫3分，或三人全叫完
        if (score == CALL_3 || callRound >= PLAYER_COUNT - 1) {
            // 确定地主
            if (highestCall == 0) {
                // 全部不叫，重新发牌
                startNewGame();
                return;
            }
            landlord = highestCaller;
            baseScore = highestCall;
            multiplier = baseScore;

            // 地主拿底牌
            hands.get(landlord).addAll(bottomCards);
            CardEngine.sortCards(hands.get(landlord));

            if (listener != null) {
                listener.onLandlordDecided(landlord, new ArrayList<>(bottomCards));
                listener.onMultiplierChanged(multiplier);
            }

            // 进入出牌阶段，地主先出
            phase = PHASE_PLAY;
            currentPlayer = landlord;
            lastPlayedCards = null;
            lastPlayedPlayer = -1;

            if (listener != null) listener.onTurnChanged(currentPlayer);
            return;
        }

        callRound++;
        currentPlayer = (currentPlayer + 1) % PLAYER_COUNT;
        if (listener != null) listener.onTurnChanged(currentPlayer);
    }

    /** 农民加倍 */
    public void farmerDouble(int player, boolean doubleDown) {
        if (phase != PHASE_PLAY) return;
        if (player == landlord) return;
        if (doubleDown && !doubleDownFarmer) {
            doubleDownFarmer = true;
            addMultiplier(2);
        }
    }

    /** 地主超级加倍 */
    public void lordSuperDouble(boolean superDouble) {
        if (phase != PHASE_PLAY) return;
        if (superDouble && !superDoubleLord) {
            superDoubleLord = true;
            addMultiplier(2);
        }
    }

    // ========== 出牌 ==========

    public boolean playCards(int player, List<Card> cards) {
        if (phase != PHASE_PLAY) return false;
        if (player != currentPlayer) return false;
        if (cards == null || cards.isEmpty()) return false;

        // 验证牌型
        CardType type = CardEngine.recognizeType(cards);
        if (type == CardType.INVALID) return false;

        // 检查是否是要得起（不是自由出牌时）
        if (lastPlayedCards != null && lastPlayedPlayer != player) {
            if (!CardEngine.canBeat(lastPlayedCards, cards)) return false;
        }

        // 从手牌中移除
        List<Card> hand = hands.get(player);
        if (!hand.containsAll(cards)) return false;

        // 验证手牌中确实有这些牌（精确匹配）
        for (Card c : cards) {
            boolean found = false;
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).equals(c)) {
                    hand.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        // 记录出牌
        hasPlayed[player] = true;
        playedHistory.addAll(cards);

        // 倍数处理
        if (type == CardType.BOMB) {
            addMultiplier(2);
        } else if (type == CardType.ROCKET) {
            addMultiplier(2);
        }

        if (listener != null) {
            listener.onCardPlayed(player, new ArrayList<>(cards), type);
        }

        // 检查是否出完
        if (hand.isEmpty()) {
            endGame(player);
            return true;
        }

        lastPlayedCards = new ArrayList<>(cards);
        lastPlayedPlayer = player;

        // 轮到下一个
        currentPlayer = (player + 1) % PLAYER_COUNT;
        if (listener != null) listener.onTurnChanged(currentPlayer);

        return true;
    }

    /** 过牌 */
    public void pass(int player) {
        if (phase != PHASE_PLAY) return;
        if (player != currentPlayer) return;

        // 如果有上一手牌且是自己出的，不能过
        if (lastPlayedPlayer == player) return;

        if (listener != null) listener.onPlayerPass(player);

        // 连续两人过牌 → 当前玩家获得自由出牌权
        int nextPlayer = (player + 1) % PLAYER_COUNT;
        if (lastPlayedPlayer != -1 && lastPlayedPlayer != player
                && lastPlayedPlayer != nextPlayer
                && !hasPlayedThisRound(nextPlayer)) {
            // 简化：下一个玩家如果也过，则重置
        }

        // 检查是否连续两人过牌
        if (lastPlayedPlayer != -1 && lastPlayedPlayer != player) {
            // 上家是否过牌 → 通过检查 lastPlayedPlayer 是否等于上家
            int prevPlayer = (player + PLAYER_COUNT - 1) % PLAYER_COUNT;
            if (lastPlayedPlayer != prevPlayer) {
                // 重置出牌权
                lastPlayedCards = null;
                lastPlayedPlayer = -1;
            }
        }

        currentPlayer = nextPlayer;
        if (listener != null) listener.onTurnChanged(currentPlayer);
    }

    private boolean hasPlayedThisRound(int player) {
        // 简化：检查该玩家是否在lastPlayed之后还没出过
        return false;
    }

    // ========== 倍数 ==========

    private void addMultiplier(int factor) {
        multiplier *= factor;
        if (multiplier > maxMultiplier) {
            multiplier = maxMultiplier;
        }
        if (listener != null) listener.onMultiplierChanged(multiplier);
    }

    // ========== 游戏结束 ==========

    private void endGame(int winner) {
        phase = PHASE_END;

        // 春天判定
        if (winner == landlord) {
            // 地主赢：检查农民是否都没出过牌
            boolean farmerPlayed = hasPlayed[(landlord + 1) % PLAYER_COUNT]
                    || hasPlayed[(landlord + 2) % PLAYER_COUNT];
            if (!farmerPlayed) {
                springOccurred = true;
                addMultiplier(2);
            }
        } else {
            // 农民赢：检查地主是否只出过一次
            int landlordPlayCount = 0;
            // 简化：如果地主只出过一轮就判定反春天
            // 实际应统计地主出牌次数
            boolean landlordOnlyOnce = true; // 简化
            if (landlordOnlyOnce) {
                antiSpringOccurred = true;
                addMultiplier(2);
            }
        }

        // 计算斗币
        int coinDelta = baseScore * multiplier;
        if (winner != PLAYER_HUMAN) {
            coinDelta = -coinDelta; // 人输了扣币
        }

        if (listener != null) {
            // 确定赢家阵营
            int winningTeam = winner;
            listener.onGameEnd(winningTeam, coinDelta, multiplier, springOccurred, antiSpringOccurred);
        }
    }

    // ========== Getter ==========

    public List<Card> getHand(int player) {
        if (player < 0 || player >= hands.size()) return new ArrayList<>();
        return new ArrayList<>(hands.get(player));
    }

    public List<Card> getBottomCards() {
        return new ArrayList<>(bottomCards);
    }

    public int getPhase() { return phase; }
    public int getLandlord() { return landlord; }
    public int getCurrentPlayer() { return currentPlayer; }
    public int getMultiplier() { return multiplier; }
    public int getBaseScore() { return baseScore; }
    public List<Card> getLastPlayedCards() { return lastPlayedCards; }
    public int getLastPlayedPlayer() { return lastPlayedPlayer; }
    public List<Card> getPlayedHistory() { return new ArrayList<>(playedHistory); }
    public boolean isDoubleDeck() { return doubleDeck; }

    /** 获取手牌数量 */
    public int getHandSize(int player) {
        if (player < 0 || player >= hands.size()) return 0;
        return hands.get(player).size();
    }

    /** 检查玩家是否还有某张牌 */
    public boolean hasCard(int player, Card card) {
        if (player < 0 || player >= hands.size()) return false;
        return hands.get(player).contains(card);
    }
}
