package com.moyan.model;

/**
 * 斗地主合法牌型枚举（13种 + 特殊牌型）
 */
public enum CardType {

    /** 非法牌型 */
    INVALID("非法牌型"),

    /** 1. 单张 */
    SINGLE("单张"),

    /** 2. 对子 */
    PAIR("对子"),

    /** 3. 三张 */
    TRIPLE("三张"),

    /** 4. 三带一 */
    TRIPLE_WITH_SINGLE("三带一"),

    /** 5. 三带二 */
    TRIPLE_WITH_PAIR("三带二"),

    /** 6. 单顺子 >=5张连续 */
    STRAIGHT("顺子"),

    /** 7. 连对 >=3组连续对子 */
    STRAIGHT_PAIR("连对"),

    /** 8. 三顺（飞机基础）>=2组连续三张 */
    PLANE("飞机"),

    /** 9. 飞机带单张 */
    PLANE_WITH_SINGLE("飞机带单张"),

    /** 10. 飞机带对子 */
    PLANE_WITH_PAIR("飞机带对子"),

    /** 11. 四带二（非炸弹） */
    FOUR_WITH_TWO("四带二"),

    /** 12. 普通炸弹 */
    BOMB("炸弹"),

    /** 13. 王炸 */
    ROCKET("王炸"),

    /** 五十K组合 */
    FIFTY_K("五十K"),

    /** 五十K纯黑 */
    FIFTY_K_BLACK("纯黑五十K"),

    /** 五十K纯红 */
    FIFTY_K_RED("纯红五十K");

    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
