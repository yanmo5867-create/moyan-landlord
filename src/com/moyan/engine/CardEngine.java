package com.moyan.engine;

import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 牌型识别 + 大小比较引擎
 * 支持13种斗地主标准牌型 + 五十K特殊牌型
 */
public class CardEngine {

    // ========== 牌型识别 ==========

    /**
     * 识别一组牌的牌型
     */
    public static CardType recognizeType(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return CardType.INVALID;

        // 深拷贝并排序
        List<Card> sorted = new ArrayList<>(cards);
        Collections.sort(sorted, (a, b) -> Integer.compare(b.getSortWeight(), a.getSortWeight()));

        int size = sorted.size();

        // 王炸
        if (size == 2) {
            boolean hasBig = false, hasSmall = false;
            for (Card c : sorted) {
                if (c.getValue() == Card.JOKER_BIG) hasBig = true;
                if (c.getValue() == Card.JOKER_SMALL) hasSmall = true;
            }
            if (hasBig && hasSmall) return CardType.ROCKET;
        }

        // 统计各点数出现次数
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : sorted) {
            countMap.put(c.getValue(), countMap.getOrDefault(c.getValue(), 0) + 1);
        }

        // 炸弹（4张）
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

        // 三带一 (4张, 3+1)
        if (size == 4) {
            for (int cnt : countMap.values()) {
                if (cnt == 3) return CardType.TRIPLE_WITH_SINGLE;
            }
        }

        // 三带二 (5张, 3+2)
        if (size == 5) {
            boolean has3 = false, has2 = false;
            for (int cnt : countMap.values()) {
                if (cnt == 3) has3 = true;
                if (cnt == 2) has2 = true;
            }
            if (has3 && has2) return CardType.TRIPLE_WITH_PAIR;
        }

        // 四带二 (6张, 4+1+1 或 4+2)
        if (size == 6) {
            boolean has4 = false;
            for (int cnt : countMap.values()) {
                if (cnt == 4) has4 = true;
            }
            if (has4) return CardType.FOUR_WITH_TWO;
        }

        // 顺子 (>=5张连续单牌, 不含2和王)
        if (size >= 5 && isConsecutive(sorted, 1, false)) {
            // 验证不含2和王
            boolean valid = true;
            for (Card c : sorted) {
                if (c.getValue() >= 15) { valid = false; break; }
            }
            if (valid) return CardType.STRAIGHT;
        }

        // 连对 (>=6张, 3组+连续对子, 不含2和王)
        if (size >= 6 && size % 2 == 0) {
            boolean allPairs = true;
            for (int cnt : countMap.values()) {
                if (cnt != 2) { allPairs = false; break; }
            }
            if (allPairs && isConsecutiveByCount(sorted, 2, false)) return CardType.STRAIGHT_PAIR;
        }

        // 飞机基础 (>=6张, 2组+连续三张, 不含2和王)
        // 先检测纯飞机（每组恰好3张）
        if (size >= 6) {
            boolean allTriples = true;
            for (int cnt : countMap.values()) {
                if (cnt != 3) { allTriples = false; break; }
            }
            if (allTriples && size % 3 == 0) {
                if (isConsecutiveByCount(sorted, 3, false)) return CardType.PLANE;
            }
        }

        // 飞机带单张 (每组3张+对应单张)
        if (size >= 8 && size % 4 == 0) {
            List<Integer> tripleVals = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : countMap.entrySet()) {
                if (e.getValue() == 3) tripleVals.add(e.getKey());
            }
            Collections.sort(tripleVals);
            if (tripleVals.size() >= 2 && isConsecutiveValues(tripleVals)) {
                // 剩余牌数 = 飞机组数
                int groups = tripleVals.size();
                if (size == groups * 4) return CardType.PLANE_WITH_SINGLE;
            }
        }

        // 飞机带对子
        if (size >= 10 && size % 5 == 0) {
            List<Integer> tripleVals = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : countMap.entrySet()) {
                if (e.getValue() == 3) tripleVals.add(e.getKey());
            }
            Collections.sort(tripleVals);
            if (tripleVals.size() >= 2 && isConsecutiveValues(tripleVals)) {
                int groups = tripleVals.size();
                // 剩余应为 groups 个对子
                int remaining = size - groups * 3;
                if (remaining == groups * 2 && remaining % 2 == 0) {
                    return CardType.PLANE_WITH_PAIR;
                }
            }
        }

        // 五十K检测
        CardType fk = detectFiftyK(sorted);
        if (fk != CardType.INVALID) return fk;

        return CardType.INVALID;
    }

    // ========== 牌型大小比较 ==========

    /**
     * 判断 prev 是否能被 cur 压制
     * @param prev 上一手牌
     * @param cur  当前要出的牌
     * @return true if cur 可以压过 prev
     */
    public static boolean canBeat(List<Card> prev, List<Card> cur) {
        if (prev == null || prev.isEmpty()) return true; // 自由出牌
        if (cur == null || cur.isEmpty()) return false;

        CardType prevType = recognizeType(prev);
        CardType curType = recognizeType(cur);

        if (curType == CardType.INVALID) return false;

        // 王炸最大
        if (curType == CardType.ROCKET) return true;

        // 炸弹可以压非炸弹
        if (curType == CardType.BOMB && prevType != CardType.BOMB && prevType != CardType.ROCKET) {
            return true;
        }

        // 同类型且同张数才能比较
        if (prevType == curType && prev.size() == cur.size()) {
            int prevKey = getCompareKey(prev);
            int curKey = getCompareKey(cur);
            return curKey > prevKey;
        }

        return false;
    }

    /**
     * 获取比较用的关键值（用于同类型比较）
     */
    private static int getCompareKey(List<Card> cards) {
        List<Card> sorted = new ArrayList<>(cards);
        Collections.sort(sorted, (a, b) -> Integer.compare(b.getSortWeight(), a.getSortWeight()));

        CardType type = recognizeType(cards);

        switch (type) {
            case SINGLE:
            case PAIR:
            case TRIPLE:
                return sorted.get(0).getValue();
            case TRIPLE_WITH_SINGLE:
            case TRIPLE_WITH_PAIR:
                // 返回三张的核心牌值
                return getCoreValue(cards, 3);
            case STRAIGHT:
                return sorted.get(0).getValue(); // 最大端点
            case STRAIGHT_PAIR:
                return getCoreValue(cards, 2); // 最大对子值
            case PLANE:
            case PLANE_WITH_SINGLE:
            case PLANE_WITH_PAIR:
                return getCoreValue(cards, 3);
            case FOUR_WITH_TWO:
                return getCoreValue(cards, 4);
            case BOMB:
                return sorted.get(0).getValue();
            case ROCKET:
                return 9999;
            default:
                return 0;
        }
    }

    /** 获取出现次数为n次的点数值（取最大） */
    private static int getCoreValue(List<Card> cards, int n) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : cards) {
            countMap.put(c.getValue(), countMap.getOrDefault(c.getValue(), 0) + 1);
        }
        int maxVal = 0;
        for (Map.Entry<Integer, Integer> e : countMap.entrySet()) {
            if (e.getValue() == n && e.getKey() > maxVal) {
                maxVal = e.getKey();
            }
        }
        return maxVal;
    }

    // ========== 五十K检测 ==========

    private static CardType detectFiftyK(List<Card> cards) {
        if (cards.size() != 3) return CardType.INVALID;
        boolean has5 = false, has10 = false, hasK = false;
        int spadeCount = 0;
        int heartCount = 0;
        for (Card c : cards) {
            if (c.getValue() == 5) { has5 = true; if (c.getSuit() == Card.SUIT_SPADE) spadeCount++; if (c.getSuit() == Card.SUIT_HEART) heartCount++; }
            if (c.getValue() == 10) { has10 = true; if (c.getSuit() == Card.SUIT_SPADE) spadeCount++; if (c.getSuit() == Card.SUIT_HEART) heartCount++; }
            if (c.getValue() == 13) { hasK = true; if (c.getSuit() == Card.SUIT_SPADE) spadeCount++; if (c.getSuit() == Card.SUIT_HEART) heartCount++; }
        }
        if (!has5 || !has10 || !hasK) return CardType.INVALID;
        if (spadeCount == 3) return CardType.FIFTY_K_BLACK;
        if (heartCount == 3) return CardType.FIFTY_K_RED;
        return CardType.FIFTY_K;
    }

    // ========== 工具方法 ==========

    /** 检测是否连续单牌 (每组count张) */
    private static boolean isConsecutive(List<Card> sorted, int count, boolean allowJoker) {
        // 取不重复的点数列表
        List<Integer> vals = new ArrayList<>();
        Integer last = null;
        for (Card c : sorted) {
            if (last == null || !last.equals(c.getValue())) {
                if (!allowJoker && c.getValue() >= 15) return false;
                vals.add(c.getValue());
                last = c.getValue();
            }
        }
        return isConsecutiveValues(vals);
    }

    /** 检测按计数分组的连续（对子/三张的连续） */
    private static boolean isConsecutiveByCount(List<Card> sorted, int groupSize, boolean allowJoker) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : sorted) {
            if (!allowJoker && c.getValue() >= 15) return false;
            countMap.put(c.getValue(), countMap.getOrDefault(c.getValue(), 0) + 1);
        }
        List<Integer> keys = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : countMap.entrySet()) {
            if (e.getValue() != groupSize) return false;
            if (!allowJoker && e.getKey() >= 15) return false;
            keys.add(e.getKey());
        }
        Collections.sort(keys);
        return isConsecutiveValues(keys);
    }

    /** 判断整数列表是否连续递增 */
    private static boolean isConsecutiveValues(List<Integer> vals) {
        Collections.sort(vals);
        for (int i = 1; i < vals.size(); i++) {
            if (vals.get(i) - vals.get(i - 1) != 1) return false;
        }
        return true;
    }

    /**
     * 按斗地主规则排序手牌：大王>小王>2>A>K>...>3
     */
    public static void sortCards(List<Card> cards) {
        Collections.sort(cards, (a, b) -> Integer.compare(b.getSortWeight(), a.getSortWeight()));
    }

    /**
     * 检查一组牌中是否包含王炸
     */
    public static boolean containsRocket(List<Card> cards) {
        boolean big = false, small = false;
        for (Card c : cards) {
            if (c.getValue() == Card.JOKER_BIG) big = true;
            if (c.getValue() == Card.JOKER_SMALL) small = true;
        }
        return big && small;
    }

    /**
     * 检查一组牌中是否包含炸弹（四张同点）
     */
    public static boolean containsBomb(List<Card> cards) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Card c : cards) {
            map.put(c.getValue(), map.getOrDefault(c.getValue(), 0) + 1);
        }
        for (int cnt : map.values()) {
            if (cnt == 4) return true;
        }
        return false;
    }
}
