package com.moyan.engine;

import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 五十K独立模式引擎
 * 无底牌、无地主、每人18张、三方混战
 */
public class FiftyKEngine {

    public static final int PLAYER_COUNT = 3;
    public static final int HAND_SIZE = 18;

    private List<Card> deck;
    private List<List<Card>> hands;
    private int currentPlayer;
    private int phase;
    private List<Card> lastPlayedCards;
    private int lastPlayedPlayer = -1;

    // 名次（0=第一名，1=第二名，2=第三名）
    private int[] rankings;
    private int finishedCount = 0;

    // 倍数
    private int multiplier = 1;
    private int maxMultiplier = 16;

    private Random random;
    private List<Card> playedHistory;

    public interface FiftyKListener {
        void onDealComplete();
        void onCardPlayed(int player, List<Card> cards, CardType type);
        void onPlayerPass(int player);
        void onTurnChanged(int player);
        void onPlayerFinished(int player, int rank); // rank 0=1st, 1=2nd, 2=3rd
        void onMultiplierChanged(int mult);
        void onGameEnd(int[] rankings);
    }

    private FiftyKListener listener;

    public FiftyKEngine() {
        this.random = new Random();
        this.hands = new ArrayList<>();
        this.playedHistory = new ArrayList<>();
        this.rankings = new int[PLAYER_COUNT];
        for (int i = 0; i < PLAYER_COUNT; i++) rankings[i] = -1;
    }

    public void setListener(FiftyKListener l) { this.listener = l; }

    public void startNewGame() {
        deck = new ArrayList<>();
        hands.clear();
        playedHistory.clear();
        finishedCount = 0;
        multiplier = 1;
        lastPlayedCards = null;
        lastPlayedPlayer = -1;
        for (int i = 0; i < PLAYER_COUNT; i++) rankings[i] = -1;

        // 构建54张牌
        for (int suit = 0; suit < 4; suit++) {
            for (int v = 3; v <= 13; v++) {
                deck.add(new Card(v, suit, suit * 13 + v));
            }
            deck.add(new Card(14, suit, suit * 100 + 1)); // A
            deck.add(new Card(15, suit, suit * 100 + 2)); // 2
        }
        deck.add(new Card(Card.JOKER_SMALL, Card.SUIT_JOKER, 200));
        deck.add(new Card(Card.JOKER_BIG, Card.SUIT_JOKER, 201));

        Collections.shuffle(deck, random);

        int idx = 0;
        for (int p = 0; p < PLAYER_COUNT; p++) {
            List<Card> hand = new ArrayList<>();
            for (int i = 0; i < HAND_SIZE; i++) {
                hand.add(deck.get(idx++));
            }
            CardEngine.sortCards(hand);
            hands.add(hand);
        }

        currentPlayer = random.nextInt(PLAYER_COUNT);
        phase = 1;

        if (listener != null) {
            listener.onDealComplete();
            listener.onTurnChanged(currentPlayer);
        }
    }

    public boolean playCards(int player, List<Card> cards) {
        if (player != currentPlayer) return false;
        if (cards == null || cards.isEmpty()) return false;

        CardType type = CardEngine.recognizeType(cards);
        // 五十K模式额外支持五十K组合
        if (type == CardType.INVALID) {
            type = checkFiftyK(cards);
        }
        if (type == CardType.INVALID) return false;

        // 压制检查
        if (lastPlayedCards != null && lastPlayedPlayer != player) {
            if (!canBeatFiftyK(lastPlayedCards, cards)) return false;
        }

        // 从手牌移除
        List<Card> hand = hands.get(player);
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

        playedHistory.addAll(cards);

        // 倍数
        if (type == CardType.BOMB || type == CardType.FIFTY_K
                || type == CardType.FIFTY_K_BLACK || type == CardType.FIFTY_K_RED) {
            multiplier *= 2;
            if (multiplier > maxMultiplier) multiplier = maxMultiplier;
            if (listener != null) listener.onMultiplierChanged(multiplier);
        }

        lastPlayedCards = new ArrayList<>(cards);
        lastPlayedPlayer = player;

        if (listener != null) listener.onCardPlayed(player, new ArrayList<>(cards), type);

        // 检查是否出完
        if (hand.isEmpty()) {
            rankings[player] = finishedCount++;
            if (listener != null) listener.onPlayerFinished(player, finishedCount - 1);
            if (finishedCount >= PLAYER_COUNT) {
                phase = 2; // 结束
                if (listener != null) listener.onGameEnd(rankings);
                return true;
            }
        }

        // 下一个
        currentPlayer = (player + 1) % PLAYER_COUNT;
        // 跳过已结束的玩家
        while (rankings[currentPlayer] != -1) {
            currentPlayer = (currentPlayer + 1) % PLAYER_COUNT;
        }
        if (listener != null) listener.onTurnChanged(currentPlayer);
        return true;
    }

    public void pass(int player) {
        if (player != currentPlayer) return;
        if (lastPlayedPlayer == player) return; // 自己出的不能过

        if (listener != null) listener.onPlayerPass(player);

        // 检查连续两人过牌 → 重置出牌权
        int next = (player + 1) % PLAYER_COUNT;
        // 简化：直接重置
        lastPlayedCards = null;
        lastPlayedPlayer = -1;

        currentPlayer = next;
        while (rankings[currentPlayer] != -1) {
            currentPlayer = (currentPlayer + 1) % PLAYER_COUNT;
        }
        if (listener != null) listener.onTurnChanged(currentPlayer);
    }

    /** 五十K模式专用压制规则 */
    private boolean canBeatFiftyK(List<Card> prev, List<Card> cur) {
        CardType pType = CardEngine.recognizeType(prev);
        CardType cType = CardEngine.recognizeType(cur);

        // 王炸最大
        if (cType == CardType.ROCKET) return true;

        // 炸弹压非炸弹
        if (cType == CardType.BOMB && pType != CardType.BOMB && pType != CardType.ROCKET) return true;

        // 五十K优先级
        if (isFiftyKType(cType) && !isFiftyKType(pType)) {
            if (pType != CardType.BOMB && pType != CardType.ROCKET) return true;
        }

        // 同类型比较
        if (pType == cType && prev.size() == cur.size()) {
            int pk = getFiftyKKey(prev, pType);
            int ck = getFiftyKKey(cur, cType);
            return ck > pk;
        }

        return false;
    }

    private boolean isFiftyKType(CardType t) {
        return t == CardType.FIFTY_K || t == CardType.FIFTY_K_BLACK || t == CardType.FIFTY_K_RED;
    }

    private int getFiftyKKey(List<Card> cards, CardType type) {
        if (type == CardType.FIFTY_K_BLACK) return 300;
        if (type == CardType.FIFTY_K_RED) return 200;
        if (type == CardType.FIFTY_K) return 100;
        // 其他按最大牌值
        int max = 0;
        for (Card c : cards) if (c.getValue() > max) max = c.getValue();
        return max;
    }

    /** 检查是否是五十K组合 */
    private CardType checkFiftyK(List<Card> cards) {
        if (cards.size() != 3) return CardType.INVALID;
        boolean has5 = false, has10 = false, hasK = false;
        int spadeCount = 0, heartCount = 0;
        for (Card c : cards) {
            if (c.getValue() == 5) { has5 = true; if (c.getSuit() == 0) spadeCount++; if (c.getSuit() == 1) heartCount++; }
            if (c.getValue() == 10) { has10 = true; if (c.getSuit() == 0) spadeCount++; if (c.getSuit() == 1) heartCount++; }
            if (c.getValue() == 13) { hasK = true; if (c.getSuit() == 0) spadeCount++; if (c.getSuit() == 1) heartCount++; }
        }
        if (!has5 || !has10 || !hasK) return CardType.INVALID;
        if (spadeCount == 3) return CardType.FIFTY_K_BLACK;
        if (heartCount == 3) return CardType.FIFTY_K_RED;
        return CardType.FIFTY_K;
    }

    // ========== Getter ==========

    public List<Card> getHand(int player) {
        if (player < 0 || player >= hands.size()) return new ArrayList<>();
        return new ArrayList<>(hands.get(player));
    }

    public int getHandSize(int player) {
        if (player < 0 || player >= hands.size()) return 0;
        return hands.get(player).size();
    }

    public int getCurrentPlayer() { return currentPlayer; }
    public int getMultiplier() { return multiplier; }
    public int[] getRankings() { return rankings.clone(); }
    public List<Card> getLastPlayedCards() { return lastPlayedCards; }
    public int getLastPlayedPlayer() { return lastPlayedPlayer; }
}
