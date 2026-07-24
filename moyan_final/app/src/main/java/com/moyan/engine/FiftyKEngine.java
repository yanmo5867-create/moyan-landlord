package com.moyan.engine;

import com.moyan.model.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FiftyKEngine {

    private CardEngine cardEngine;
    private List<Card> deck;
    private List<Card> playerHand;
    private List<Card> ai1Hand;
    private List<Card> ai2Hand;
    private List<Card> lastPlayed;
    private int currentTurn; // 0=玩家, 1=AI1, 2=AI2

    // 特殊牌值
    public static final int VALUE_5 = 5;
    public static final int VALUE_10 = 10;
    public static final int VALUE_K = 13;

    // 五十K类型
    public static final int FK_NONE = 0;
    public static final int FK_MIXED = 1;   // 混搭
    public static final int FK_RED = 2;     // 纯红
    public static final int FK_BLACK = 3;   // 纯黑

    public FiftyKEngine() {
        cardEngine = new CardEngine();
        playerHand = new ArrayList<>();
        ai1Hand = new ArrayList<>();
        ai2Hand = new ArrayList<>();
        lastPlayed = new ArrayList<>();
    }

    public void startNewGame() {
        // 五十K模式：54张牌均分，每人18张
        deck = cardEngine.createDeck();
        cardEngine.shuffle(deck);

        playerHand.clear();
        ai1Hand.clear();
        ai2Hand.clear();
        lastPlayed.clear();

        for (int i = 0; i < 54; i++) {
            if (i % 3 == 0) playerHand.add(deck.get(i));
            else if (i % 3 == 1) ai1Hand.add(deck.get(i));
            else ai2Hand.add(deck.get(i));
        }

        cardEngine.sortCards(playerHand);
        cardEngine.sortCards(ai1Hand);
        cardEngine.sortCards(ai2Hand);

        currentTurn = new Random().nextInt(3);
    }

    /**
     * 判断是否为五十K组合
     */
    public int checkFiftyK(List<Card> cards) {
        if (cards == null || cards.size() != 3) return FK_NONE;

        boolean has5 = false, has10 = false, hasK = false;
        boolean allSameSuit = true;
        String firstSuit = null;

        for (Card c : cards) {
            int v = c.getValue();
            if (v == VALUE_5) has5 = true;
            else if (v == VALUE_10) has10 = true;
            else if (v == VALUE_K) hasK = true;

            if (firstSuit == null) firstSuit = c.getSuit();
            else if (!c.getSuit().equals(firstSuit)) allSameSuit = false;
        }

        if (!has5 || !has10 || !hasK) return FK_NONE;

        if (allSameSuit) {
            // 判断花色
            if (firstSuit.contains("♠")) return FK_BLACK;  // 黑桃
            else if (firstSuit.contains("♥")) return FK_RED; // 红心
            else return FK_MIXED; // 方块/梅花也算混搭
        }

        return FK_MIXED;
    }

    /**
     * 获取五十K的优先级
     */
    public int getFiftyKPriority(List<Card> cards) {
        int type = checkFiftyK(cards);
        switch (type) {
            case FK_BLACK: return 100; // 纯黑最高
            case FK_RED: return 90;   // 纯红
            case FK_MIXED: return 80;  // 混搭
            default: return 0;
        }
    }

    /**
     * 判断牌型并比较大小
     */
    public boolean canBeat(List<Card> cards1, List<Card> cards2) {
        if (cards1 == null || cards1.isEmpty()) return false;

        // 先检查五十K
        int fk1 = checkFiftyK(cards1);
        if (fk1 != FK_NONE) {
            if (cards2 == null || cards2.isEmpty()) return true;
            int fk2 = checkFiftyK(cards2);
            if (fk2 != FK_NONE) {
                return getFiftyKPriority(cards1) > getFiftyKPriority(cards2);
            }
            return true; // 五十K压普通牌
        }

        // 普通牌型用CardEngine判断
        return cardEngine.canBeat(cards1, cards2);
    }

    /**
     * 随机选一个AI出牌
     */
    public List<Card> aiPlay(int aiIndex, List<Card> aiHand, List<Card> lastCards) {
        // 简化AI：随机出合法牌
        if (aiHand.isEmpty()) return null;

        // 50%概率出最小单张
        if (lastCards == null || lastCards.isEmpty()) {
            List<Card> play = new ArrayList<>();
            play.add(aiHand.get(aiHand.size() - 1));
            return play;
        }

        // 尝试压牌
        if (canBeat(aiHand, lastCards)) {
            // 找一张能压的牌
            for (int i = 0; i < aiHand.size(); i++) {
                List<Card> tryCard = new ArrayList<>();
                tryCard.add(aiHand.get(i));
                if (canBeat(tryCard, lastCards)) {
                    return tryCard;
                }
            }
        }

        return null; // 过牌
    }

    private boolean canBeat(List<Card> hand, List<Card> target) {
        for (int i = 0; i < hand.size(); i++) {
            List<Card> single = new ArrayList<>();
            single.add(hand.get(i));
            if (canBeat(single, target)) return true;
        }
        return false;
    }

    // ===== Getters =====
    public List<Card> getPlayerHand() { return playerHand; }
    public List<Card> getAi1Hand() { return ai1Hand; }
    public List<Card> getAi2Hand() { return ai2Hand; }
    public void setLastPlayed(List<Card> cards) { this.lastPlayed = cards; }
    public List<Card> getLastPlayed() { return lastPlayed; }
    public int getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(int t) { this.currentTurn = t; }

    private static class Random {
        private java.util.Random r = new java.util.Random();
        public int nextInt(int bound) { return r.nextInt(bound); }
    }
}
