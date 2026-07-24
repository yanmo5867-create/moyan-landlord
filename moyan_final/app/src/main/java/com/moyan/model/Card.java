package com.moyan.model;

public class Card {
    private int value;      // 牌值: 3~17
    private String suit;    // 花色: ♠♥♣♦☀☽
    private boolean selected; // 是否被选中

    public Card(int value, String suit) {
        this.value = value;
        this.suit = suit;
        this.selected = false;
    }

    public int getValue() { return value; }
    public String getSuit() { return suit; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean s) { this.selected = s; }

    public String getDisplayName() {
        switch (value) {
            case 11: return "J" + suit;
            case 12: return "Q" + suit;
            case 13: return "K" + suit;
            case 14: return "A" + suit;
            case 15: return "2" + suit;
            case 16: return "小王" + suit;
            case 17: return "大王" + suit;
            default: return value + suit;
        }
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Card)) return false;
        Card other = (Card) obj;
        return this.value == other.value && this.suit.equals(other.suit);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value) + suit.hashCode();
    }
}
