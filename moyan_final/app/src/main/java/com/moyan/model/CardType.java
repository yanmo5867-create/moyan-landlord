package com.moyan.model;

public enum CardType {
    INVALID(-1, "非法牌型"),
    SINGLE(1, "单张"),
    PAIR(2, "对子"),
    TRIPLE(3, "三张"),
    TRIPLE_WITH_SINGLE(4, "三带一"),
    TRIPLE_WITH_PAIR(5, "三带二"),
    STRAIGHT(6, "顺子"),
    PAIR_STRAIGHT(7, "连对"),
    AIRPLANE(8, "飞机"),
    FOUR_WITH_TWO(9, "四带二"),
    BOMB(10, "炸弹"),
    ROCKET(11, "王炸");

    private int priority;
    private String name;

    CardType(int priority, String name) {
        this.priority = priority;
        this.name = name;
    }

    public int getPriority() { return priority; }
    public String getName() { return name; }
}
