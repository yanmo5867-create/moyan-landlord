package com.moyan.engine;

import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AI 引擎 - 10档难度
 * 1=纯随机  2=新手  3=入门  4=熟手  5=进阶
 * 6=高手    7=精英  8=大师  9=宗师  10=至尊(极限)
 */
public class AIEngine {

    private Random random;
    private int difficulty; // 1~10
    private CardCounter counter; // 记牌器引用

    // 难度名称
    public static final String[] DIFFICULTY_NAMES = {
            "", "菜鸟", "新手", "入门", "熟手",
            "进阶", "高手", "精英", "大师", "宗师", "至尊"
    };

    public AIEngine(int difficulty, CardCounter counter) {
        this.random = new Random();
        this.difficulty = Math.max(1, Math.min(10, difficulty));
        this.counter = counter;
    }

    public void setDifficulty(int diff) {
        this.difficulty = Math.max(1, Math.min(10, diff));
    }

    public int getDifficulty() { return difficulty; }

    // ========== 出牌决策 ==========

    /**
     * AI决定出什么牌
     * @param myHand 我的手牌
     * @param lastCards 上家出的牌（null表示自由出牌）
     * @param lastPlayer 上家是谁
     * @param isLandlord 我是否是地主
     * @param teammates 队友索引列表（农民时队友是另一个农民）
     */
    public List<Card> decidePlay(List<Card> myHand, List<Card> lastCards,
                                 int lastPlayer, boolean isLandlord,
                                 List<Integer> teammates) {
        List<Card> hand = new ArrayList<>(myHand);
        CardEngine.sortCards(hand);

        if (lastCards == null || lastCards.isEmpty()) {
            // 自由出牌 - 选择最佳首引牌
            return chooseLeadCards(hand, isLandlord);
        }

        CardType lastType = CardEngine.recognizeType(lastCards);

        if (lastType == CardType.INVALID) {
            return chooseLeadCards(hand, isLandlord);
        }

        // 尝试压制
        List<Card> beat = findBeat(hand, lastCards, lastType);
        if (beat != null) {
            // 高难度：判断是否需要压（队友出牌时可能放过）
            if (difficulty >= 6 && shouldPass(beat, lastCards, isLandlord, teammates, lastPlayer)) {
                return null; // 过牌
            }
            return beat;
        }

        return null; // 要不起，过牌
    }

    /**
     * 叫分决策
     */
    public int decideCallScore(List<Card> myHand, int currentHighestCall) {
        CardEngine.sortCards(myHand);

        // 统计牌力
        int bombCount = countBombs(myHand);
        int twoCount = countValue(myHand, 15);
        int bigJoker = countValue(myHand, Card.JOKER_BIG);
        int smallJoker = countValue(myHand, Card.JOKER_SMALL);
        int aCount = countValue(myHand, 14);
        int kCount = countValue(myHand, 13);

        int power = bombCount * 8 + twoCount * 3 + bigJoker * 5
                + smallJoker * 3 + aCount * 2 + kCount * 1;

        // 根据难度调整叫分策略
        int threshold1 = getThreshold(3);
        int threshold2 = getThreshold(6);
        int threshold3 = getThreshold(9);

        int call = 0;
        if (power >= threshold3) call = 3;
        else if (power >= threshold2) call = 2;
        else if (power >= threshold1) call = 1;

        // 不能超过当前最高分
        if (call <= currentHighestCall) call = 0;

        // 低难度有时乱叫
        if (difficulty <= 2 && random.nextDouble() < 0.3) {
            call = random.nextInt(4);
        }

        return call;
    }

    // ========== 自由出牌（首引） ==========

    private List<Card> chooseLeadCards(List<Card> hand, boolean isLandlord) {
        // 高难度：找最小有效牌型出
        // 低难度：随机出

        if (difficulty <= 2) {
            // 随机出单张或小对子
            if (random.nextDouble() < 0.5 && hand.size() > 1) {
                return Collections.singletonList(hand.get(hand.size() - 1)); // 最小单张
            }
            return Collections.singletonList(hand.get(hand.size() - 1));
        }

        // 找最小单张
        // 优先出掉小牌
        List<Card> singles = findAllSingles(hand);
        if (!singles.isEmpty()) {
            // 出最小的非关键牌
            Card minCard = singles.get(singles.size() - 1);
            // 高难度：留着王和2
            if (difficulty >= 7) {
                if (minCard.getValue() >= 15) {
                    // 找次小的
                    for (int i = singles.size() - 2; i >= 0; i--) {
                        if (singles.get(i).getValue() < 15) {
                            return Collections.singletonList(singles.get(i));
                        }
                    }
                }
            }
            return Collections.singletonList(minCard);
        }

        // 只剩炸弹等特殊牌型
        return Collections.singletonList(hand.get(hand.size() - 1));
    }

    // ========== 找压制牌 ==========

    private List<Card> findBeat(List<Card> hand, List<Card> lastCards, CardType lastType) {
        switch (lastType) {
            case SINGLE:
                return findBeatSingle(hand, lastCards);
            case PAIR:
                return findBeatPair(hand, lastCards);
            case TRIPLE:
                return findBeatTriple(hand, lastCards);
            case TRIPLE_WITH_SINGLE:
                return findBeatTripleWithSingle(hand, lastCards);
            case TRIPLE_WITH_PAIR:
                return findBeatTripleWithPair(hand, lastCards);
            case STRAIGHT:
                return findBeatStraight(hand, lastCards);
            case STRAIGHT_PAIR:
                return findBeatStraightPair(hand, lastCards);
            case PLANE:
            case PLANE_WITH_SINGLE:
            case PLANE_WITH_PAIR:
                return findBeatPlane(hand, lastCards, lastType);
            case FOUR_WITH_TWO:
                return findBeatFourWithTwo(hand, lastCards);
            case BOMB:
                return findBeatBomb(hand, lastCards);
            case ROCKET:
                return null; // 王炸无法被压
            default:
                return null;
        }
    }

    private List<Card> findBeatSingle(List<Card> hand, List<Card> last) {
        Card lastCard = last.get(0);
        // 找最小的能压过的牌
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            if (c.getValue() > lastCard.getValue() && c.getValue() <= 15) {
                // 高难度：不轻易出王
                if (difficulty >= 8 && c.getValue() >= 16 && hand.size() > 3) continue;
                return Collections.singletonList(c);
            }
        }
        // 找不到单张，看能不能炸
        if (difficulty >= 4) return tryUseBomb(hand, last);
        return null;
    }

    private List<Card> findBeatPair(List<Card> hand, List<Card> last) {
        // 统计对子
        Map<Integer, Integer> count = countMap(hand);
        int lastVal = last.get(0).getValue();
        List<Integer> candidates = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            if (e.getValue() >= 2 && e.getKey() > lastVal && e.getKey() <= 15) {
                candidates.add(e.getKey());
            }
        }
        Collections.sort(candidates);
        if (!candidates.isEmpty()) {
            int val = candidates.get(0);
            List<Card> result = new ArrayList<>();
            int found = 0;
            for (Card c : hand) {
                if (c.getValue() == val && found < 2) {
                    result.add(c);
                    found++;
                }
            }
            return result;
        }
        if (difficulty >= 4) return tryUseBomb(hand, last);
        return null;
    }

    private List<Card> findBeatTriple(List<Card> hand, List<Card> last) {
        Map<Integer, Integer> count = countMap(hand);
        int lastVal = last.get(0).getValue();
        List<Integer> candidates = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            if (e.getValue() >= 3 && e.getKey() > lastVal && e.getKey() <= 15) {
                candidates.add(e.getKey());
            }
        }
        Collections.sort(candidates);
        if (!candidates.isEmpty()) {
            int val = candidates.get(0);
            List<Card> result = new ArrayList<>();
            int found = 0;
            for (Card c : hand) {
                if (c.getValue() == val && found < 3) {
                    result.add(c);
                    found++;
                }
            }
            return result;
        }
        if (difficulty >= 5) return tryUseBomb(hand, last);
        return null;
    }

    private List<Card> findBeatTripleWithSingle(List<Card> hand, List<Card> last) {
        // 找三张的核心值
        Map<Integer, Integer> count = countMap(hand);
        // 从last中找三张的值
        Map<Integer, Integer> lastCount = countMap(last);
        int lastCore = 0;
        for (Map.Entry<Integer, Integer> e : lastCount.entrySet()) {
            if (e.getValue() == 3) lastCore = e.getKey();
        }

        List<Integer> tripleVals = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            if (e.getValue() >= 3 && e.getKey() > lastCore && e.getKey() <= 15) {
                tripleVals.add(e.getKey());
            }
        }
        Collections.sort(tripleVals);
        if (!tripleVals.isEmpty()) {
            int val = tripleVals.get(0);
            List<Card> result = new ArrayList<>();
            // 取三张
            int found = 0;
            for (Card c : hand) {
                if (c.getValue() == val && found < 3) {
                    result.add(c);
                    found++;
                }
            }
            // 取一个最小单张
            for (Card c : hand) {
                if (c.getValue() != val && c.getValue() < 15) {
                    result.add(c);
                    break;
                }
            }
            return result;
        }
        if (difficulty >= 5) return tryUseBomb(hand, last);
        return null;
    }

    private List<Card> findBeatTripleWithPair(List<Card> hand, List<Card> last) {
        Map<Integer, Integer> count = countMap(hand);
        Map<Integer, Integer> lastCount = countMap(last);
        int lastCore = 0;
        for (Map.Entry<Integer, Integer> e : lastCount.entrySet()) {
            if (e.getValue() == 3) lastCore = e.getKey();
        }

        List<Integer> tripleVals = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            if (e.getValue() >= 3 && e.getKey() > lastCore && e.getKey() <= 15) {
                tripleVals.add(e.getKey());
            }
        }
        Collections.sort(tripleVals);
        if (!tripleVals.isEmpty()) {
            int val = tripleVals.get(0);
            List<Card> result = new ArrayList<>();
            int found = 0;
            for (Card c : hand) {
                if (c.getValue() == val && found < 3) {
                    result.add(c);
                    found++;
                }
            }
            // 找一个对子
            for (Map.Entry<Integer, Integer> e : count.entrySet()) {
                if (e.getValue() >= 2 && e.getKey() != val) {
                    int pVal = e.getKey();
                    int pFound = 0;
                    for (Card c : hand) {
                        if (c.getValue() == pVal && pFound < 2) {
                            result.add(c);
                            pFound++;
                        }
                    }
                    break;
                }
            }
            return result;
        }
        if (difficulty >= 5) return tryUseBomb(hand, last);
        return null;
    }

    private List<Card> findBeatStraight(List<Card> hand, List<Card> last) {
        int len = last.size();
        // 找连续单牌
        List<Card> sorted = new ArrayList<>(hand);
        CardEngine.sortCards(sorted);
        List<List<Card>> straights = findStraights(sorted, len);

        // 找能压过last的顺子
        int lastMax = 0;
        for (Card c : last) if (c.getValue() > lastMax) lastMax = c.getValue();

        for (List<Card> st : straights) {
            if (st.get(0).getValue() > lastMax) {
                return st;
            }
        }
        if (difficulty >= 5) return tryUseBomb(hand, last);
        return null;
    }

    private List<Card> findBeatStraightPair(List<Card> hand, List<Card> last) {
        int pairs = last.size() / 2;
        Map<Integer, Integer> count = countMap(hand);
        List<Integer> pairVals = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            if (e.getValue() >= 2 && e.getKey() < 15) {
                pairVals.add(e.getKey());
            }
        }
        Collections.sort(pairVals);

        // 找连续pairs组
        int lastMax = 0;
        Map<Integer, Integer> lastCount = countMap(last);
        for (int v : pairVals) {
            if (lastCount.getOrDefault(v, 0) >= 2) lastMax = v;
        }

        // 简化：找任何连续pairs
        for (int i = 0; i <= pairVals.size() - pairs; i++) {
            boolean consecutive = true;
            for (int j = 1; j < pairs; j++) {
                if (pairVals.get(i + j) - pairVals.get(i + j - 1) != 1) {
                    consecutive = false;
                    break;
                }
            }
            if (consecutive && pairVals.get(i + pairs - 1) > lastMax) {
                List<Card> result = new ArrayList<>();
                for (int j = 0; j < pairs; j++) {
                    int v = pairVals.get(i + j);
                    int found = 0;
                    for (Card c : hand) {
                        if (c.getValue() == v && found < 2) {
                            result.add(c);
                            found++;
                        }
                    }
                }
                return result;
            }
        }
        if (difficulty >= 5) return tryUseBomb(hand, last);
        return null;
    }

    private List<Card> findBeatPlane(List<Card> hand, List<Card> last, CardType type) {
        Map<Integer, Integer> count = countMap(hand);
        List<Integer> tripleVals = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            if (e.getValue() >= 3 && e.getKey() < 15) {
                tripleVals.add(e.getKey());
            }
        }
        Collections.sort(tripleVals);

        Map<Integer, Integer> lastCount = countMap(last);
        int lastCore = 0;
        for (Map.Entry<Integer, Integer> e : lastCount.entrySet()) {
            if (e.getValue() == 3) lastCore = Math.max(lastCore, e.getKey());
        }

        // 找连续三张组
        for (int i = 0; i < tripleVals.size(); i++) {
            int groups = 1;
            for (int j = i + 1; j < tripleVals.size() && tripleVals.get(j) - tripleVals.get(j-1) == 1; j++) {
                groups++;
            }
            if (groups >= 2 && tripleVals.get(i + groups - 1) > lastCore) {
                // 取前groups组
                List<Card> result = new ArrayList<>();
                for (int g = 0; g < groups; g++) {
                    int v = tripleVals.get(i + g);
                    int found = 0;
                    for (Card c : hand) {
                        if (c.getValue() == v && found < 3) {
                            result.add(c);
                            found++;
                        }
                    }
                }
                // 飞机带牌
                if (type == CardType.PLANE_WITH_SINGLE) {
                    for (Card c : hand) {
                        if (result.size() >= groups * 4) break;
                        boolean inTriple = false;
                        for (int g = 0; g < groups; g++) {
                            if (c.getValue() == tripleVals.get(i + g)) inTriple = true;
                        }
                        if (!inTriple && c.getValue() < 15) {
                            result.add(c);
                        }
                    }
                } else if (type == CardType.PLANE_WITH_PAIR) {
                    // 找对子补充
                    for (Map.Entry<Integer, Integer> e : count.entrySet()) {
                        if (result.size() >= groups * 5) break;
                        if (e.getValue() >= 2) {
                            boolean inTriple = false;
                            for (int g = 0; g < groups; g++) {
                                if (e.getKey() == tripleVals.get(i + g)) inTriple = true;
                            }
                            if (!inTriple) {
                                int found = 0;
                                for (Card c : hand) {
                                    if (c.getValue() == e.getKey() && found < 2) {
                                        result.add(c);
                                        found++;
                                    }
                                }
                            }
                        }
                    }
                }
                return result;
            }
        }
        if (difficulty >= 6) return tryUseBomb(hand, last);
        return null;
    }

    private List<Card> findBeatFourWithTwo(List<Card> hand, List<Card> last) {
        Map<Integer, Integer> count = countMap(hand);
        Map<Integer, Integer> lastCount = countMap(last);
        int lastCore = 0;
        for (Map.Entry<Integer, Integer> e : lastCount.entrySet()) {
            if (e.getValue() == 4) lastCore = e.getKey();
        }

        List<Integer> fourVals = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            if (e.getValue() == 4 && e.getKey() > lastCore) {
                fourVals.add(e.getKey());
            }
        }
        Collections.sort(fourVals);
        if (!fourVals.isEmpty()) {
            int val = fourVals.get(0);
            List<Card> result = new ArrayList<>();
            int found = 0;
            for (Card c : hand) {
                if (c.getValue() == val && found < 4) {
                    result.add(c);
                    found++;
                }
            }
            // 补两张散牌
            for (Card c : hand) {
                if (result.size() >= 6) break;
                if (c.getValue() != val && c.getValue() < 15) {
                    result.add(c);
                }
            }
            return result;
        }
        if (difficulty >= 6) return tryUseBomb(hand, last);
        return null;
    }

    private List<Card> findBeatBomb(List<Card> hand, List<Card> last) {
        Map<Integer, Integer> count = countMap(hand);
        int lastVal = last.get(0).getValue();
        List<Integer> bombVals = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            if (e.getValue() == 4 && e.getKey() > lastVal) {
                bombVals.add(e.getKey());
            }
        }
        Collections.sort(bombVals);
        if (!bombVals.isEmpty()) {
            int val = bombVals.get(0);
            List<Card> result = new ArrayList<>();
            int found = 0;
            for (Card c : hand) {
                if (c.getValue() == val && found < 4) {
                    result.add(c);
                    found++;
                }
            }
            return result;
        }
        // 王炸压炸弹
        if (hasRocket(hand)) {
            List<Card> result = new ArrayList<>();
            for (Card c : hand) {
                if (c.getValue() == Card.JOKER_BIG || c.getValue() == Card.JOKER_SMALL) {
                    result.add(c);
                }
            }
            return result;
        }
        return null;
    }

    /** 尝试用炸弹 */
    private List<Card> tryUseBomb(List<Card> hand, List<Card> last) {
        // 高难度才舍得用炸弹
        if (difficulty < 4) return null;
        if (random.nextDouble() > difficulty * 0.1) return null; // 难度越高越敢炸

        Map<Integer, Integer> count = countMap(hand);
        List<Integer> bombs = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            if (e.getValue() == 4) bombs.add(e.getKey());
        }
        Collections.sort(bombs);
        if (!bombs.isEmpty()) {
            int val = bombs.get(0);
            List<Card> result = new ArrayList<>();
            int found = 0;
            for (Card c : hand) {
                if (c.getValue() == val && found < 4) {
                    result.add(c);
                    found++;
                }
            }
            return result;
        }
        // 王炸
        if (hasRocket(hand)) {
            List<Card> result = new ArrayList<>();
            for (Card c : hand) {
                if (c.getValue() == Card.JOKER_BIG || c.getValue() == Card.JOKER_SMALL) {
                    result.add(c);
                }
            }
            return result;
        }
        return null;
    }

    // ========== 智能判断：是否放过队友 ==========

    private boolean shouldPass(List<Card> beat, List<Card> lastCards, boolean isLandlord,
                               List<Integer> teammates, int lastPlayer) {
        // 队友出牌时不压队友
        if (!isLandlord && teammates.contains(lastPlayer)) {
            // 除非我有王炸且局势需要
            if (difficulty >= 8) {
                CardType beatType = CardEngine.recognizeType(beat);
                if (beatType == CardType.ROCKET) return false; // 王炸不浪费
            }
            return true;
        }
        return false;
    }

    // ========== 工具方法 ==========

    private Map<Integer, Integer> countMap(List<Card> cards) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Card c : cards) {
            map.put(c.getValue(), map.getOrDefault(c.getValue(), 0) + 1);
        }
        return map;
    }

    private int countValue(List<Card> hand, int value) {
        int c = 0;
        for (Card card : hand) {
            if (card.getValue() == value) c++;
        }
        return c;
    }

    private int countBombs(List<Card> hand) {
        Map<Integer, Integer> m = countMap(hand);
        int b = 0;
        for (int v : m.values()) if (v == 4) b++;
        return b;
    }

    private boolean hasRocket(List<Card> hand) {
        boolean big = false, small = false;
        for (Card c : hand) {
            if (c.getValue() == Card.JOKER_BIG) big = true;
            if (c.getValue() == Card.JOKER_SMALL) small = true;
        }
        return big && small;
    }

    private List<Card> findAllSingles(List<Card> hand) {
        List<Card> singles = new ArrayList<>();
        Integer lastVal = null;
        for (Card c : hand) {
            if (lastVal == null || lastVal != c.getValue()) {
                singles.add(c);
                lastVal = c.getValue();
            }
        }
        return singles;
    }

    private List<List<Card>> findStraights(List<Card> sortedDesc, int length) {
        // sortedDesc: 从大到小排序
        List<List<Card>> results = new ArrayList<>();
        List<Card> sorted = new ArrayList<>(sortedDesc);
        Collections.reverse(sorted); // 从小到大

        List<Integer> vals = new ArrayList<>();
        Integer last = null;
        for (Card c : sorted) {
            if (c.getValue() >= 15) continue; // 排除2和王
            if (last == null || c.getValue() != last) {
                vals.add(c.getValue());
                last = c.getValue();
            }
        }

        for (int i = 0; i <= vals.size() - length; i++) {
            boolean ok = true;
            for (int j = 1; j < length; j++) {
                if (vals.get(i + j) - vals.get(i + j - 1) != 1) { ok = false; break; }
            }
            if (ok) {
                List<Card> st = new ArrayList<>();
                for (int j = 0; j < length; j++) {
                    int v = vals.get(i + j);
                    for (Card c : sorted) {
                        if (c.getValue() == v && !st.contains(c)) {
                            st.add(c);
                            break;
                        }
                    }
                }
                results.add(st);
            }
        }
        return results;
    }

    private int getThreshold(int level) {
        // 不同叫分档位的牌力阈值
        switch (level) {
            case 1: return 3;
            case 2: return 5;
            case 3: return 8;
            default: return 12;
        }
    }

    /** 获取思考延迟（模拟真人） */
    public int getThinkDelay() {
        // 难度越高思考越快（更果断），但也不会秒出
        int base = 12 - difficulty; // 10~2秒
        if (base < 2) base = 2;
        int jitter = random.nextInt(4); // 0~3秒随机
        return (base + jitter) * 1000; // 转毫秒
    }
}
