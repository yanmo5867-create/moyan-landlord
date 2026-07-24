package com.moyan.engine;

import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AIEngine {

    private int difficulty; // 0~4
    private CardEngine cardEngine;
    private Random random;
    private Map<Integer, Integer> memoryCount; // 记牌器（AI记牌用）

    // 难度参数
    private boolean canBluff;       // 是否会骗牌
    private boolean smartBomb;      // 是否留炸控场
    private boolean countCards;     // 是否记牌
    private boolean aggressivePlay; // 是否主动压牌
    private int mistakeRate;        // 失误率 0~30

    public AIEngine(int difficulty) {
        this.difficulty = difficulty;
        this.cardEngine = new CardEngine();
        this.random = new Random();
        this.memoryCount = new HashMap<>();

        // 根据难度设置AI行为
        switch (difficulty) {
            case 0: // 新手：几乎不记牌，经常失误
                canBluff = false;
                smartBomb = false;
                countCards = false;
                aggressivePlay = false;
                mistakeRate = 25;
                break;
            case 1: // 普通：基础压牌，偶尔记牌
                canBluff = false;
                smartBomb = false;
                countCards = true;
                aggressivePlay = true;
                mistakeRate = 15;
                break;
            case 2: // 高手：完整记牌，合理拆牌
                canBluff = false;
                smartBomb = true;
                countCards = true;
                aggressivePlay = true;
                mistakeRate = 8;
                break;
            case 3: // 大师：精准算牌，会诱炸
                canBluff = true;
                smartBomb = true;
                countCards = true;
                aggressivePlay = true;
                mistakeRate = 3;
                break;
            case 4: // 至尊：极限控场，对标真人高手
                canBluff = true;
                smartBomb = true;
                countCards = true;
                aggressivePlay = true;
                mistakeRate = 0;
                break;
            default:
                canBluff = false;
                smartBomb = false;
                countCards = false;
                aggressivePlay = false;
                mistakeRate = 20;
        }
    }

    /**
     * AI决策出牌
     * @param lastPlayed 上家打出的牌（空=自由出牌）
     * @return 要打出的牌，null=过牌
     */
    public List<Card> decidePlay(List<Card> myHand, List<Card> lastPlayed) {
        // 更新记牌
        if (countCards && lastPlayed != null) {
            for (Card c : lastPlayed) {
                memoryCount.put(c.getValue(), memoryCount.getOrDefault(c.getValue(), 0) + 1);
            }
        }

        // 随机失误（低难度）
        if (mistakeRate > 0 && random.nextInt(100) < mistakeRate) {
            return makeMistakePlay(myHand);
        }

        // 自由出牌（没人压我）
        if (lastPlayed == null || lastPlayed.isEmpty()) {
            return makeFreePlay(myHand);
        }

        // 需要压牌
        CardType lastType = cardEngine.identifyCardType(lastPlayed);
        if (lastType == CardType.INVALID) return null;

        return makeCounterPlay(myHand, lastPlayed, lastType);
    }

    /**
     * 自由出牌：尽量出能出的牌
     */
    private List<Card> makeFreePlay(List<Card> hand) {
        if (hand.isEmpty()) return null;

        // 如果有炸弹且是高级AI，留着不随便出
        if (smartBomb && hand.size() > 8) {
            // 不出炸弹
        } else {
            // 检查有没有炸弹，有就出（低难度乱出）
            for (int v = 3; v <= 17; v++) {
                List<Card> bombs = findCardsByValue(hand, v, 4);
                if (!bombs.isEmpty() && difficulty <= 1) return bombs;
            }
        }

        // 出单张最小的
        if (difficulty <= 1) {
            // 新手/普通：出最小单张
            List<Card> single = new ArrayList<>();
            single.add(hand.get(hand.size() - 1));
            return single;
        }

        // 高手以上：尝试出顺子/连对清牌
        // 简化：出最小的单张或对子
        Map<Integer, List<Card>> groups = groupByValue(hand);
        // 找最小的对子出
        for (int v = 3; v <= 17; v++) {
            if (groups.containsKey(v) && groups.get(v).size() >= 2) {
                return new ArrayList<>(groups.get(v).subList(0, 2));
            }
        }

        // 没有对子出单张
        List<Card> single = new ArrayList<>();
        single.add(hand.get(hand.size() - 1));
        return single;
    }

    /**
     * 压牌逻辑
     */
    private List<Card> makeCounterPlay(List<Card> hand, List<Card> lastPlayed, CardType lastType) {
        Map<Integer, List<Card>> groups = groupByValue(hand);

        switch (lastType) {
            case SINGLE:
                return findBiggerSingle(hand, lastPlayed, groups);
            case PAIR:
                return findBiggerPair(hand, lastPlayed, groups);
            case TRIPLE:
            case TRIPLE_WITH_SINGLE:
            case TRIPLE_WITH_PAIR:
                return findBiggerTriple(hand, lastPlayed, groups, lastType);
            case STRAIGHT:
                return findBiggerStraight(hand, lastPlayed);
            case PAIR_STRAIGHT:
                return findBiggerPairStraight(hand, lastPlayed);
            case BOMB:
                return findBiggerBomb(hand, lastPlayed, groups);
            case ROCKET:
                return null; // 王炸无敌
            default:
                return null;
        }
    }

    private List<Card> findBiggerSingle(List<Card> hand, List<Card> lastPlayed, Map<Integer, List<Card>> groups) {
        int needValue = lastPlayed.get(0).getValue() + 1;
        for (int v = needValue; v <= 17; v++) {
            if (groups.containsKey(v) && !groups.get(v).isEmpty()) {
                List<Card> result = new ArrayList<>();
                result.add(groups.get(v).get(0));
                return result;
            }
        }
        // 没有更大的单张，高级AI考虑用炸弹/王炸
        if (smartBomb) return tryUseBombOrRocket(hand);
        return null; // 过牌
    }

    private List<Card> findBiggerPair(List<Card> hand, List<Card> lastPlayed, Map<Integer, List<Card>> groups) {
        int needValue = lastPlayed.get(0).getValue() + 1;
        for (int v = needValue; v <= 15; v++) {
            if (groups.containsKey(v) && groups.get(v).size() >= 2) {
                return new ArrayList<>(groups.get(v).subList(0, 2));
            }
        }
        if (smartBomb) return tryUseBombOrRocket(hand);
        return null;
    }

    private List<Card> findBiggerTriple(List<Card> hand, List<Card> lastPlayed, Map<Integer, List<Card>> groups, CardType type) {
        int needValue = lastPlayed.get(0).getValue() + 1;
        for (int v = needValue; v <= 15; v++) {
            if (groups.containsKey(v) && groups.get(v).size() >= 3) {
                List<Card> result = new ArrayList<>(groups.get(v).subList(0, 3));
                if (type == CardType.TRIPLE_WITH_SINGLE && hand.size() >= 4) {
                    // 带一张
                    for (Card c : hand) {
                        if (c.getValue() != v) { result.add(c); break; }
                    }
                } else if (type == CardType.TRIPLE_WITH_PAIR && hand.size() >= 5) {
                    // 带一对
                    Map<Integer, List<Card>> g2 = groupByValue(hand);
                    for (int k = 3; k <= 15; k++) {
                        if (k != v && g2.containsKey(k) && g2.get(k).size() >= 2) {
                            result.add(g2.get(k).get(0));
                            result.add(g2.get(k).get(1));
                            break;
                        }
                    }
                }
                return result;
            }
        }
        if (smartBomb) return tryUseBombOrRocket(hand);
        return null;
    }

    private List<Card> findBiggerStraight(List<Card> hand, List<Card> lastPlayed) {
        int size = lastPlayed.size();
        // 简化：找任意更大的顺子
        List<Integer> values = new ArrayList<>();
        Map<Integer, Card> valueCardMap = new HashMap<>();
        for (Card c : hand) {
            if (c.getValue() < 15) { // 2和王不能参与
                values.add(c.getValue());
                valueCardMap.put(c.getValue(), c);
            }
        }
        Collections.sort(values);

        // 找连续序列
        for (int i = 0; i <= values.size() - size; i++) {
            boolean ok = true;
            for (int j = 0; j < size; j++) {
                if (values.get(i + j) - values.get(i) != j) { ok = false; break; }
            }
            if (ok) {
                List<Card> result = new ArrayList<>();
                for (int j = 0; j < size; j++) {
                    result.add(valueCardMap.get(values.get(i + j)));
                }
                return result;
            }
        }
        return null;
    }

    private List<Card> findBiggerPairStraight(List<Card> hand, List<Card> lastPlayed) {
        // 简化实现：高级AI才尝试
        if (difficulty < 2) return null;
        // 找3组以上连续对子
        Map<Integer, List<Card>> groups = groupByValue(hand);
        List<Integer> pairValues = new ArrayList<>();
        for (int v = 3; v <= 14; v++) {
            if (groups.containsKey(v) && groups.get(v).size() >= 2) pairValues.add(v);
        }
        Collections.sort(pairValues);
        if (pairValues.size() >= 3) {
            List<Card> result = new ArrayList<>();
            for (int v : pairValues.subList(0, 3)) {
                result.add(groups.get(v).get(0));
                result.add(groups.get(v).get(1));
            }
            return result;
        }
        return null;
    }

    private List<Card> findBiggerBomb(List<Card> hand, List<Card> lastPlayed, Map<Integer, List<Card>> groups) {
        int needValue = lastPlayed.get(0).getValue() + 1;
        for (int v = needValue; v <= 15; v++) {
            if (groups.containsKey(v) && groups.get(v).size() == 4) {
                return new ArrayList<>(groups.get(v));
            }
        }
        // 王炸压一切炸弹
        return tryUseRocket(hand);
    }

    private List<Card> tryUseBombOrRocket(List<Card> hand) {
        List<Card> rocket = tryUseRocket(hand);
        if (rocket != null) return rocket;
        // 找最小的炸弹
        Map<Integer, List<Card>> groups = groupByValue(hand);
        for (int v = 3; v <= 15; v++) {
            if (groups.containsKey(v) && groups.get(v).size() == 4) {
                return new ArrayList<>(groups.get(v));
            }
        }
        return null;
    }

    private List<Card> tryUseRocket(List<Card> hand) {
        Card smallJoker = null, bigJoker = null;
        for (Card c : hand) {
            if (c.getValue() == CardEngine.JOKER_SMALL) smallJoker = c;
            if (c.getValue() == CardEngine.JOKER_BIG) bigJoker = c;
        }
        if (smallJoker != null && bigJoker != null) {
            List<Card> rocket = new ArrayList<>();
            rocket.add(smallJoker);
            rocket.add(bigJoker);
            return rocket;
        }
        return null;
    }

    private List<Card> makeMistakePlay(List<Card> hand) {
        // 随机出一张牌（可能不合法）
        if (hand.isEmpty()) return null;
        int idx = random.nextInt(hand.size());
        List<Card> result = new ArrayList<>();
        result.add(hand.get(idx));
        return result;
    }

    /**
     * 按牌值分组
     */
    private Map<Integer, List<Card>> groupByValue(List<Card> hand) {
        Map<Integer, List<Card>> groups = new HashMap<>();
        for (Card c : hand) {
            if (!groups.containsKey(c.getValue())) {
                groups.put(c.getValue(), new ArrayList<>());
            }
            groups.get(c.getValue()).add(c);
        }
        return groups;
    }

    /**
     * 按牌值找N张牌
     */
    private List<Card> findCardsByValue(List<Card> hand, int value, int count) {
        List<Card> result = new ArrayList<>();
        for (Card c : hand) {
            if (c.getValue() == value) {
                result.add(c);
                if (result.size() == count) break;
            }
        }
        return result;
    }

    /**
     * 获取AI思考延迟（模拟真人）
     */
    public int getThinkDelayMs() {
        switch (difficulty) {
            case 0: return 2000 + random.nextInt(3000);  // 2~5秒
            case 1: return 3000 + random.nextInt(4000);  // 3~7秒
            case 2: return 4000 + random.nextInt(5000);  // 4~9秒
            case 3: return 5000 + random.nextInt(5000);  // 5~10秒
            case 4: return 6000 + random.nextInt(6000);  // 6~12秒
            default: return 3000;
        }
    }

    /**
     * 获取AI难度名称
     */
    public String getDifficultyName() {
        String[] names = {"新手", "普通", "高手", "大师", "至尊"};
        return names[Math.min(difficulty, 4)];
    }
}
