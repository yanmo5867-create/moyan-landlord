package com.moyan.engine;

import com.moyan.model.Card;

import java.util.HashMap;
import java.util.Map;

public class CardCounter {

    // 记录每种牌值已出了几张（总共4张，大小王各1张）
    private Map<Integer, Integer> playedCount;

    // 每种牌值被谁打出
    private Map<Integer, StringBuilder> playedBy;

    public CardCounter() {
        reset();
    }

    public void reset() {
        playedCount = new HashMap<>();
        playedBy = new HashMap<>();
        for (int v = 3; v <= 17; v++) {
            playedCount.put(v, 0);
            playedBy.put(v, new StringBuilder());
        }
    }

    /**
     * 记录打出的牌
     */
    public void recordCards(List<Card> cards, String playerName) {
        for (Card c : cards) {
            int v = c.getValue();
            playedCount.put(v, playedCount.get(v) + 1);
            playedBy.get(v).append(playerName).append(" ");
        }
    }

    /**
     * 获取某牌值剩余张数
     */
    public int getRemaining(int value) {
        int total = (value == CardEngine.JOKER_SMALL || value == CardEngine.JOKER_BIG) ? 1 : 4;
        return total - playedCount.getOrDefault(value, 0);
    }

    /**
     * 获取某牌值已出详情
     */
    public String getPlayedDetail(int value) {
        int played = playedCount.getOrDefault(value, 0);
        String by = playedBy.get(value).toString().trim();
        if (by.isEmpty()) by = "无";
        return "已出" + played + "张, 由: " + by;
    }

    /**
     * 是否可组成炸弹（剩余>=4）
     */
    public boolean canBomb(int value) {
        return getRemaining(value) >= 4;
    }

    /**
     * 获取总剩余牌数
     */
    public int getTotalRemaining() {
        int total = 0;
        for (int v = 3; v <= 17; v++) {
            total += getRemaining(v);
        }
        return total;
    }

    /**
     * 获取记牌器文本摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("记牌器 [剩余").append(getTotalRemaining()).append("张]\n");

        // 大王小王
        appendLine(sb, "王", getRemaining(CardEngine.JOKER_BIG) + getRemaining(CardEngine.JOKER_SMALL));

        // 2
        appendLine(sb, "2", getRemaining(15));

        // A~3
        for (int v = 14; v >= 3; v--) {
            int remain = getRemaining(v);
            if (remain > 0) {
                appendLine(sb, valueToName(v), remain);
            }
        }

        return sb.toString();
    }

    private void appendLine(StringBuilder sb, String name, int count) {
        sb.append(name).append(":").append(count);
        if (count >= 4) sb.append("(炸!)");
        sb.append(" ");
    }

    private String valueToName(int v) {
        if (v == 11) return "J";
        if (v == 12) return "Q";
        if (v == 13) return "K";
        if (v == 14) return "A";
        if (v == 15) return "2";
        return String.valueOf(v);
    }
}
