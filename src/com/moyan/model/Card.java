package com.moyan.model;

/**
 * 扑克牌模型
 * 54张标准牌 + 支持双副牌扩展
 * 点数: 3=3 ... 10=10, J=11, Q=12, K=13, A=14, 2=15, 小王=16, 大王=17
 * 花色: 0=黑桃 1=红桃 2=梅花 3=方块 (大小王花色=4)
 */
public class Card implements Comparable<Card> {

    public static final int JOKER_SMALL = 16;
    public static final int JOKER_BIG = 17;

    public static final int SUIT_SPADE = 0;
    public static final int SUIT_HEART = 1;
    public static final int SUIT_CLUB = 2;
    public static final int SUIT_DIAMOND = 3;
    public static final int SUIT_JOKER = 4;

    private int value;   // 点数 3~17
    private int suit;    // 花色 0~4
    private int id;      // 唯一ID（用于区分双副牌中的相同牌）

    public Card(int value, int suit, int id) {
        this.value = value;
        this.suit = suit;
        this.id = id;
    }

    public int getValue() { return value; }
    public int getSuit() { return suit; }
    public int getId() { return id; }

    /** 用于排序的权重：大王>小王>2>A>K>...>3 */
    public int getSortWeight() {
        if (value == JOKER_BIG) return 100;
        if (value == JOKER_SMALL) return 99;
        if (value == 15) return 98; // 2
        if (value == 14) return 97; // A
        return value; // 3~13
    }

    @Override
    public int compareTo(Card o) {
        return Integer.compare(this.getSortWeight(), o.getSortWeight());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Card) {
            Card c = (Card) o;
            return c.value == this.value && c.suit == this.suit && c.id == this.id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id * 31 + value * 7 + suit;
    }

    /** 获取牌面显示文字 */
    public String getLabel() {
        if (value == JOKER_BIG) return "大王";
        if (value == JOKER_SMALL) return "小王";
        switch (value) {
            case 11: return "J";
            case 12: return "Q";
            case 13: return "K";
            case 14: return "A";
            default: return String.valueOf(value);
        }
    }

    /** 获取花色符号 */
    public String getSuitSymbol() {
        switch (suit) {
            case 0: return "♠";
            case 1: return "♥";
            case 2: return "♣";
            case 3: return "♦";
            default: return "";
        }
    }

    /** 是否为红色牌（红桃/方块/小王/大王） */
    public boolean isRed() {
        return suit == SUIT_HEART || suit == SUIT_DIAMOND || value == JOKER_BIG || value == JOKER_SMALL;
    }
}
