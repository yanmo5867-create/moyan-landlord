package com.moyan.engine;

import com.moyan.model.Card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记牌器 - 实时统计已打出卡牌
 * 追踪每张牌的剩余数量、打出者
 */
public class CardCounter {

    // 每个点数总张数（单副牌4张，双副牌8张）
    private int totalPerValue;
    // 已打出记录：value -> 打出次数
    private Map<Integer, Integer> playedCount;
    // 详细记录：value -> 谁打出的
    private Map<Integer, List<Integer>> playedBy;

    public CardCounter(boolean doubleDeck) {
        this.totalPerValue = doubleDeck ? 8 : 4;
        this.playedCount = new HashMap<>();
        this.playedBy = new HashMap<>();

        // 初始化所有点数
        for (int v = 3; v <= 17; v++) {
            playedCount.put(v, 0);
            playedBy.put(v, new ArrayList<>());
        }
        // 2 = 15
        if (!playedCount.containsKey(15)) {
            playedCount.put(15, 0);
            playedBy.put(15, new ArrayList<>());
        }
    }

    /** 记录一组打出的牌 */
    public void recordCards(List<Card> cards, int player) {
        for (Card c : cards) {
            int v = c.getValue();
            playedCount.put(v, playedCount.getOrDefault(v, 0) + 1);
            playedBy.get(v).add(player);
        }
    }

    /** 某点数剩余张数 */
    public int getRemaining(int value) {
        int played = playedCount.getOrDefault(value, 0);
        return Math.max(0, totalPerValue - played);
    }

    /** 总剩余张数 */
    public int getTotalRemaining() {
        int total = 0;
        for (int v : playedCount.keySet()) {
            total += getRemaining(v);
        }
        return total;
    }

    /** 是否可组成炸弹（剩余>=4） */
    public boolean canMakeBomb(int value) {
        return getRemaining(value) >= 4;
    }

    /** 获取打出该点数牌的玩家列表 */
    public List<Integer> getPlayedBy(int value) {
        return new ArrayList<>(playedBy.getOrDefault(value, new ArrayList<>()));
    }

    /** 获取打出次数 */
    public int getPlayedCount(int value) {
        return playedCount.getOrDefault(value, 0);
    }

    /** 获取所有点数剩余信息（用于UI展示） */
    public Map<Integer, Integer> getAllRemaining() {
        Map<Integer, Integer> result = new HashMap<>();
        for (int v : playedCount.keySet()) {
            result.put(v, getRemaining(v));
        }
        return result;
    }

    /** 重置 */
    public void reset(boolean doubleDeck) {
        this.totalPerValue = doubleDeck ? 8 : 4;
        for (int v : new ArrayList<>(playedCount.keySet())) {
            playedCount.put(v, 0);
        }
        for (int v : playedBy.keySet()) {
            playedBy.get(v).clear();
        }
    }
}
