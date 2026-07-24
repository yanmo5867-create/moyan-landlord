package com.moyan.engine;

import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardEngine {

    // 牌值定义：3=3 ... 10=10, J=11, Q=12, K=13, A=14, 2=15, 小王=16, 大王=17
    public static final int JOKER_SMALL = 16;
    public static final int JOKER_BIG = 17;

    /**
     * 判断一组牌的牌型
     */
    public CardType identifyCardType(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return CardType.INVALID;

        int size = cards.size();
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : cards) {
            countMap.put(c.getValue(), countMap.getOrDefault(c.getValue(), 0) + 1);
        }

        // 王炸
        if (size == 2) {
            boolean hasSmall = false, hasBig = false;
            for (Card c : cards) {
                if (c.getValue() == JOKER_SMALL) hasSmall = true;
                if (c.getValue() == JOKER_BIG) hasBig = true;
            }
            if (hasSmall && hasBig) return CardType.ROCKET;
        }

        // 炸弹
        if (size == 4) {
            for (int cnt : countMap.values()) {
                if (cnt == 4) return CardType.BOMB;
            }
        }

        // 单张
        if (size == 1) return CardType.SINGLE;

        // 对子
        if (size == 2 && countMap.size() == 1) return CardType.PAIR;

        // 三张
        if (size == 3) {
            for (int cnt : countMap.values()) {
                if (cnt == 3) return CardType.TRIPLE;
            }
        }

        // 三带一
        if (size == 4 && countMap.size() == 2) {
            for (int cnt : countMap.values()) {
                if (cnt == 3) return CardType.TRIPLE_WITH_SINGLE;
            }
        }

        // 三带二
        if (size == 5 && countMap.size() == 2) {
            for (int cnt : countMap.values()) {
                if (cnt == 3) return CardType.TRIPLE_WITH_PAIR;
            }
        }

        // 顺子（5张及以上连续单牌）
        if (size >= 5 && isConsecutive(cards, 1)) {
            // 检查是否含2和王
            for (Card c : cards) {
                int v = c.getValue();
                if (v >= 15) return CardType.INVALID; // 2和王不能参与顺子
            }
            return CardType.STRAIGHT;
        }

        // 连对（3对及以上连续对子）
        if (size >= 6 && size % 2 == 0 && isConsecutivePairs(cards)) {
            return CardType.PAIR_STRAIGHT;
        }

        // 飞机（连续三张）
        if (size >= 6 && isTripleStraight(cards)) {
            return CardType.AIRPLANE;
        }

        // 四带二
        if (size == 6 && countMap.size() == 3) {
            for (int cnt : countMap.values()) {
                if (cnt == 4) return CardType.FOUR_WITH_TWO;
            }
        }

        return CardType.INVALID;
    }

    /**
     * 比较两组牌的大小
     * @return true if cards1 > cards2
     */
    public boolean canBeat(List<Card> cards1, List<Card> cards2) {
        if (cards1 == null || cards1.isEmpty()) return false;

        CardType type1 = identifyCardType(cards1);
        if (type1 == CardType.INVALID) return false;

        // 王炸最大
        if (type1 == CardType.ROCKET) return true;

        if (cards2 == null || cards2.isEmpty()) return true; // 自由出牌

        CardType type2 = identifyCardType(cards2);
        if (type2 == CardType.INVALID) return false;

        // 王炸压一切
        if (type2 == CardType.ROCKET) return false;

        // 炸弹优先级
        boolean bomb1 = (type1 == CardType.BOMB);
        boolean bomb2 = (type2 == CardType.BOMB);

        if (bomb1 && !bomb2) return true;
        if (!bomb1 && bomb2) return false;
        if (bomb1 && bomb2) {
            return getMainValue(cards1) > getMainValue(cards2);
        }

        // 同牌型比较
        if (type1 == type2 && cards1.size() == cards2.size()) {
            return getMainValue(cards1) > getMainValue(cards2);
        }

        return false;
    }

    /**
     * 获取牌组的主牌值（用于比较大小）
     */
    private int getMainValue(List<Card> cards) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : cards) {
            countMap.put(c.getValue(), countMap.getOrDefault(c.getValue(), 0) + 1);
        }

        // 找出现次数最多的牌值
        int maxCount = 0;
        int mainValue = 0;
        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mainValue = entry.getKey();
            }
        }
        return mainValue;
    }

    /**
     * 判断是否是连续单牌（顺子）
     */
    private boolean isConsecutive(List<Card> cards, int groupSize) {
        List<Integer> values = new ArrayList<>();
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : cards) {
            values.add(c.getValue());
            countMap.put(c.getValue(), countMap.getOrDefault(c.getValue(), 0) + 1);
        }

        // 每张牌只能出现一次（顺子）
        for (int cnt : countMap.values()) {
            if (cnt != groupSize) return false;
        }

        Collections.sort(values);
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) - values.get(i - 1) != 1) return false;
        }
        return true;
    }

    /**
     * 判断是否是连对
     */
    private boolean isConsecutivePairs(List<Card> cards) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : cards) {
            int v = c.getValue();
            if (v >= 15) return false; // 2和王不能参与
            countMap.put(v, countMap.getOrDefault(v, 0) + 1);
        }

        List<Integer> pairValues = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() != 2) return false;
            pairValues.add(entry.getKey());
        }

        if (pairValues.size() < 3) return false;
        Collections.sort(pairValues);
        for (int i = 1; i < pairValues.size(); i++) {
            if (pairValues.get(i) - pairValues.get(i - 1) != 1) return false;
        }
        return true;
    }

    /**
     * 判断是否是飞机（连续三张）
     */
    private boolean isTripleStraight(List<Card> cards) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : cards) {
            int v = c.getValue();
            if (v >= 15) return false;
            countMap.put(v, countMap.getOrDefault(v, 0) + 1);
        }

        List<Integer> tripleValues = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() == 3) tripleValues.add(entry.getKey());
        }

        if (tripleValues.size() < 2) return false;
        Collections.sort(tripleValues);
        for (int i = 1; i < tripleValues.size(); i++) {
            if (tripleValues.get(i) - tripleValues.get(i - 1) != 1) return false;
        }
        return true;
    }

    /**
     * 创建一副标准54张扑克牌
     */
    public List<Card> createDeck() {
        List<Card> deck = new ArrayList<>();
        String[] suits = {"♠", "♥", "♣", "♦"};

        for (int value = 3; value <= 15; value++) {
            for (String suit : suits) {
                deck.add(new Card(value, suit));
            }
        }
        // 大小王
        deck.add(new Card(JOKER_SMALL, "☽"));
        deck.add(new Card(JOKER_BIG, "☀"));

        return deck;
    }

    /**
     * 洗牌
     */
    public void shuffle(List<Card> deck) {
        Collections.shuffle(deck);
    }

    /**
     * 发牌：三人各17张，底牌3张
     */
    public void dealCards(List<Card> deck, List<Card> player1, List<Card> player2, List<Card> player3, List<Card> bottomCards) {
        player1.clear();
        player2.clear();
        player3.clear();
        bottomCards.clear();

        for (int i = 0; i < 51; i++) {
            if (i % 3 == 0) player1.add(deck.get(i));
            else if (i % 3 == 1) player2.add(deck.get(i));
            else player3.add(deck.get(i));
        }
        for (int i = 51; i < 54; i++) {
            bottomCards.add(deck.get(i));
        }

        sortCards(player1);
        sortCards(player2);
        sortCards(player3);
    }

    /**
     * 排序手牌：从大到小
     */
    public void sortCards(List<Card> cards) {
        Collections.sort(cards, new Comparator<Card>() {
            @Override
            public int compare(Card a, Card b) {
                return Integer.compare(b.getValue(), a.getValue()); // 降序
            }
        });
    }
}
